## Stars, coins, boosters, lives, renovation and the save file.
extends RefCounted

const SaveData := preload("res://src/save/save_data.gd")
const SaveStore := preload("res://src/save/save_store.gd")
const Progression := preload("res://src/logic/progression.gd")
const Lives := preload("res://src/logic/lives.gd")
const Renovation := preload("res://src/logic/renovation.gd")

const TEST_DIR := "user://test_save"

var args: PackedStringArray = []
var failures: Array = []


func _expect(condition: bool, message: String) -> void:
	if not condition:
		failures.append(message)


func test_replays_only_add_the_star_improvement() -> void:
	var data := SaveData.defaults()
	var first := Progression.record_win(data, 1, 15, 10) # 1 star
	_expect(first.stars_gained == 1 and data.wallet.stars_balance == 1, "first clear: +1")
	var better := Progression.record_win(data, 1, 10, 10) # 3 stars
	_expect(better.stars_gained == 2 and data.wallet.stars_balance == 3, "improve to 3: only +2")
	for i in 5:
		Progression.record_win(data, 1, 10, 10)
	_expect(data.wallet.stars_balance == 3 and data.wallet.stars_earned == 3, "same rating replays add nothing")
	var worse := Progression.record_win(data, 1, 30, 10)
	_expect(worse.stars_gained == 0 and Progression.best_stars_for(data, 1) == 3, "worse replay keeps the best")


func test_coins_follow_the_libgdx_rules() -> void:
	var data := SaveData.defaults()
	_expect(Progression.record_win(data, 2, 12, 10).coins == 25, "first clear: 25")
	_expect(Progression.record_win(data, 3, 10, 10).coins == 40, "first clear at par: 25 + 15")
	_expect(Progression.record_win(data, 3, 10, 10).coins == 13, "replay at par: 40 / 3")
	_expect(Progression.record_win(data, 2, 14, 10).coins == 8, "replay: 25 / 3")
	_expect(data.wallet.coins == 25 + 40 + 13 + 8, "balance adds up")


func test_levels_unlock_in_order() -> void:
	var data := SaveData.defaults()
	_expect(Progression.is_unlocked(data, 1) and not Progression.is_unlocked(data, 2), "only level 1 at start")
	Progression.record_win(data, 1, 10, 10)
	_expect(Progression.is_unlocked(data, 2) and not Progression.is_unlocked(data, 3), "winning 1 unlocks 2")


func test_balances_never_go_negative() -> void:
	var data := SaveData.defaults()
	_expect(not Progression.spend_coins(data, 1), "cannot spend coins you do not have")
	_expect(not Progression.spend_stars(data, 1), "cannot spend stars you do not have")
	_expect(not Progression.spend_coins(data, -5), "negative amounts refused")
	_expect(data.wallet.coins == 0 and data.wallet.stars_balance == 0, "nothing changed")


func test_boosters_are_consumed_only_when_available() -> void:
	var data := SaveData.defaults()
	data.boosters.hint = 1
	_expect(Progression.consume_booster(data, "hint"), "one hint to use")
	_expect(not Progression.consume_booster(data, "hint"), "none left")
	_expect(data.boosters.hint == 0, "never below zero")


func test_lives_regenerate_from_the_saved_timestamp() -> void:
	var data := SaveData.defaults()
	var t0 := 1_800_000_000
	_expect(Lives.count(data, t0) == 5, "full at start")
	Lives.lose(data, t0)
	Lives.lose(data, t0 + 60)
	_expect(Lives.count(data, t0 + 60) == 3, "two lost")
	_expect(Lives.seconds_to_next(data, t0 + 60) == Lives.REGEN_SECONDS - 60, "countdown from the first loss")
	# Simulate closing the app: save, reload, come back 45 minutes later.
	var reloaded := SaveData.normalize(JSON.parse_string(JSON.stringify(data)))
	_expect(Lives.count(reloaded, t0 + 45 * 60) == 5, "two lives back after 40+ minutes, across a restart")
	Lives.lose(reloaded, t0 + 50 * 60)
	_expect(Lives.count(reloaded, t0 + 30) == 4, "clock going backwards never adds lives")


