## Ball-sort search, port of logic/Solver.java (same move order, same budgets, same results).
##
## find_any: depth-first graph search with move ordering and symmetry reduction.
## find_optimal: IDA* with an admissible heuristic (shortest solution, used for par and hints).
## Boards are PackedInt32Array of packed tubes (see packed.gd). Positions are memoised under an
## order-independent key: tubes are interchangeable.
extends RefCounted

const Packed := preload("res://src/model/packed.gd")

const MAX_DEPTH := 20000
const NO_MOVE := -1
const FOUND := -1
const BUDGET := -2
const INFINITY := 2147483647


## Result: {"solved": bool, "moves": Array[PackedInt32Array(from,to,count,color)], "nodes": int, "exhausted": bool}
static func _result(moves, nodes: int, exhausted: bool) -> Dictionary:
	return {"solved": moves != null, "moves": moves if moves != null else [], "nodes": nodes, "exhausted": exhausted}


static func key_of(tubes: PackedInt32Array) -> PackedInt32Array:
	var sorted := tubes.duplicate()
	sorted.sort()
	return sorted


static func find_any(root: PackedInt32Array, node_budget: int) -> Dictionary:
	if is_solved(root):
		return _result([], 0, false)
	var visited := {}
	visited[key_of(root)] = true
	# Stack frames as parallel arrays: state, moves, arrived_by, next index.
	var states: Array = [root]
	var move_lists: Array = [ordered_moves(root)]
	var arrived: PackedInt32Array = [NO_MOVE]
	var nexts: PackedInt32Array = [0]
	var nodes := 0
	while not states.is_empty():
		if nodes >= node_budget:
			return _result(null, nodes, true)
		var top := states.size() - 1
		var moves: PackedInt32Array = move_lists[top]
		if nexts[top] >= moves.size() or states.size() >= MAX_DEPTH:
			states.pop_back()
			move_lists.pop_back()
			arrived.remove_at(top)
			nexts.remove_at(top)
			continue
		var packed_move := moves[nexts[top]]
		nexts[top] += 1
		var child := apply_packed(states[top], packed_move)
		nodes += 1
		var key := key_of(child)
		if visited.has(key):
			continue
		visited[key] = true
		if is_solved(child):
			var path: Array = []
			for i in arrived.size():
				if arrived[i] != NO_MOVE:
					path.append(unpack(arrived[i]))
			path.append(unpack(packed_move))
			return _result(path, nodes, false)
		states.append(child)
		move_lists.append(ordered_moves(child))
		arrived.append(packed_move)
		nexts.append(0)
	return _result(null, nodes, false)


static func find_optimal(root: PackedInt32Array, node_budget: int) -> Dictionary:
	if is_solved(root):
		return _result([], 0, false)
	var threshold := heuristic(root)
	var ctx := {"nodes": 0, "budget": node_budget, "path": PackedInt32Array(), "length": 0}
	ctx.path.resize(MAX_DEPTH)
	while threshold < MAX_DEPTH:
		var seen := {}
		var next := _search(root, 0, threshold, seen, ctx)
		if next == FOUND:
			var moves: Array = []
			for i in ctx.length:
				moves.append(unpack(ctx.path[i]))
			return _result(moves, ctx.nodes, false)
		if next == BUDGET:
			return _result(null, ctx.nodes, true)
		if next == INFINITY:
			return _result(null, ctx.nodes, false)
		threshold = maxi(next, threshold + 1)
	return _result(null, ctx.nodes, true)


