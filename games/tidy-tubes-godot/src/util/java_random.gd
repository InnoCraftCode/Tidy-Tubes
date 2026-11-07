## Bit-exact port of java.util.Random (48-bit LCG), so seeded levels match the libGDX game.
##
## GDScript ints are 64-bit and wrap on overflow; every step masks to 48 bits, so the low bits
## of a wrapped product are the same as Java's.
extends RefCounted

const MULTIPLIER := 0x5DEECE66D
const ADDEND := 0xB
const MASK := (1 << 48) - 1
const INT_MAX := 2147483647

var _seed: int


func _init(seed_value: int) -> void:
	_seed = (seed_value ^ MULTIPLIER) & MASK


## Java: protected int next(int bits)
func next_bits(bits: int) -> int:
	_seed = (_seed * MULTIPLIER + ADDEND) & MASK
	var value := _seed >> (48 - bits) # _seed is non-negative: >> equals >>>
	if bits == 32 and value > INT_MAX:
		value -= 1 << 32 # Java's (int) cast
	return value


## Java: public int nextInt(int bound), legacy algorithm (unchanged since JDK 1.2).
func next_int(bound: int) -> int:
	assert(bound > 0)
	var r := next_bits(31)
	var m := bound - 1
	if (bound & m) == 0:
		return (bound * r) >> 31
	var u := r
	r = u % bound
	# Java loops while (u - r + m) overflows an int.
	while u - r + m > INT_MAX:
		u = next_bits(31)
		r = u % bound
	return r


## Java: Collections.shuffle(list, random) for a RandomAccess list.
func shuffle(list: Array) -> void:
	var i := list.size()
	while i > 1:
		var j := next_int(i)
		var tmp = list[i - 1]
		list[i - 1] = list[j]
		list[j] = tmp
		i -= 1
