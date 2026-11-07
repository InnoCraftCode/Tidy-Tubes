## The whole persistent state as plain data, with safe defaults, validation and schema
## migration. No file access here (see save_store.gd) and no game rules (see logic/).
##
## Schema history:
##   1 - first Godot schema (2.0.0).
extends RefCounted

const CURRENT_VERSION := 1


## A fresh profile. Every field the game reads exists here, so loading never meets a hole.
static func defaults() -> Dictionary:
	return {
		"version": CURRENT_VERSION,
		"progress": {
			"current_level": 1,
			"highest_unlocked": 1,
			"best_moves": [],   # index = level - 1, 0 = never cleared
			"best_stars": [],   # index = level - 1, 0..3
		},
		"wallet": {
			"coins": 0,
			"stars_balance": 0,   # spendable on renovation
			"stars_earned": 0,    # lifetime, statistics only
		},
		"boosters": {"undo": 3, "tube": 1, "hint": 1, "shuffle": 1},
		"lives": {"count": 5, "regen_from": 0}, # unix seconds the next life started charging
		"renovation": {},  # room id -> Array of finished task ids
		"story": {"chapter": 1, "seen": []}, # seen = dialogue ids already played
		"cosmetics": {"owned": [], "equipped": {}},
		"daily": {"last_claim_day": -1, "streak": 0},
		"settings": {
			"music_on": true, "sfx_on": true, "music_volume": 0.8, "sfx_volume": 0.8,
			"language": "", "music_track": -1, "vibration": true,
		},
	}


## Brings any loaded dictionary to the current schema: unknown keys are kept (a newer build may
## have written them), missing keys get defaults, wrong types are replaced by defaults.
static func normalize(loaded: Variant) -> Dictionary:
	var base := defaults()
	if typeof(loaded) != TYPE_DICTIONARY:
		return base
	var data: Dictionary = loaded
	data = _migrate(data)
	return _merge(base, data)


static func _migrate(data: Dictionary) -> Dictionary:
	var version := int(data.get("version", 0))
	# Future migrations go here, one step at a time: if version < 2: ...; version = 2
	if version < CURRENT_VERSION:
		data["version"] = CURRENT_VERSION
	return data


static func _merge(defaults_value: Variant, loaded: Variant) -> Variant:
	if typeof(defaults_value) == TYPE_DICTIONARY:
		if typeof(loaded) != TYPE_DICTIONARY:
			return defaults_value
		var out: Dictionary = loaded.duplicate()
		for key in defaults_value:
			out[key] = _merge(defaults_value[key], loaded.get(key))
		return out
	if loaded == null:
		return defaults_value
	# JSON turns ints into floats: accept any number where a number is expected.
	var number_types := [TYPE_INT, TYPE_FLOAT]
	if typeof(defaults_value) in number_types and typeof(loaded) in number_types:
		return int(loaded) if typeof(defaults_value) == TYPE_INT else float(loaded)
	if typeof(defaults_value) != typeof(loaded):
		return defaults_value
	return loaded