static func _search(state: PackedInt32Array, depth: int, threshold: int, seen: Dictionary, ctx: Dictionary) -> int:
	var f := depth + heuristic(state)
	if f > threshold:
		return f
	if is_solved(state):
		ctx.length = depth
		return FOUND
	if ctx.nodes >= ctx.budget:
		return BUDGET
	if depth + 1 >= MAX_DEPTH:
		return INFINITY
	var minimum := INFINITY
	var cut_by_table := false
	for packed_move in ordered_moves(state):
		var child := apply_packed(state, packed_move)
		ctx.nodes += 1
		var key := key_of(child)
		var best = seen.get(key)
		if best != null and best <= depth + 1:
			cut_by_table = true
			continue
		seen[key] = depth + 1
		ctx.path[depth] = packed_move
		var result := _search(child, depth + 1, threshold, seen, ctx)
		if result == FOUND or result == BUDGET:
			return result
		if result < minimum:
			minimum = result
	if cut_by_table and minimum > threshold + 1:
		minimum = threshold + 1
	return minimum


## Admissible lower bound: per colour, the tubes it is spread across minus one.
static func heuristic(tubes: PackedInt32Array) -> int:
	var per_color := PackedInt32Array()
	per_color.resize(16)
	for tube in tubes:
		var s := Packed.size(tube)
		var seen_mask := 0
		for i in s:
			var bit := 1 << (((tube >> (i << 2)) & 0xF) - 1)
			if (seen_mask & bit) == 0:
				seen_mask |= bit
				per_color[((tube >> (i << 2)) & 0xF) - 1] += 1
	var estimate := 0
	for count in per_color:
		if count > 1:
			estimate += count - 1
	return estimate


static func pack(from: int, to: int, count: int, color: int) -> int:
	return (from & 0xFF) | ((to & 0xFF) << 8) | ((count & 0xFF) << 16) | ((color & 0xFF) << 24)


## @return PackedInt32Array(from, to, count, color)
static func unpack(packed_move: int) -> PackedInt32Array:
	return PackedInt32Array([packed_move & 0xFF, (packed_move >> 8) & 0xFF, (packed_move >> 16) & 0xFF, (packed_move >> 24) & 0xFF])


static func apply_packed(state: PackedInt32Array, packed_move: int) -> PackedInt32Array:
	var from := packed_move & 0xFF
	var to := (packed_move >> 8) & 0xFF
	var count := (packed_move >> 16) & 0xFF
	var color := (packed_move >> 24) & 0xFF
	var next := state.duplicate()
	next[from] = Packed.pop(next[from], count)
	next[to] = Packed.push(next[to], color, count)
	return next


## Every move worth trying, best first (finish a tube, free a tube, merge, then empty tubes).
static func ordered_moves(tubes: PackedInt32Array) -> PackedInt32Array:
	var count := tubes.size()
	var moves := PackedInt32Array()
	var scores := PackedInt32Array()
	var seen_sources := PackedInt32Array()
	for from in count:
		var from_encoded := tubes[from]
		if from_encoded == Packed.EMPTY or Packed.is_complete(from_encoded):
			continue
		if seen_sources.has(from_encoded):
			continue
		seen_sources.append(from_encoded)
		var run := Packed.top_run(from_encoded)
		var s := Packed.size(from_encoded)
		var color := Packed.top(from_encoded)
		var source_uniform := run == s
		var seen_targets := PackedInt32Array()
		for to in count:
			if to == from:
				continue
			var to_encoded := tubes[to]
			var moved := Packed.transfer_count(from_encoded, to_encoded)
			if moved == 0:
				continue
			var target_empty := to_encoded == Packed.EMPTY
			if target_empty and source_uniform:
				continue
			if seen_targets.has(to_encoded):
				continue
			seen_targets.append(to_encoded)
			var score := moved
			if Packed.is_complete(Packed.push(to_encoded, color, moved)):
				score += 1000
			if moved == s:
				score += 500
			score += 300 if not target_empty else -200
			# Stable insertion by descending score (same order as Java's insertion sort).
			var j := scores.size()
			while j > 0 and scores[j - 1] < score:
				j -= 1
			moves.insert(j, pack(from, to, moved, color))
			scores.insert(j, score)
	return moves


static func is_solved(tubes: PackedInt32Array) -> bool:
	for tube in tubes:
		if tube != Packed.EMPTY and not Packed.is_complete(tube):
			return false
	return true
