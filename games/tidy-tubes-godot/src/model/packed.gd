## A tube packed into one int: four 4-bit slots, slot 0 (bits 0-3) is the bottom.
## A slot holds colour + 1, so 0 means empty. Port of model/Packed.java.
extends RefCounted

const CAPACITY := 4
const EMPTY := 0
const NO_COLOR := -1


static func size(encoded: int) -> int:
	var count := 0
	while count < CAPACITY and ((encoded >> (count << 2)) & 0xF) != 0:
		count += 1
	return count


static func slot(encoded: int, index: int) -> int:
	return ((encoded >> (index << 2)) & 0xF) - 1


static func top(encoded: int) -> int:
	var s := size(encoded)
	return NO_COLOR if s == 0 else slot(encoded, s - 1)


static func top_run(encoded: int) -> int:
	var s := size(encoded)
	if s == 0:
		return 0
	var color := slot(encoded, s - 1)
	var run := 1
	var i := s - 2
	while i >= 0 and slot(encoded, i) == color:
		run += 1
		i -= 1
	return run


static func free_space(encoded: int) -> int:
	return CAPACITY - size(encoded)


static func is_complete(encoded: int) -> bool:
	return size(encoded) == CAPACITY and top_run(encoded) == CAPACITY


static func is_uniform(encoded: int) -> bool:
	var s := size(encoded)
	return s <= 1 or top_run(encoded) == s


static func push(encoded: int, color: int, count: int) -> int:
	var s := size(encoded)
	assert(count >= 0 and s + count <= CAPACITY, "cannot push %d onto %d" % [count, s])
	var result := encoded
	for i in count:
		result |= (color + 1) << ((s + i) << 2)
	return result


static func pop(encoded: int, count: int) -> int:
	var s := size(encoded)
	assert(count >= 0 and count <= s, "cannot pop %d from %d" % [count, s])
	var result := encoded
	for i in range(1, count + 1):
		result &= ~(0xF << ((s - i) << 2))
	return result


## Java MoveValidator.transferCount: balls a pour from -> to would move (0 = illegal).
static func transfer_count(from_encoded: int, to_encoded: int) -> int:
	if from_encoded == EMPTY:
		return 0
	var free := free_space(to_encoded)
	if free == 0:
		return 0
	var color := top(from_encoded)
	if to_encoded != EMPTY and top(to_encoded) != color:
		return 0
	return mini(top_run(from_encoded), free)


static func solved_board(colors: int, empty_tubes: int) -> PackedInt32Array:
	var board := PackedInt32Array()
	board.resize(colors + empty_tubes)
	for i in colors:
		board[i] = push(EMPTY, i, CAPACITY)
	return board
