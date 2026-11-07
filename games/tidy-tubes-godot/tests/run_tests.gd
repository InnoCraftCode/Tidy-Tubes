## Headless test runner (no add-on needed):
##   Godot_v4.7.2-stable_win64_console.exe --headless --path . --script res://tests/run_tests.gd [-- levels=1-120]
## Exits with code 1 on any failure. Each suite is a script in tests/ with test_* methods.
##
## A GDScript runtime error does not stop the calling function's caller, so a crashing test
## would otherwise look green: every engine/script error raised while a test runs (captured
## with a Logger, Godot 4.5+) fails that test, and a suite that does not compile fails the run.
extends SceneTree

const SUITES := [
	"res://tests/test_parity.gd",
	"res://tests/test_gameplay.gd",
	"res://tests/test_progression.gd",
]


class ErrorCatcher extends Logger:
	var errors: Array = []
	var _mutex := Mutex.new()

	func _log_error(function: String, file: String, line: int, code: String, rationale: String,
			_editor_notify: bool, error_type: int, _backtraces: Array) -> void:
		if error_type == ERROR_TYPE_WARNING:
			return
		_mutex.lock()
		errors.append("%s:%d %s %s" % [file, line, code, rationale])
		_mutex.unlock()

	func take() -> Array:
		_mutex.lock()
		var out := errors.duplicate()
		errors.clear()
		_mutex.unlock()
		return out


func _init() -> void:
	var catcher := ErrorCatcher.new()
	OS.add_logger(catcher)
	var failed := 0
	var passed := 0
	var args := OS.get_cmdline_user_args()
	for path in SUITES:
		catcher.take()
		var script: GDScript = load(path)
		if script == null or not script.can_instantiate():
			failed += 1
			print("  FAIL %s does not compile" % path)
			for e in catcher.take().slice(0, 5):
				print("       " + e)
			continue
		var suite = script.new()
		suite.args = args
		for method in suite.get_method_list():
			var name: String = method.name
			if not name.begins_with("test_"):
				continue
			suite.failures.clear()
			catcher.take()
			var started := Time.get_ticks_msec()
			suite.call(name)
			var ms := Time.get_ticks_msec() - started
			for e in catcher.take():
				suite.failures.append("script error: " + e)
			if suite.failures.is_empty():
				passed += 1
				print("  PASS %s (%d ms)" % [name, ms])
			else:
				failed += 1
				print("  FAIL %s (%d ms)" % [name, ms])
				for f in suite.failures.slice(0, 10):
					print("       " + f)
	OS.remove_logger(catcher)
	print("%d passed, %d failed" % [passed, failed])
	quit(1 if failed > 0 else 0)
