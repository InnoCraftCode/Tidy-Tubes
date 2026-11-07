## One play of one level: the board, the move count and the undo history. Pure logic, no
## nodes: the game screen calls these and animates the result.
##
## Boosters act here:
## - undo(): reverts the last move exactly (and the move counter).
## - add_extra_tube(): appends one empty tube, keeping every ball where it is.
## - shuffle(): a new deal of the SAME balls (same colours, same counts), built by the level
##   generator from the solved position, so it is solvable by construction. Undo history is
##   cleared (there is nothing meaningful to undo into).
## Each returns false when it cannot apply, so the caller only spends a booster on success.
extends RefCounted

const Packed := preload("res://src/model/packed.gd")
const Rules := preload("res://src/logic/rules.gd")
const LevelGenerator := preload("res://src/logic/level_generator.gd")
const JavaRandom := preload("res://src/util/java_random.gd")

const MAX_EXTRA_TUBES := 1

var board: PackedInt32Array
var moves := 0
var extra_tubes := 0
var history: Array = [] # applied moves, oldest first


func _init(start: PackedInt32Array) -> void:
	board = start.duplicate()


## @return the move played, or an empty array when the tap pair is illegal (nothing changes)
func play(from: int, to: int) -> PackedInt32Array:
	var move := Rules.move_for(board, from, to)
	if move.is_empty():
		return move
	board = Rules.apply(board, move)
	history.append(move)
	moves += 1
	return move


func can_undo() -> bool:
	return not history.is_empty()


## @return the move that was undone (to animate it backwards), or empty when nothing to undo
func undo() -> PackedInt32Array:
	if history.is_empty():
		return PackedInt32Array()
	var move: PackedInt32Array = history.pop_back()
	board = Rules.revert(board, move)
	moves = maxi(0, moves - 1)
	return move


func can_add_extra_tube() -> bool:
	return extra_tubes < MAX_EXTRA_TUBES and not is_won()


func add_extra_tube() -> bool:
	if not can_add_extra_tube():
		return false
	board.append(Packed.EMPTY)
	extra_tubes += 1
	return true


func can_shuffle() -> bool:
	return not is_won()


## New solvable deal of the same balls. depth/seed come from the caller (seeded = testable).
func shuffle(seed_value: int, depth := 30) -> bool:
	if not can_shuffle():
		return false
	var counts := Rules.inventory(board)
	var colors := counts.size()
	var empty := board.size() - colors
	if empty < 1:
		return false
	var dealt := LevelGenerator.shuffle(Packed.solved_board(colors, empty), depth,
		JavaRandom.new(seed_value), 1.0)
	# The solved board uses colours 0..n-1: map them back onto the real colours in play.
	var palette := counts.keys()
	palette.sort()
	var mapped := PackedInt32Array()
	for tube in dealt:
		var remapped := Packed.EMPTY
		for i in Packed.size(tube):
			remapped = Packed.push(remapped, palette[Packed.slot(tube, i)], 1)
		mapped.append(remapped)
	board = mapped
	history.clear()
	return true


func is_won() -> bool:
	return Rules.is_won(board)


func is_stuck() -> bool:
	return not is_won() and not Rules.has_legal_move(board)
