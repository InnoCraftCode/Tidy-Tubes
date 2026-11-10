extends SceneTree
## Renders the Google Play store kit (needs a window, NOT --headless):
##   Godot_v4.7.2-stable_win64_console.exe --path . --script res://tests/capture_store.gd -- <kit_dir>
## Writes, under <kit_dir> (play-store\threadbound-festival):
##   screenshots\phone\0N-*.png   1080x1920, 24-bit PNG (Play: 9:16, >= 1080 px, no alpha)
##   graphics\icon-512.png        512x512, 32-bit PNG
##   graphics\feature-graphic-1024x500.png   1024x500, 24-bit PNG
## and, in the project, assets\icons\launcher_*.png: the Android adaptive launcher icon layers
## (foreground, background, monochrome, 432x432) and the legacy 192x192 icon used by export_presets.cfg.
## The game is rendered offscreen in a SubViewport whose 2D size is overridden to the project's
## 720x1280 base, so every screen lays out exactly as on a phone and is drawn at 1.5x.
## The icon and feature graphic reuse the game's procedural art (Art.pip, lantern, town, fonts).

var kit := "user://store"
var vp: SubViewport
var main: Control


func _init() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		kit = args[0]
	DirAccess.make_dir_recursive_absolute(kit.path_join("screenshots/phone"))
	DirAccess.make_dir_recursive_absolute(kit.path_join("graphics"))
	_run.call_deferred()


func _frames(n: int) -> void:
	for i in n:
		await process_frame


func _grab(v: SubViewport) -> Image:
	await RenderingServer.frame_post_draw
	await RenderingServer.frame_post_draw
	return v.get_texture().get_image()


func _save(img: Image, path: String, alpha: bool) -> void:
	img.convert(Image.FORMAT_RGBA8 if alpha else Image.FORMAT_RGB8)
	var err := img.save_png(path)
	print("Wrote ", path, " (", img.get_width(), "x", img.get_height(), ") ", error_string(err))


func _shot(name: String) -> void:
	var img := await _grab(vp)
	_save(img, kit.path_join("screenshots/phone").path_join(name + ".png"), false)


func _pull(n: int, wait_frames: int) -> void:
	for step in n:
		var lv = main.current
		var r: int = Solver.hint(lv.level, lv.state)
		if r < 0:
			return
		lv._tap(r)
		await _frames(wait_frames)


