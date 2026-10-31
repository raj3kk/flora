# CEO D-28 — SD-6 Per-Behaviour Record (Part 1, 2026-10-31)

**Blanket parity claim kabhi nahi.** SD-6 = Maya task-list parity
(SD plan §SD-6: "Maya ke builtin task list se iski parity ka koi
verified comparison record nahi hai — parity-claim detail
UNVERIFIED hai"). Neeche har behaviour have/held ke saath hai —
jo code me hai wo HAVE, jo spec/held hai wo HELD. Source:
`docs/FLORA_SD_IMPLEMENTATION_PLAN.md` §SD-6 + `planner/TaskListStore.kt`
+ `ui/TaskListScreen.kt` (live tip se verify).

## Behaviours

| # | Behaviour (SD-6 spec se) | Status | Evidence (code anchor) |
|---|---|---|---|
| 1 | Task create | HAVE | `planner/TaskListStore.kt` `create(title, notes, dueText, planId)` — khaali title reject; `ui/TaskListScreen.kt` "Task banao" button |
| 2 | Task list | HAVE | `TaskListStore.list()` (created-desc sort, encrypted store); screen par "Tasks (n)" list |
| 3 | Task complete | HAVE | `TaskListStore.setDone(id, true)`; screen button "Complete karo" + Auto Log "SD-6: task complete" |
| 4 | Task reopen | HAVE | `TaskListStore.setDone(id, false)`; screen button "Dobara kholo" + Auto Log "SD-6: task dobara khola" |
| 5 | Due | HAVE | `UserTask.dueText` field; screen par "Due: …" (80 char cap) + due input field |
| 6 | Notes | HAVE | `UserTask.notes` field; screen par "Notes: …" (200 char cap) + notes input field |
| 7 | Planner completion-marker link | HAVE (link only, no auto-sync) | `UserTask.planId` field + screen par `markerFor(task)` line; screen header saaf kehta hai: "Task complete karna plan ko complete nahi karta, aur plan DONE hona task ko auto-complete nahi karta — dono alag" |
| 8 | Maya task-list parity (behaviour-by-behaviour comparison) | HELD / UNVERIFIED | SD plan §SD-6 khud kehta hai: 178 records me Maya task-list ka ek-ek vyavhaar verified nahi mila. Isliye is record me Maya parity ka koi daava nahi hai — sirf upar wale Flora-new behaviours darj hain |
| 9 | CRUD + marker + reboot-resume + partial-state tests (SD-6 tests) | HELD (phone/unit-test gate baaki) | SD plan §SD-6 tests darj hain (CRUD + completion markers + reboot-resume last VERIFIED step se + partial-state compensation reversible-only + user yes) — ye tests is cycle me execute nahi hue hain; planner core M1–M15 DONE hai, par SD-6 surface ka test gate baaki hai |
| 10 | Planner core rewrite (SD-6 ke liye) | HELD by design (mana hai) | SD plan §SD-6: "planner ko dobara nahi likha jayega" — surface planner core ke UPAR bani hai, core rewrite kabhi nahi hoga |

## Summary

HAVE: create / list / complete / reopen / due / notes / planId
marker-link (7 behaviours, code anchors ke saath). HELD: Maya
parity comparison, SD-6 test gate, planner rewrite (by design
mana). **Is record se "SD-6 parity poori hai" ka daava kabhi
nahi kiya ja raha hai.**
