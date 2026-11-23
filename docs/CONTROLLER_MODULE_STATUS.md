# Controller and module level of the HOL-CSP generator: status (23 Nov 2026)

Read-only investigation. Repositories: `~/Git/robochart-isacircus-gen` (branch `composite-states`, HEAD 292def4;
`main` = 2976313, extracted at `.../scratchpad/main-2976313`), `~/Git/robochart-csp-gen` (HEAD a6412b4),
`~/Git/RoboChart2Circus_development_repo` (older history of RC2Z.eol). Abbreviations: CS = composite-states, M = main.
`RC2Z` = `circus.robocalc.robochart.epsilon.isacircus/erules/RC2Z.eol`; templates are in the same `erules/` folder.
Statements marked INFERRED were not read directly in code or output but deduced. Paths under `scratchpad/` are
generated outputs of the investigation session (temporary, not kept); the models they come from are named. Checked
again by reading the code (23 Nov 2026): the dispatch (RC2Z 357-375), the machine-only addition of the action
paragraphs (443-447), the wrapping into the controller's BasicProcess (2196-2209), the channel declarations
(2291/2306, 3960-3966, 4046-4049) and the template lines (circusM2T_HOLCSP.egl 193-203, 246-249).

## Summary

1. The controller and module rules (Chapter 4 Rules 1-23, 25, 27, 29-37 in the numbering used by RC2Z) exist in
   RC2Z and are NOT switched off in RC2Z. The dispatch at the top level calls them whenever the model has a module
   or a controller (CS RC2Z 357-375, M 235-252), and has done so unchanged since the first commit of the generator
   repository (a8177dc, 24 May 2026) and since c0e67a0 (8 Feb 2026) of RoboChart2Circus_development_repo. No commit
   that switches them off exists in either history. The controller/module rule bodies are textually identical on
   CS and M (the `git diff main composite-states` of RC2Z has no hunk in lines 1143-2890 except two unrelated
   ones).
2. What is "off" is the HOL-CSP PRINTING of the controller/module level, and it was never implemented: the three
   templates print a `Z!ProcessPara` (controller, module) in IsaCircus syntax (`locale <name> begin actions is
   ... definition "ProcDef = cProcess ..."`), and print the HOL-CSP `locale actions` block with the fixrecs only
   when there is NO process paragraph (`procparas.size()=0`). On CS: circusM2T_HOLCSP.egl 223-245 and 248-249;
   circusM2T_HOLCSP_trans_core.egl 206-232 (prints only the Transc fixrecs of a controller) and 235-236;
   circusM2T.egl 137-166 and 168-169. In RC2Z the action paragraphs are put into the Z section only in the
   machine-only case (CS 443-447); with a controller they are swallowed into the controller's BasicProcess
   (CS 2196-2209). The only parts of RC2Z that ARE commented out are: the "TEMPORARILY COMMENTTED OUT" block
   (CS 394-424) which among other things renamed every state-machine event to `<e>_<stm FQN>` (line 406), test
   blocks (CS 263-319, 335-338), steps 5 and 6 of Rule 1 (CS 1229-1233), the removal of references in replaceRef
   (CS 1004-1005, 1015-1017) and the original interleaving version of Rule 17 (CS 1909-1935). In practice the
   controller level was "switched off" for the tests by using models without a controller (e.g. the variant
   `scratchpad/runex3/example_robot_itp_stm2.rct`, whose header says it removes the controller because of DD).
3. The two duplicates are NOT caused by a switched-off part. They are caused by the channel-declaration
   operations, which run once per container (controller: createchanParaCtrl; every state machine:
   createchanParaNC) and name event channels `<e>_in`/`<e>_out` and variable channels `get_<v>`/`set_<v>`/
   `set_EXT_<v>` without any qualification, while the template prints every ChannelPara of the Z section into ONE
   datatype `chan_event` without removing duplicates. c7: `a_in` from `event_channel_ctrl_C7_composite_c7`
   (RC2Z 2291-2316) and `event_channel_stmbd_s_C7_composite_c7` (RC2Z 4016-4058). ITP running example
   (`robot_ctrl`, c0e67a0 `example_robot.rct`, one machine): `get_obs` and `set_obs` twice, from
   `var_channels_ctrl_robot_ctrl` (RC2Z 2233-2252) and `shared_var_channel_stmbd_ObsDetect_robot_ctrl` (RC2Z
   3954-3967). Its two-machine variant `example_robot_DDLK.rct`: `get_obs`
   three times, from `var_channels_ctrl_robot_ctrl_D` (RC2Z 2233-2252, controller variable `obs`) and from
   `shared_var_channel_stmbd_stm0_...` and `..._stm1_...` (RC2Z 3955-3967, required interface variable `obs`).
   The template line that prints them is circusM2T_HOLCSP.egl 193-203 (the same in trans_core 188-198 and
   circusM2T.egl 101-115). The commented-out event renaming (RC2Z 406) would have removed the event duplicates but
   is deliberately not restored (comment RC2Z 341-351, header 92).
