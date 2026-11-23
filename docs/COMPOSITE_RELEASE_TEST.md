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

CORRECTED by Section 11 (second search, 22 Nov 2026): the models of the running example and of LRE were
found, and the history of the GasAnalysis model; the statements below on these three are superseded.

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
| running example (SUPERSEDED, 11.1) | `RoboChart2Circus_development_repo/Abstract_examples/example_robot_DDLK.rct`, stm0 | Trans the same except one `Skip ;` removed by hand in the ITP theory; but the file is a controller `robot_ctrl_D` with two machines, the ITP theory names `robot_ctrl` |
| GasAnalysis | two candidates, neither the ITP model: `ChemicalDetector-autonomous8_GA_corrected/GasAnalysis.rct` (stm GasAnalysis, functions in the imported package Chemical, `gs` initially `<>`); `Test1/GasAnalysis/GasAnalysis.rct` (package Chem, stm GA, all in one file) | the ITP Trans sets `gs` to `[⦇c=0,i=0⦈]` and `anl` to `Front` on the initial transition and has an assume after the input trigger; neither candidate has this |
| LRE (SUPERSEDED, 11.1: not the ITP model) | `LRE_2D_coordToPair/lre_beh.rct`, `LRE/lre_beh.rct` | not generated: M2M fails ("Called feature 'name' on undefined object"), on `main` 2976313 as well |

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

CORRECTED by Section 11: the evidence below was a single version (FF) or models that are not the ITP
models (GG, HH). Section 11 has the check on all generator versions and the causes.

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

Local branch `release-composite`, created from `origin/main` 2976313 (fetched 22 Nov 2026; `main` has no
commit after 2976313), with `composite-states` merged by `git merge --no-ff`. No conflict: `composite-states`
descends from 2976313 (the merge base), so the tree after the merge is the tree of `composite-states`
(checked: `git diff composite-states release-composite` is empty). Build of the merged tree as the CI does
it, with `verify` instead of `install` (JDK 11, Maven 3.9.16, `mvn -B clean verify` on a `git archive`
export): BUILD SUCCESS, plugin 1.0.0.<build time>. NOT pushed, neither `main` nor `release-composite`.

What a push does (`.github/workflows/main.yml`): every push of any branch runs the job `build`
(`mvn clean install`, JDK 11). Only a push to `main` also deploys: the update site
`circus.robocalc.robochart.epsilon.isacircus.site/target/repository/` is copied by
UoY-RoboStar/ga-eclipse-deploy to robostar.cs.york.ac.uk under `robotool/robochart-isacircus-gen/`, and a
commit comment with the address is created. RoboTool installations that use that update site
(`.../robotool/robochart-isacircus-gen/update`, the source of the installed 1.0.0.202611071400) are then
offered the new version by their automatic update check.

## 11. Second search for the ITP models and re-check of FF, GG, HH (22 Nov 2026, evening)

Investigation only; nothing in any repository changed except this document and CLAUDE.md. Models taken with
`git archive` / `git show` from RoboChart2Circus_development_repo (no checkout); generator versions exported
to the scratchpad with `git archive`; all generation with dev-tools GenAll (its loader is the current
RctLoader for every version: for versions before 25e7662 this is NOT how they ran).

### 11.1 Where the ITP models are

RoboChart2Circus_development_repo has one branch (`main`) and 23 commits; the first, c0e67a0 (8 Feb 2026), is
a bulk import, so no state from December 2025 is in git. Relevant commits: c0e67a0 and 5e58d63 (12 May 2026,
two days after the ITP commit 59b534a). The Epsilon launch configurations and generated theories stored in
the repository give the history:

- Running example: launch configurations of 4 Sep 2025 read `Abstract_examples/example_robot.model` (EMF
  XMI, tracked since c0e67a0): controller `robot_ctrl`, machine `stm0`, states `detect`, `stop`, `move`,
  `i0`, `f0`. This is the ITP model. It has no `.rct` counterpart (`example_robot.rct` has other names and
  types, `example_robot_DDLK.rct` is a controller `robot_ctrl_D` with two machines). A textual equivalent
  written by hand from the XMI (scratchpad, not in a repository) gives, with the current generator, a
  Trans core equal to the ITP `Trans` token for token except one `Skip ;` that the ITP theory removed.
