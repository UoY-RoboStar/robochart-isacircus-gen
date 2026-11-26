# Plan: HOL-CSP generation of the controller and module level (24 Nov 2026)

Branch `controller-layer`, from main f242344 (release of composite-states, plugin 1.0.0.202611232203). PLAN ONLY:
no code is changed in this step. Background: `docs/CONTROLLER_MODULE_STATUS.md` (Sections 1-5). Rules are those of
Chapter 4 of the RoboChart reference manual (August 2022 PDF; `chapter4.tex` of `~/Git/robochart-reference`).
CSP-M = RoboTool's generator, `~/Git/robochart-csp-gen` a6412b4 (untimed `ControllerGenerator.xtend`,
`RenamingStateMachineChanset` and `composeStateMachines`; comp/untimed `CControllerGenerator.xtend` `compileAux`).
RC2Z line numbers are those of main f242344.

Summary. Three things block the controller and module level today: (1) the event channels and the interface
variable channels are not named per component, so a controller and its machines declare the same constructor twice
(c7 `a_in`; running example `get_obs`, `set_obs`); (2) the HOL-CSP templates print the process paragraphs of a
controller or module in IsaCircus syntax, so the theory cannot be processed, and the composite-state ordering and the
generator notes exist only in the machine-only branch; (3) the controller and module rules of RC2Z (Rules 1-23,
present and running since the first commit) have never been loaded in Isabelle and contain at least sixteen known
defects. The plan: name the channels per component as the manual's `vid(v)`, `vid(v, m)`, `eventId(e)` (outermost
component unqualified, so models without controller keep their output), print every controller and module paragraph
as a fixrec of type `chan_event process` in the existing `locale actions` through one shared printing function, fix
the defects rule by rule, and test against RoboTool's CSP-M with FDR.

## 1. Channel naming

### 1.1 Principle

NAMING PRINCIPLE (Fang, 25 Nov 2026; also SEMANTICS_DIFFERENCES.md 20): the channel names need not agree literally
with the CSP-M, which uses modules as name spaces, while the channels of HOL-CSP and Circus are global. They must be
unique (as the manual requires of `vid`, `eventId`, `id`) and correspond injectively to the events of the CSP-M,
with the same behaviour under that correspondence. FDR comparisons with RoboTool's CSP-M rename by this
correspondence and then check refinement.

DECIDED (Fang, 25 Nov 2026): variant (ii) of 7.10. REWRITTEN 25 Nov 2026; the scheme of 24 Nov 2026 (names qualified
INSIDE the machine paragraphs, variant (i)) is withdrawn.

- The semantics of a machine does not depend on where it is used (manual Rule 25): the paragraphs of a machine in a
  controller are printed exactly as for the machine alone, up to its FQN (the instance's FQN, `m1_C_ctl2`, which
  already qualifies its nodes, triggers and local variables). Its event channels (`done_in`), its copy of a
  required variable (`set_EXT_obs`), `sharedMemory_obs` and the channel sets keep their names.
- Uniqueness is made at the composition, by the renaming of each machine instance m (`renamingMachine_<m>`, Rule 22):
  an event connected to the controller becomes the controller's event (`go_in` to `start_in`); the target of a
  connection from machine src becomes the source's event, qualified by the source (`done_in` of m2 to
  `done_m1_C_ctl2_out`); the source of such a connection is qualified by itself (`done_in` of m1 to
  `done_m1_C_ctl2_in`); the copy `set_EXT_v` becomes `set_EXT_v_<FQN m>`, written by the controller memory. These are
  the CSP-M's `stm::f <- e`, `tgt::f.in <- src::e.out`, `src::e`, `m::set_EXT_v` under the injective correspondence
  `m::e` ↦ `e_<FQN m>`, `C::e` ↦ `e` (the controller is the outermost component and keeps unqualified names). The
  pair for `set_EXT` is not in the manual's Rule 22 (SEMANTICS_DIFFERENCES 22, to ask Alvaro).
- The events of a machine that are not connected are hidden in `renamingMachine_<m>`, the internal connection events
  in `composeMachines_<c>` (as the CSP-M, SEMANTICS_DIFFERENCES 23).
- Every channel is declared once, at its first declaration (the templates; a shared name such as `set_obs`, `done_in`,
  `aviol` is one constructor); a paragraph with the same name and the same text (`sharedMemory_<v>`) is created once
  (RC2Z).
- Constants: `consts` at both levels, no `set_` events (decision 7, SEMANTICS_DIFFERENCES 21).

So a model without controller and without module produces the same channels as before.

### 1.2 Per kind of channel (before → after)

Example names: controller `C` in package `ctl2`, machines `m1`, `m2` in it (FQN `m1_C_ctl2`), event `done`,
interface variable `obs`, machine variable `k`.

| Channel | Today | After, machine without controller | After, inside a controller | Manual |
|---|---|---|---|---|
| machine event | `done_in`, `done_out` | unchanged | unchanged inside the machine; after `renamingMachine_<m>`: the controller's event, or `done_<src FQN>_in/out` for an internal connection, or hidden (not connected) | `eventId(e).in/.out` |
| controller event | `start_in`, `start_out` | — | unchanged if the controller is outermost; `start_C_mod_in` inside a module | `eventId(e)` |
| platform / module event | `e_in`, `e_out` | — | unchanged (module outermost) | `eventId(e)` |
| trigger channel | `<e>_<container FQN>__in` | unchanged | unchanged (the FQN already contains the controller) | `eventId(e)__.id(src).in` |
| machine variable | `get_/set_/setL_/setR_<v>_<m FQN>` | unchanged | unchanged | `vid(v)` |
| machine's required variable: write | `set_<v>` | unchanged | `set_<v>` of the providing context (controller), declared once | `set_vid(v)` |
| machine's required variable: read (inside the machine, hidden there) | `get_<v>` | unchanged | `get_<v>`, declared once (hidden in each machine, so no synchronisation between machines) | `get_vid(v)` |
| copy for machine m | `set_EXT_<v>` | unchanged | `set_EXT_<v>` inside the machine, renamed to `set_EXT_<v>_<m FQN>` by `renamingMachine_<m>` | `set_EXT_vid(v)`, `set_Ext_vid(v, m)` |
| controller variable | `get_<v>`, `set_<v>` | — | unchanged if outermost; `<v>_<C FQN>` inside a module | `vid(v)` |
| controller's required variable (from the module) | `set_EXT_<v>` | — | `set_EXT_<v>_<C FQN>` | `set_Ext_vid(v, c)` |
| operation call (undefined operation) | `<op>Call` | unchanged | the machine's `<op>Call` is the controller's (CSP-M `stm::oCall <- oCall`), declared once | `o.nameCall` |
| defined operation | not translated | — | a fixrec `op_<op FQN>`, no channel (Section 2) | `nproc(op)` |
| constants | `consts <c>_<m FQN>` (machine), bare in a controller | unchanged; required constants of a machine NOW declared (`consts <c>`, fixes JJ) | declared once, by the providing context | `set_vid(c)` (constants as events) |
| `aviol`, `gviol`, `share`, `terminate` | per machine | unchanged | declared once | (`end` = `terminate`) |
| node, flow, internal channels | `enter_<n>`, `exit_<nc>`, `internal__<nc>` ... | unchanged | unchanged (already FQN) | `enter.id(n)` ... |

Constants: the generator translates constants as uninterpreted `consts` (every value; Rules 32, 34, 36, 38 not
implemented, RC2Z comment at Rule 31), so the `set_` events of constants in Rules 15, 16, 19-23 (`set_vid(v) ← set_vid(v,c)`,
`rconsts`, `constInit`) are not generated; a required constant is the same `consts` as the providing one. Recorded as
a convention in SEMANTICS_DIFFERENCES.

### 1.3 Effect on the existing output

Models without controller and module (c1-c6, c8, c9, c9b, d1-d3, the flat and full Adaptation_Plan, ex10, ex12,
Legitimate, the ITP Trans-core theories, the regression tests): no channel changes its name. Two changes in the
generated text are possible and both only for models that do not load today: (a) required constants of a machine
without controller are declared (JJ; e.g. a variant of the running example with `dest` in the interface); (b) a
duplicate declaration, if any existed, disappears. Checked by the usual token comparison against f242344 at every step.
Not avoidable: models WITH a controller change their names (they do not load today: c7, the running example
`robot_ctrl`, `example_robot_DDLK`, DTI `Adaptation.rct`). The IsaCircus theory (same Z model) changes in the same way;
the list goes to Arjun (as `docs/ISACIRCUS_CHANGES_trigger-fqn-naming.md`).

Where in RC2Z: `createchanParaNC` (3934-; event channels 4128 `e.name + "_in"`, shared variable channels 4044),
`createchanParaCtrl` (2267-), `createchanParaModule` (1299-), one new operation `eventChan(e, context)` used
everywhere a name of an event channel is built (as `triggerChan` for triggers), one `vidChan(v, m)`; the declaration
loop of the three templates (circusM2T_HOLCSP.egl 193-203 and the copies) or, better, RC2Z when it adds the
ChannelParas to the section (one place), deduplicates.

## 2. HOL-CSP printing of Rules 1-23

### 2.1 Form

