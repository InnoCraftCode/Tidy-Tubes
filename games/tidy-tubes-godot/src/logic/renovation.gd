## Kingdom renovation rules: rooms open in order, tasks inside a room are bought in order with
## spendable stars. Pure: works on the content (data/kingdom.json) and the save dictionary.
extends RefCounted

const Progression := preload("res://src/logic/progression.gd")

const CONTENT_PATH := "res://data/kingdom.json"

var rooms: Array = []


func _init(content: Variant = null) -> void:
	if content == null:
		content = JSON.parse_string(FileAccess.get_file_as_string(CONTENT_PATH))
	rooms = content.rooms


func room(room_id: String) -> Dictionary:
	for r in rooms:
		if r.id == room_id:
			return r
	return {}


static func _done(data: Dictionary, room_id: String) -> Array:
	if not data.renovation.has(room_id):
		data.renovation[room_id] = []
	return data.renovation[room_id]


func is_room_complete(data: Dictionary, room_id: String) -> bool:
	var r := room(room_id)
	return not r.is_empty() and _done(data, room_id).size() >= r.tasks.size()


func is_room_open(data: Dictionary, room_id: String) -> bool:
	for i in rooms.size():
		if rooms[i].id == room_id:
			return i == 0 or is_room_complete(data, rooms[i - 1].id)
	return false


## @return the task to do next in the room, or {} when the room is complete or closed
func next_task(data: Dictionary, room_id: String) -> Dictionary:
	if not is_room_open(data, room_id):
		return {}
	var r := room(room_id)
	var done := _done(data, room_id)
	for task in r.tasks:
		if not done.has(task.id):
			return task
	return {}


func can_buy(data: Dictionary, room_id: String) -> bool:
	var task := next_task(data, room_id)
	return not task.is_empty() and int(data.wallet.stars_balance) >= int(task.cost)


## Buys the next task of the room. @return the task bought, or {} (nothing changed)
func buy_next(data: Dictionary, room_id: String) -> Dictionary:
	var task := next_task(data, room_id)
	if task.is_empty() or not Progression.spend_stars(data, int(task.cost)):
		return {}
	_done(data, room_id).append(task.id)
	if task.has("reward") and not data.cosmetics.owned.has(task.reward):
		data.cosmetics.owned.append(task.reward)
	return task


## 0..1 restoration of one room, for the before/after visuals.
func room_progress(data: Dictionary, room_id: String) -> float:
	var r := room(room_id)
	if r.is_empty() or r.tasks.is_empty():
		return 0.0
	return float(_done(data, room_id).size()) / float(r.tasks.size())


func total_cost() -> int:
	var sum := 0
	for r in rooms:
		for task in r.tasks:
			sum += int(task.cost)
	return sum