4. Switching the controller level on for HOL-CSP needs much more than un-commenting: the template must print the
   controller paragraphs as fixrecs (they are actions), the channel names must be made unique, and RC2Z's
   controller rules contain several defects that have never been exercised (old `\<cdot>` event syntax, name
   mismatch `crtlMemory_`/`ctrlMemory_`, `set_Ext_` vs `set_EXT_`, synchronisation on undeclared names, references
   (`sref`, `opref`) not matched, missing action of the controller memory, typos in Rule 1 and 14). Details in
   Section 2.
5. RoboTool's CSP-M generator writes one file per state machine, operation, controller and module, each with its
   own core-assertions file, but each component file is SELF-CONTAINED: the controller file re-generates its
   machines as nested CSP-M modules, and namespacing (`stm_ref0::e`) plus renaming (`stm_ref0::e <- e`) separates
   equal channel names. Isabelle has no such namespaces for datatype constructors, so a per-component HOL-CSP output
   needs one shared `chan_event` (with all NIDS types and all channels, uniquely named) in a common theory.

## 1. Rule code and how it is switched off

### 1.1 Entry point (top level of RC2Z)

| What | CS lines | M lines | Status |
|---|---|---|---|
| `replaceRef()` (clones `ControllerRef`, `StateMachineRef`, `RoboticPlatformRef` into their containers; NO `OperationRef`) | call 322, def 988-1022 | call ~210, def 690 | active |
| `FQNllLocalVariables(stm)` for all machines | 352-354 | (restored 3b23960, 9 Oct 2026) | active |
| dispatch: module, else controller, else state machines | 357-375 | 235-252 | ACTIVE; unchanged since a8177dc (and c0e67a0 in RoboChart2Circus) |
| `createModuleAsProcessPara(m)` for every RCModule | 360 | 238 | active |
| `createControllerAsProcessPara(c)` for every ControllerDef (only if no module) | 366 | 244 | active |
| `createStateMachineDefAsActionPara(stm)` for every machine (only if no module and no controller) | 372 | 250 | active |
| Z section: channels, channel sets, free types, given types, AxParas, ProcessParas | 427-439 | 296-303 | active |
| ActionParas added to the Z section ONLY when there is no module and no controller | 443-447 | 307-311 | active (this is the effective switch) |
| "TEMPORARILY COMMENTTED OUT" block (`declareSharedVarAndConstAsLocalVar`, `Shared2FQLocalVariablesAndConst`, event renaming `evt.name = evt.name + "_" + stm.FQN()`, `declareConstants`, `ncBehaviour`, `createCallOps`) | 394-424 (event rename 406) | 258-288 (270) | commented out since a8177dc |
| test blocks (stm "stm0"/"GasAnalysis_stm", controller "C1", module loop) | 274-319, 335-338 | ~150-216 | commented out |

History: `git log -S 'createControllerAsProcessPara(c);'` and `-S 'if(RC!ControllerDef.all().size()>0)'` on RC2Z
find only a8177dc (initial commit). In RoboChart2Circus_development_repo the dispatch is in Test1/RC2Z.eol since
c0e67a0 (lines 213-227 there) and was touched in 5e58d63 and e8f660c; the copy `RoboChart2IsaCircus/RC2Z.eol`
(older plugin copy) calls only `createModuleAsProcessPara(m)` (line 164) and adds no ActionParas to the section.
No revision comments the dispatch out.

### 1.2 Rules (operation, CS line, M line)

Rule numbers are those written in the comments of RC2Z (they follow Chapter 4 of the manual as numbered when the
code was written; I did not re-check each against the August 2022 PDF).