Every paragraph is a `fixrec ... :: "... chan_event process"` with `[simp del]`, inside the one `locale actions` of
the theory, as the machine paragraphs today (non-recursive paragraphs as fixrecs too, for uniform `.simps` names). The
IsaCircus `locale <proc> ... actions is ... cProcess` block (circusM2T_HOLCSP.egl 223-245) is not printed by the
HOL-CSP templates any more; the IsaCircus template keeps it.

| Rule | Paragraph | HOL-CSP |
|---|---|---|
| 25 (exists) | `stm_<m>` | as today |
| 27 operation definition | `op_<op FQN>` | `fixrec op_X :: "T1 \<rightarrow> ... \<rightarrow> chan_event process"` (parameters as arguments, the body as a machine whose parameters are initial values of its variables, as `[[opdef]]STM`) |
| 19 required operation | — | inline `<op>Call\<^bold>!(args) \<rightarrow> Skip` (as today for undefined operations) |
| 22 renamingMachine | `renamingMachine_<m>` | `(stm_<m> \<lbrakk>\<lbrakk>R\<rbrakk>\<rbrakk>)`, R by the existing relation printer (Section 4) |
| 23 renStmEvts | — | a set expression, used in 21 (no paragraph) |
| 21 composeMachines | `composeMachines_<c>` | `(renamingMachine_<m1> \<lbrakk> connevts \<rbrakk> (renamingMachine_<m2> \<lbrakk> ... \<rbrakk> ...))`, right-nested as the rule |
| 20 ctrlMemory | `Memory_<v>_<c>` per variable, `ctrlMemory_<c>` | one fixrec per variable `Memory_<v>_<c> :: "T \<rightarrow> chan_event process"` (set_v?x, then the copies set_EXT_v_m!x for each machine m that requires v, in sequence, then Memory\<cdot>x; \<box> terminate \<rightarrow> Skip), `ctrlMemory_<c> = Memory_<v1>_<c>\<cdot>init1 \<lbrakk>{terminate}\<rbrakk> ...`, as the machine memory (Rules 31, 35) is generated today; no variables: `terminate \<rightarrow> Skip` |
| 19 controller | `controller_<c>` | `(((composeMachines_<c> \<lbrakk> S \<rbrakk> ctrlMemory_<c>) \ H) \<Theta> e \<in> {terminate}. Skip)` |
| 18 singleBuffer | `Buffer_<conn>` | typed: `fixrec Buffer_X :: "T list \<rightarrow> chan_event process" where Buffer_X\<cdot>s = (efrom_out\<^bold>?x \<rightarrow> Buffer_X\<cdot>[x]) \<box> ((s \<noteq> []) \<^bold>& (eto_in\<^bold>!(hd s) \<rightarrow> Buffer_X\<cdot>[]))`; untyped: a bool parameter |
| 17 buffer | `buffer_<conn>` | `Buffer_X\<cdot>[]` or, for `mult`, `Buffer_X\<cdot>[] ||| Buffer_Y\<cdot>[]` |
| 12 modMemory | `Memory_<v>_<mod>`, `modMemory_<mod>` | as Rule 20 with `set_EXT_<v>_<c FQN>` for each controller requiring v |
| 13 constInit | — | not generated (constants as `consts`, Section 1.2) |
| 15 renamingController | `renamingController_<c>` | `(controller_<c> \<lbrakk>\<lbrakk>R\<rbrakk>\<rbrakk>)` |
| 16 renCtrlEvts | — | a set expression, used in 14 |
| 14 composeControllers | `composeControllers_<mod>` | right-nested `\<lbrakk> connevts \<rbrakk>` |
| 2, 3 hiddenModuleChannels, memoryChannels | `definition ... :: "chan_event set"` | as the channel sets today |
| 1 module | `module_<mod>` | `((((\<interleave>buffers) \<lbrakk> evasyncs \<rbrakk> (modMemory_<mod> \<lbrakk> memoryChannels \<rbrakk> composeControllers_<mod>)) \ hiddenModuleChannels) \<Theta> e \<in> {terminate}. Skip) \ {terminate}` |

Exception `\<Theta>`: HOL-CSPM's `Throw` (`P \<Theta> a \<in> A. Q`, Throw.thy), available through HOL-CSP_RS. RC2Z
deliberately has no exception in the Z metamodel ("option 2", RC2Z header 72-77: push `terminate` into the processes,
as the machine level does today). Two options, to be decided by Fang: (a) a new Z node for the exception (a CZT
deviation, documented as `generatorNote` was) and `Throw` in the HOL-CSP templates; (b) option 2 as at the machine
level. Recommendation (a): it is the manual's form and the CSP-M's (`[|{|terminate|}|> SKIP`), checkable with FDR.

Sets S and H of Rule 19: the manual has `S = lvars \<union> rvars \<union> lconsts \<union> rconsts`,
`H = lvars \<union> rvars \<union> lconsts`; the CSP-M (`compileAux`) synchronises on `set_v` of the controller's
variables AND on the copies `stm::set_EXT_v` of the machines, and hides `set_v`, `get_v` and the copies. Without the
copies in S the machines' `set_EXT` events would not synchronise with the memory; the CSP-M's form is taken
(SEMANTICS_DIFFERENCES, new entry). The CSP-M also hides the events of a machine that are not connected
(`\ {| stm::e |}` in `RenamingStateMachineChanset`); the manual does not. Decision needed (Fang); the plan follows
the CSP-M, as elsewhere when they differ, and records it.

### 2.2 Example

Model `ctl2` (to be written in step 2 and checked with the RoboChart validator first; a sketch):

```
interface IObs { var obs : int }
stm M1 { requires IObs  event go  event done : int
         initial i0  state A { }  state B { }
         transition t0 { from i0 to A }
         transition t1 { from A to B trigger go action done ! obs }
         transition t2 { from B to A } }
stm M2 { requires IObs  event done : int  event ack  var k : int = 0
         initial i0  state W { }
         transition t0 { from i0 to W }
         transition t1 { from W to W trigger done ? k action obs = k ; ack } }
controller C { var obs : int = 0  event start  event ack
               sref m1 = M1  sref m2 = M2
               connection C on start to m1 on go
               connection m1 on done to m2 on done
               connection m2 on ack to C on ack }
```

Expected HOL-CSP (abridged; FQNs `m1_C_ctl2`, `m2_C_ctl2`, `C_ctl2`):

```
datatype chan_event = "share" | "terminate"
  | "start_in" | "start_out" | "ack_in" | "ack_out"
  | "go_m1_C_ctl2_in" | "go_m1_C_ctl2_out" | "done_m1_C_ctl2_in" "int" | "done_m1_C_ctl2_out" "int"
  | "done_m2_C_ctl2_in" "int" | "done_m2_C_ctl2_out" "int" | "ack_m2_C_ctl2_in" | "ack_m2_C_ctl2_out"
  | "get_obs" "int" | "set_obs" "int" | "set_EXT_obs_m1_C_ctl2" "int" | "set_EXT_obs_m2_C_ctl2" "int"
  | ... (machine variables, node, flow, trigger channels as today) | "aviol" | "gviol"

locale actions
begin
  ... the paragraphs of m1 and m2 as today (one shared printing function, 2.3), ending with stm_m1_C_ctl2, stm_m2_C_ctl2 ...

fixrec renamingMachine_m1_C_ctl2 :: "chan_event process" where
[simp del]: \<open>renamingMachine_m1_C_ctl2 =
  (stm_m1_C_ctl2
   \<lbrakk>\<lbrakk>(\<lambda>rn_a rn_b. (rn_a = go_m1_C_ctl2_in \<and> rn_b = start_in)
      \<or> (rn_a = go_m1_C_ctl2_out \<and> rn_b = start_out)
      \<or> (\<not> (rn_a = go_m1_C_ctl2_in) \<and> \<not> (rn_a = go_m1_C_ctl2_out) \<and> rn_b = rn_a))\<rbrakk>\<rbrakk>)\<close>

fixrec renamingMachine_m2_C_ctl2 :: "chan_event process" where
[simp del]: \<open>renamingMachine_m2_C_ctl2 =
  (stm_m2_C_ctl2
   \<lbrakk>\<lbrakk>(\<lambda>rn_a rn_b. (\<exists>rn_v1. rn_a = (done_m2_C_ctl2_in rn_v1) \<and> rn_b = (done_m1_C_ctl2_out rn_v1))
      \<or> (\<exists>rn_v1. rn_a = (done_m2_C_ctl2_out rn_v1) \<and> rn_b = (done_m1_C_ctl2_in rn_v1))
      \<or> (rn_a = ack_m2_C_ctl2_in \<and> rn_b = ack_in) \<or> (rn_a = ack_m2_C_ctl2_out \<and> rn_b = ack_out)
      \<or> (... \<and> rn_b = rn_a))\<rbrakk>\<rbrakk>)\<close>

fixrec composeMachines_C_ctl2 :: "chan_event process" where
[simp del]: \<open>composeMachines_C_ctl2 =
  (renamingMachine_m1_C_ctl2
   \<lbrakk> ({terminate} \<union> range done_m1_C_ctl2_in \<union> range done_m1_C_ctl2_out) \<rbrakk>
   renamingMachine_m2_C_ctl2)\<close>

fixrec Memory_obs_C_ctl2 :: "int \<rightarrow> chan_event process" where
[simp del]: \<open>Memory_obs_C_ctl2\<cdot>value__v =
  ((set_obs\<^bold>?x__v \<rightarrow> ((set_EXT_obs_m1_C_ctl2\<^bold>!x__v \<rightarrow> Skip) \<^bold>; (set_EXT_obs_m2_C_ctl2\<^bold>!x__v \<rightarrow> Skip))
      \<^bold>; Memory_obs_C_ctl2\<cdot>x__v)
   \<box> (terminate \<rightarrow> Skip))\<close>

fixrec ctrlMemory_C_ctl2 :: "chan_event process" where
[simp del]: \<open>ctrlMemory_C_ctl2 = Memory_obs_C_ctl2\<cdot>0\<close>

fixrec controller_C_ctl2 :: "chan_event process" where
[simp del]: \<open>controller_C_ctl2 =
  (((composeMachines_C_ctl2
     \<lbrakk> ({terminate} \<union> range set_obs \<union> range set_EXT_obs_m1_C_ctl2 \<union> range set_EXT_obs_m2_C_ctl2) \<rbrakk>
     ctrlMemory_C_ctl2)
    \ (range set_obs \<union> range get_obs \<union> range set_EXT_obs_m1_C_ctl2 \<union> range set_EXT_obs_m2_C_ctl2))
   \<Theta> e \<in> {terminate}. Skip)\<close>
end
```

