## The difficulty curve as a pure function of the level index. Port of logic/Difficulty.java.
## The scramble ratio is computed in 32-bit float like Java, so rounding matches bit for bit.
extends RefCounted

const EASY_LAST_LEVEL := 10
const MEDIUM_LAST_LEVEL := 50
const MAX_COLORS := 10
const SCRAMBLE_FLOOR := 0.35
const TAIL_CYCLE := 10
const MOVE_LIMIT_BASE_SLACK := 6
const MOVE_LIMIT_SLACK_RATIO := 0.8


## Rounds a double to the nearest float32 (Java float arithmetic).
static func f32(x: float) -> float:
	var a := PackedFloat32Array([x])
	return a[0]


static func colors_for(level: int) -> int:
	assert(level >= 1)
	if level <= EASY_LAST_LEVEL:
		return mini(4, 3 + (level - 1) / 4)
	if level <= MEDIUM_LAST_LEVEL:
		return mini(7, 5 + (level - EASY_LAST_LEVEL - 1) / 14)
	return mini(MAX_COLORS, 8 + (level - MEDIUM_LAST_LEVEL - 1) / 25)


static func empty_tubes_for(level: int) -> int:
	return 2 if level <= MEDIUM_LAST_LEVEL else 1


static func shuffle_depth_for(level: int) -> int:
	return mini(200, 10 + 6 * colors_for(level) + mini(level, 120) / 2)


static func scramble_ratio_for(level: int) -> float:
	if level <= 4: return _ramp(level - 1, 4)
	if level <= EASY_LAST_LEVEL: return _ramp(level - 5, 6)
	if level <= 24: return _ramp(level - 11, 14)
	if level <= 38: return _ramp(level - 25, 14)
	if level <= MEDIUM_LAST_LEVEL: return _ramp(level - 39, 12)
	if level <= 75: return _ramp(level - 51, 25)
	if level <= 100: return _ramp(level - 76, 25)
	return _ramp((level - 101) % TAIL_CYCLE, TAIL_CYCLE)


static func _ramp(index: int, span: int) -> float:
	var position := 1.0 if span <= 1 else f32(float(index) / float(span - 1))
	var floor32 := f32(SCRAMBLE_FLOOR)
	return f32(floor32 + f32(f32(1.0 - floor32) * position))


## Java Math.round(float): floor(x + 0.5f) in float arithmetic.
static func round_f32(x: float) -> int:
	return int(floor(f32(x + 0.5)))


static func move_limit_for(par_moves: int) -> int:
	if par_moves <= 0:
		return 0
	var slack := maxi(MOVE_LIMIT_BASE_SLACK, round_f32(f32(par_moves * f32(MOVE_LIMIT_SLACK_RATIO))))
	return par_moves + slack
