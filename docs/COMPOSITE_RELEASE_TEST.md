# Release test of composite-states (22 Nov 2026)

Tests before merging `composite-states` into `main`. Generator `composite-states` 3bf32cf (fix of issue CC);
plugin built from it, 1.0.0.202611221803. Comparisons with `composite-states` 8c1a37c (before the fix of CC)
and with `main` 2976313 (= published plugin 1.0.0.202611071400). Isabelle2025-2; library `RC_HOL-CSP` of
RoboChart-Deadlock-HOLCSP-AG-dev (main tree, branch `trigger-fqn-naming`; the library files are the same as
on its master), except for the ITP baseline (Section 3), which uses the library of the ITP repository.

How the theories were generated: dev-tools `GenAll <rct> <project> <erules> <ecore> <out> t rcpkg alltemplates`
with the `erules/` of each generator version (for 8c1a37c and 2976313 exported with `git archive`); the
plugin itself was NOT run in RoboTool by me (list for Fang in Section 9). How they were checked:
`isabelle process_theories -l RC_HOL-CSP -f <theory>`, one process at a time, started with `setsid`, the
process groups of all descendants recorded and checked afterwards (no process left over). Trans-core theories
are judged by their text, not by whether their lemma closes (generator issue T: outside the plugin the
`deadlock_free'` lemma stops at `done` whenever a goal needs sledgehammer or an invariant).

## 1. Fix of CC

Change: `createAssignmentStmtAsAction` (Rule 66) with `userPreconditionGuard` and `substitutedPrecondition`
(RC2Z.eol); details in `docs/SEMANTICS_DIFFERENCES.md` entry 7 and in the change record of RC2Z.eol.

Position of the guards. CC and AA are both placed after the reads of the statement and before it:
`readState(usedVariables(stmt), AA guards & (CC guard & stmt))` (the AA guards outside, the CC guard
inside). The kinds of statement DIFFER: AA covers assignment, operation call, communication and if
statement (and descends into divisions, sequence indexing and arguments); CC only an assignment whose
right-hand side is the call itself, as before the fix. NOT extended (as decided).

Test models (RoboChart-Deadlock-HOLCSP-AG-dev `robochart-models/composite-tests/`, new):

| Model | Statement (in the top-level state A and in B inside the composite state S) | After the fix (3bf32cf) | Before (8c1a37c) |
|---|---|---|---|
| `c9_preconditions.rct` | `x = head(q)` (formal of head: `s`) | one guard `q_C9_composite_c9 ≠ []` (AA) | two guards: `q_C9_composite_c9 ≠ []` and `s ≠ []` (formal) |
| | `y = small(a + 1)`, precondition `n * 2 < 10` | `((a_C9_composite_c9 + 1) * 2) < 10` | `(n * 2) < 10` (formal) |
| | `y = shifted(g(d))`, precondition `let g == 1 @ n + g > 0` (bound g, function g in the argument) | `(let g_1 = 1 in (g((d_C9_composite_c9)) + g_1)) > 0` | `(let g = 1 in (n + g)) > 0` |
| `c9b_precondition_quantifier.rct` | `y = half(k(m))`, precondition `exists k : int @ 2 * k == n` | guard `GENERATOR_ERROR_untranslated_precondition_half`; Isabelle stops at `Inactive_B_S_C9b_composite_c9b_def` with "Extra variables on rhs" naming it | generation stops in M2M: "Called method 'createMemPredEquality' on undefined object", no theory |

Isabelle: the HOL-CSP theory of c9 loads (1:11). Each guard occurs twice (A and B), as expected.
Variable capture cannot happen with a machine variable: RC2Z renames the variables of the machine with the
suffix of their container before translating, so only other names (here a function name) can clash; c9
tests that case. The generator translates no quantifier in any position (createExpr has no case), so the
capture test with `exists`/`forall` that was asked for is not possible; c9b records what happens instead.