(m1's `done` is the source of the internal connection, so m1 is not renamed on it; m2's `done` is renamed to m1's, the
manual's `eventId(e.eto).in \<leftarrow> eventId(e.efrom).out` and the CSP-M's `tgt::e.in <- src::e.out`. With the short notation
of branch renaming-short the relations would read `ren_else [ren_chan done_m2_C_ctl2_in done_m1_C_ctl2_out, ...]`.)

### 2.3 One printing function for both paths

Today circusM2T_HOLCSP.egl prints the machine paragraphs only in the branch `procparas.size()=0` (246-360): the Trans
fixrecs, then per node container (innermost first, `containerDepth`, 15 Nov 2026) the node paragraphs, with
RunEntered, During, Behaviour of a composite state as separate fixrecs before the node and the generator note
(`generatorNoteComment`) before them, then the remaining paragraphs. Change:

1. move that body into one EGL operation `printActionParas(actparas: Sequence)` (uses `out.print`, as the body does
   now) in a shared file `holcspPrint.eol`, imported by circusM2T_HOLCSP.egl (and the Transc part into
   `printTranscParas` for circusM2T_HOLCSP_trans_core.egl, which today prints one `locale trans_core_action` per
   machine of a controller; one locale for all);
2. machine-only models: the branch calls `printActionParas` with all ActionParas of the section (same output);
3. controller/module models: RC2Z adds the ActionParas to the section as in the machine-only case (RC2Z 463-467; the condition is dropped) and keeps the ProcessPara for the IsaCircus template; the HOL-CSP
   template prints `printActionParas` over all ActionParas, followed by the controller and module paragraphs in
   dependency order (operations before the machines that call them; memories, renamingMachine, composeMachines,
   controller; buffers, modMemory, renamingController, composeControllers, module);
4. the Basic Process of a controller collects only its own paragraphs (today `Z!ActionPara.all()`, RC2Z 2238-2246:
   with two controllers each takes all paragraphs).

## 3. Known defects of the M2M part and their fixes

From `CONTROLLER_MODULE_STATUS.md` Section 2.3, re-read on main f242344, plus three found for this plan (13-15).

