## Progress rules on the save data: wins, stars, coins, boosters. Pure functions on the
## dictionary from save_data.gd; the caller persists afterwards.
##
## Stars (ADR-003 decision 2026-11-07): the best rating per level is kept; a replay only adds
## the improvement, to both the spendable balance (renovation) and the lifetime total.
## Coins follow the libGDX rules: 25 per win, +15 at par, replays pay a third (minimum 5).
extends RefCounted

const LevelRating := preload("res://src/logic/level_rating.gd")

const COINS_PER_WIN := 25
const COINS_PAR_BONUS := 15
const REPLAY_MIN_COINS := 5


static func _grow(list: Array, size: int) -> void:
	while list.size() < size:
		list.append(0)


## @return {"stars": this run, "stars_gained": added to balance, "coins": earned,
##          "first_clear": bool, "new_record": bool, "perfect": bool}
static func record_win(data: Dictionary, level: int, moves: int, par_moves: int) -> Dictionary:
	assert(level >= 1 and moves >= 1)
	var progress: Dictionary = data.progress
	var best_moves: Array = progress.best_moves
	var best_stars: Array = progress.best_stars
	_grow(best_moves, level)
	_grow(best_stars, level)
	var i := level - 1
	var first_clear: bool = int(best_moves[i]) == 0
	var new_record := not first_clear and moves < int(best_moves[i])
	if first_clear or new_record:
		best_moves[i] = moves
	var stars := LevelRating.stars_for(moves, par_moves)
	var gained := maxi(0, stars - int(best_stars[i]))
	best_stars[i] = maxi(int(best_stars[i]), stars)
	data.wallet.stars_balance += gained
	data.wallet.stars_earned += gained

	progress.highest_unlocked = maxi(int(progress.highest_unlocked), level + 1)
	progress.current_level = mini(level + 1, int(progress.highest_unlocked))

	var perfect := par_moves > 0 and moves <= par_moves
	var coins := COINS_PER_WIN + (COINS_PAR_BONUS if perfect else 0)
	if not first_clear:
		coins = maxi(REPLAY_MIN_COINS, coins / 3)
	data.wallet.coins += coins
	return {"stars": stars, "stars_gained": gained, "coins": coins, "first_clear": first_clear,
		"new_record": new_record, "perfect": perfect}


static func best_stars_for(data: Dictionary, level: int) -> int:
	var list: Array = data.progress.best_stars
	return int(list[level - 1]) if level >= 1 and level <= list.size() else 0


static func is_unlocked(data: Dictionary, level: int) -> bool:
	return level >= 1 and level <= int(data.progress.highest_unlocked)


## Spends coins; refuses (and changes nothing) when the balance is short.
static func spend_coins(data: Dictionary, amount: int) -> bool:
	if amount < 0 or int(data.wallet.coins) < amount:
		return false
	data.wallet.coins -= amount
	return true


static func spend_stars(data: Dictionary, amount: int) -> bool:
	if amount < 0 or int(data.wallet.stars_balance) < amount:
		return false
	data.wallet.stars_balance -= amount
	return true


static func booster_count(data: Dictionary, booster: String) -> int:
	return int(data.boosters.get(booster, 0))


## Call only AFTER the booster really applied (BoardSession returned true).
static func consume_booster(data: Dictionary, booster: String) -> bool:
	if booster_count(data, booster) <= 0:
		return false
	data.boosters[booster] = booster_count(data, booster) - 1
	return true


static func add_booster(data: Dictionary, booster: String, amount: int) -> void:
	data.boosters[booster] = booster_count(data, booster) + maxi(0, amount)