- GasAnalysis: launch configurations of 1 and 2 Dec 2025 read `Test1/GasAnalysis/GasAnalysis.rct`
  (Z model `GA_circus.model`); generated theories of 1 Dec 2025 to 16 Jan 2026 are stored in
  `Test1/GasAnalysis/circus_gen/hol-csp_gen/`. The ITP generator (11.3) on `Test1/GasAnalysis/GasAnalysis.rct`
  of c0e67a0 reproduces the stored generations of 5 Dec 2025 and 16 Jan 2026 exactly (Trans normalised, ratio
  1.000; the package and machine were renamed in between). The ITP theory descends from the stored
  `HOLCSP_GasAnalysis.thy`, which has `gs = <>; anl = Front` on the initial transition: a model state between
  1 and 5 Dec 2025 that is not in git. On top of that the ITP theory is HAND-EDITED (its own comment says so):
  `gs` initially `[⦇c=0,i=0⦈]`, the input trigger written `gas__in_NID_Reading`, an assume after the trigger,
  the sequential compositions moved under the assumes, the final state's share/interrupt branches dropped.
  No model reproduces it; the closest is `Test1/GasAnalysis/GasAnalysis.rct` of c0e67a0. Its imported
  packages: none besides the sequence toolkit (types and functions in the same file), so issue GG does not
  apply to the ITP GasAnalysis.
- LRE: launch configurations of 18 Nov 2025 read `LRE_2D_coordToPair/Circus-gen/lre_beh.model` (EMF,
  tracked since c0e67a0); the stored `Circus-gen/hol-csp_gen/HOLCSP_LRE_2D.thy` equals the ITP
  `Trans_LRE_Beh` except the hand-edited trigger (ratio 0.997); `Trans_LRE_Beh_core` was written from it by
  hand. GenAll cannot read XMI. `LRE_2D_coordToPair/lre_beh_complete.rct` (c0e67a0; moved to `Test1/` in
  5e58d63) is the same machine in one file: the ITP generator on it reproduces the stored generation except
  one read (`get_reqV?reqV` before `advVel!reqV`), which the EMF route lost because the variables of the
  EMF model still pointed to the interface (RC2Z comment "rct Vs EMF (1)"). `lre_beh.rct` (with `uses`/
  `requires` of interfaces in other files) is NOT the ITP model.

### 11.2 Regression with the new generator (d364c8c), the ITP invariants and the ITP scripts

SUPERSEDED by Section 12: these runs omitted the declarations that belong to the ITP proof scripts and did not
apply the ITP hand edits; with both, the running example, GasAnalysis and LRE are proved.

| Example | Model | Result |
|---|---|---|
| TurtleBot ddlf, ag_true, ddl | as Section 3 | proved / counterexamples found (Section 3) |
| running example, `Trans_ag_true_findcounterexample` | textual equivalent of `example_robot.model`, variant without the controller and with `dest` a constant of the machine (see below) | nitpick `[expect = genuine]` holds: counterexample found, as in the baseline |
| running example, `Trans_enriched_ddlf` | same, invariant at both assumes and at the guarantee of `set_pos` | NOT PROVED: `apply blast+` did not finish in 30 min (stopped by timeout). `deadlock_free'` leaves 5 goals; 1-3 are the guard coverage, 4-5 are `DFI ⊑ Trans⋅NID_move ❙; enter_detect → ...`: the fallback `aviol → Trans⋅n` of the inner assume is followed by the rest of the transition, because the generator puts the assume inside the sequential composition. The ITP theory removed that `Skip ;` by hand, the "manual step" its GasAnalysis comment describes. The ITP generator (a8177dc) prints the same `Skip ;`, so this is not a regression |
| GasAnalysis (ADAPTED: closest model, ITP invariant `size(gs) > 0` at the reads of gs before `location(gs)` and at the guarantee of `set_anl`) | `Test1/GasAnalysis/GasAnalysis.rct` of c0e67a0 | NOT PROVED: 2 goals left after the ITP script, of the same shape (`Trans⋅NID_GasDetected ❙; ...` after the fallback). The ITP's own hand restructuring is what made its proof go through |
| LRE (`Trans_ddlf` on the core) | `lre_beh_complete.rct` of c0e67a0, with ONE hand correction: the type of `CDA` (issue II below; the ITP theory has the same correction) | NOT PROVED: `using atLeastAtMost_iff apply blast+` did not finish in 30 min. `deadlock_free'` leaves 12 goals of the form `DFI ⊑ (guard) ❙& ...` that it did not decompose. Differences to the ITP core: the extra read of `reqV` with its assume (EMF/rct, 11.1), the order of the reads of `pos` and `vel`, the trigger written as generated. Not analysed further |

