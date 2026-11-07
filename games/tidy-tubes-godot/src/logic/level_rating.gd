## Stars for a win, port of logic/LevelRating.java: 3 at par or better, 2 for a close solve,
## 1 for any other win. Unknown par (<= 0) still gives 1 star.
extends RefCounted

const Difficulty := preload("res://src/logic/difficulty.gd")


static func stars_for(moves: int, par_moves: int) -> int:
	assert(moves >= 1, "moves must be positive")
	if par_moves <= 0:
		return 1
	if moves <= par_moves:
		return 3
	var allowance := maxi(2, Difficulty.round_f32(Difficulty.f32(par_moves * Difficulty.f32(0.25))))
	return 2 if moves <= par_moves + allowance else 1