Regression of the fix (all models of Sections 4 and 5, three theories each, 8c1a37c against 3bf32cf):
only c9, c9b, d1, d2 and the two GasAnalysis candidates differ; all other theories have the same tokens
(some in another order: element order of generated sets, as between two runs). d1, d2: the second guard
`s ≠ []` with the formal `s` is gone. GasAnalysis: the guard of `anl = location(gs)` is now
`size((gs_<suffix>)) > 0`; before it was `size((gs)) > 0` with the formal `gs`. Before the fix these
theories did NOT load: d1 "Extra variables on rhs: "s"" (in `Inactive_A_D1_definedness_d1_def`), GasAnalysis
(t1 candidate) "Extra variables on rhs: "gs"" (in `Trans_GA_Chem_def`); after the fix both load.

## 2. Build and installation

`git archive 3bf32cf` into a scratch folder, JDK 11 (`/usr/lib/jvm/java-11-openjdk-amd64`), Maven 3.9.16,
`mvn -B package`: BUILD SUCCESS (20 s). p2 director of the RoboTool copy `~/tools/robotool-1.3-composite`:
uninstall feature 1.0.0.202611221555, install 1.0.0.202611221803 ("Operation completed"). Checked:
`bundles.info` lists `circus.robocalc.robochart.epsilon.isacircus_1.0.0.202611221803.jar`; the ten
`erules/` files in the installed jar are identical to 3bf32cf; automatic updates of the copy still off
(`org.eclipse.equinox.p2.ui.sdk.scheduler.prefs`, `enabled=false`). The original
`~/tools/robotool-1.3.2026102101` still has 1.0.0.202611071400. In `plugins/` of the copy the jar 1555 is
still present (not loaded); the jar 1400 is no longer there (removed by p2, not by me).

## 3. ITP 2026 examples

Repository laila-fangyan/RoboChart-Deadlock-HOLCSP-AG, commit 59b534a (cloned to
`~/Git/RoboChart-Deadlock-HOLCSP-AG`, read only). It contains the library and the six example theories
(generated semantics with hand-written invariants and proofs), NOT the RoboChart models (.rct).

Baseline. A scratch copy of 59b534a with the sessions renamed (`ITP_RC_HOL-CSP`,
`ITP_RC_HOL-CSP_examples`, and `"RC_HOL-CSP.` in the imports accordingly; nothing else changed), because the
names collide with the registered RC_HOL-CSP of AG-dev, whose library differs (`Proof_Methods.thy`,
`RoboChart_Library.thy`). `isabelle build -d <copy> ITP_RC_HOL-CSP ITP_RC_HOL-CSP_examples`: both sessions
pass (examples 9:01): running example, GasAnalysis, LRE_2D, TurtleBot ddl, ddl_ag_true, ddlf_auto. The
counterexample lemmas do not fail in a batch run whatever nitpick or quickcheck report, so they were
restated in separate theories importing the examples unchanged, with `expect`: running example
`Trans_ag_true` nitpick `[expect = genuine]`, TurtleBot ag_true and ddl quickcheck
`[expect = counterexample]`: all three hold.

Models. Searched on this machine (all `.rct` under the home directory), matched by node and variable names
and then by comparing the generated Trans with the Trans of the ITP theory (names, parentheses, invariants
normalised away):

| Example | Model found | Match |
|---|---|---|
| TurtleBot ddlf (ddlf_auto, ddl_ag_true) | `RoboChart2Circus_development_repo/Turtlebot-robochart-deadlock-free/Adaptation_Plan_flat_ddlf.rct` | Trans token for token |
| TurtleBot ddl | `RoboChart2Circus_development_repo/Turtlebot-robochart2/Adaptation_Plan_flat_ddlk.rct` | Trans token for token |
| running example | `RoboChart2Circus_development_repo/Abstract_examples/example_robot_DDLK.rct`, stm0 | Trans the same except one `Skip ;` removed by hand in the ITP theory; but the file is a controller `robot_ctrl_D` with two machines, the ITP theory names `robot_ctrl` |
| GasAnalysis | two candidates, neither the ITP model: `ChemicalDetector-autonomous8_GA_corrected/GasAnalysis.rct` (stm GasAnalysis, functions in the imported package Chemical, `gs` initially `<>`); `Test1/GasAnalysis/GasAnalysis.rct` (package Chem, stm GA, all in one file) | the ITP Trans sets `gs` to `[⦇c=0,i=0⦈]` and `anl` to `Front` on the initial transition and has an assume after the input trigger; neither candidate has this |
| LRE | `LRE_2D_coordToPair/lre_beh.rct`, `LRE/lre_beh.rct` | not generated: M2M fails ("Called feature 'name' on undefined object"), on `main` 2976313 as well |