func _run() -> void:
	# ---------------------------------------------------------------- screenshots
	vp = SubViewport.new()
	vp.size = Vector2i(1080, 1920)
	vp.size_2d_override = Vector2i(720, 1280)
	vp.size_2d_override_stretch = true
	vp.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	vp.transparent_bg = false
	root.add_child(vp)
	var args := OS.get_cmdline_user_args()
	if args.size() > 1: # second argument: interface language (en, fr, ar...); not saved
		var g = root.get_node("Game")
		g.lang = args[1]
		g.apply_lang()
	main = load("res://scenes/main.tscn").instantiate()
	vp.add_child(main)
	await _frames(30)

	# 01: a fresh Lantern Lift tangle (World 2) - "Free the festival!"
	main.play_level(24)
	await _frames(45)
	await _shot("01-tangle")

	# 02: one tap, the ribbon in flight to its spool
	main.play_level(12)
	await _frames(35)
	var lv = main.current
	var h: int = Solver.hint(lv.level, lv.state)
	if h >= 0:
		lv._tap(h)
		await _frames(12)
	await _shot("02-one-tap")

	# 03: Lantern Lift after a few pulls, bubble level visible
	main.play_level(24)
	await _frames(35)
	await _pull(3, 40)
	await _frames(20)
	await _shot("03-lantern-lift")

	# 04: a win, the town lit behind the win card
	main.play_level(2)
	await _frames(25)
	for step in 40:
		lv = main.current
		if lv.win_t >= 0.0:
			break
		var r: int = Solver.hint(lv.level, lv.state)
		if r < 0:
			break
		lv._tap(r)
		await _frames(45)
	await _frames(78)
	await _shot("04-town")

	# 05: the daily puzzle (same for everyone, UTC day)
	main.play_daily()
	await _frames(45)
	await _shot("05-daily")

	# 06: the market, companions tab
	main.show_shop()
	await _frames(12)
	main.current._set_kind("animal")
	await _frames(20)
	await _shot("06-shop")

	# 07: Windmill Hills, wind hooks (World 3)
	main.play_level(45)
	await _frames(45)
	await _shot("07-wind")

	# 08: the festival map, no overlay, scrolled to the first lanterns
	main.show_home()
	await _frames(30)
	var home = main.current
	home.close_overlay()
	await _frames(12)
	await _shot("08-map")

	vp.queue_free()
	await _frames(2)

	# ---------------------------------------------------------------- icon + feature graphic
	var icon_vp := _canvas(Vector2i(512, 512), IconArt.new())
	await _frames(6)
	_save(await _grab(icon_vp), kit.path_join("graphics/icon-512.png"), true)
	icon_vp.queue_free()

	var feat_vp := _canvas(Vector2i(1024, 500), FeatureArt.new())
	await _frames(6)
	_save(await _grab(feat_vp), kit.path_join("graphics/feature-graphic-1024x500.png"), false)
	feat_vp.queue_free()

	# Android launcher icons for the export preset (launcher_icons/* in export_presets.cfg):
	# adaptive foreground/background/monochrome at 432x432 and the legacy 192x192 icon.
	var icons := ProjectSettings.globalize_path("res://assets/icons")
	DirAccess.make_dir_recursive_absolute(icons)
	var fg_vp := _canvas(Vector2i(432, 432), ForegroundArt.new(), true)
	await _frames(6)
	_save(await _grab(fg_vp), icons.path_join("launcher_foreground_432.png"), true)
	fg_vp.queue_free()
	var bg_vp := _canvas(Vector2i(432, 432), BackgroundArt.new())
	await _frames(6)
	_save(await _grab(bg_vp), icons.path_join("launcher_background_432.png"), true)
	bg_vp.queue_free()
	var mono_vp := _canvas(Vector2i(432, 432), MonochromeArt.new(), true)
	await _frames(6)
	_save(await _grab(mono_vp), icons.path_join("launcher_monochrome_432.png"), true)
	mono_vp.queue_free()
	var legacy_vp := _canvas(Vector2i(512, 512), IconArt.new())
	await _frames(6)
	var legacy := await _grab(legacy_vp)
	legacy.resize(192, 192, Image.INTERPOLATE_LANCZOS)
	_save(legacy, icons.path_join("launcher_192.png"), true)
	legacy_vp.queue_free()
	await _frames(2)
	quit()


func _canvas(size: Vector2i, drawing: Control, transparent := false) -> SubViewport:
	var v := SubViewport.new()
	v.size = size
	v.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	v.transparent_bg = transparent
	root.add_child(v)
	drawing.size = Vector2(size)
	v.add_child(drawing)
	return v


# -------------------------------------------------------------------- shared drawing helpers