| # | Defect | Where (f242344) | Fix |
|---|---|---|---|
| 1 | event channels not unique per component (c7 `a_in` twice) | createchanParaNC 4128, createchanParaCtrl 2346, createchanParaModule 1312 | Section 1 (`eventChan`) |
| 2 | old IsaCircus event syntax `e\<cdot>in` in Rules 15 and 22 | renamingController 1873-1879, renamingMachine 2744-2750 | build the pairs with `eventChan(e, ...)` + `_in`/`_out`; typed events become prefix pairs (the relation printer binds the value, issue G) |
| 3 | synchronisation sets with names that are not channels (`arrival`, `set_dest`) | renStmEvts 2775-2800, renCtrlEvts 1903- | `range eventChan_in ∪ range eventChan_out` (or the constants for untyped events); no `set_` of constants (Section 1.2) |
| 4 | `crtlMemory_<c>` (paragraph) vs `ctrlMemory_<c>` (recursive calls) | 2408 vs 2501, 2505, 2584, 2588 | one name `ctrlMemory_<c>`; per-variable cells (Section 2.1) |
| 5 | `set_Ext_` in the memories vs declared `set_EXT_` | 1659-1666, 2451-2458, 2535-2550 vs createchanParaNC | one spelling `set_EXT_`, qualified by `vid(v, m)` in declaration and use |
| 6 | controller memory without variables: an external choice without left action (c7 marker `GENERATOR_ERROR_missing_action_crtlMemory_...`) | ctrl_memory 2425- | `terminate \<rightarrow> Skip` when there is no variable |
| 7 | the machines that require v are found by object identity (`requiredVariables(m).includes(v)`: the controller's Variable vs the interface's): the set is empty | ctrl_memory 2445 | match by name, as the CSP-M (`rvarsNames.contains(v.name)`) |
| 8 | connections still point to the references (sref), `renamingMachine` compares with the clones: no renaming (c7) | replaceRef 1028-1060 (removal commented out), renamingMachine 2713-2740 | map each reference to its clone once (a Map in replaceRef) and compare connection ends through it; use the `cons` parameter (`c.connections`) instead of `RC!Connection.all()` (which takes the connections of every controller) |
| 9 | OperationRef not cloned, defined operations not translated (c7 `opref o = Op`) | replaceRef; createControllerAsProcessPara loop over `c.lOperations` with `op.nodes.isDefined()` | resolve `OperationRef.ref`; Rule 27 fixrec `op_<FQN>`; a call statement of a machine in that controller calls `op_<FQN>\<cdot>args` (the manual's `nops`), others stay `<op>Call` |
| 10 | Basic Process of a controller takes all ActionParas of the model | 2238-2246 | Section 2.3 (4) |
| 11 | Rule 1: `var bufferSet;` used undefined; `createParallelProcess(evasyncs.` / `proc1,` parses as `evasyncs.proc1`; `memoryChannels.add` on a copy | createModuleAsProcessPara 1200-1256 | rewrite Rule 1 with the paragraphs of Section 2.1 |
| 12 | Rule 14 adds `"teminate"` | composeControllers 1825 | `"terminate"` |
| 13 | constants declared twice (controller and machine, `declareConstants` in Rule 19 and Rule 25) | 1066, 2127-, 2802- | declare once by name (Section 1.1); required constants of a top-level machine declared (JJ) |
| 14 | `renamingMachine` renames `set_c` of a required constant to itself (TBC comment) | renamingMachine 2764 (Rule 15: 1893) | removed (constants as `consts`) |
| 15 | composite-state paragraph ordering and generator notes lost through the controller path | circusM2T_HOLCSP.egl 258-339 only in the machine-only branch | Section 2.3 |
| 16 | Rule 19 synchronisation set without the machines' copies `set_EXT_v_m`; unconnected machine events not hidden | createControllerAsProcessPara | as the CSP-M (Section 2.1), recorded as differences from the manual |

## 4. Relational renaming at the controller and module level

Direction (manual `x \<leftarrow> y` = CSP-M `x <- y` = generator `rn_a = x \<and> rn_b = y` = Relational_Renaming R with
`R x y`: the event x of the operand becomes y; checked for Rule 41 in `docs/RENAMING.md`):

| Rule | Pairs | Operand's event (rn_a) → new event (rn_b) | RC2Z today |
|---|---|---|---|
| 22, internal connection m1.e → m2.f | `eventId(f).in \<leftarrow> eventId(e).out`, `eventId(f).out \<leftarrow> eventId(e).in` | in the target machine m2: `f_m2_in v` → `e_m1_out v`, `f_m2_out v` → `e_m1_in v`; the source m1 is not renamed (CSP-M the same) | L = eto, R = efrom: right direction |
| 22, from the controller C.e → m.f | `eventId(f) \<leftarrow> eventId(e)` | `f_m_in/out v` → `e_in/out v` (both channels of the event, CSP-M `stm::f <- e`) | L = eto, R = efrom: right |
| 22, to the controller m.e → C.f | `eventId(e) \<leftarrow> eventId(f)` | `e_m_in/out v` → `f_in/out v` | L = efrom, R = eto: right |
| 15 | as 22 with controllers and the platform | likewise | same pattern |
| 16, 23 | channel sets | no renaming | |

A controller event connected to two machines gives many-to-one pairs (two machine events become the same controller
event); a machine event connected to the controller and, by a second connection, to another machine would give
one-to-many pairs; both are covered by the relational printer (`docs/RENAMING.md`, laws in RC_HOL-CSP).

Continuity: `Relational_Renaming_cont` needs `finitary_rel` (every event has finitely many preimages). At these places
the relations map each channel value to one value (prefix renaming with a shared variable) and a value of the new
event has finitely many preimages (one per connection), so `finitary_rel` would hold; it is not needed anyway: as at
the machine level the renamed operand is always a call of another constant (`stm_<m>`, `controller_<c>`, each its own
fixrec), so the fixrec of `renamingMachine_<m>` is constant in its recursion variable. Only a renaming under a
recursive call in the same fixrec would need it; none of Rules 1-23 has one.

## 5. Test models

| Model | What | Expected | Against the CSP-M |
|---|---|---|---|
| c7 (`composite-tests/c7_operation.rct`) | controller with `opref` to an operation with a composite state and a parameter; connection C7.a → s.a | after step 3: generates without marker; the HOL-CSP theory loads; the operation is a fixrec `op_Op_...`; s's call `Op(1)` calls it | CSP-M of c7 not yet generated (Fang, RoboTool); then the method below |
| running example, original (RoboChart2Circus c0e67a0 `Abstract_examples/example_robot.rct`: controller `robot_ctrl`, `var obs : int = 0`, `const dest : real`, machine `ObsDetect`) | controller variable required by the machine; required constant | after step 2: loads; `get_obs`, `set_obs` once, `set_EXT_obs_ObsDetect_robot_ctrl`; `consts dest` once; the Trans-core theory unchanged except the channel declarations (ITP configurations of the EMF variant are not affected) | the paper's FDR screenshot (`robot_ctrl::ObsDetect::Transitions(0,0) ; RUN({r__})` deadlock free); controller level as below |
| `example_robot_DDLK.rct` (two machines stm0, stm1 in `robot_ctrl_D`, both requiring `obs`) | two machines sharing a controller variable | `set_EXT_obs_stm0_...`, `set_EXT_obs_stm1_...`; the memory sends to both | as below |
| `ctl2` (new, Section 2.2) | two machines connected to each other and to the controller, a typed connection | Section 2.2 | as below |
| `ctl_comp` (new) | a machine with a composite state in a controller, a connection to an inner trigger | the composite paragraphs in the same order as without controller; loads | as below |
| `ctl_op` (new) | controller with a defined operation (`opref`) and a required (undefined) operation used by a machine | `op_<FQN>` fixrec; `<op>Call` for the undefined one, declared once | as below |
| `mod1` (new) | module with a robotic platform (an event, a variable), one controller, a synchronous and an asynchronous connection | buffer fixrec, modMemory, module paragraph; loads | as below |
| DTI `Adaptation.rct` (enriched; controller `Adaptation` with six machines) and `ScrewDetection_module.rct` | realistic size | after step 4: loads (memory: the full Adaptation_Plan alone takes 3-4 min) | RoboTool's CSP-M exists: `robochart-models/dti/original/csp-gen/defs/Adaptation_Adaptation.csp` |

Method against the CSP-M (FDR, run by Fang on Windows): there is no translator from the HOL-CSP output to CSP-M; for
the small models (ctl2, ctl_comp, ctl_op, mod1, the running example) a CSP-M transcription of the generated HOL-CSP is
written (mechanically: the HOL-CSP syntax of the generated text is close to CSP-M; a small script, with the
data types reduced as in the earlier FDR packages), and the controller process is compared with RoboTool's
`<ctrl>::D__` (or `O__`) by refinement in both directions in the failures-divergences model, after renaming the
CSP-M module names (`m1::done` → `done_m1_C_ctl2`). As for the A2 pilots (`a2-general-fdr`): the scripts are written
and checked with `check_syntax.py`, results recorded as "checked with FDR" only after Fang's run. For DTI only the
loading and, later, Isabelle proofs.

## 6. Steps and verification

| Step | Content | Verification | Effect on the regression |
|---|---|---|---|
| 1 | channel naming and deduplication (Section 1), JJ | the declarations of c7, the running example, DDLK: no duplicate (the datatype `chan_event` is accepted by Isabelle; the theory still stops later, at the IsaCircus locale) | token comparison of all models without controller against f242344: identical; only JJ models gain declarations |
| 2 | HOL-CSP printing of Rules 19-23 (Section 2), defects 2-8, 10, 13-16, shared printing function | `ctl2`, `ctl_comp`, running example, DDLK load; Section 2.2 output; the machine paragraphs of a machine in a controller are those of the same machine without controller, up to the names | as step 1; composite models (c1-c9) identical (the printing function is moved, not changed) |
| 3 | operations (Rule 27, `nops`, defect 9) | c7 and `ctl_op` load | as step 1 |
| 4 | module (Rules 1-18, defects 11, 12), buffers, the exception (decision 2.1) | `mod1`, ScrewDetection module, DTI `Adaptation.rct` load | as step 1 |
| 5 | FDR comparison (Fang) and SEMANTICS_DIFFERENCES entries (hiding of unconnected events, the `set_EXT` copies in Rule 19, constants as `consts`, exception form) | refinement both ways for ctl2, ctl_comp, ctl_op, mod1, running example | none |
| 6 | IsaCircus output: list of changed names for Arjun | — | IsaCircus theories of controller models change |

At every step: `GenAll` of c1-c9, c9b, d1-d3, flat and full Adaptation_Plan, ex10, ex12, Legitimate, the ITP models,
the regression tests; token comparison against f242344; HOL-CSP loads of the composite tests and the full
Adaptation_Plan; the ITP Trans-core proof theories rebuilt (they must stay byte-identical, as in COMPOSITE_RELEASE_TEST
Section 16). The deadlock-freedom proofs of AG-dev import fixed older generated files and are not affected. No step
changes the relation printer (the short notation of `renaming-short` is independent).

Decisions needed from Fang before step 2: the exception form (2.1 (a) or (b)); following the CSP-M in hiding
unconnected machine events and in synchronising the `set_EXT` copies in Rule 19 (2.1); constants as `consts` (1.2).

## 7. Answers to the questions of 24 Nov 2026 (read-only; nothing implemented)

Outputs read: main f242344 (GenAll) for ctl2 (Section 2.2, now written and valid by the RoboChart parser and
validator: the controller declares `var obs : int = 0`; a controller cannot `provides` an interface), c7, and the
original running example (`robot_ctrl`, RoboChart2Circus c0e67a0 `Abstract_examples/example_robot.rct`).
No IsaCircus environment exists on this workstation (the theories import `Test_Generation.Ax_CyPhyCircus`; no such
session is installed): the IsaCircus output was analysed as text only.

### 7.1 Channel naming today

Qualified by the FQN of their container: node channels (`enter_/entered_/interrupt_<node FQN>`), flow channels
(`exit_/exited_/interrupt_<nc FQN>`), `internal__<nc FQN>`, NIDS types, trigger channels (`<e>_<nc FQN>__in`, since
14 Nov 2026), the variables and constants declared in a state machine (`<v>_<stm FQN>`, `FQNllLocalVariables`, RC2Z
360-374, restored 3b23960 on 9 Oct 2026), the variables of defined operations (`get_<op FQN>_<v>`).
NOT qualified: the event channels `<e>_in`/`<e>_out` of machines, controllers and the platform; the interface variable
channels `get_<v>`, `set_<v>`, `set_EXT_<v>`; the variables of controllers and of the platform (`get_<v>`, `set_<v>`;
`FQNllLocalVariables` runs only for state machines); `<op>Call`; `aviol`, `gviol`.
Reasons on record: events, "deliberately not qualified (connections match them by name)" (RC2Z header 92; 369-372
"renaming events: event channels keep their names (they are matched by connections)", the 9 Oct 2026 restoration;
5221-5222, comment of `triggerChan`). The original qualified forms are still there commented out
(`e.FQN() + "_in"` in createchanParaNC, present since the first commit a8177dc). Interface variables: only open
questions, "TBC with Pedro: v to be FQN at which level? stm or ctrl or pkg?" (2320, 4045) and "TBC here we use v.name
instead of FQN because we assume all variables have the fully qualified names" (1678, 2466, 2550). `<op>Call`,
`aviol`, `gviol`: no comment. Consequence (main f242344): duplicated constructors in BOTH outputs: ctl2 `get_obs`,
`set_obs` (three times), `set_EXT_obs`, `done_in/out`, `ack_in/out`, `aviol`, `gviol` (twice); c7 `a_in`, `a_out`;
robot_ctrl `get_obs`, `set_obs`.

### 7.2 Printing of the controller level: IsaCircus today, HOL-CSP planned (ctl2)

IsaCircus today (`IsaCircus_enriched_ctl2_t.thy` 215-422, abridged): one `locale controller_proc_C_ctl2` whose
`actions is "(mychan, unit) action" where` lists ALL paragraphs of the model (the nodes of m1 and m2, ..., then)

```
"composeMachines_C_ctl2 = ((cRename (cCompleteRenaming ({})) (stm_m1_C_ctl2))
  \<lbrakk> \<^bold>0 | \<lbrace> terminate \<rbrace> | \<^bold>0 \<rbrakk>
  (cRename (cCompleteRenaming ({})) (stm_m2_C_ctl2)))" |
"crtlMemory_C_ctl2(obs :: int) = (((set_obs\<^bold>?x__v \<rightarrow> ctrlMemory_C_ctl2(x__v)) \<box> (terminate \<rightarrow> Skip)))" |
"controller_action_C_ctl2 = ((composeMachines_C_ctl2
  \<lbrakk> \<^bold>0 | \<lbrace> set_obs,terminate \<rbrace> | \<^bold>0 \<rbrakk> crtlMemory_C_ctl2((0::int))) \<Zhide> \<lbrace> set_obs \<rbrace>)"
definition "ProcDef = cProcess controller_action_C_ctl2"
```

The HOL-CSP theory prints the same locale with HOL-CSP operators inside (lines 409-419): it cannot be processed
(IsaCircus commands in a HOL-CSP theory). Errors visible in the text, in both: the three connections are lost (empty
renamings; the machines synchronise only on terminate); the memory sends nothing to the machines (7.4); the memory
paragraph is `crtlMemory_` but calls `ctrlMemory_`; the duplicated constructors of 7.1.

HOL-CSP planned: Section 2.2 (every paragraph a `fixrec ... :: "chan_event process"` with `[simp del]` in the one
`locale actions`; the memory one fixrec per variable with a value parameter). fixrec or definition: a paragraph that
calls itself (memories, buffers, Trans, node paragraphs) must be a fixrec (least fixed point over the cpo of
processes; its parameter types need `discrete_cpo`, which the templates provide for the basic types, enumerations,
records, char and lists). A paragraph that does not (renamingMachine, composeMachines, ctrlMemory, controller,
renamingController, composeControllers, module) can be a `definition`; the plan keeps `fixrec` for them too, as the
generator does today for every non-recursive paragraph (ncCoreBehaviour, Behaviour, stm_...): one form, the same
`.simps` names for the proof methods (`deadlock_free'` and the AG-dev methods unfold with `<name>.simps`), and no
continuity obligation (a fixrec without recursion is constant in its argument). The channel sets (renStmEvts,
renCtrlEvts, hiddenModuleChannels, memoryChannels) are `definition`s of type `chan_event set`, as the channel sets today.
Difference to IsaCircus: the IsaCircus form is a Circus process (state, actions, a main action, `cProcess`), the
HOL-CSP form flat CSP processes over one `chan_event`; the IsaCircus template keeps its form.

### 7.3 set_Ext and set_EXT

Manual (chapter4.tex, Aug 2022 numbering): `set_EXT_vid(v)` in Rule 3 memoryChannels (line 161, `set_EXT_vid(v,c)`),
Rule 25 (538 the renaming `share \<leftarrow> set_EXT_vid(v)`, 552 sharedVarSync), Rule 37 sharedMemory (828,
`set_EXT_vid(v)?x \<rightarrow> Memory(x)`), and the text at 561; `set_Ext_vid(v,c)` in Rule 12 modMemory (263);
`set_Ext_vid(v)` in Rule 19 rvars (424, also the timed copy 2312); `set_Ext_vid(v,m)` and `set_Ext_vid(v)` in Rule
20 ctrlMemory (437, 440-441). Meaning: the same kind of event, "a write of the required variable v arriving from
outside the component": the memory of the providing component (controller, module) sends `set_Ext_vid(v,m)` to each
component m that requires v, and the requiring component receives it as its own `set_EXT_vid(v)` (Rule 37). The
manual spells it two ways and never defines `vid(v,m)`; the robochart-reference annotation (branch csp-m-differences,
`appendix7.tex` 77 and 123) already says "Spelling differs within the manual ... the CSP-M has one spelling; the
second argument of vid(v,c) is realised by qualification with the module of c". CSP-M: one channel name `set_EXT_x`,
declared in the state machine module and in the controller module (appendix7.tex 77); the controller memory writes
`stm::set_EXT_x` (comp/untimed `CMemoryGenerator.xtend` 214 for local variables, 219 for required ones, which it
receives as its own `set_EXT_x`), the machine's shared memory reads `set_EXT_x` (`sharedMemory_rule`, 172-184). So:
ONE channel per (variable, receiving component), two spellings in the manual. RC2Z: the memories write `set_Ext_<v>_<m
FQN>` (1659-1666, 2451-2458, 2535-2550) and the machines declare `set_EXT_<v>`: different names. Not changed yet.

### 7.4 The controller memory does not reach the machines

Output: robot_ctrl, both theories: `crtlMemory_robot_ctrl\<cdot>obs\<cdot>dest = ((set_obs\<^bold>?x__v \<rightarrow>
ctrlMemory_robot_ctrl\<cdot>x__v\<cdot>dest) \<box> (terminate \<rightarrow> Skip))`: no `set_Ext_obs_ObsDetect_...`; ctl2: the
same, nothing for m1, m2; c7 has no variable (marker `GENERATOR_ERROR_missing_action_crtlMemory_C7_composite_c7`,
defect 6). Cause: `ctrl_memory` (RC2Z 2425-) selects the machines that require v by
`ms.select(m|requiredVariables(m).includes(v))` (2445): v is the controller's Variable object, `requiredVariables(m)`
(1507) returns the Variable objects of the interfaces m requires; they are different objects with the same name, so
the set is always empty and the branch that builds `set_Ext_..._m!x \<rightarrow> Skip` (2448-2462) is skipped. The CSP-M
matches by name (`gu.requiredVariables(...).map[x|x.name].contains(v.name)`, CMemoryGenerator 214).

### 7.5 The sixteen defects (Section 3), with their effect on both outputs

| # | Where (f242344) | Effect HOL-CSP | Effect IsaCircus | Fix |
|---|---|---|---|---|
| 1 | event channels not unique | duplicate datatype constructors (c7, robot_ctrl, ctl2) | duplicate chantype entries (the same) | Section 1 |
| 2 | `e\<cdot>in` in Rules 15, 22 (1873-1879, 2744-2750) | renamings with names that are no channels (not reached today: defect 8 empties the renaming first) | the same | eventChan names, prefix pairs for typed events |
| 3 | renStmEvts, renCtrlEvts: event names and `set_<const>` | `set_dest` (robot_ctrl) in a sync set, undeclared | the same | channel sets of the event channels; no `set_` of constants (7.9) |
| 4 | `crtlMemory_` vs `ctrlMemory_` (2408 vs 2501, 2505, 2584, 2588) | undefined constant in the memory body | undefined action | a typing error: one name, `ctrlMemory_` |
| 5 | `set_Ext_` vs `set_EXT_` | different channels (not reached today: defect 7) | the same | 7.3: one channel `set_EXT_<v>_<m FQN>` |
| 6 | memory without variables (c7) | marker, the theory stops | marker | `terminate \<rightarrow> Skip` |
| 7 | machines found by object identity (2445) | memory never writes to the machines (7.4) | the same | by name |
| 8 | connections point to the references (replaceRef), `RC!Connection.all()` | renamings empty: connections lost (c7, ctl2) | the same (`cCompleteRenaming ({})`) | see replaceRef below |
| 9 | operations: OperationRef not resolved, defined operations not translated | `CALL__Op(1)` (c7) calls no paragraph | the same (`Active_s0` line 133) | Rule 19 / issue R, below |
| 10 | Basic Process takes all ActionParas (2238-2246) | only one controller works | with two controllers (module) wrong grouping | Section 2.3 |
| 11 | Rule 1: `bufferSet` undefined, `evasyncs.proc1`, memoryChannels copy (1200-1256) | module with async connections: RC2Z fails or wrong process | the same | rewrite (Section 2.1) |
| 12 | Rule 14 `"teminate"` (1825) | an undeclared name in the sync set of two controllers | the same | `terminate` |
| 13 | constants declared twice (1066 in Rule 19 and Rule 25) | possible duplicate `consts` | the same | declare once |
| 14 | renamingMachine `set_c \<leftarrow> set_c` for required constants (2764) | identity pair with an undeclared name | the same | remove (7.9) |
| 15 | ordering and notes only in the machine-only branch | composite states in a controller printed in model order | not affected (its own order) | Section 2.3 |
| 16 | Rule 19 sync set without the machines' `set_EXT` copies; unconnected events not hidden | copies not synchronised (7.8) | the same | as the CSP-M (7.7, 7.8), recorded as differences |

IsaCircus (text): c7's IsaCircus theory has the duplicate `a_in`, `a_out`, the call `CALL__Op(1)` of an undefined
action, the marker in the memory and the empty renaming of the connection; ctl2's and robot_ctrl's the `crtlMemory_`/
`ctrlMemory_` mismatch and the duplicates. So the controller part of the IsaCircus output is already wrong at these
places, independently of HOL-CSP (not loaded: no IsaCircus environment).

Operations (defect 9 and issue R): issue R (generator CLAUDE.md, 14 Nov 2026) is the general one: a call statement is
translated to `CALL__<op>(args)` and no paragraph of that name is generated (`createCallOpActionPara` builds `<op>Call`
paragraphs whose result is not used). Defect 9 is its controller part: Rule 19 requires, for every operation defined
in the controller (`c.lOperations`), a process `nproc(op)(params) = [[opdef]]STM^nops` (the operation definition's
semantics, Rule 27), for every required operation `nproc(op)(params) = o.nameCall.params \<rightarrow> SKIP`, and passes
`nops` (operation name to nproc) to `composeMachines`, so that a machine's call statement is `nproc(op)(args)`. So
defect 9 is issue R plus Rule 27 for defined operations; fixing R (calls name an existing paragraph) is a prerequisite.

replaceRef (RC2Z 1028-1060): for every `RoboticPlatformRef`, `ControllerRef`, `StateMachineRef` it inserts a COPY of
the referenced definition (`clone(x.ref)`) into the container of the reference, so that every instance has its own
objects, hence its own FQN-based names (`m1_C_ctl2` and `m2_C_ctl2` for two references to one definition). The removal
of the reference (`ctl(stmr).machines.remove(stmr)`, `delete stmr`) is commented out with "THIS is commented out
BECAUSE EOL will fail, msg: Unknown reason" (1044-1057); Fang recalls that it fails once the source model of the
transformation has been modified before replaceRef is called. Not reproduced here; a likely cause (INFERRED): the
connections and other objects still refer to the reference objects, and deleting a referenced object in the source
resource (an Xtext resource with lazy cross-references) fails. Consequence: the references stay, the connections
still point to them, and the code compares with the copies (defect 8). Alternative (no copy, no deletion, the source
model unchanged; follow `.ref` when a reference is met): defect 8 disappears (a connection end IS the reference, and
the translation of the reference is the translation of `ref.ref`), no stale object remains, the source model is not
modified (which also removes the failure Fang recalls). Cost: every name of an instance (machine, its nodes, variables,
events) must be computed from the PATH of references (`m1` in `C`), not from the container of the definition, because
two references share one definition: the FQN operations and every caller would take an instance context. A middle
way with less change: keep the copies and record the map reference -> copy in replaceRef, and resolve connection ends
through it (fixes defect 8 only). Duplicate declarations (defect 1, 13) are NOT caused by the copies; they come from
the naming (7.1) and go with Section 1 either way.

### 7.6 Termination and the exception \<Theta>_end Skip

- State machine (Rule 25, chapter4.tex 530-557): `((stateful(stm) \ {end} ; SStop \<triangle> end \<rightarrow> Skip)
  [[share \<leftarrow> set_EXT_vid(v)]] |[{shared}]| Skip) |[sharedVarSync \<union> sharedConstSync \<union> {end}]| (sharedVarMemory
  |[{end}]| sharedConstMemory)) \ (...)`: no \<Theta> at the machine level; `;`, `\<triangle>` and synchronisation on `end`, as Fang
  recalls. RC2Z (2805-2811) prints exactly this with `terminate` for `end`; `terminate` IS the manual's `end` (one to
  one: Rules 25, 29, 31-37, 41, 52, 53, 57-60; the CSP-M also calls it `terminate`, appendix7.tex 74, 120).
