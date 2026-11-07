## Reads and writes the save under user:// safely.
##
## Write: serialise to save.json.tmp, keep the previous good file as save.bak, then rename the
## temp file over save.json. A crash mid-write leaves either the old file or the new one, never
## half a file. Read: save.json, else save.bak, else defaults; corrupted files never crash.
extends RefCounted

const SaveData := preload("res://src/save/save_data.gd")

var directory: String
var recovered_from := "" # "", "backup" or "defaults": what load() had to fall back to


func _init(dir := "user://") -> void:
	directory = dir


func _path(name: String) -> String:
	return directory.path_join(name)


func load_data() -> Dictionary:
	recovered_from = ""
	var main: Variant = _read(_path("save.json"))
	if main != null:
		return SaveData.normalize(main)
	var backup: Variant = _read(_path("save.bak"))
	if backup != null:
		recovered_from = "backup"
		return SaveData.normalize(backup)
	if FileAccess.file_exists(_path("save.json")) or FileAccess.file_exists(_path("save.bak")):
		recovered_from = "defaults"
	return SaveData.defaults()


func save_data(data: Dictionary) -> bool:
	var text := JSON.stringify(data, "", false)
	var tmp := _path("save.json.tmp")
	var file := FileAccess.open(tmp, FileAccess.WRITE)
	if file == null:
		push_error("save: cannot open %s (%s)" % [tmp, error_string(FileAccess.get_open_error())])
		return false
	file.store_string(text)
	file.flush()
	file.close()
	# Verify what reached the disk before replacing the good copy.
	if _read(tmp) == null:
		push_error("save: written file does not read back")
		return false
	var dir := DirAccess.open(directory)
	if dir == null:
		return false
	if FileAccess.file_exists(_path("save.json")):
		if FileAccess.file_exists(_path("save.bak")):
			dir.remove("save.bak")
		dir.rename("save.json", "save.bak")
	return dir.rename("save.json.tmp", "save.json") == OK


## @return the parsed JSON, or null when the file is missing, empty or not valid JSON
func _read(path: String) -> Variant:
	if not FileAccess.file_exists(path):
		return null
	var text := FileAccess.get_file_as_string(path)
	if text.is_empty():
		return null
	var json := JSON.new()
	if json.parse(text) != OK:
		return null
	return json.data if typeof(json.data) == TYPE_DICTIONARY else null