Regeneration and proof (new generator, library RC_HOL-CSP of AG-dev; the Trans-core theory, which is the
form of the ITP theories, with the ITP invariants put in at the same places and the ITP proof scripts):

| Theory | Invariant / script | Result |
|---|---|---|
| TurtleBot ddlf | `assume (fst minMaxRotation ≤ snd minMaxRotation)` at the read; `deadlock_free'` + the same `smt` step | PROVED (0:55) |
| TurtleBot ag_true | none (assume True); `find_counterexample`, `quickcheck [expect = counterexample]` | counterexample found, as in the baseline |
| TurtleBot ddl | none; same | counterexample found, as in the baseline |
| running example | — | NOT RUN: the model found is a controller with two machines; its theories do not load (`Duplicate constant declaration chan_event.get_obs`, controllers are not supported, issue DD; the same on `main`) |
| GasAnalysis | — | NOT RUN (model missing). The CC guard of both candidates is `size(gs) > 0` with the machine's variable (the case of argument = formal parameter): the same guard as in the ITP theory. The t1 candidate loads; the a8 candidate does not ("Extra variables on rhs: "location", "goreq"": functions of an imported package are not declared, see Section 8; the same before the fix) |
| LRE | — | NOT RUN (no theory generated) |

Missing for a full regression (to be copied from the ITP folder on Windows): the `.rct` files of the
running example (controller `robot_ctrl`), of GasAnalysis (package Chemical, as in the ITP theory) and of
LRE (`LRE_Beh`), with the files they import.

## 4. Composite states

| Model | HOL-CSP theory | Trans-core text | Notes |
|---|---|---|---|
| c1 minimal | loads (0:55) | unchanged by CC | lemma closes |
| c2 entry/exit | loads (1:09) | unchanged | lemma closes |
| c3 during | loads (1:04) | unchanged | stops at `done` (T) |
| c4 boundary | loads (1:05) | unchanged | stops at `done` (T) |
| c5 junction/final | loads (2:03) | unchanged | stops at `done` (T) |
| c6 nested | loads (2:10) | unchanged | stops at `done` (T) |
| c7 operation (controller) | does NOT load: `Duplicate constant declaration chan_event.a_in` | — | expected, controllers not supported (DD); marker `GENERATOR_ERROR_missing_action_crtlMemory_C7_composite_c7` |
| c8 exit/input | loads (2:04) | unchanged | one `GENERATOR WARNING` as expected |
| c9 preconditions | loads (1:11) | unchanged | Section 1 |
| c9b quantifier | stops at the marker | — | Section 1 |
| DTI Adaptation_Plan (full, `dti/enriched/`) | loads (3:25) | unchanged | |

## 5. Other models

| Model | HOL-CSP theory | Notes |
|---|---|---|
| ex10, ex12 (`Abstract_examples/`) | load (0:31, 0:24) | Trans-core: ex12 closes, ex10 stops at `done` (T) |
| Legitimate (`dti/enriched/Adaptation_Legitimate.rct`) | loads (0:58) | |
| flat MakePlan (`DTI-simple-Plan-CaseStudy/Adaptation_Plan.rct`) | loads (6:57) | |
| definedness d1, d2 | load (0:31 each) | d1 did not load before the fix (Section 1) |
| definedness d3 | not generated: M2M `StackOverflowError`, also with 8c1a37c and `main` | Section 8 |

## 6. IsaCircus output

Searched in all IsaCircus (and HOL-CSP, Trans-core) theories of Sections 4 and 5 for `INCOMPLETE`,
`GENERATOR_ERROR_` and `GENERATOR WARNING`: none except the expected ones (c7:
`GENERATOR_ERROR_missing_action_crtlMemory_C7_composite_c7`; c8: one warning; c9b:
`GENERATOR_ERROR_untranslated_precondition_half`). No partial file. The IsaCircus theories were NOT loaded
(no IsaCircus session on this machine).

## 7. Against the published version (main 2976313)