- \<Theta>_end Skip in the manual: Rule 1 (chapter4.tex 128) and Rule 19 (416) only (2304 is the timed Rule 19, 698 is
  commented out).
- RC2Z (header 72-77, "06.Sep.2024"): "option 1: an Exception action/process in the Circus metamodel, not adopted, to
  stay consistent with the Circus AST; option 2: remove the Exception by pushing the end event into the process".
  Option 2 is implemented: Rule 19 adds `terminate` to the synchronisation set of machines and memory (2188, and to
  the machines' composition 2690 "terminate is added to solve the issue with exception operator"), every memory gets
  a choice `terminate \<rightarrow> Skip` (2004, 2030, 2076, 2105), Rule 1 adds `terminate` to `evasyncs` and to the memory
  channels (1203, 1243 "this terminate is added to deal with the EXCEPTION operator issue"); the exception itself is
  commented out (1270 `createExceptionProcess(...) //TBC ??`). No \<Theta> is printed.
- CSP-M: Rule 19, `compileAux` (comp/untimed `CControllerGenerator.xtend` 103-136) ends with `[|{|terminate|}|>SKIP`
  (134), the CSP-M exception operator; the module (`CModuleGenerator.xtend`, `moduleProcesses`) likewise.
- HOL-CSPM: `Throw` (Throw.thy 65, `P \<Theta> a \<in> A. Q a`): the traces of P without A, then, at the first event a of A,
  Q a; divergences likewise. Laws: `Throw_Mprefix` (Step_CSPM_Laws 56), `Throw_read`, `Throw_write`, `Throw_write0`
  (Read_Write_CSPM_Laws 54-68), `Throw_Det`, `Throw_GlobalDet`, `Throw_NdetR` (CSPM_Laws 54-135),
  `inj_on_Renaming_Throw` (549), `Throw_STOP`, `Throw_SKIP`, `Throw_BOT`, `Throw_disjoint_events_of`, `mono_Throw`,
  `Throw_cont` (Throw.thy 396-653), `Throw_non_destructive` (HOL-CSP_RS); no law with `\<lbrakk>S\<rbrakk>` or hiding.
  Usable in RC_HOL-CSP directly (HOL-CSP_RS is built on HOL-CSPM; the generated theories already use `\<triangle>` from it).
- A (print Throw) against B (option 2). Meaning: A is the manual's and the CSP-M's form: the first `end` of any
  machine (machines synchronise `end` among themselves, renStmEvts) ends the controller, whatever the memory is doing.
  B needs the memory to take part in `terminate`: it does so only in a state that offers `terminate` (between two
  writes, not in the middle of the sequence `set_EXT_..._m1!x \<rightarrow> set_EXT_..._m2!x` that forwards one write). They
  agree when the memory can always finish a forwarding (each machine's shared memory accepts `set_EXT` until its own
  `terminate`, Rule 37), which holds for the generated shapes, but this is an argument, not a check: to be checked with
  FDR on ctl2 (Throw form against option 2) before deciding. Work: A needs a new Z node (exception), its creation in
  Rules 1 and 19 and printing in the HOL-CSP and IsaCircus templates (IsaCircus: does the IsaCircus library have an
  exception? not checked); B exists. Proofs: A gives the manual's process, but HOL-CSPM has no law moving Throw through
  `\<lbrakk>S\<rbrakk>` or hiding, so deadlock arguments at the controller level would need new laws; B is plain parallel composition
  and hiding (existing laws, the machine-level methods), with `terminate` an ordinary synchronised event. Recommendation:
  B for the implementation (no metamodel change, proofs with existing laws), with the FDR check of A against B on ctl2
  and mod1 as the justification, recorded in SEMANTICS_DIFFERENCES; A if the FDR check fails.

### 7.7 Events of a machine that are not connected

CSP-M: comp/untimed `CControllerGenerator.xtend` 408-412 (`connectedEvents`, `unconnectedEvents = allEvents(stm)
minus connected`), 424-429 (`\ {| stm::e |}` for each, in `RenamingStateMachineChanset` 276-); also `composeStateMachines`
(247-270) hides the events of the connections between machines (`\ diff({| ... |}, {|terminate|})`, 264-270). Manual:
Rule 21 composeMachines (chapter4.tex 461-480) and Rule 22 renamingMachine (482-501) rename and synchronise, hide
nothing; Rule 19 (393-430) hides only `lvars \<union> rvars \<union> lconsts` and Rule 1 `hiddenModuleChannels` (asynchronous
connections and memory channels, 143-151). So in the manual the unconnected events of a machine, and the events of the
connections between machines, remain visible at the controller's interface; in the CSP-M they are internal.
Minimal example: a controller `C` with one machine `M`, `event e`, `initial i0`, `state s0 { }`, `transition t0 { from
i0 to s0 }`, `transition t1 { from s0 to s0 trigger e }`, and no connection. Manual: the controller offers `e` forever
(an environment may send it); deadlock-free and divergence-free. CSP-M: `e` is hidden, so `t1` fires on its own
infinitely often: the process diverges ([FD] deadlock check and divergence check fail; the [F] deadlock check passes,
there is no stable deadlock). In general hiding does not change whether a stable deadlock state is reachable (a
deadlock reached by a trace with e is reached by the same trace without e, and vice versa), but it changes divergence,
refusals (an unconnected input event becomes a spontaneous internal step) and every refinement result. (Read, not run;
the example is for FDR, not checked.)

### 7.8 Synchronisation set of Rule 19 and the memory copies

Rewritten 25 Nov 2026 after the FDR experiment. Rule numbers as in the PDF of the manual (August 2022); line numbers of
chapter4.tex as "upstream master 04e7b96 / branch csp-m-differences 0eabbe8".

**Main point: the synchronisation set of Chapter 4 Rule 19 omits the writes from the controller memory to the
machines.** Rule 19 (376-412 / 393-429) is `((composeMachines(c,ms,cs) |[lvars \<union> rvars \<union> lconsts \<union> rconsts]|
ctrlMemory(c)) \ (lvars \<union> rvars \<union> lconsts)) \<Theta>_{end} Skip` (390, 394-396 / 407, 411-413), with `lvars = {| v :
allLocalVariables(c) @ set_vid(v) |}` and `rvars = {| v : requiredVariables(c) @ set_Ext_vid(v) |}` (406-407 / 423-424), the
latter the controller's OWN required variables. Rule 20 (416-440 / 433-457) lets the memory, after `set_vid(v)?x` (or
`set_Ext_vid(v)?x`), perform `(; m : rmachines(v) @ set_Ext_vid(v,m)!x \<rightarrow> Skip)` (420, 424 / 437, 441), with `rmachines =
\<lambda> v @ {m: ms | v \<in> requiredVariables(m)}` (437 / 454). These events are in neither the synchronisation set nor the hidden
set of Rule 19. The machine side receives external writes on `set_EXT_vid(v)` (Rule 25: the renaming `share \<leftarrow>
set_EXT_vid(v)` 521 / 538 and `sharedVarSync` 535 / 552; Rule 37: `set_EXT_vid(v)?x \<rightarrow> Memory(x)` 795 / 828), which Rule 25
does not hide. So even with the names matched, a literal reading lets the memory's writes and the machine's inputs happen
independently, both visible at the controller's interface.