func test_lives_cannot_go_below_zero() -> void:
	var data := SaveData.defaults()
	for i in 5:
		Lives.lose(data, 100)
	_expect(not Lives.can_play_challenge(data, 100), "no lives, no challenge")
	_expect(not Lives.lose(data, 100), "cannot lose a sixth life")
	Lives.refill(data, 100)
	_expect(Lives.count(data, 100) == 5, "refill restores all")


func test_renovation_in_order_and_paid_with_stars() -> void:
	var data := SaveData.defaults()
	var kingdom := Renovation.new()
	_expect(kingdom.is_room_open(data, "main_hall"), "first room open")
	_expect(not kingdom.is_room_open(data, "royal_garden"), "second room closed")
	_expect(kingdom.buy_next(data, "main_hall").is_empty(), "no stars, no purchase")
	data.wallet.stars_balance = 100
	var hall_tasks: int = kingdom.room("main_hall").tasks.size()
	for i in hall_tasks:
		_expect(not kingdom.buy_next(data, "main_hall").is_empty(), "task %d bought" % i)
	_expect(kingdom.is_room_complete(data, "main_hall"), "hall complete")
	_expect(kingdom.room_progress(data, "main_hall") == 1.0, "progress 100%")
	_expect(kingdom.is_room_open(data, "royal_garden"), "garden opens after the hall")
	_expect(data.wallet.stars_balance == 100 - 15, "hall costs 15 stars in total")
	_expect(kingdom.buy_next(data, "magic_library").is_empty(), "library still closed")


func test_save_round_trip_and_corruption_recovery() -> void:
	DirAccess.make_dir_recursive_absolute(TEST_DIR)
	for f in ["save.json", "save.bak", "save.json.tmp"]:
		DirAccess.remove_absolute(TEST_DIR.path_join(f))
	var store := SaveStore.new(TEST_DIR)
	var data := store.load_data()
	_expect(store.recovered_from == "" and data.wallet.coins == 0, "fresh install gets defaults")
	Progression.record_win(data, 1, 10, 10)
	data.wallet.coins = 120 # set after the win (which pays 40 coins)
	_expect(store.save_data(data), "first save")
	data.wallet.coins = 150
	_expect(store.save_data(data), "second save keeps a backup")
	var loaded := SaveStore.new(TEST_DIR).load_data()
	_expect(loaded.wallet.coins == 150 and Progression.best_stars_for(loaded, 1) == 3, "round trip")
	# Corrupt the main file: the backup (previous save) must come back.
	var f := FileAccess.open(TEST_DIR.path_join("save.json"), FileAccess.WRITE)
	f.store_string("{\"wallet\": {\"coins\": 15")
	f.close()
	var recovered := SaveStore.new(TEST_DIR)
	var from_backup := recovered.load_data()
	_expect(recovered.recovered_from == "backup" and from_backup.wallet.coins == 120, "falls back to save.bak")
	# Both corrupted: defaults, no crash.
	var g := FileAccess.open(TEST_DIR.path_join("save.bak"), FileAccess.WRITE)
	g.store_string("not json")
	g.close()
	var broken := SaveStore.new(TEST_DIR)
	_expect(broken.load_data().wallet.coins == 0 and broken.recovered_from == "defaults", "defaults when all is lost")


func test_normalize_repairs_types_and_keeps_unknown_keys() -> void:
	var odd := {"version": 0, "wallet": {"coins": 42.0, "stars_balance": "oops"}, "future_field": [1, 2]}
	var data := SaveData.normalize(odd)
	_expect(data.version == SaveData.CURRENT_VERSION, "migrated to current version")
	_expect(typeof(data.wallet.coins) == TYPE_INT and data.wallet.coins == 42, "JSON float back to int")
	_expect(data.wallet.stars_balance == 0, "wrong type replaced by default")
	_expect(data.has("future_field"), "unknown keys from a newer build are kept")
	_expect(data.boosters.undo == 3 and data.lives.count == 5, "missing sections get defaults")
