## Lives for challenge levels only (the campaign stays unlimited). Pure functions on
## data.lives = {"count", "regen_from"}; time is always passed in (unix seconds), so they are
## testable and survive app restarts: regeneration is computed from the stored timestamp.
extends RefCounted

const MAX_LIVES := 5
const REGEN_SECONDS := 20 * 60


## Applies the lives regenerated since regen_from. Call before reading or spending.
static func refresh(data: Dictionary, now: int) -> void:
	var lives: Dictionary = data.lives
	if int(lives.count) >= MAX_LIVES:
		lives.count = MAX_LIVES
		lives.regen_from = now
		return
	var from := int(lives.regen_from)
	if from <= 0 or from > now: # never started, or the clock went backwards
		lives.regen_from = now
		return
	var gained := (now - from) / REGEN_SECONDS
	if gained <= 0:
		return
	lives.count = mini(MAX_LIVES, int(lives.count) + gained)
	lives.regen_from = now if int(lives.count) >= MAX_LIVES else from + gained * REGEN_SECONDS


static func count(data: Dictionary, now: int) -> int:
	refresh(data, now)
	return int(data.lives.count)


## @return seconds until the next life, 0 when full
static func seconds_to_next(data: Dictionary, now: int) -> int:
	refresh(data, now)
	if int(data.lives.count) >= MAX_LIVES:
		return 0
	return maxi(0, int(data.lives.regen_from) + REGEN_SECONDS - now)


## A failed challenge costs one life. @return false when there was none to lose
static func lose(data: Dictionary, now: int) -> bool:
	refresh(data, now)
	if int(data.lives.count) <= 0:
		return false
	var was_full := int(data.lives.count) >= MAX_LIVES
	data.lives.count = int(data.lives.count) - 1
	if was_full:
		data.lives.regen_from = now # the charge starts from the first life lost
	return true


static func can_play_challenge(data: Dictionary, now: int) -> bool:
	return count(data, now) > 0


## Recovery (rewarded video or coins): back to full.
static func refill(data: Dictionary, now: int) -> void:
	data.lives.count = MAX_LIVES
	data.lives.regen_from = now