RoboTool's CSP-M closes this: comp/untimed `CControllerGenerator.xtend` `compileAux` (103-136) synchronises the machines
with the memory on `{| set_x |}` UNION `{| stm::set_EXT_x |}` for every machine that requires a variable (107-118, the
machine part at 115) and hides `set_x`, `get_x` and `stm::set_EXT_x` (122-133, 130); the memory writes `stm::set_EXT_x!x__`
(`CMemoryGenerator.xtend` 214, 219). Running example (`isacircus-generator-demo/csp-gen/defs/ctrl.csp` in AG-dev): memory
726, synchronisation 745-753, hiding 757-764.

FDR evidence (checked with FDR 4.2.7, 25 Nov 2026; AG-dev `robochart-models/controller-fdr/rule19-sync/`, trigger-fqn-naming
46a35ef; finite data, not a proof). A = RoboTool's `ctrl.csp` unchanged; B = the same with `ObsD::set_EXT_obs` removed
from the synchronisation and the hidden set (Rule 19 read literally, names matched); B' = B with `ObsD::set_EXT_obs` hidden.
In the running example ObsD only reads the controller variable `obs` (initially 0) and nobody writes it.
- A and B differ in traces: B has `<ObsD::set_EXT_obs.1, terminate>`: the environment writes 1 into ObsD's copy of `obs`,
  ObsD takes t1 and t3 and terminates, while the controller memory still holds 0. In A this trace is impossible.