| Rule (comment in RC2Z) | Operation | CS | M | Status on both branches |
|---|---|---|---|---|
| R1 module | `createModuleAsProcessPara` | 1143-1237 | 840 | called by the dispatch; steps 5 (exception) and 6 (hide terminate) commented out at CS 1229-1233 (M 926-930) |
| module channels (not in the manual) | `RC!RCModule createchanParaModule` | 1259-1370 | 956 | called at CS 1147 |
| R2 | `hiddenModuleChannels` | 1391 | 1088 | called at 1223 |
| R3 | `memoryChannels` | 1410 | 1107 | called at 1201-1204 |
| R5-R11 (allVariables, requiredVariables, allLocalVariables, allConstants, requiredConstants, allLocalConstants, requiredOperations) | same names | 1440, 1467, 1482, 1501, 1528, 1542, 1562 | 1137-1259 | helpers, used |
| R12 module memory | `modMemory`, `mod_memory` | 1575, 1592 | 1272, 1289 | called at 1206 |
| R13 | embedded in R19, helper `allLocalVariablesandConst` | 1748-1755 | 1452 | |
| R14 compose controllers | `composeControllers` | 1767-1800 | 1464 | called at 1167 |
| R15 controller renaming | `renamingController` | 1803-1860 | 1500 | called by R14; calls `createControllerAsProcessPara` at 1857 |
| R16 | `renCtrlEvts` | 1863 | 1560 | |
| R17 buffer (modified: sync on terminate) | `buffer` | 1893-1906 | 1592 | the original interleaving version is commented out at 1909-1935 |
| R18 | `singleBuffer`, `Buffer_empty`, `Buffer_false`, `controllerDefOfCon` | 1939, 1949, 2025, 2019 | 1636-1722 | |
| R19 controller | `createControllerAsProcessPara` | 2087-2213 | 1784 | called by the dispatch or by R15 |
| controller channels (not in the manual) | `RC!Controller createchanParaCtrl` | 2227-2364 | 1924 | called at 2090 |
| constants (not in the manual) | `declareConstants` | 1026 | 728 | called at 2094 (controller) and 2765 (machine) |
| R20 controller memory | `crtlMemory`, `ctrl_memory` | 2368, 2385 | 2065, 2082 | called at 2176 |
| R21 compose machines | `composeMachines` | 2638-2669 | 2335 | called at 2154 |
| R22 machine renaming | `renamingMachine` | 2673-2731 | 2370 | calls `createStateMachineDefAsActionPara(m)` at 2728 |
| R23 | `renStmEvts` | 2735-2759 | 2432 | |
| R25 state machine (outer level) | `createStateMachineDefAsActionPara` | 2762-2806 | 2459 | active in both the machine-only and the controller path |
| R26 | `getsetChannels` | 2810 | 2507 | uses the undefined variable `s` instead of `stm` (2812, 2816); not called (INFERRED from grep) |
| R27 operation definition | `createOperationDefAsActionPara` | 2825-2836 | 2522 | called only from R19 (2097-2104) for `c.lOperations` with `op.nodes.isDefined()` |
| R28 | not implemented (comment 2838-2840) | | | |
| R29 stateful, R29-2 machineBody | `stateful`, `machineBody` | 2847, 2866 | 2544, 2563 | active |
| R30 | `getsetLocalChannels` | 2878 | 2575 | active |
| R31 / R33 | `varMemory`, `sharedVarMemory` | 2888, 2974 | 2585, 2671 | active |
| R35 / R37 | `Memory`, `IndividualVarMemory`, `sharedMemory`, `IndividualSharedVarMemory` | 3049, 3083, 3237, 3244 | 2746-2941 | active |
| R32, R34, R36, R38 | not implemented (constants are parameters; comment 2845) | | | |
| operation calls | `createCallOps`, `createCallOpActionPara` | 4500, 4515 | 4145, 4160 | `createCallOpActionPara` called by R19 at 2131-2134 |
| R40, R41 | `ncBehaviour` (calls `createchanParaNC` at 4594), `ncCoreBehaviour` | 4593, 4616 | 4238, 4261 | active |
| machine/container channels | `RC!NodeContainer createchanParaNC` | 3854-4193 | 3538 | active |

### 1.3 How the controller/module level is kept out of the HOL-CSP output (templates)

| Template | CS lines | M lines | Behaviour |
|---|---|---|---|
| circusM2T_HOLCSP.egl | 223-245 | 170-192 | for each ProcessPara: `locale <name> begin actions is "(mychan, unit) action" where` + `parseBasicProcess` (IsaCircus syntax, `"<name> = <action>" \| ...`) + `definition "ProcDef = cProcess ..."`; Isabelle stops with "Malformed command syntax" (DD) |
| circusM2T_HOLCSP.egl | 247-249 (block to 360) | 194-197 | `locale actions` with fixrecs, the composite-state ordering (CS 258-339) and the generator-note comments, ONLY if `procparas.size()=0` |
| circusM2T_HOLCSP_trans_core.egl | 206-232 | 163-190 | for each ProcessPara, for each `Transc_` paragraph inside it: its own `locale trans_core_action ... end` with the fixrec and the two lemmas (since a8d4304, 19 Jun 2026; the only controller support that exists). With two machines it prints `locale trans_core_action` twice (itp_run trans_core file lines 175 and 207; Isabelle would reject the second, INFERRED) |
| circusM2T_HOLCSP_trans_core.egl | 235-236 | 192-193 | `procparas.size()=0` guard of the machine-only branch |
| circusM2T.egl (IsaCircus) | 137-166 | 110-140 | ProcessParas in IsaCircus syntax (intended there) |
| circusM2T.egl | 168-169 | 142 | `procparas.size()=0` guard of the machine-only `actions is ... cyphyaction` block |
| CircusM2T.egx | 23-46 | same | three rules `transform z: Z!Spec`, one output file each; no per-component rule |

History of the template guard: `git log -S 'locale [%=procpara.name.word%]'` gives a8177dc (initial), 8a019bf (18
Jun 2026, trans core template added) and a8d4304 (19 Jun 2026, trans core prints the Transc of a ProcessPara).
Nothing switched a working HOL-CSP controller printing off; there never was one.

## 2. Re-enabling on composite-states

### 2.1 What happens today with a controller (read in the generated files)

