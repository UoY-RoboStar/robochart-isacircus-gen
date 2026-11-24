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

The manual: `vid`, `eventId` and `id` "calculate unique identifiers ... One possible implementation is to calculate
the qualified name" (chapter4.tex 14-18). The CSP-M gets uniqueness from CSP-M modules (`m1::done`, `m2::done`) and
renames them in the composition (`stm::e <- e`, `tgt::e.in <- src::e.out`, `stm::set_v <- set_v`). One Isabelle
theory has one name space, so the names themselves must be unique. Scheme:

- the OUTERMOST component of the theory (the module; else the controller; else the state machine) keeps the
  unqualified names it has today;
- every component inside it qualifies its event and interface-variable channels with its FQN (the generator's FQN,
  innermost first, as for nodes and triggers since 14 Nov 2026);
- a channel that several components must share by the semantics (the controller's variable written by its machines)
  is declared ONCE; every declaration is deduplicated by name (a second declaration of the same name with the same
  argument type is dropped; with another type the generation stops with an error naming both);
- `vid(v, m)` (the copy of a variable v for machine m, Rules 19, 20) is `<v>_<FQN of m>`.

So a model without controller and without module produces the same channels as today.

### 1.2 Per kind of channel (before → after)

Example names: controller `C` in package `ctl2`, machines `m1`, `m2` in it (FQN `m1_C_ctl2`), event `done`,
interface variable `obs`, machine variable `k`.

| Channel | Today | After, machine without controller | After, inside a controller | Manual |
|---|---|---|---|---|
| machine event | `done_in`, `done_out` | unchanged | `done_m1_C_ctl2_in`, `done_m1_C_ctl2_out` | `eventId(e).in/.out` |
| controller event | `start_in`, `start_out` | — | unchanged if the controller is outermost; `start_C_mod_in` inside a module | `eventId(e)` |
| platform / module event | `e_in`, `e_out` | — | unchanged (module outermost) | `eventId(e)` |
| trigger channel | `<e>_<container FQN>__in` | unchanged | unchanged (the FQN already contains the controller) | `eventId(e)__.id(src).in` |
| machine variable | `get_/set_/setL_/setR_<v>_<m FQN>` | unchanged | unchanged | `vid(v)` |
| machine's required variable: write | `set_<v>` | unchanged | `set_<v>` of the providing context (controller), declared once | `set_vid(v)` |
| machine's required variable: read (inside the machine, hidden there) | `get_<v>` | unchanged | `get_<v>`, declared once (hidden in each machine, so no synchronisation between machines) | `get_vid(v)` |
| copy for machine m | `set_EXT_<v>` | unchanged | `set_EXT_<v>_<m FQN>` | `set_EXT_vid(v)`, `set_Ext_vid(v, m)` |
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
