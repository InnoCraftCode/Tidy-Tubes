## Builds levels by walking backwards from the solved position, in macro moves that go from
## one tidy board (every non-empty tube full) to another. Port of logic/LevelGenerator.java,
## bit for bit: same seed -> same board as the libGDX game, so level 37 stays level 37.
extends RefCounted

const Packed := preload("res://src/model/packed.gd")
const Solver := preload("res://src/logic/solver.gd")
const Difficulty := preload("res://src/logic/difficulty.gd")
const JavaRandom := preload("res://src/util/java_random.gd")

const MACRO_MAX_DEPTH := 10
const MACRO_NODE_BUDGET := 6000
const MAX_DEAD_ENDS := 250
const HISTORY_LIMIT := 48
const RANK_SAMPLE := 32
const RANK_NODE_BUDGET := 200000
const LEVEL_SEED_BASE := 0x5DEECE66D ^ -7046029254386353131 # 0x9E3779B97F4A7C15 as signed


## @return {"level", "tubes": PackedInt32Array, "colors", "empty", "depth"}
static func generate(level: int) -> Dictionary:
	return generate_with_seed(level, seed_for(level))


static func generate_with_seed(level: int, seed_value: int) -> Dictionary:
	var colors := Difficulty.colors_for(level)
	var empty := Difficulty.empty_tubes_for(level)
	var depth := Difficulty.shuffle_depth_for(level)
	var board := shuffle(Packed.solved_board(colors, empty), depth, JavaRandom.new(seed_value),
		Difficulty.scramble_ratio_for(level))
	return {"level": level, "tubes": board, "colors": colors, "empty": empty, "depth": depth}


## Java >>> on a 64-bit long.
static func _ushr(x: int, n: int) -> int:
	return (x >> n) & ((1 << (64 - n)) - 1)


## SplitMix64 mix of the level index (same constants as Java; ints wrap like longs).
static func seed_for(level: int) -> int:
	var z := LEVEL_SEED_BASE + level * -7046029254386353131
	z = (z ^ _ushr(z, 30)) * -4658895280553007687 # 0xBF58476D1CE4E5B9
	z = (z ^ _ushr(z, 27)) * -7723592293110705685 # 0x94D049BB133111EB
	return z ^ _ushr(z, 31)


static func shuffle(solved: PackedInt32Array, macro_moves: int, random, difficulty_ratio: float) -> PackedInt32Array:
	var current := solved.duplicate()
	var current_key := key_of(current)
	var previous_key := ""
	var history: Array = [current]
	var candidates: Array = []
	var dead_ends := 0
	for step in macro_moves:
		var next = _find_macro_move(current, current_key, previous_key, random)
		if next == null:
			dead_ends += 1
			if history.is_empty() or dead_ends > MAX_DEAD_ENDS:
				break
			current = history[random.next_int(history.size())]
			current_key = key_of(current)
			previous_key = ""
			continue
		previous_key = current_key
		current = next
		current_key = key_of(current)
		if history.size() < HISTORY_LIMIT:
			history.append(current)
		else:
			history[random.next_int(HISTORY_LIMIT)] = current
		if not _has_complete_tube(current):
			candidates.append(current)
	return _empty_tubes_last(_pick_by_difficulty(candidates, difficulty_ratio, current))


static func _pick_by_difficulty(candidates: Array, ratio: float, fallback: PackedInt32Array) -> PackedInt32Array:
	if candidates.is_empty():
		return fallback
	var pool := candidates
	if pool.size() > RANK_SAMPLE:
		var sampled: Array = []
		for i in RANK_SAMPLE:
			sampled.append(pool[i * (pool.size() - 1) / (RANK_SAMPLE - 1)])
		pool = sampled
	# Rank by solution length (then heuristic), ties by the Java key string: a total order.
	# Two candidates can be the same position with tubes in another order (equal score and key):
	# Java's List.sort is stable and keeps them in walk order, Godot's sort_custom is not, so the
	# walk index is the last tie-break.
	var ranked: Array = []
	for i in pool.size():
		var board: PackedInt32Array = pool[i]
		ranked.append({"score": _rank_score(board), "key": key_of(board), "index": i, "board": board})
	ranked.sort_custom(func(a, b):
		if a.score != b.score:
			return a.score < b.score
		if a.key != b.key:
			return a.key < b.key
		return a.index < b.index)
	var clamped := clampf(ratio, 0.0, 1.0)
	var index := Difficulty.round_f32(Difficulty.f32(clamped * float(ranked.size() - 1)))
	return ranked[index].board


