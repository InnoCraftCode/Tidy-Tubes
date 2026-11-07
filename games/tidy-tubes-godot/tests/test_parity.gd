## Parity with the libGDX game: fixtures exported from the Java classes themselves
## (tools/ExportFixtures.java). Same random stream, same levels, same solver results.
extends RefCounted

const JavaRandom := preload("res://src/util/java_random.gd")
const LevelGenerator := preload("res://src/logic/level_generator.gd")
const Difficulty := preload("res://src/logic/difficulty.gd")
const Solver := preload("res://src/logic/solver.gd")

var args: PackedStringArray = []
var failures: Array = []
var _fixture: Dictionary = {}


func _data() -> Dictionary:
	if _fixture.is_empty():
		var text := FileAccess.get_file_as_string("res://tests/fixtures/levels.json")
		_fixture = JSON.parse_string(text)
	return _fixture


func _expect(condition: bool, message: String) -> void:
	if not condition:
		failures.append(message)


func _level_range() -> Vector2i:
	for a in args:
		if a.begins_with("levels="):
			var parts := a.substr(7).split("-")
			return Vector2i(int(parts[0]), int(parts[1]))
	return Vector2i(1, 30)


func test_java_random_stream_matches() -> void:
	for stream in _data().random:
		var random := JavaRandom.new(int(stream.seed))
		for pair in stream.ints:
			var got := random.next_int(int(pair[0]))
			_expect(got == int(pair[1]), "seed %s bound %d: got %d want %d" % [stream.seed, pair[0], got, pair[1]])


func test_seeds_and_difficulty_match() -> void:
	for level in _data().levels:
		var n := int(level.level)
		_expect(str(LevelGenerator.seed_for(n)) == level.seed, "seed L%d: %d vs %s" % [n, LevelGenerator.seed_for(n), level.seed])
		_expect(Difficulty.colors_for(n) == int(level.colors), "colors L%d" % n)
		_expect(Difficulty.empty_tubes_for(n) == int(level.empty), "empty L%d" % n)
		_expect(Difficulty.shuffle_depth_for(n) == int(level.depth), "depth L%d" % n)
		_expect(is_equal_approx(Difficulty.scramble_ratio_for(n), float(level.ratio)), "ratio L%d" % n)


func test_levels_are_identical_to_java() -> void:
	var r := _level_range()
	var total_ms := 0
	var worst := 0
	var worst_level := 0
	for level in _data().levels:
		var n := int(level.level)
		if n < r.x or n > r.y:
			continue
		var started := Time.get_ticks_msec()
		var generated := LevelGenerator.generate(n)
		var ms := Time.get_ticks_msec() - started
		total_ms += ms
		if ms > worst:
			worst = ms
			worst_level = n
		var want := PackedInt32Array(level.tubes)
		_expect(generated.tubes == want, "L%d board %s vs java %s" % [n, generated.tubes, want])
	print("       generation: %d ms total, worst L%d %d ms (JVM was ~4 ms/level)" % [total_ms, worst_level, worst])


func test_solver_matches_java() -> void:
	var r := _level_range()
	for level in _data().levels:
		var n := int(level.level)
		if n < r.x or n > r.y:
			continue
		var result := Solver.find_any(PackedInt32Array(level.tubes), 200000)
		_expect(result.moves.size() == int(level.anyMoves), "L%d moves %d vs %d" % [n, result.moves.size(), level.anyMoves])
		_expect(result.nodes == int(level.anyNodes), "L%d nodes %d vs %d" % [n, result.nodes, level.anyNodes])