- A and B' differ already in traces: B' has `<terminate>` (ObsD's copy changes internally), A has only `<>`.
- A deadlocks (ObsD reads 0 forever in Detect; an active state offers no `terminate`), B is deadlock free, B' diverges.
So the literal Rule 19 is distinguishable from RoboTool's CSP-M in traces, failures, deadlock and divergence. Not covered:
a write by one machine forwarded to another (the running example has no writer).

Proposed issue text for the manual: "Rule 19: the events `set_Ext_vid(v,m)` by which the controller memory (Rule 20)
forwards a write to the machines m that require v are neither synchronised between `composeMachines(c,ms,cs)` and
`ctrlMemory(c)` nor hidden. As written, the memory's write and the machine's input `set_EXT_vid(v)` (Rules 25, 37) occur
independently and both remain visible, so a controller-local variable can be written from outside the controller. RoboTool's
CSP-M synchronises on and hides `{| m : ms; v : requiredVariables(m) @ stm::set_EXT_v |}` (FDR comparison on the running
example attached). Should Rule 19 include `{| m : ms; v : requiredVariables(m) @ set_Ext_vid(v,m) |}` in both sets?"

Questions, secondary:
- Meaning of `vid(v,m)`. vid is introduced with one argument (14-18 in both versions: "unique identifiers for their
  parameters ... One possible implementation ... is to calculate the qualified name"); the two-argument form occurs in Rules
  3, 12, 15, 16, 20, 22, 23 (e.g. 144, 246, 420 / 161, 263, 437) without a definition. Is `set_Ext_vid(v,m)` intended to be
  the event `set_EXT_vid(v)` of m, i.e. m's input named from outside m? (The CSP-M names it so, by module qualification
  `m::set_EXT_x`, not by a renaming.)
- Spelling: `set_EXT_` (Rules 3, 25, 37) and `set_Ext_` (Rules 12, 19, 20); the CSP-M has one name.
- For comparison only: for required constants the manual does relate the two identifiers, by the renaming `set_vid(v)
  \<leftarrow> set_vid(v,c)` of Rule 22 (474 / 491), and Rule 15 (313 / 330) for controllers. For variables there is no such
  relation; the CSP-M does not need one, because it connects the two ends by the qualified name.

Implementation (decision of 25 Nov 2026): one event per target machine, which IS m's `set_EXT` event (the name of m's
own channel, qualified by m), in the synchronisation set between memory and machines and hidden, as the CSP-M. Recorded
in SEMANTICS_DIFFERENCES.

### 7.9 Constants

State machines: constants are uninterpreted `consts` (machine constants `consts <c>_<stm FQN>`, `declareConstants`
1066 called at 2802-), no `set_` or `get_` events and no constant memory: "Rule 32, 34, 36, 38 for constants are not
implemented because constants are processed as parameters" (RC2Z 2885); confirmed in the outputs (e.g. the running
example: `consts dest_stm0 :: "nat"`, no `set_dest`). Controller and module: the plan does the same: `consts` declared
once (by the providing context), no `set_vid(c)` events, no constInit (Rules 13, 20), no `rconsts`/`lconsts` in the
sets of Rules 15, 16, 19, 22, 23. Today Rule 19 is not consistent with this: it passes the constants as parameters of
the memory (`crtlMemory_robot_ctrl\<cdot>obs\<cdot>dest`) and puts `set_dest` (undeclared) in the sync set (defects 3, 14);
both go. Difference from the manual and the CSP-M (which give constants values through `set_`/instantiations):
recorded in SEMANTICS_DIFFERENCES (an uninterpreted constant covers every value, as the manual's choice of the initial
value of a constant without value).

### 7.10 Naming of the machine copies of controller variables: (i) or (ii) (25 Nov 2026; evaluation, nothing implemented)

Read from the ctl2 output of controller-layer a051d64 (dev-tools GenAll, scratch copy of ctl2: controller C with
`var obs : int = 0`, machines m1 (reads obs) and m2 (writes obs), connections C.start→m1.go, m1.done→m2.done,
m2.ack→C.ack).

- (i) inside a controller, the machine paragraphs print qualified names (`set_EXT_obs_m1_C_ctl2`,
  `sharedMemory_obs_m1_C_ctl2`).
- (ii) the machine paragraphs are printed as for a machine alone (`set_EXT_obs`, `sharedMemory_obs`, the renaming
  share ↦ set_EXT_obs, the channel sets); the composition renames each machine m,
  set_EXT_obs ↦ set_EXT_obs_<FQN m>; the controller memory writes `set_EXT_obs_<FQN m>`, synchronised and hidden;
  every channel declared once, a paragraph with the same name and text printed once. The structure of the manual:
  the semantics of a machine (Rule 25) does not depend on where it is used; Rule 22 renames the constants
  set_vid(v) ↦ set_vid(v,c) at the composition.

0. Where set_EXT_obs occurs. Not in the Trans-core theory except its channel declaration (HOLCSP_trans_core lines
   112, 142); not in Trans, the nodes, ncCoreBehaviour, ncBehaviour, machineBody, stateful. Only in: the channel
   declaration (RC2Z 4044), `shared_variable_events_in_stmbd_m`, `sem__events_stm_m` (5165), `sharedMemory_obs`
   (3334), and `stm_m`: the renaming share ↦ set_EXT_obs (2816) and the synchronisation set with the memory (2828).
   Trans offers `share` only. So under (i) and (ii) Trans core and its deadlock-freedom proof are the same; the
   difference is in these five places (i) or in the controller paragraphs only (ii).
1. Reuse of the machine theory. Both are generated per instance (the local names already carry the instance FQN,
   `get_k_m2_C_ctl2`), so "verbatim as alone" means: the machine printing does not depend on the context beyond the
   FQN. (ii): `stm_m` is the same process as for the machine alone (up to the FQN), so a theorem about `stm_m` is a
   theorem about the controller's component. (i): `stm_m` is a different process (other alphabet); only Trans core
   carries over.
2. Models without controller. (ii): unchanged by construction (only controller paragraphs change). (i): unchanged
   only if the condition "inside a controller" is right at every one of the five places; checked only by the
   regression.
3. One renaming with Rule 22. Yes under (ii): the renaming of m at the composition (`renamingMachine_m`, Rule 22) is
   one relation; set_EXT_obs ↦ set_EXT_obs_<FQN m> is one more disjunct, beside the connection disjuncts. RC2Z prints
   no connection renaming for ctl2 today (`composeMachines_C_ctl2 = stm_m1 ⟦{terminate}⟧ stm_m2`; IsaCircus
   `cCompleteRenaming ({})`), so this paragraph has to be written in any case. Under (i) there is no renaming for
   set_EXT, only the connection renaming.
4. Proofs. The renaming is a function (set_EXT_obs v ↦ set_EXT_obs_m v, identity elsewhere).
   - Deadlock freedom is preserved by a functional renaming without injectivity: HOL-CSPM
     `deadlock_free⇩S⇩K⇩I⇩P⇩S_imp_deadlock_free⇩S⇩K⇩I⇩P⇩S_Renaming` (CSPM_Deadlock_Results.thy 502; HOL-CSPM is an ancestor of
     RC_HOL-CSP); `Relational_Renaming_functional` (RC_HOL-CSP, Relational_Renaming_Laws.thy 1913) turns the printed
     relational renaming into `Renaming P f id`. Converse: `deadlock_free⇩S⇩K⇩I⇩P⇩S_Renaming_imp` (RC_HOL-CSP 1888, no
     injectivity), HOL-CSPM `..._Renaming_iff` (inj f, inj g).
   - Through synchronisation: `Relational_Renaming_Sync_eq`, `Relational_Renaming_Sync_eq_id` (RC_HOL-CSP 968,
     1104), condition on the relation and the set. HOL-CSPM `bij_Renaming_Sync` (CSPM_Laws 1152) and
     `bij_Renaming_Hiding` (842) need a bijection; "identity elsewhere" is not one (set_EXT_obs v and set_EXT_obs_m v
     have the same image). If needed, printing the renaming as a swap (also set_EXT_obs_m v ↦ set_EXT_obs v) gives a
     bijection with the same effect on `stm_m`, which never performs set_EXT_obs_m. No relational law for hiding
     exists in RC_HOL-CSP.
   - No controller-level proof exists or is planned in this step; under (i) none of these laws is needed for
     set_EXT, under (ii) the first item suffices to carry a machine-level result to the renamed machine.