Without the variant the running example does not load: the controller version declares `get_obs` twice
(controller variable and interface variable; DD); a machine without controller does not declare the constant
`dest` of its required interface (new finding, JJ below). The machine itself is unchanged in the variant.

So of the ITP proofs only TurtleBot ddlf goes through unchanged; the other three depend on hand edits of the
generated text that the ITP generator also needed (moving sequential compositions under the assumes; the type
of CDA; the GasAnalysis rewrite). The counterexample results (running example, TurtleBot ag_true and ddl) are
the same as in the baseline.

### 11.3 Generator versions

| Name | What | Source |
|---|---|---|
| ITP | rules of December 2025 to May 2026 | RoboChart2Circus_development_repo `Test1/` (RC2Z.eol, circusM2T_HOLCSP.egl, circusM2T.egl, CircusM2T.egx, circus_metamodel.ecore) at c0e67a0 and at 5e58d63; the generator repository starts on 24 May 2026 (a8177dc) with these rules (RC2Z.eol differs from 5e58d63 in 16 lines, the HOL-CSP template in 1, the EGX in the output names) |
| a8177dc | first commit of the generator repository | |
| 2fba5ea | RoboTool 1.3's plugin 1.0.0.202610092009 | |
| 2976313 | main, published 1.0.0.202611071400 | |
| 8c1a37c | composite-states before the CC fix | |
| d364c8c | composite-states, latest | |

The rules of December 2025 themselves (before c0e67a0) are not available.

### 11.4 FF: `if` without `else` in a state action

Models: d3; minimal ff1 (one state, `entry if x > 0 then x = 1 end`); ff2 (with `else`); ff3 (the same `if`
in a transition action); ff4 (`entry x = 1 ; if x > 0 then x = 2 end`).

| | ITP (c0e67a0, 5e58d63) | a8177dc | 2fba5ea | 2976313 | 8c1a37c | d364c8c |
|---|---|---|---|---|---|---|
| d3, ff1, ff4 | fail | fail | fail | fail | fail | fail |
| ff2, ff3 | generate | generate | generate | generate | generate | generate |

Real, in every version since the ITP rules; NOT introduced by composite-states and not related to AA. Cause
(stack trace and reading, confirmed by a patched scratch copy): Rule 50 `vars(stmt)` for an `IfStmt` computes
`vars(stmt.then).includingAll(vars(stmt.else))`; without `else` the argument is undefined, EOL dispatches
`vars(undefined)` to the Rule 46 overload `vars(nodeset)`, whose `for(n in nodeset) ... vars(n)` calls itself
again: StackOverflowError (reported as "Unknown reason" or StackOverflowError). Rule 50 is reached through
Rule 48 (variables of the actions of a state, for the write synchronisation), not for transition actions,
hence ff3 works. With `if(stmt.else.isDefined())` around the second part (as createIfStmtAsAction already
does) d3 and ff1-ff4 generate (scratch copy only). Suggested fix: that guard; optionally make Rule 46 reject
an undefined argument.

### 11.5 GG: functions of an imported package

Minimal model: package `gglib` with `function noBody(n : int) : int { }` and `function withBody(n : int) :
int { precondition n > 0 postcondition result == n + 1 }`; package `gg` imports it and calls both. All
versions generate; no version declares either function (`consts` only for functions of the selected file's
package: `createFunctionDefsAsAxParas(pkg)`, which also skips a function named `card`). Isabelle: d364c8c
"Extra variables on rhs: "withBody", "noBody""; 2976313 the same and `"n"` (the formal of the CC guard before
its fix). The same for GasAnalysis of `ChemicalDetector-autonomous8_GA_corrected` ("location", "goreq").
Real, in every version; NOT introduced by composite-states (W, on composite-states, declares the types of
imported packages, not the functions). Does not affect the ITP GasAnalysis (one file). Suggested fix: collect
the functions of the non-toolkit packages the selected file reaches, as `typeDeclsToTranslate` does for types,
with the same name-clash check.

### 11.6 HH: required interface declared in another file

Minimal models: hha (interface `I` with `var v : int` in another file and package, machine `requires I`),
hhb (the same with `var v : int = 3`), hhc (interface in the same file); and `lre_beh.rct`.

| | a8177dc | 2fba5ea | 2976313 | 8c1a37c | d364c8c |
|---|---|---|---|---|---|
| hha, lre_beh.rct | fail | fail | fail | fail | fail |
| hhb, hhc | generate | generate | generate | generate | generate |