class H:
	## A ribbon drawn as a thick curve: ink outline, hue body, a soft shine along one side.
	static func ribbon(ci: CanvasItem, sp: PackedVector2Array, w: float, col: Color) -> void:
		Art.glow(ci, sp, col, w, 0.9)
		ci.draw_polyline(sp, Art.INK, w + 10.0, true)
		ci.draw_polyline(sp, col, w, true)

		ci.draw_polyline(Art.along_normal(sp, -w * 0.22), Color(1, 1, 1, 0.28), w * 0.26, true)
		var caps := [sp[0], sp[sp.size() - 1]]
		for c in caps:
			ci.draw_circle(c, w * 0.5 + 5.0, Art.INK)
			ci.draw_circle(c, w * 0.5, col)


	## Smooth curve through a few control points (Catmull-Rom), n points per segment.
	static func curve(points: Array, n := 24) -> PackedVector2Array:
		var out := PackedVector2Array()
		var m := points.size()
		for i in m - 1:
			var p0: Vector2 = points[maxi(i - 1, 0)]
			var p1: Vector2 = points[i]
			var p2: Vector2 = points[i + 1]
			var p3: Vector2 = points[mini(i + 2, m - 1)]
			for k in n:
				var t := k / float(n)
				var t2 := t * t
				var t3 := t2 * t
				out.append(0.5 * ((2.0 * p1) + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
					+ (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3))
		out.append(points[m - 1])
		return out


	static func stars(ci: CanvasItem, rect: Rect2, seed_value: int, count: int) -> void:
		var rng := RandomNumberGenerator.new()
		rng.seed = seed_value
		for i in count:
			var p := Vector2(rng.randf_range(rect.position.x, rect.end.x), rng.randf_range(rect.position.y, rect.end.y))
			var r := rng.randf_range(1.2, 3.2)
			ci.draw_circle(p, r, Color(Art.PAPER, rng.randf_range(0.35, 0.9)))


# -------------------------------------------------------------------- icon (512 x 512)

class IconArt extends Control:
	func _draw() -> void:
		var r := Rect2(Vector2.ZERO, size)
		Art.sky(self, r, Art.NIGHT, Art.NIGHT_DEEP)
		H.stars(self, r.grow(-30), 7, 26)
		# warm light behind Pip
		draw_circle(Vector2(256, 262), 190, Color(Art.LANTERN_GLOW, 0.10))
		draw_circle(Vector2(256, 262), 130, Color(Art.LANTERN_GLOW, 0.12))
		# a ruby ribbon loop Pip peeks through: back half first, Pip, then the front half
		var loop := PackedVector2Array()
		for k in 73:
			var a := TAU * k / 72.0
			loop.append(Vector2(256, 262) + Vector2(cos(a) * 186, sin(a) * 172))
		capture_ribbon(self, loop, 54.0, Art.RIBBON[0])
		Art.pip(self, Vector2(256, 262), 4.4)
		# a sky-blue ribbon tail crossing the corner, and a small lit lantern
		var tail := H.curve([Vector2(-20, 470), Vector2(120, 430), Vector2(200, 500), Vector2(330, 540)], 20)
		capture_ribbon(self, tail, 40.0, Art.RIBBON[2])
		Art.lantern(self, Vector2(438, 30), 0.72, 1.0, -0.12)

	static func capture_ribbon(ci: CanvasItem, sp: PackedVector2Array, w: float, col: Color) -> void:
		ci.draw_polyline(sp, Art.INK, w + 10.0, true)
		ci.draw_polyline(sp, col, w, true)
		ci.draw_polyline(Art.along_normal(sp, -w * 0.22), Color(1, 1, 1, 0.28), w * 0.26, true)


# -------------------------------------------------------------------- feature graphic (1024 x 500)

class FeatureArt extends Control:
	func _draw() -> void:
		var r := Rect2(Vector2.ZERO, size)
		Art.sky(self, r, Art.NIGHT_DEEP, Art.DUSK)
		H.stars(self, Rect2(0, 0, 1024, 300), 11, 60)
		# the town of Lumina, lit for the festival
		Art.town(self, Rect2(0, 0, 1024, 500), 3, 0.85, Color("#2A1F3D"))
		draw_rect(Rect2(0, 470, 1024, 30), Color("#15122A"))
		# lantern strings
		for x in [700, 790, 880, 970]:
			draw_line(Vector2(x, 0), Vector2(x, 40), Color(Art.INK, 0.8), 3.0, true)
		Art.lantern(self, Vector2(700, 40), 1.05, 1.0, 0.10)
		Art.lantern(self, Vector2(790, 40), 1.25, 1.0, -0.06)
		Art.lantern(self, Vector2(880, 40), 1.05, 1.0, 0.08)
		Art.lantern(self, Vector2(970, 40), 1.2, 1.0, -0.12)
		# ribbons winding across the bottom and behind the title
		var ruby := H.curve([Vector2(-30, 330), Vector2(140, 400), Vector2(330, 330), Vector2(520, 420), Vector2(700, 360), Vector2(900, 440), Vector2(1060, 380)], 22)
		var sky_r := H.curve([Vector2(-30, 440), Vector2(180, 380), Vector2(380, 450), Vector2(600, 390), Vector2(780, 470), Vector2(1060, 430)], 22)
		var sun := H.curve([Vector2(560, 520), Vector2(620, 420), Vector2(700, 470), Vector2(760, 400), Vector2(840, 480), Vector2(930, 430)], 22)
		H.ribbon(self, sky_r, 30.0, Art.RIBBON[2])
		H.ribbon(self, sun, 26.0, Art.RIBBON[1])
		H.ribbon(self, ruby, 34.0, Art.RIBBON[0])
		# Pip, big and happy, on the right
		draw_circle(Vector2(830, 300), 150, Color(Art.LANTERN_GLOW, 0.10))
		Art.pip(self, Vector2(830, 296), 3.4)
		# title block
		var title := Art.bold()
		var body := Art.font()
		var tpos := Vector2(56, 212)
		draw_string(title, tpos + Vector2(5, 6), "Knotlight", HORIZONTAL_ALIGNMENT_LEFT, -1, 118, Color(Art.INK, 0.85))
		draw_string(title, tpos, "Knotlight", HORIZONTAL_ALIGNMENT_LEFT, -1, 118, Art.PAPER)
		draw_string(body, Vector2(62, 268), "Ribbon Puzzle", HORIZONTAL_ALIGNMENT_LEFT, -1, 46, Art.LANTERN)
		draw_string(body, Vector2(62, 316), "Untangle the ribbons, light the lanterns.", HORIZONTAL_ALIGNMENT_LEFT, -1, 28, Color(Art.PAPER, 0.9))


# -------------------------------------------------------------------- launcher icon layers (432 x 432)
# Adaptive icons: Android masks the outer third, so the subject stays inside the central 66 dp
# (the middle 264 px of 432). The background fills the whole square.

class ForegroundArt extends Control:
	func _draw() -> void:
		var c := Vector2(216, 216)
		draw_circle(c, 118, Color(Art.LANTERN_GLOW, 0.14))
		var loop := PackedVector2Array()
		for k in 73:
			var a := TAU * k / 72.0
			loop.append(c + Vector2(cos(a) * 112, sin(a) * 104))
		draw_polyline(loop, Art.INK, 40.0, true)
		draw_polyline(loop, Art.RIBBON[0], 32.0, true)
		draw_polyline(Art.along_normal(loop, -7.0), Color(1, 1, 1, 0.28), 8.0, true)
		Art.pip(self, c - Vector2(0, 2), 2.7)


class BackgroundArt extends Control:
	func _draw() -> void:
		var r := Rect2(Vector2.ZERO, size)
		Art.sky(self, r, Art.NIGHT, Art.NIGHT_DEEP)
		H.stars(self, r, 5, 40)


class MonochromeArt extends Control:
	func _draw() -> void:
		var c := Vector2(216, 216)
		var white := Color(1, 1, 1)
		var loop := PackedVector2Array()
		for k in 73:
			var a := TAU * k / 72.0
			loop.append(c + Vector2(cos(a) * 112, sin(a) * 104))
		draw_polyline(loop, white, 30.0, true)
		# Pip's head as one white silhouette (ears + face), same shape as Art.pip
		draw_set_transform(c - Vector2(0, 2), 0.0, Vector2(2.7, 2.7))
		draw_colored_polygon(PackedVector2Array([Vector2(-26, -6), Vector2(-20, -34), Vector2(-6, -16)]), white)
		draw_colored_polygon(PackedVector2Array([Vector2(26, -6), Vector2(20, -34), Vector2(6, -16)]), white)
		draw_colored_polygon(PackedVector2Array([Vector2(-30, -8), Vector2(30, -8), Vector2(0, 30)]), white)
		draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)