static func _rank_score(board: PackedInt32Array) -> int:
	var solution := Solver.find_any(board, RANK_NODE_BUDGET)
	if solution.solved:
		return solution.moves.size() * 4 + Solver.heuristic(board)
	return 2147483647


static func _find_macro_move(start: PackedInt32Array, start_key: String, previous_key: String, random):
	for limit in range(1, MACRO_MAX_DEPTH + 1):
		var visited := {start_key: true}
		var budget := [MACRO_NODE_BUDGET]
		var found = _descend(start, limit, visited, budget, random, start_key, previous_key)
		if found != null:
			return found
	return null


static func _descend(state: PackedInt32Array, remaining: int, visited: Dictionary, budget: Array,
		random, start_key: String, previous_key: String):
	if remaining <= 0 or budget[0] <= 0:
		return null
	var moves := _reverse_moves(state)
	random.shuffle(moves)
	for move in moves:
		if budget[0] <= 0:
			return null
		budget[0] -= 1
		var child := _apply_reverse(state, move)
		var key := key_of(child)
		if visited.has(key):
			continue
		visited[key] = true
		if _is_tidy(child) and key != start_key and key != previous_key:
			return child
		var deeper = _descend(child, remaining - 1, visited, budget, random, start_key, previous_key)
		if deeper != null:
			return deeper
	return null


## Every legal reverse move as [source, amount, target], in Java's loop order.
static func _reverse_moves(tubes: PackedInt32Array) -> Array:
	var moves: Array = []
	for source in tubes.size():
		var source_encoded := tubes[source]
		if source_encoded == Packed.EMPTY:
			continue
		var run := Packed.top_run(source_encoded)
		var s := Packed.size(source_encoded)
		var color := Packed.top(source_encoded)
		var source_full := s == Packed.CAPACITY
		for amount in range(1, run + 1):
			if amount == run and s != run:
				continue
			for target in tubes.size():
				if target == source:
					continue
				var target_encoded := tubes[target]
				if Packed.free_space(target_encoded) < amount:
					continue
				var same_color_on_top := target_encoded != Packed.EMPTY and Packed.top(target_encoded) == color
				if same_color_on_top and not source_full:
					continue
				moves.append([source, amount, target])
	return moves


static func _apply_reverse(tubes: PackedInt32Array, move: Array) -> PackedInt32Array:
	var next := tubes.duplicate()
	var color := Packed.top(next[move[0]])
	next[move[0]] = Packed.pop(next[move[0]], move[1])
	next[move[2]] = Packed.push(next[move[2]], color, move[1])
	return next


static func _is_tidy(tubes: PackedInt32Array) -> bool:
	for tube in tubes:
		if tube != Packed.EMPTY and Packed.size(tube) != Packed.CAPACITY:
			return false
	return true


static func _has_complete_tube(tubes: PackedInt32Array) -> bool:
	for tube in tubes:
		if Packed.is_complete(tube):
			return true
	return false


static func _empty_tubes_last(tubes: PackedInt32Array) -> PackedInt32Array:
	var ordered := PackedInt32Array()
	for tube in tubes:
		if tube != Packed.EMPTY:
			ordered.append(tube)
	while ordered.size() < tubes.size():
		ordered.append(Packed.EMPTY)
	return ordered


## Java Arrays.toString(sorted copy): "[0, 4369, 8738]". Also the tie-break order of ranking.
static func key_of(tubes: PackedInt32Array) -> String:
	var sorted := tubes.duplicate()
	sorted.sort()
	var parts := PackedStringArray()
	for v in sorted:
		parts.append(str(v))
	return "[" + ", ".join(parts) + "]"
