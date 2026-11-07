## Headless test runner (no add-on needed):
##   Godot_v4.7.2-stable_win64_console.exe --headless --path . --script res://tests/run_tests.gd [-- levels=1-120]
## Exits with code 1 on any failure. Each suite is a script in tests/ with test_* methods.
extends SceneTree

const SUITES := [
	"res://tests/test_parity.gd",
]


func _init() -> void:
	var failed := 0
	var passed := 0
	var args := OS.get_cmdline_user_args()
	for path in SUITES:
		var suite = load(path).new()
		suite.args = args
		for method in suite.get_method_list():
			var name: String = method.name
			if not name.begins_with("test_"):
				continue
			suite.failures.clear()
			var started := Time.get_ticks_msec()
			suite.call(name)
			var ms := Time.get_ticks_msec() - started
			if suite.failures.is_empty():
				passed += 1
				print("  PASS %s (%d ms)" % [name, ms])
			else:
				failed += 1
				print("  FAIL %s (%d ms)" % [name, ms])
				for f in suite.failures.slice(0, 10):
					print("       " + f)
	print("%d passed, %d failed" % [passed, failed])
	quit(1 if failed > 0 else 0)