With a controller, RC2Z produces: the controller channels (createchanParaCtrl), the full machine semantics of every
machine (via R21 -> R22 -> R25, with createchanParaNC through ncBehaviour), the controller paragraphs
`composeMachines_<c>`, `crtlMemory_<c>`, `controller_action_<c>`, and then wraps ALL ActionParas of the model into
one BasicProcess `controller_proc_<c>` (RC2Z 2196-2209: `for(action_para in Z!ActionPara.all())`). The HOL-CSP
template prints this as the IsaCircus locale. Example `scratchpad/cmpbase/new2/itp_run/HOLCSP_example_robot_DDLK_t.thy`
(source `RoboChart2Circus_development_repo/Abstract_examples/example_robot_DDLK.rct`): line 209 `locale
controller_proc_robot_ctrl_D`, 212 `actions is "(mychan, unit) action" where`, lines 557-566 show the controller
paragraphs. The same holds on main (`scratchpad/reg-main/itp_run/...`, I did not verify which jar produced it).

### 2.2 The two duplicate declarations, exact causes

c7 (`~/Git/RoboChart-Deadlock-HOLCSP-AG-dev/robochart-models/composite-tests/c7_operation.rct`; output
`scratchpad/cmpbase/new2/c7/HOLCSP_c7_operation_t.thy`, Isabelle log `isa_load.log`: "Duplicate constant
declaration ...chan_event.a_in ... (line 83)"):

- line 72-74 `event_channel_ctrl_C7_composite_c7`: `"a_in" | "a_out"`. Created by `createchanParaCtrl` Case 3, RC2Z
  2291 (`createChannelPara("event_channel_ctrl_" + self.FQN())`), names at 2306 (`e.name + "_in"`, `e.name +
  "_out"`), for the event `a` of controller C7 (`self.events`, 2296).
- line 81-84 `event_channel_stmbd_s_C7_composite_c7`: `"a_in" | "a_out" | "a_s_C7_composite_c7__in" ...`. Created
  by `createchanParaNC` step 6, RC2Z 4016, names at 4048 (Case 1, `isMachine`) for event `a` of machine `s` (the
  clone of M made by replaceRef); the trigger channel at 4060-4077 uses `triggerChan` (5138-5140) and is unique.
  createchanParaNC is called by `ncBehaviour` (4594), reached through R22 `renamingMachine` (2728) -> R25 (2768
  `stateful`) -> R29 (2849 `machineBody`) -> R29-2 (2868 `ncBehaviour`).
- printed by circusM2T_HOLCSP.egl 193-203 (loop over all ChannelParas, one constructor per name, no duplicate
  check; `keptChans`, 916-922, only filters unused names of `event_channel_stmbd_` paragraphs, and `a` is used).
- Not caused by the switched-off part. If the operation definition were translated (see 2.3, item 9), Op's own
  `event a` would add a third `a_in` (createchanParaNC is called for an OperationDef too, `isMachine` = true,
  3863). In the CSP-M these are three distinct channels `C7::a`, `C7::s::a`, `OP_o::a`.

ITP running example as in the paper's FDR check (`RoboChart2Circus_development_repo` c0e67a0
`Abstract_examples/example_robot.rct`: controller `robot_ctrl` with `var obs : int = 0` and `const dest : real`,
one machine `ObsDetect` requiring `Interface1` with `var obs : int`), generated with composite-states 292def4
(release test Section 14.3). Both `HOLCSP_example_robot_*.thy` and `HOLCSP_trans_core_example_robot_*.thy`:

