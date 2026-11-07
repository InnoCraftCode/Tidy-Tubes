## Original ball-sort rules and the boosters, on real generated levels.
extends RefCounted

const Packed := preload("res://src/model/packed.gd")
const Rules := preload("res://src/logic/rules.gd")
const BoardSession := preload("res://src/logic/board_session.gd")
const LevelGenerator := preload("res://src/logic/level_generator.gd")
const LevelRating := preload("res://src/logic/level_rating.gd")
const HintEngine := preload("res://src/logic/hint_engine.gd")
const Solver := preload("res://src/logic/solver.gd")

var args: PackedStringArray = []
var failures: Array = []


func _expect(condition: bool, message: String) -> void:
	if not condition:
		failures.append(message)


## tube helper: bottom first, e.g. _t([0, 0, 1]) = two of colour 0 under one of colour 1
static func _t(colors: Array) -> int:
	var encoded := Packed.EMPTY
	for c in colors:
		encoded = Packed.push(encoded, c, 1)
	return encoded


func test_moves_whole_top_run_that_fits() -> void:
	var board := PackedInt32Array([_t([0, 1, 1]), _t([2, 2, 1]), Packed.EMPTY])
	var move := Rules.move_for(board, 0, 1)
	_expect(move == PackedInt32Array([0, 1, 1, 1]), "only one slot free on target: move 1, got %s" % move)
	var to_empty := Rules.move_for(board, 0, 2)
	_expect(to_empty[2] == 2, "the whole run of 2 goes to an empty tube, got %s" % to_empty)


func test_illegal_moves_are_rejected() -> void:
	var board := PackedInt32Array([_t([0, 1]), _t([2]), _t([3, 3, 3, 3]), Packed.EMPTY])
	_expect(Rules.move_for(board, 0, 1).is_empty(), "colour 1 onto colour 2 must be refused")
	_expect(Rules.move_for(board, 0, 2).is_empty(), "a full tube accepts nothing")
	_expect(Rules.move_for(board, 3, 0).is_empty(), "an empty tube has nothing to give")
	_expect(Rules.move_for(board, 0, 0).is_empty(), "same tube is not a move")
	_expect(Rules.move_for(board, 0, 9).is_empty(), "out of range is not a move")


func test_capacity_is_never_exceeded() -> void:
	var session := BoardSession.new(LevelGenerator.generate(12).tubes)
	for i in 40:
		var moves := Rules.legal_moves(session.board)
		if moves.is_empty():
			break
		var m: PackedInt32Array = moves[(i * 7) % moves.size()]
		session.play(m[0], m[1])
		for tube in session.board:
			_expect(Packed.size(tube) <= Packed.CAPACITY, "tube over capacity")


func test_win_detection() -> void:
	_expect(Rules.is_won(Packed.solved_board(4, 2)), "solved board is won")
	_expect(not Rules.is_won(LevelGenerator.generate(3).tubes), "a fresh level is not won")


func test_generated_levels_are_solvable_and_complete() -> void:
	for level in range(1, 31):
		var board: PackedInt32Array = LevelGenerator.generate(level).tubes
		var inv := Rules.inventory(board)
		for color in inv:
			_expect(inv[color] == Packed.CAPACITY, "L%d colour %d has %d balls" % [level, color, inv[color]])
		_expect(Solver.find_any(board, 200000).solved, "L%d not solvable" % level)


func test_solving_with_hints_wins_the_level() -> void:
	var session := BoardSession.new(LevelGenerator.generate(6).tubes)
	var guard := 0
	while not session.is_won() and guard < 60:
		var hint := HintEngine.advise(session.board)
		_expect(hint.kind == HintEngine.Kind.MOVE, "hint should give a move")
		if hint.kind != HintEngine.Kind.MOVE:
			return
		_expect(not session.play(hint.move[0], hint.move[1]).is_empty(), "hint move must be legal")
		guard += 1
	_expect(session.is_won(), "following hints must win")


func test_undo_restores_the_exact_previous_board() -> void:
	var start: PackedInt32Array = LevelGenerator.generate(20).tubes
	var session := BoardSession.new(start)
	var snapshots: Array = [session.board.duplicate()]
	for i in 8:
		var moves := Rules.legal_moves(session.board)
		var m: PackedInt32Array = moves[i % moves.size()]
		session.play(m[0], m[1])
		snapshots.append(session.board.duplicate())
	for i in range(8, 0, -1):
		_expect(not session.undo().is_empty(), "undo %d should apply" % i)
		_expect(session.board == snapshots[i - 1], "undo %d must restore the board exactly" % i)
	_expect(session.board == start and session.moves == 0, "back to the start, 0 moves")
	_expect(session.undo().is_empty(), "nothing left to undo")


func test_extra_tube_keeps_every_ball() -> void:
	var session := BoardSession.new(LevelGenerator.generate(60).tubes)
	var before := Rules.inventory(session.board)
	var tubes := session.board.size()
	_expect(session.add_extra_tube(), "first extra tube applies")
	_expect(session.board.size() == tubes + 1 and session.board[tubes] == Packed.EMPTY, "one empty tube appended")
	_expect(Rules.inventory(session.board) == before, "balls unchanged")
	_expect(not session.add_extra_tube(), "only one extra tube per level")


func test_shuffle_keeps_the_same_balls_and_is_solvable() -> void:
	for level in [5, 30, 80]:
		var session := BoardSession.new(LevelGenerator.generate(level).tubes)
		session.play(Rules.legal_moves(session.board)[0][0], Rules.legal_moves(session.board)[0][1])
		var before := Rules.inventory(session.board)
		var size := session.board.size()
		_expect(session.shuffle(1234 + level), "L%d shuffle applies" % level)
		_expect(Rules.inventory(session.board) == before, "L%d shuffle lost or duplicated balls" % level)
		_expect(session.board.size() == size, "L%d tube count changed" % level)
		_expect(Solver.find_any(session.board, 400000).solved, "L%d shuffled board not solvable" % level)
		_expect(not session.can_undo(), "history cleared after shuffle")


func test_star_rating_matches_java() -> void:
	_expect(LevelRating.stars_for(10, 10) == 3, "par = 3 stars")
	_expect(LevelRating.stars_for(12, 10) == 2, "par+2 = 2 stars")
	_expect(LevelRating.stars_for(13, 10) == 2, "par+3 = 2 stars: Java Math.round(2.5f) = 3")
	_expect(LevelRating.stars_for(14, 10) == 1, "par+4 = 1 star")
	_expect(LevelRating.stars_for(25, 20) == 2, "allowance is 25%% of par")
	_expect(LevelRating.stars_for(9, 0) == 1, "unknown par = 1 star")