Real, in every version run through the current loader, and NOT introduced by composite-states. Cause (probe in
a scratch copy): RctLoader (since 25e7662, 5 Nov 2026, issue C) gives RC2Z a copy of the selected file in a
resource of its own (`memory:/<file>`, in a resource set of its own); objects of the other loaded files are
reached through references but are not owned by the RC model (`RC.owns(...)` false). EOL's `type()` is
undefined for them, so `defaultValueAsZExpr` (`self.type().name`, for a variable without initial value) fails
with "Called feature 'name' on undefined object"; the ITP rules fail at another `type()` /
`RC!Variable.all()` (shared array variable). Before 25e7662 the plugin loaded the selected file alone (GenAll
`single` on 2fba5ea and 2976313: fails as well, with unresolved references, issue C), so a required interface in
another file never worked in the plugin. The ITP LRE was generated from the EMF `lre_beh.model`, so the ITP
did not meet it. Suggested fix: give the RC model all loaded resources (or keep the selected resource in the
loader's resource set and make the model expand to it) instead of the lone copy, then check that
`RC!RCPackage.all()` and the other `.all()` (60 places) still select what they should; or, narrower, replace
EOL `type()` by `eClass().name` in RC2Z (45 uses).

### 11.7 Further findings (not fixed)

- II. Function types with three or more parameters: the parameter product is printed nested to the left,
  `((A×B)×C) ⇒ R`, while calls are printed as flat tuples `(a, b, c)`, which Isabelle reads as `A×(B×C)`:
  type error at the first call (LRE: `CDA`). Introduced by 9faa957 (9 Oct 2026, "Extend RC2Z and IsaCircus/
  HOL-CSP templates for DTI ..."); before (ITP rules, a8177dc, 8a019bf, a8d4304) the type was printed
  without parentheses, `real×real×(real×real list)×real×real`, also wrong (the components of a product
  parameter run together, `real×real list` is `real × (real list)`). The ITP theory has the type corrected by
  hand. Two parameters are right. Suggested fix: print the parameter types as a right-nested product with
  each component parenthesised, matching the call.
- JJ. A machine without controller that requires an interface with a constant does not declare the constant
  (running example variant: "Extra variables on rhs: "dest""); with the controller the controller declares it,
  but then the variable of the interface is declared twice (DD).
- The `Skip ;` / sequential composition after an assume (11.2) is the generator's form since the ITP rules;
  the deadlock-freedom scripts of the ITP need it moved under the assume by hand.

## 12. Baseline of the ITP 2026 examples with the hand edits of the paper (22-23 Nov 2026)

Read-only experiment in the scratchpad; the generated text is that of composite-states d364c8c (Trans-core
theories); the invariants and proof scripts are those of 59b534a; library RC_HOL-CSP of AG-dev.

### 12.1 Running example model

`Abstract_examples/example_robot.rct` of RoboChart2Circus c0e67a0 (controller `robot_ctrl`) is NOT the model of
`example_robot.model` (XMI): the `.rct` has machine `ObsDetect`, states `Detect`, `Stop`, `Move`, `dest` and
`pos` of type `real`, the controller's `obs` initialised to 0; the XMI has machine `stm0`, states `detect`,
`stop`, `move`, `dest` and `pos` of type `nat` (core type 0), no initial value. The transitions are the same.
The ITP theory (`dest :: nat`, `NID_detect`, `internal_stm0`) follows the XMI. The regression therefore keeps
the textual equivalent of the XMI (Section 11.1), in the variant without controller and with `dest` a constant
of the machine (DD, JJ).

### 12.2 Hand edits of the ITP theories against the generation of their time

Generation of the time: running example, the ITP rules (RoboChart2Circus `Test1/` at 5e58d63) on the textual
equivalent of the XMI; GasAnalysis, the stored `Test1/GasAnalysis/circus_gen/hol-csp_gen/HOLCSP_GasAnalysis.thy`;
LRE, the stored `LRE_2D_coordToPair/Circus-gen/hol-csp_gen/HOLCSP_LRE_2D.thy`. Common to all three (not
listed again): the Trans core written by hand from the full Trans (share, interrupt and the final state's
branches removed; the generator has a Trans-core template since 8a019bf, 18 Jun 2026); the assume fallback
`Trans_tbd` (running example) or a copy of the body (GasAnalysis, LRE) replaced by `Trans⋅n`; names without
the container suffix; the invariants in the assumes and guarantees.

| | Edit | Still needed on d364c8c? |
|---|---|---|
| E1 | running example, t2 (`move` to `detect`): `(... (guar ... (set_pos!(pos + 1) → Skip)))) ❙; (enter_detect → Trans⋅NID_detect)` written `... (set_pos!(pos + 1) → (enter_detect → Trans⋅NID_detect))`: the rest of the transition moved under the assume and the guarantee, `→ Skip ❙;` removed | YES: without it `deadlock_free'` leaves two goals `DFI ⊑ Trans⋅NID_move ❙; enter_detect → ...` (the fallback of the inner assume followed by the rest) and the script fails |
| G1 | GasAnalysis, initial transition: `set_gs![]` written `set_gs![⦇c=0,i=0⦈]` | NO (proved without it) |
| G2 | GasAnalysis, trigger `gas__in.NID_Reading?gs` written as a channel of its own `gas__in_NID_Reading?gs` (declared by hand); the ITP comment: HOL-CSP did not accept the generated form | NO: the generator prints `(gas__in NID_Reading)?gs` (curried channel, 4104fb4) and the proof goes through |
| G3 | GasAnalysis, after the trigger: an assume `size(gs) > 0` (with `guar` on `set_gs`) added, and the rest of the transition (exit, exited, enter) moved under it | YES |
| G4 | GasAnalysis, `anl = location(gs); turn!anl`: invariant at the read of gs, and the rest of the action and `❙; enter_Reading → ...` moved under that assume | YES |
| G5 | GasAnalysis, final state written `terminate → Skip` | done by the Trans-core template |
| L1 | LRE, trigger `reqVel__in.NID_OCM?reqV` written as a channel of its own `reqVel__in_NID_OCM?reqV` (declared by hand) | NO (proved without it; curried channel) |
| L3 | LRE, `consts CDA` with the parameter type right-nested | YES on d364c8c (issue II); fixed in Section 13 |

Part of the PROOF SCRIPTS, not edits of the generated text, and needed: the declarations before the lemma
(running example `declare GlobalDet_Guard [normalisation]`; LRE `declare Det_Guard_to_GlobalDet
[normalisation] and Guard_Det_to_GlobalDet [normalisation]`). Section 11.2 had run the scripts WITHOUT them; the
timeouts reported there for the running example and LRE come from that omission (with E1 alone, the three
coverage goals left for the running example made `blast+` run until the timeout), and the statement there that
the scripts fail on the new output is withdrawn.

### 12.3 Results (d364c8c, the edits marked "still needed", the ITP invariants, declarations and scripts)

| Example | Edits applied | Result |
|---|---|---|
| TurtleBot ddlf | none | PROVED (Section 3) |
| TurtleBot ag_true, ddl | none | counterexample found, as the baseline (Section 3) |
| running example `Trans_enriched_ddlf` | E1 | PROVED (0:34) |
| running example `Trans_ag_true_findcounterexample` | E1 (none needed) | counterexample found (nitpick, genuine), as the baseline |
| GasAnalysis `Trans_stm4_core_ddlf_auto` (closest model, `Test1/GasAnalysis/GasAnalysis.rct` of c0e67a0) | G3, G4 (also proved with G1-G4) | PROVED (1:33) |
| LRE `Trans_ddlf` (`lre_beh_complete.rct` of c0e67a0) | L3 (also proved with L1 and L3) | PROVED (1:17) |

### 12.4 Differences of the new output from the ITP output, and their effect

- LRE reads `reqV` once more (an outer read before the two statements of the OCM action, each of which also reads
  it). The outer read is that of the action as a whole (Rule 62, readState of usedVariables); the ITP output
  lacked it because it was generated from the EMF model, whose statements still referred to the interface
  variables (RC2Z comment "rct Vs EMF (1)"). A correct change; no effect on the proof.
- LRE reads `vel` before `pos` at the head of Trans (ITP: `pos` before `vel`). The order of the elements of a
  generated set; no meaning; no effect on the proof.
- GasAnalysis reads `gs` and `anl` once for the action `anl = location(gs); turn!anl` and again in each
  statement. Already so in the ITP rules (generation of 5 Dec 2025); no effect on the proof once G4 puts the
  invariant at the inner read.
- Triggers are curried channels `(e__in NID_X)?x` (4104fb4, 4 Nov 2026); the ITP needed G2, L1 by hand. A
  correct change (a fix); the hand edits are no longer needed.
- The sequential composition after an assume (E1, G3, G4): the generator's form since the ITP rules; NOT a
  regression. Moving it under the assume is the remaining manual step (to-do after the release: generate that
  form, CLAUDE.md).
- CDA (L3): issue II, a regression of 9faa957 against the (also wrong) form of the ITP rules; fixed below.
No difference is a regression of the proofs except II.