Same models, all three theories, compared by tokens (time stamps removed). On `main` the composite models
(c1-c9, full Adaptation_Plan) and d3 do not generate (M2M fails; composite states are new), and the a8
GasAnalysis candidate generates but does not load ("Undefined type name: "Intensity"", issue W). Where both
generate, every difference is one of:

- trigger naming: `<e>__in` to `<e>_<container FQN>__in` (all models with triggers);
- W: types of imported packages declared (Legitimate, GasAnalysis a8);
- X: the extra read of a variable used only in a `let` (flat MakePlan: `get_image`);
- Z: `datatype (plugins del: ...)` and a comment for `chan_event` above 80 constructors (running example
  candidate, 82);
- AA: toolkit functions printed as Isabelle functions (`hd`, `tl`) and their preconditions as guards
  (`s ≠ []`) (d1, d2);
- CC: the guards of Section 1;
- Trans-core theory of a model with several machines (running example candidate, controller with stm0
  and stm1): on `main` `Transc_stm0_robot_ctrl_D` is typed `NIDS_stm1_robot_ctrl_D \<rightarrow> ...`
  (wrong) and both machines get lemmas of the same name; on the branch the type is printed from the
  parameter declaration (`NIDS_stm0_...`) and the lemma names contain the container (trigger-fqn-naming,
  template change);
- order of elements of generated sets.

Checked with the trigger names normalised (`<e>_<...>__in` to `<e>__in`) and token multisets: ex10, ex12,
TurtleBot ddlf and ddlk have no other difference; Legitimate only the declarations of W; flat MakePlan only
the three reads of X (`get_image`, `get_imagesWithLight`, `get_imagesWithPoorLight`); d1 only AA and CC;
GasAnalysis t1 only CC; the running example candidate only Z and the Trans-core item above. No
unexplained difference.

## 8. Found while testing, not fixed

- An `if` statement in an entry action (toy model d3: `entry if size(s) > 0 then x = head(s) end`) makes
  RC2Z fail with `StackOverflowError`, on `main` 2976313, on 8c1a37c and on 3bf32cf. Not investigated.
- Functions of an imported package are not declared (`consts`) in the HOL-CSP theory: types are (W), functions
  are not. GasAnalysis a8 (functions in `Chemical.rct`): "Extra variables on rhs: "location", "goreq"". Same
  on 8c1a37c; on `main` the theory stops earlier (types, W).
- The LRE models fail in RC2Z ("Called feature 'name' on undefined object"), also on `main`. Not
  investigated.
- Quantifiers are not translated by `createExpr` anywhere (only the precondition case now gives a marker).

## 9. To be done in RoboTool by Fang (RoboTool 1.3 (composite-states), plugin 1.0.0.202611221803)

Before importing projects into the copy's workspace, switch off Project → Build Automatically. Right-click
the `.rct` → the IsaCircus compile command, for:

1. `robochart-models/composite-tests/c1_minimal.rct` ... `c6_nested.rct`, `c9_preconditions.rct`: three
   theories each, no dialog.
2. `c7_operation.rct`: three theories with the marker `GENERATOR_ERROR_missing_action_crtlMemory_...`;
   whether a dialog appears.
3. `c8_exit_input.rct`: the warning dialog naming A and B; the comment `(* GENERATOR WARNING ... *)` in the
   HOL-CSP and IsaCircus theories.
4. `c9b_precondition_quantifier.rct`: theories with `GENERATOR_ERROR_untranslated_precondition_half`.
5. `definedness-tests/d3_guarded_entry.rct`: expected an error dialog (M2M stack overflow) and an entry in
   `~/.isacircus/compile.log`.
6. `dti/enriched/Adaptation_Plan.rct` and `Adaptation_Legitimate.rct`; `DTI-simple-Plan-CaseStudy/Adaptation_Plan.rct`.
7. A template failure (partial output, INCOMPLETE): not reachable with these models; test only if wanted
   (needs a template changed on purpose).
8. The ITP models (TurtleBot ddlf and ddlk, running example, GasAnalysis, LRE), once the `.rct` files are
   there.

Then send me the folders `hol-csp_gen/` and `circus_gen/` (or tell me where they are): I compare them with
the dev-tools output of Sections 4 and 5 and load them in Isabelle.

## 10. Merge (not pushed)

MERGE_RESULT