- line 78-80 `var_channels_ctrl_robot_ctrl`: `"get_obs" "int" | "set_obs" "int"` (createchanParaCtrl, RC2Z 2233-2252,
  the controller's own variable `obs`);
- line 90-92 `shared_var_channel_stmbd_ObsDetect_robot_ctrl`: `"get_obs" "int" | "set_obs" "int" | "set_EXT_obs" "int"`
  (createchanParaNC step 4, RC2Z 3954-3967, the required interface variable `obs` of ObsDetect);
- printed into the one `chan_event` by circusM2T_HOLCSP.egl 193-203 (trans core: 188-198), so `get_obs` and
  `set_obs` are declared twice; with one machine there is no third copy and no duplicate event channel.

Same cause as below; the model used in the ITP theory was the EMF model with machine `stm0` and no controller.

The two-machine variant: Running example (`example_robot_DDLK.rct`: controller `robot_ctrl_D` with `var obs : int`, machines stm0 and stm1
each `requires Interface0` with `var obs : int`). In `HOLCSP_example_robot_DDLK_t.thy` `get_obs` and `set_obs` are
declared three times, `set_EXT_obs`, `arrival_in`, `arrival_out`, `aviol`, `gviol` twice:

- line 96-99 `var_channels_ctrl_robot_ctrl_D`: `"get_obs" "int" | "set_obs" "int"`. `createchanParaCtrl` Case 1.1,
  RC2Z 2233 and 2236-2252 (`"get_" + v.name`, `"set_" + v.name` for the controller's own variables).
- line 109-111 `shared_var_channel_stmbd_stm0_robot_ctrl_D` and 143-145 `..._stm1_...`: `"get_obs" | "set_obs" |
  "set_EXT_obs"`. `createchanParaNC` step 4, RC2Z 3954-3967 (`Sequence{"get_" + v.name, "set_" + v.name,
  "set_EXT_" + v.name}` at 3964), once per machine that requires the interface.
- `arrival_in/out` twice: createchanParaNC 4048, once per machine (both declare `event arrival`). `aviol/gviol`
  twice: createchanParaNC step 11, RC2Z 4189, once per machine.
- On main additionally `arrival__in` twice with two different NIDS types (scratch `reg-main/itp_run`, lines
  114 and 148; M RC2Z ~3716 `e.name + "__in"`). The FQN trigger naming of CS removed exactly this duplicate.
- printed by circusM2T_HOLCSP.egl 193-203. The IsaCircus output has the same triplicate (`get_obs ::` three times in
  `IsaCircus_enriched_example_robot_DDLK_t.thy`, chantype at line 38; printed by circusM2T.egl 101-115).
- Not caused by the switched-off part: the controller code runs; the names are unqualified by design (CS header
  92: "The event channels <e>_in / <e>_out are deliberately not qualified (connections match them by name)";
  variables of interfaces keep short names, CLAUDE.md "FQN pre-processing restored"). JJ is the other side: in the
  machine-only case nobody declares the interface constant.

### 2.3 Further obstacles once the printing is enabled (all in code that has never been loaded in Isabelle)

1. Event identity. With unqualified `<e>_in/<e>_out` shared by all machines and the controller, Rule 22 cannot
   express a connection: in CSP-M `stm_ref0::e.in <- stm_ref5::f.out` maps between namespaces
   (`dti/original/csp-gen/defs/Adaptation_Adaptation.csp` ~6233-6262); here both sides are the same constructor.
   Qualified event channels (like the commented line 406) plus renaming to the controller's names are needed.
2. Old IsaCircus event syntax in Rules 15 and 22: `con.eto.name + "\\<cdot>in"` etc. at RC2Z 1833-1839 and 2704-2711
   (CLAUDE.md "Other known open issues" item 4 cites the old lines 1525-1531, 2396-2402). Output: `rn_a =
   arrival\<cdot>in` (itp_run line 557-558), not a constructor.
3. Synchronisation sets on non-channels: `renStmEvts` adds `con.efrom.name` (2749) and `"set_" + v.name` for
   required constants (2755); output `\<lbrakk> { arrival,set_dest,terminate } \<rbrakk>` (itp_run 559). `arrival`
   and `set_dest` are not declared. Rule 16 `renCtrlEvts` (1863-1890) has the same pattern (INFERRED, same code).
4. Name mismatch in the controller memory: the paragraph is `crtlMemory_<c>` (2378), the recursive calls are
   `ctrlMemory_<c>` (2461, 2465, 2544, 2548); output line 562 `"crtlMemory_robot_ctrl_D\<cdot>obs ... =
   ((set_obs?x__v -> ctrlMemory_robot_ctrl_D ...`. Also `set_Ext_` (2411-2418, 2495-2510) against the declared
   `set_EXT_` (2279, 3964).
5. Controller memory without variables: `ctrl_memory` builds an external choice without left action; c7 output line
   200 `GENERATOR_ERROR_missing_action_crtlMemory_C7_composite_c7` (DD).
6. Variable identity between controller and machines: `ctrl_memory` selects the machines that require `v` by
   `requiredVariables(m).includes(v)` (2405) where `v` is the controller's Variable object and the machine requires
   the interface's Variable object, so the set is empty (INFERRED; consistent with the output, which has no
   `set_EXT_obs` in `crtlMemory_robot_ctrl_D`). In CSP-M the controller memory writes `stm::set_EXT_v`.
7. References: replaceRef adds clones but keeps the references (removal commented out at 1004-1005, 1015-1017);
   connections still point to the `StateMachineRef`, so `renamingMachine`'s `con.to = m` / `con.from = m`
   (2680-2700) never match the clone. Output c7 line 198 `"composeMachines_C7_composite_c7 =
   (stm_s_C7_composite_c7)"` has no renaming although `connection C7 on a to s on a` exists (cause INFERRED).
   `RC!Connection.all()` (2680, 2687, 2694, 1809-1823) also takes connections of every controller, not `c`'s.
8. `OperationRef` is not handled by replaceRef; R19 loops `c.lOperations` with `op.nodes.isDefined()` (2097-2104);
   for c7's `opref o = Op` no `stm_op_` paragraph exists in the output (cause INFERRED). M's call of Op is
   translated as an undefined operation (`OpCall` channel, c7 line 87, createchanParaNC 4086-4114), not as the
   defined operation of the controller (CSP-M does this via `requiredOperationDefinitions`,
   CStateMachineGenerator.xtend 52-60).
9. BasicProcess collection: `Z!ActionPara.all()` at 2198 takes every ActionPara of the model, so with several
   controllers (module case) each controller process takes all paragraphs; with EMF containment the later
   controller moves them out of the earlier one (INFERRED).
10. Rule 1 defects (never run on a model with async connections, INFERRED from the code): `var bufferSet;` then
    `bufferSet.add(...)` on an undefined variable (1175-1178); `createParallelProcess(evasyncs.` / `proc1,` /
    `proc2)` (1214-1216) parses as `evasyncs.proc1`; `memoryChannels.add("terminate")` (1203) is added to a local
    copy and `memoryChannels(m)` is called again (1205). Rule 14 adds `"teminate"` (1785).
11. Constants: R19 passes constants as parameters of `crtlMemory` (2170-2181) and declares them with
    `declareConstants(c)` (2094); R25 again calls `declareConstants(stm)` (2765). One `consts dest` in the running
    example (line 86), but a constant declared both in a controller and a machine would be declared twice (INFERRED).
12. Composite states: the ordering of the paragraphs (containers innermost first, separate fixrecs for RunEntered,
    During, Behaviour; circusM2T_HOLCSP.egl 258-339) and the generator-note comments exist only in the
    `procparas.size()=0` branch. Through the controller path every paragraph is printed by `parseBasicProcess`
    (circusM2T_HOLCSP.egl 430-444) in model order in one `actions is` block.
13. W and GG (types and functions of imported packages) are translated before the dispatch (createTypeDeclPara 329,
    createFunctionDefsAsAxParas 333) and go into the section as FreePara/AxPara (427-439) in either path; they are
    not affected by the controller path. FQN trigger names (`triggerChan`, 5138) are unique per container and also
    unaffected; the FQN of a machine inside a controller includes the controller (`RC!StateMachineDef FQN`,
    3763-3786; e.g. `s_C7_composite_c7`).

Minimal route to a loadable controller theory (assessment): (a) in RC2Z, in the controller and module case, add the
ActionParas to the section as in the machine-only case (443-447) instead of wrapping them (2196-2209), so the
`locale actions` branch prints the controller paragraphs as fixrecs in the "rest of actionpara" loop
(circusM2T_HOLCSP.egl 341-357); or print ProcessParas in HOL-CSP form in 223-245; (b) qualify or deduplicate channel
names (events per machine/controller, interface variables once per controller with machine-specific `set_EXT_`),
then rewrite Rules 15, 16, 22, 23 to rename between the qualified names; (c) fix items 2-8.

## 3. RoboTool CSP-M: per-component output

Plugin `~/Git/robochart-csp-gen/circus.robocalc.robochart.generator.csp`, `plugin.xml` 6-20 registers four
generators (untimed, timed, comp.untimed, comp.timed). The csp-gen folders under `robochart-models/` were written
by `comp/untimed/CUntimedGenerator.xtend` (they contain `defs/state_defs.csp`, which only that generator copies,
line 392; header "RoboChart generator version 3.0.0.qualifier" in `dti/original`, 3.1.0 in composite-tests).

Files written by `CUntimedGenerator.generateRoboChart` (230-344), paths relative to `src/.../csp/`:

| File | Source | Content |
|---|---|---|
| `<pkg>.csp` | 270-275, `CPackageGenerator.compilePackage` 543-560 | only `include` lines (defs of all packages, `instantiations.csp`) and `channel r__` |
| `defs/<pkg>.csp` | 281-288, `compilePackageModule` 571-614 | `module <pkg> exports` with every module, controller, state machine, operation of the package, each compiled with `global = false` (nested CSP-M modules) |
| `<pkg>_coreassertions.csp` | 311, `CAssertionGenerator.compileCoreAssertionsFile` 1154-1182 | `include "<pkg>.csp"`, then core assertions of every machine, operation, controller (with its machines) and module (with its controllers) |
| `defs/<file>_defs.csp` | 317-324, `compileOthers` 616-629 | types and function preconditions of the package (per .rct file) |
| `defs/<stm id>.csp` + `defs/<stm id>_coreassertions.csp` | 327-332 (loop from 327) | `compileStateMachine(e, {}, true)` (CStateMachineGenerator 52-187): `module <id> exports` with NIDS, `internal__`, flow channels, variable channels, events, nodes as nested modules, Transitions, MachineBody, Stateful, memory, `D__`, `O__`, `VS__`, `VS_O__`, `HEXT__`, `HUP__`; standalone, all required operations undefined |
| `defs/<op id>.csp` + `_coreassertions` | 336-341 | `COperationGenerator.compileSelfContainedOperation` 74-128 |
| `defs/<ctrl id>.csp` + `_coreassertions` | 345-350 | `CControllerGenerator.compileController(e, {}, true)` 181-243: `module <id>` with `shared_variable_events`, controller channels (`controllerChannels` 53), defined operations, then EVERY MACHINE RE-GENERATED inside it (`stmg.compileStateMachine(m, allOps, false)`, 235-237), then `controllerProcesses` (154): memory and `D__` = composition of `stm::D__` renamed (`stm::terminate <- terminate`, `stm::e <- e`, `stm::e.in <- other::f.out`) in parallel with Memory, hidden, `[|{|terminate|}|> SKIP` |
| `defs/<module id>.csp` + `_coreassertions` | 354-359 | `CModuleGenerator.compileModule(e, true)` 116-147: `module <id>` with module channels, every controller re-generated (`cg.compileController(c, plat_ops, false)` 140-142), `moduleProcesses` |
| `instantiations.csp` | 247, `InstantiationGenerator.generateInstantiations` 53-56 | `nametype core_nat = ...`, constant values `const_<path>_<c> = ...` (generator with gap) |
| `defs/core_defs.csp` etc. | `generateLibraries` 375-393 | library semantics |

Core assertions per component (CAssertionGenerator): machine 991-1025, operation 1027-1046, controller 1049-1093
(controller assertions and, for each machine, the machine IN THE CONTROLLER, `<ctrl>::<stm>::O__`), module
1096-1137 (module and its controllers recursively). Each is four assertions: deadlock-free, deadlock-free with
`;RUN({r__})`, deterministic, divergence-free, on `P_<name> = let <constants> within <name>::O__(id__, ...)`.
The per-component file includes the defs of the imported packages, `robochart_defs`, `core_defs`, `state_defs`,
`<id>.csp` and `../instantiations.csp` (`coreAssertionsImport` 1139-1152).

Examples. `robochart-models/dti/original/csp-gen/` (controllers `Screwdetection::ScrewDetection` and
`Adaptation::Adaptation`; the module `ScrewDetection` of `ScrewDetection_module.rct` has no generated files there):
- `defs/Screwdetection_ScrewDetection.csp` (525 lines, no `include`): `module Screwdetection_ScrewDetection` (line 10),
  controller channels 20-36 (`channel switchModel: InOut.core_nat`, `channel terminate`), nested `module stm0`
  55-~420 with its own `channel terminate` and nodes, then `CALL__move`, `Memory(id__) = SKIP`,
  `D__(id__) = ((stm0::D__(id__)[[stm0::terminate <- terminate]]) [| ... |] Memory(id__)) \ ... [|{|terminate|}|>SKIP`.
- `defs/Screwdetection_ScrewDetection_stm0.csp` (371 lines): the same machine as a standalone module.
- `defs/Screwdetection_ScrewDetection_coreassertions.csp`: `P_Screwdetection_ScrewDetection =
  Screwdetection_ScrewDetection::O__(id__)` and `P_Screwdetection_ScrewDetection_stm0 =
  Screwdetection_ScrewDetection::stm0::O__(id__)`, four assertions each.
- `defs/Adaptation_Adaptation.csp` (6972 lines, no `include`): module at 10, nested `module stm_ref0` 45-1017,
  `module stm_ref1` from 1018, ...; `channel ambientLight` declared at 22 (controller) and again at 92 inside
  stm_ref0, `channel terminate` at 31, 69, 1040; the composition renames `stm_ref0::ambientLight <- ambientLight`,
  `stm_ref0::imagesWithLight.in <- stm_ref5::imagesWithLight.out` etc. The standalone machine files
  `defs/Adaptation_Plan_Adaptation_Plan.csp` (1804 lines) and the package module `defs/Adaptation_Plan.csp` (1812)
  contain the same machine again.
- `robochart-models/composite-tests/csp-gen/`: per package `composite_cN.csp`, `composite_cN_coreassertions.csp`,
  `defs/composite_cN.csp`, `defs/composite_cN_CN.csp` + `_coreassertions` (the machine), `defs/cN_<file>_defs.csp`.
  No controller (c7 has no CSP-M there).

So the CSP-M is not a chain of files importing each other: every component file regenerates its subcomponents, and
equal channel names are kept apart by CSP-M module namespaces.

## 4. Assessment: one HOL-CSP theory per component

Target: a common theory (types, NIDS, `chan_event`, channel sets, abbreviations), one theory per state machine (and
per operation definition), a controller theory importing its machines' theories, a module theory importing its
controllers' theories.

Hard constraint: all processes are typed `chan_event process` (fixrecs, e.g. circusM2T_HOLCSP.egl 252-256, 305-320,
341-357), and an Isabelle datatype cannot be extended in a later theory. So `chan_event` must contain every channel
of every component and live in the common theory, and every NIDS type and every record/enum type used as a
channel argument must be declared before it (they are now: NIDS and enum types at 72-98, records 147-168, chan_event
184-203). The common theory is therefore not much smaller than today's single theory; only the process paragraphs
are split. The plugin threshold (`CHAN_EVENT_PLUGIN_THRESHOLD = 80`, circusM2T_HOLCSP.egl 58) will be exceeded
earlier for controllers with several machines (the full Adaptation_Plan already has 117 constructors).

Changes needed (assessment):

1. Unique channel names (prerequisite, also for the single theory): qualify event and interface-variable channels
   per component (`<e>_<container FQN>_in`, as triggerChan does for `__in`), or declare each name once and decide
   per name which component owns it; adapt createchanParaCtrl (2227-2364), createchanParaNC (3854-4193),
   createchanParaModule (1259-1370), `sem__events_stm_` (4153-4180), `sharedVarSet` (5074), the renamings of Rules
   15, 16, 22, 23 (1803-1890, 2673-2759) and Rule 25 (2762-2806). aviol/gviol (4189), OpCall (4089-4092, 2322-2325,
   1292-1295) and terminate (336) must be declared once. The proof theories of AG-dev rely on the current names
   (e.g. `a__in`, `share`, `set_x`); any renaming changes them (CLAUDE.md, trigger-fqn-naming lesson).
2. RC2Z output structure: today one `Z!Spec` with one `Z!ZSect` "circus" (createZSect at 427, createSpec 3596-3602).
   The metamodel already has sections with parents (`createZSect(name, paras, parents)` 3605-3620, `Z!Parent`
   3632-3636), which could carry the import structure: one ZSect per component, parents = the sections it uses.
   Alternatively keep one section and let the templates select paragraphs by a component tag; no such attribute
   exists (only the name suffix `_<FQN>`, which is ambiguous for prefixes). The controller/module paragraphs must be
   ActionParas (or HOL-CSP processes), not the IsaCircus BasicProcess of 2196-2209 / 1236.
3. Templates: every template reads `z.eContents.select(s|s.instanceOf(Z!ZSect)).at(0)` (e.g. circusM2T_HOLCSP.egl
   61, 74, 147, 186, 206, 223, 247); they would take a section parameter. The theory header (circusM2T_HOLCSP.egl
   58-60: `theory HOLCSP_<baseName>_<ts> imports "RC_HOL-CSP.RoboChart_Library" HOL.Real`) must name the component
   and import the common theory and the subcomponent theories. The common part (lines 61-221: types, cpo instances,
   records, constants/functions, chan_event, channel sets, `assume`/`guar`/`SSTOP` abbreviations) goes to the common
   theory only; abbreviations repeated in several imported theories would clash or become ambiguous (INFERRED).
4. Locale: the machine-only output wraps everything in `locale actions` (circusM2T_HOLCSP.egl 250-360), and the
   hand-written proof theories re-enter it with `context actions`. Several machine theories each with a `locale
   actions` imported into one controller theory give an ambiguous or duplicate locale name (INFERRED); use
   `locale actions_<FQN>` or no locale (the locale has no parameters; it is only a name space). The trans core
   template has the same issue already now (two `locale trans_core_action` for two machines).
5. fixrec structure: per machine nothing changes (the node fixrecs and Trans are local to the machine). Controller
   theory: `composeMachines_<c>`, `crtlMemory_<c>` (a parameterised fixrec, its parameters are the variable values),
   `controller_action_<c>` as fixrecs referring to the imported `stm_<FQN>` constants; module theory likewise with
   `buffer_connct_*`, `modMemory`, `composeControllers_<m>`, `RCModule_<m>`. Mutual recursion crosses no theory
   boundary.
6. Trans core theory: today a separate theory with its OWN `chan_event` (circusM2T_HOLCSP_trans_core.egl 52-54
   header, 179-200 datatype), not importing the HOL-CSP theory. With a common theory it should import it and hold
   only the Transc fixrecs and lemmas, one per machine (lemma names `<z.version><lemmaSuffix>_trans_core_ddlf`,
   218, 247, `lemmaSuffix` 775).
7. IsaCircus theory (circusM2T.egl): its own `chantype mychan` (95-116) with the same duplicates; per-component
   IsaCircus theories would need a shared chantype in the same way (Arjun's use; not assessed further).
8. EGX and launcher: CircusM2T.egx has three rules `transform z: Z!Spec` with fixed targets (23-46) and one
   timestamp from the `pre` block (13-19, shared by all rules, which keeps the imports consistent). Per component:
   one rule per kind (`transform s : Z!ZSect` with a guard, or a loop in a driver template) and target names such as
   `hol-csp_gen/HOLCSP_<baseName>_<component FQN>_<ts>.thy`; the theory names in the templates must match the file
   names exactly. Java: CompileToIsaCircusHandler (`baseName` 140, EGX run 355-385, failure dialog 397-416) and
   PartialOutput.java (per-template failure records, 79-106) need no structural change if the EGX produces the
   files, but the messages assume three theories; dev-tools GenAll.java (115) passes `baseName` and runs the
   templates one by one, so it must learn the new rules; the Isabelle runner (RunIsabelleHandler.java) builds the
   generated theories and would need the new names (not read in detail).
9. Consistency with the CSP-M: the CSP-M per-component files are self-contained (they regenerate subcomponents)
   rather than importing; a HOL-CSP controller theory importing machine theories cannot regenerate the machine in
   a controller-specific namespace, so the machine theory must already use the names the controller expects
   (e.g. defined operations of the controller inlined into the machine, as `compileStateMachine(m, allOps, false)`
   does). Today RC2Z translates a machine differently inside and outside a controller (OpCall vs defined
   operation; constants declared by the controller), so the machine theory would depend on its context; the CSP-M
   avoids this by generating a standalone machine file AND a copy inside the controller file.
10. Proof side (AG-dev): the pilots import the generated machine theory unchanged (`Seq_ex10.thy`,
    `Seq_Legitimate.thy`); a split into common + machine theory changes their imports and, if names are qualified,
    their copied definitions (`sync_set_gen`, `Rint`, `SG`).

Files read: `~/Git/claude-context/robochart-isacircus-gen/CLAUDE.md` (DD 818-830, JJ 919-923, open issue 4 at 1154);
`~/Git/robochart-isacircus-gen/docs/COMPOSITE_RELEASE_TEST.md` (89, 101, 119, 158, 279-280, 368-370).