5. Effort and risk. (i): five places in the machine printing plus the name of `sharedMemory_obs`, each with a
   condition on the context; risk: the non-controller output. (ii): no change of the machine printing; in the
   controller paragraphs one disjunct per machine and required variable in `renamingMachine_m`, the qualified
   channels declared, the memory, the two sets; plus the deduplication of declarations and of identical paragraphs
   (needed for step 1 in any case: `sharedMemory_obs` is printed twice today with the same text). Risk: the size of
   the printed relations (known issue of the HOL-CSP plugin with large relations) and the order of renaming and
   synchronisation (the renaming must be applied to each machine before `composeMachines` synchronises them).

ctl2 under (ii), HOL-CSP (machine events with today's machine names; connection disjuncts as Rule 22 in the CSP-M's
direction; the qualification of machine EVENTS is a separate question of step 1):

    "renamingMachine_m1_C_ctl2 = (stm_m1_C_ctl2
       ⟦⟦(λrn_a rn_b. (rn_a = go_in ∧ rn_b = start_in) ∨ (rn_a = go_out ∧ rn_b = start_out)
          ∨ (∃rn_v1. rn_a = set_EXT_obs rn_v1 ∧ rn_b = set_EXT_obs_m1_C_ctl2 rn_v1)
          ∨ (¬ (rn_a = go_in) ∧ ¬ (rn_a = go_out) ∧ ¬ (∃rn_v1. rn_a = set_EXT_obs rn_v1) ∧ rn_b = rn_a))⟧⟧)" |
    "renamingMachine_m2_C_ctl2 = (stm_m2_C_ctl2
       ⟦⟦(λrn_a rn_b. (∃rn_v1. rn_a = done_in rn_v1 ∧ rn_b = done_out rn_v1)
          ∨ (∃rn_v1. rn_a = done_out rn_v1 ∧ rn_b = done_in rn_v1)
          ∨ (∃rn_v1. rn_a = set_EXT_obs rn_v1 ∧ rn_b = set_EXT_obs_m2_C_ctl2 rn_v1)
          ∨ (¬ (∃rn_v1. rn_a = done_in rn_v1) ∧ ¬ (∃rn_v1. rn_a = done_out rn_v1)
             ∧ ¬ (∃rn_v1. rn_a = set_EXT_obs rn_v1) ∧ rn_b = rn_a))⟧⟧)" |
    "composeMachines_C_ctl2 = (renamingMachine_m1_C_ctl2
       ⟦ ({ terminate } ∪ range done_in ∪ range done_out) ⟧
       renamingMachine_m2_C_ctl2)" |
    "ctrlMemory_C_ctl2⋅obs = ((set_obs❙?x__v → (set_EXT_obs_m1_C_ctl2❙!x__v → (set_EXT_obs_m2_C_ctl2❙!x__v
                                 → ctrlMemory_C_ctl2⋅x__v)))
       □ (terminate → Skip))" |
    "controller_action_C_ctl2 = ((composeMachines_C_ctl2
       ⟦ ({ terminate } ∪ range set_obs ∪ range set_EXT_obs_m1_C_ctl2 ∪ range set_EXT_obs_m2_C_ctl2) ⟧
       ctrlMemory_C_ctl2⋅0)
       \ (range set_obs ∪ range set_EXT_obs_m1_C_ctl2 ∪ range set_EXT_obs_m2_C_ctl2))"

Notes: m2's ack is renamed to C's ack by the identity, as the names agree today. The memory forwards in the CSP-M's
order (m1, then m2, the writer m2 included; agreed by Fang 25 Nov 2026) as nested prefixes, the CSP-M's form
(`set_obs?x__ -> m1::set_EXT_obs!x__ -> m2::set_EXT_obs!x__ -> Memory(...)`); Rule 20's `(;  m @ ... → Skip) ; Memory`
is equivalent. `get_obs` is not hidden at the controller (the CSP-M hides it, but each `stm_m` already hides it).
The relations are written in the form printed today for the renaming share ↦ set_EXT_obs. IsaCircus: the same with
`cRename` on each machine. Declared once: `set_EXT_obs`, `set_EXT_obs_m1_C_ctl2`, `set_EXT_obs_m2_C_ctl2`;
`sharedMemory_obs` printed once.

## 8. Implementation of 25 Nov 2026 (controller-layer; steps 1 and 2 and decisions 1-8 of Fang)

RC2Z.eol:
- replaceRef: no copy of a referenced state machine definition (decision 6). The controller translation follows
  `StateMachineRef.ref`: `instDef`, `instFQN` (`<reference name>_<controller FQN>`), `enterInstance`/`leaveInstance`
  set Epsilon's extended property `~inst` of the definition while it is translated as that instance, so that
  `StateMachineDef.FQN()` (and everything built on it: nodes, triggers, channel names) gives the instance's names;
  `FQNllLocalVariables` keeps the original name in `~baseName` and computes the local names again. The extended
  properties are not features of the model; but `FQNllLocalVariables` still renames the variables IN MEMORY, as it
  did before for every machine (never saved; the plugin's RC model is an InMemoryEmfModel). The references to
  controllers and platforms (module level) are unchanged (step 4).
- Rule 19 rewritten (`createControllerAsProcessPara`): each instance translated once, then `renamingMachinePara`;
  synchronisation set `set_v` (local variables) ∪ copies `set_EXT_v_<m>` ∪ {terminate}; hidden `set_v` ∪ copies (not
  the controller's own `set_EXT_v`, SEMANTICS_DIFFERENCES 24); no constants (decision 7); channel paragraphs
  `machine_copy_channels_ctrl_<c>` and `internal_connection_channels_ctrl_<c>` declare the new names.
- Rule 22 `renamingMachinePara` (variant (ii), 1.1): pairs for the connections and `set_EXT_v <- set_EXT_v_<m>`;
  unconnected events hidden before the renaming (decision 2); no pairs for constants.
- Rule 21 `composeMachinesAction`: right-nested, synchronised on the internal connection channels and terminate,
  which are hidden afterwards (as the CSP-M).
- Rule 20 `ctrlMemoryPara`: `ctrlMemory_<c>` (decision 5), parameters the variables only, nested prefixes to the
  copies in the order of the machines, `□ terminate → Skip` (decision 1, option 2); `terminate → Skip` without
  variables.
- Name matching of required variables by name at Rule 20 and at the module memory (decision 4; the old object
  comparisons at 2445 and 2530 were in the old `ctrl_memory`, which is replaced; 1653 changed).
- `sharedMemory_<v>` created once (shared by the machines of a controller).
- Operations of a controller: generatorNote on `controller_action_<c>` (decision 8): "The operations of controller
  ... are not supported yet ..."; printed as a comment in the HOL-CSP theory and as `GENERATOR WARNING` by GenAll
  and the plugin. `CALL__<op>` is still printed (the theory does not load, as before).

Templates:
- circusM2T_HOLCSP.egl: the action paragraphs of a controller's Basic Process are printed by the machine-only code
  (fixrecs in `locale actions`, Trans, nodes innermost first, then the rest in RC2Z's order); the IsaCircus form
  (`locale <proc>` with `actions is`, `cProcess`) is no longer printed in the HOL-CSP theory; generator notes of the
  remaining paragraphs are printed as comments.
- circusM2T_HOLCSP.egl, circusM2T_HOLCSP_trans_core.egl, circusM2T.egl: every channel declared once, at its first
  declaration (`chanOwners`, `keptChans`; IsaCircus `declaredChans`).

Verification: see the report of 25 Nov 2026 and CONTROLLER_MODULE_STATUS.md Section 6.

Decisions of Fang, 26 Nov 2026, on the points of the report of 25 Nov 2026: (1) the internal connection events are
hidden as in the CSP-M (SEMANTICS_DIFFERENCES 23; to ask Pedro, with the unconnected events); (2) the events are
treated by (ii), renamed in renamingMachine: agreed; (3) the controller's own `set_EXT_v` is not hidden
(SEMANTICS_DIFFERENCES 24; to ask Alvaro); (4) c7's `CALL__Op` stays as it is until operations are implemented.

To do (recorded 25 Nov 2026, not done):
- Initial value of a machine's copy of a controller variable (SEMANTICS_DIFFERENCES 25): the default of the type in
  both generators; read the manual, ask Alvaro (Fang, 26 Nov 2026); then decide.
- Defect of the MACHINE level found with ctl_comp (setL_v, setR_v of a required variable written in a composite
  state not declared; also without a controller and on f242344): generator issue OO, FIXED 26 Nov 2026 on branch
  required-write-composite (04cfbd2, from composite-states), merged here (fdd0bdf); ctl_comp's HOL-CSP theory loads.
- Trans-core theory of a controller model (found 26 Nov 2026 when loading ctl_comp): one `locale trans_core_action`
  is opened per machine container, Isabelle "Duplicate definition of locale trans_core_action" (ctl2, ctl_comp,
  DDLK; obsDetect and the running example have one machine and one locale). The machine alone prints all Transc in
  one locale. Not fixed.
- Operations (issue R, Rule 27): only the warning (decision 8); `CALL__<op>` is still printed.
- Two references to ONE definition in a controller: supported by enterInstance/leaveInstance, not tested.
- Module level (step 4): ControllerRef/RoboticPlatformRef still cloned by replaceRef; module memory still writes
  `set_Ext_`.
- The Trans-core theory of a controller model prints one `Transc` per machine; not checked beyond loading of the
  HOL-CSP theory (the Trans-core theories were not loaded).
