## Turns the solver into advice, port of logic/HintEngine.java. Tries the shortest path, then
## any path; tells "you are stuck, undo" (proven) apart from "no answer within budget".
## Bounded but not instant on a ten-colour board: call from a WorkerThreadPool task.
extends RefCounted

const Solver := preload("res://src/logic/solver.gd")
const Rules := preload("res://src/logic/rules.gd")

const OPTIMAL_BUDGET := 250000   # GameConfig.HINT_OPTIMAL_NODE_BUDGET
const FALLBACK_BUDGET := 2000000 # GameConfig.ANY_SOLUTION_NODE_BUDGET

enum Kind { MOVE, UNDO, ALREADY_WON, UNKNOWN }


## @return {"kind": Kind, "move": PackedInt32Array, "remaining": int, "exact": bool}
static func advise(board: PackedInt32Array, optimal_budget := OPTIMAL_BUDGET,
		fallback_budget := FALLBACK_BUDGET) -> Dictionary:
	if Rules.is_won(board):
		return {"kind": Kind.ALREADY_WON, "move": PackedInt32Array(), "remaining": 0, "exact": true}
	var optimal := Solver.find_optimal(board, optimal_budget)
	if optimal.solved:
		return {"kind": Kind.MOVE, "move": optimal.moves[0], "remaining": optimal.moves.size(), "exact": true}
	var fallback := Solver.find_any(board, fallback_budget)
	if fallback.solved:
		return {"kind": Kind.MOVE, "move": fallback.moves[0], "remaining": fallback.moves.size(), "exact": false}
	var kind := Kind.UNKNOWN if fallback.exhausted else Kind.UNDO
	return {"kind": kind, "move": PackedInt32Array(), "remaining": 0, "exact": false}
