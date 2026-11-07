## Move rules of Tidy Tubes, port of logic/MoveValidator.java + WinChecker.java.
##
## The player taps a source tube then a target tube (or drags). The whole top run of the
## source's colour moves, as many balls as fit. The target must be empty or show the same
## colour on top. A tube is finished when it holds CAPACITY balls of one colour; the level is
## won when every non-empty tube is finished.
## A move is PackedInt32Array(from, to, count, color); an empty array means "illegal".
extends RefCounted

const Packed := preload("res://src/model/packed.gd")


static func move_for(board: PackedInt32Array, from: int, to: int) -> PackedInt32Array:
	if from == to or from < 0 or to < 0 or from >= board.size() or to >= board.size():
		return PackedInt32Array()
	var count := Packed.transfer_count(board[from], board[to])
	if count == 0:
		return PackedInt32Array()
	return PackedInt32Array([from, to, count, Packed.top(board[from])])


static func can_move(board: PackedInt32Array, from: int, to: int) -> bool:
	return not move_for(board, from, to).is_empty()


## Returns a new board; the input is not modified.
static func apply(board: PackedInt32Array, move: PackedInt32Array) -> PackedInt32Array:
	var next := board.duplicate()
	next[move[0]] = Packed.pop(next[move[0]], move[2])
	next[move[1]] = Packed.push(next[move[1]], move[3], move[2])
	return next


## The exact inverse of apply(): puts the balls back where they came from.
static func revert(board: PackedInt32Array, move: PackedInt32Array) -> PackedInt32Array:
	var previous := board.duplicate()
	previous[move[1]] = Packed.pop(previous[move[1]], move[2])
	previous[move[0]] = Packed.push(previous[move[0]], move[3], move[2])
	return previous


static func legal_moves(board: PackedInt32Array) -> Array:
	var moves: Array = []
	for from in board.size():
		if board[from] == Packed.EMPTY:
			continue
		for to in board.size():
			if to != from:
				var move := move_for(board, from, to)
				if not move.is_empty():
					moves.append(move)
	return moves


static func has_legal_move(board: PackedInt32Array) -> bool:
	for from in board.size():
		if board[from] == Packed.EMPTY:
			continue
		for to in board.size():
			if to != from and Packed.transfer_count(board[from], board[to]) > 0:
				return true
	return false


## Never take balls out of a finished tube; never tip a single-coloured tube into an empty one.
static func is_useful(board: PackedInt32Array, from: int, to: int) -> bool:
	if Packed.transfer_count(board[from], board[to]) == 0:
		return false
	if Packed.is_complete(board[from]):
		return false
	return not (board[to] == Packed.EMPTY and Packed.is_uniform(board[from]))


static func is_won(board: PackedInt32Array) -> bool:
	for tube in board:
		if tube != Packed.EMPTY and not Packed.is_complete(tube):
			return false
	return true


## Every ball on the board as a count per colour (used to prove nothing is lost or duplicated).
static func inventory(board: PackedInt32Array) -> Dictionary:
	var counts := {}
	for tube in board:
		for i in Packed.size(tube):
			var color := Packed.slot(tube, i)
			counts[color] = counts.get(color, 0) + 1
	return counts
