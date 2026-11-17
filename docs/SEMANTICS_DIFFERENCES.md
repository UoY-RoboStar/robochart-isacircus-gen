# Semantic differences: manual, RoboTool's CSP-M generator, this generator

Started 16 Nov 2026 (Fang). One entry per difference between the RoboChart Reference Manual, the CSP-M that
RoboTool generates, and the HOL-CSP / IsaCircus output of this generator (RC2Z.eol and the templates).

Convention: every difference found from now on is recorded here AND in a comment at the code it concerns
(RC2Z.eol, the templates or the shared .eol files), in the existing comment format. FDR results are
written "checked with FDR"; a result that was predicted or read, not run, says so.

Sources:

- Manual: RoboChart Reference Manual, the PDF of August 2022 used by Fang (created 15 Aug 2022, built from
  commit 646c38f of `UoY-RoboStar/robochart-reference`; page numbers are the printed ones, which are the PDF
  page numbers). Rules are numbered per chapter (Chapter 5, the probabilistic semantics, has its own Rule 1,
  2, ...); every rule cited here is a rule of Chapter 4 (Semantics) and is written "Chapter 4 Rule n".
  Rule numbers are unchanged in the source of June 2026 (master 04e7b96, which adds only Appendix F); page
  numbers can differ by one in a new build, so they are given for the August 2022 PDF. Annotated copy:
  `laila-fangyan/robochart-reference`, branch `csp-m-differences` (notes "Note on RoboTool's CSP-M").
- CSP-M generator: `UoY-RoboStar/robochart-csp-gen` at commit `a6412b40c83ded4ab2c18c2b9bd28575ec270983`
  (given by Pedro; read-only clone in `~/Git/robochart-csp-gen`). The links below point to that commit:
  - [C1] [CStateGenerator.xtend 80-108, `NCCoreBehaviour_rule`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStateGenerator.xtend#L80-L108)
  - [C2] [CStateGenerator.xtend 446-458, `nodeVars`, `stmVars`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStateGenerator.xtend#L446-L458)
  - [C3] [CTransitionGenerator.xtend 281-305, `transitionGuardVariables`, `transitionTriggerVariables`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CTransitionGenerator.xtend#L281-L305)
  - [C4] [CGeneratorUtils.xtend 1514-1542, `memParallelCompose` (six sets)](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CGeneratorUtils.xtend#L1514-L1542)
  - [C5] [GeneratorUtils.xtend 483-511, `allVariables`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/untimed/GeneratorUtils.xtend#L483-L511)
  - [C6] [CStateGenerator.xtend 340-356, `compileCompositeStateBehaviour`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStateGenerator.xtend#L340-L356)
  - [C7] [CStateGenerator.xtend 273-423, `compileNode`/`Node_rule` for states (enteredSS 288-294, frame 377-380, During 388-389)](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStateGenerator.xtend#L273-L423)
  - [C8] [ExpressionGenerator.xtend 190-237, `compileCallExpression`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/untimed/ExpressionGenerator.xtend#L190-L237)
  - [C9] [CStatementGenerator.xtend 447-562, `guards`, `guard`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStatementGenerator.xtend#L447-L562)
  - [C10] [lib/semantics/sequence_toolkit.csp 43-50 (head, tail, front), 100-103 (pre_)](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/lib/semantics/sequence_toolkit.csp#L43-L103)
  - [C11] [GeneratorUtils.xtend 285-324, `typeDefaultValue`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/untimed/GeneratorUtils.xtend#L285-L324)
    and [ExpressionGenerator.xtend 547-559, `initialValue`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/untimed/ExpressionGenerator.xtend#L547-L559)
  - [C12] [lib/semantics/state.csp 19-20, `CRUN` (untimed)](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/lib/semantics/state.csp#L19-L20)
    and [lib/semantics/timed/state_timed.csp 20-21, `CRUN` (timed)](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/lib/semantics/timed/state_timed.csp#L20-L21)
- This generator: operations of `circus.robocalc.robochart.epsilon.isacircus/erules/RC2Z.eol` and of the
  templates, named by operation (line numbers change).
- Other records: the generator's CLAUDE.md (in the claude-context repository; issue letters), the research
  notes `docs/COMPOSITE_STATES_RESEARCH.md` (branch `composite-research`), the proof repository's CLAUDE.md
  (differences D1 to D4 of the FDR comparison on Legitimate).

Kinds: **semantic** (the processes differ), **form** (written differently, same semantics), **manual
incomplete** (the manual omits or under-specifies it), **convention** (the manual says nothing; a choice of
the generator).

---

## 1. Chapter 4 Rule 41: variables of the write synchronisation (node side and Trans side)

- Manual: Chapter 4 Rule 41 with Definition C.2.2 (pp. 47 and 165 of the Aug 2022 PDF):
  `|[vars(nc.nodes) | flowevts ∪ transSync(nc) ∪ {end} | vtguards(nc) : {}]|`, i.e. pr = pw =
  vars(nc.nodes) (Chapter 4 Rules 46, 48, 50), qr = the variables read by the guards, qw = {}. Reason given in the
  manual: the transitions run between the nodes and are taken to write nothing in parallel.
- CSP-M: [C1], [C2], [C3], [C4]: `memParallelCompose(nodes, nodeVars, nodeVars, cs,
  transitionGuardVariables(nc), transitionTriggerVariables(nc), Transitions)`, `nodeVars =
  allVariables(stm)` ([C5]): node side every variable of the machine; Trans side reads = guard variables,
  writes = variables bound by trigger parameters. Hence `setL_v` for every variable read by a guard (also
  one no node writes, e.g. v1 of ex10) and `setR_v` for a trigger variable (candidateModel of Legitimate).
- This generator: `ncCoreBehaviour` (Chapter 4 Rule 41), `createMetaParallelAction(nc, ..., vars(nc.nodes),
  vars(nc.nodes), cs, vtguards(nc), Set{}, ...)`: as the manual.
- Kind: form (with respect to the CSP-M: extra synchronised variables, same behaviour) and manual
  incomplete (node side, see the hazard).
- Background: difference D2 of the proof repository. Checked with FDR on Legitimate (flat; `Real_M` against
  `Real_G_exited`, both directions): no effect on the behaviour. Pedro (16 Nov 2026): the empty write set of
  the Trans side is right: the behaviour of a state machine is sequential; while a transition action runs no
  state is active, and when a transition is triggered no action of any state (inner states included) is
  running. The extra variables of the CSP-M on the Trans side are not needed but harmless.
- HAZARD (manual incomplete, node side): vars(nc.nodes) contains for an operation call only the
  arguments and requiredVariables (Chapter 4 Rule 50, p. 49 of the Aug 2022 PDF: the operation's parameters), not the variables that the
  called operation's definition reads or writes. A node whose action calls an operation that writes a
  variable occurring nowhere in the nodes would not have that write synchronised. Pedro (16 Nov 2026):
  vars(s.nodes) should include the variables of the called operations (see entry 2). No effect now:
  operation calls are not supported (generator issue R).
- Status: DECIDED (Fang, 16 Nov 2026): the manual is followed; D2 closed. When operation calls (issue R)
  are implemented, the node side must include the variables read and written by the called operations, or
  use all variables of the machine as the CSP-M does. Comment at `ncCoreBehaviour` in RC2Z.eol.

## 2. Chapter 4 Rule 60: variables of the write synchronisation of Behaviour (composite state)

- Manual: Chapter 4 Rule 60, p. 53 of the Aug 2022 PDF: `Behaviour = (... |[vars(s.nodes) | enteredSS(s) ∪ {|end, interrupt|} |
  vars(s.during)]| During) \ ...`, Definition C.2.2 (p. 165, Aug 2022 PDF) with the variables of each side.
- CSP-M: [C6], [C2]: `memParallelCompose(ncBehaviour_rule(state), stmVars, stmVars, cs, stmVars, stmVars,
  During)`, `stmVars = allVariables(stm)` ([C5]: parameters of an operation definition, own variables,
  variables of provided, defined (`uses`) and required interfaces), on all four sides.
- This generator: Chapter 4 Rule 60 (`createCompositeStateAsActionParas`), `createTwoSidedWriteSync(ncBehaviour_S,
  During_S, cs, machineVarNames(S))`; `machineVarNames` = parameters of an operation definition, own
  variables, provided, defined and required interfaces, in the order of [C5] (a name already in the list is
  not added again; `stmVars` collects them in a set). The required interfaces (`RInterfaces`, last loop of
  [C5]) were missing until 16 Nov 2026 (generator issue EE, found while writing this entry, fixed the same
  day); no generated model changed (Adaptation_Plan has only `uses`; c7, the only test model with
  `requires`, cannot be generated; c1-c6, ex10, Legitimate, flat MakePlan and full Adaptation_Plan
  regenerated: the same tokens).
- Kind: manual incomplete; this generator follows the CSP-M (superset).
- Background: our analysis (research notes 5.7): `During` is not under SStop at `entered_S`; with smaller
  sets a write of the inner machine at that moment would not be synchronised with `During`. Pedro
  (16 Nov 2026): the formula of the manual is right in intent but incomplete: vars(s.nodes) should include
  the variables read and written by the definitions of the software operations called in the composite state
  (the variables the machine provides and those required by the called operations); the CSP semantics of
  the nodes alone, or of the operations alone, is not enough; the CSP-M uses all variables of the machine
  for convenience, a superset that guarantees the right behaviour.
- Status: DECIDED (Fang, 16 Nov 2026): allVariables as the CSP-M, required interfaces included (issue EE
  fixed). Comments at `createTwoSidedWriteSync` and `machineVarNames` in RC2Z.eol.

## 3. exited under SStop (Chapter 4 Rules 58 to 60)

- Manual: Chapter 4 Rule 59, p. 52 of the Aug 2022 PDF: `Exiting = (SStop △ exit → Skip) ; [[s.exit]] ; exited → Inactive`: `exited` is
  NOT under SStop; Chapter 4 Rule 60 (p. 53, Aug 2022 PDF) the same for a composite state. Chapter 4 Rule 58 (final state, p. 52 of the Aug 2022 PDF):
  `Interrupted = SStop △ exit → exited → Inactive`.
- CSP-M: [C7] 377-380: `share__choice(exit -> SKIP) ; ExitAction ; share__choice(exited -> SKIP) ;
  Inactive` for every state, simple and composite.
- This generator: Chapter 4 Rules 59 and 60 (`createSimpleStateAsActionParas`, `createCompositeStateAsActionParas`):
  `(SSTOP △ (exited → Skip)) ; Inactive_X`, as the CSP-M, since 6 Nov 2026 (branch `holcsp-fixes`, commit
  e7cf9ef); before, as the manual. Final states and junctions agreed with the CSP-M already.
- Kind: semantic (the manual's process refuses `share` between the exit action and `exited`).
- Background: the common protocol confirmed by Pedro: in a composite state the outer during action may write
  shared variables after the inner state's `exit` and before its `exited`; every waiting point of a node
  must accept `share`. Difference D1 of the proof repository: with the earlier HOL-CSP, checked with FDR on
  Legitimate: counterexample after interrupt and exit (the HOL-CSP form could do `share`, the CSP-M only
  `exited`); after the change all assertions pass, checked with FDR (Fang, Windows). For flat models both
  forms are acceptable for deadlock freedom.
- Status: DECIDED, CSP-M form adopted (6 Nov 2026).

## 4. Chapter 4 Rule 60: interrupt in the synchronisation set of Behaviour

- Manual: Chapter 4 Rule 60, p. 53 of the Aug 2022 PDF: the set `enteredSS(s) ∪ {|end, interrupt|}`: the whole channel `interrupt`.
- CSP-M: [C6]: `union(enteredSS, {| interrupt, terminate |})` inside the CSP-M module of the state, where
  `interrupt` is the one event of that module (channels are declared per module).
- This generator: `interrupt_<state FQN>` only (Chapter 4 Rule 60, `cs = enteredSS(self) ∪ {terminate,
  interrupt_S}`).
- Kind: form. The interrupt events of the inner nodes do not reach the synchronisation of Behaviour: inside
  the inner level they are images of the trigger events under the interrupt renaming of composeNodes (Rule
  41), so only the interrupt of the state itself can occur there (our analysis, research notes 5.8; not
  checked with FDR).
- Status: adopted (same as the CSP-M).

## 5. enteredSS: states only

- Manual: Chapter 4 Rule 29, p. 44 of the Aug 2022 PDF: `enteredSS = {|x : stm.nodes • entered.id(x)|}` (all nodes of the machine, in the
  where clause of that rule); Chapter 4 Rule 60, p. 53 of the Aug 2022 PDF, defines `enteredSS(s)` in its where clause:
  `enteredSS(s) = {| x : s.nodes @ entered.id(x) |}` (all nodes of the state, junctions included).
- CSP-M: [C7] 288-294: `entered` of the nodes that are states (`instanceof State`; final states are states).
- This generator: `enteredSS(nc)`: the `RC!State` nodes (a Final is a State).
- Kind: form (junctions have no `entered` event, so `entered.id(j)` of a junction never occurs).
- CORRECTED 17 Nov 2026: this entry said that `enteredSS(s)` of Chapter 4 Rule 60 is not defined in the manual and
  counted it as "manual incomplete". That was wrong: the where clause of Chapter 4 Rule 60 defines it (checked in the
  August 2022 PDF and in `chapter4.tex` of the manual's source).
- Status: adopted (same as the CSP-M).

## 6. Exit actions of inner states and the assignment of an input trigger (composite states)

- Manual: Chapter 4 Rule 54 (p. 50, Aug 2022 PDF) with Chapter 4 Rule 55 (p. 51, Aug 2022 PDF): a transition from a state with input trigger `e?x` is
  `e.id(src).in?x → (SStop △ set_x!x → Skip) ; (SStop △ exit → Skip) ; ...`; the same event, renamed to
  `interrupt.id(S)` (Chapter 4 Rule 41), interrupts the composite state S, whose inner states then run their exit
  actions inside Behaviour (Chapter 4 Rules 59, 60). The write `set_x!x` of Trans and these exit actions are not
  ordered. The exit action of S itself runs after `exit`, i.e. after the write.
- CSP-M: the same structure ([C1], [C7]); no check.
- This generator: the same structure. Since 16 Nov 2026 (branch `composite-states`) a check:
  `exitTriggerConflicts` (RC2Z.eol, at Chapter 4 Rule 60): for every transition leaving S whose trigger binds x, every
  state inside S (any depth) whose exit action reads or writes x (vars of Chapter 4 Rule 50) gives a warning; it is
  stored on the paragraph `Behaviour_<S>` (attribute `generatorNote`, entry 15), printed as
  `(* GENERATOR WARNING: ... *)` at S in the HOL-CSP and IsaCircus theories, reported by the plugin (dialog
  and log) and by dev-tools (a line `GENERATOR WARNING`). The generation is not stopped and the
  translation is unchanged. Test model: `robochart-models/composite-tests/c8_exit_input.rct` (proof
  repository): warnings for A (depth 1, writes x) and B (depth 2, reads x), none for S's own exit action
  and for C; checked with dev-tools. Not covered: x used inside an operation called by the exit action
  (entry 1, hazard).
- Kind: manual incomplete (a known problem of the semantics itself, the same in the manual and the CSP-M);
  the warning is a convention of this generator.
- Background: known problem, mentioned again by Pedro in his reply of 16 Nov 2026. The models of the
  proofs are not affected: Adaptation_Plan has no exit action and no trigger with a parameter
  (proof repository, `stm_nodes_and_parallel/COMPOSITE_NOTES.md`, Section 2).
- Status: warning implemented (16 Nov 2026); the semantics is unchanged.

## 7. Toolkit functions

- Manual: Appendix D (pp. 167-171, Aug 2022 PDF) lists the four toolkits (set p. 168, relation p. 169, function p. 170,
  sequence p. 171); nothing about how their functions are translated.
  Chapter 4 Rule 45 (`usedVariables`, p. 48 of the Aug 2022 PDF) is "standard and omitted".
- CSP-M: [C8]: `size` → `length` (sequence) or `card` (set); any other function → `<package>_<function>(args)`,
  defined in the library files `lib/semantics/<package>.csp`, copied into `csp-gen/defs/<package>_defs.csp`;
  statements guarded by `pre_<package>_<function>(args)` ([C9]; entry 11).
- This generator: `toolkitFunctions.eol` (table of Isabelle terms for the sequence and set toolkits;
  `hd`, `tl`, `butlast`, `@`, set operators, ...), RC2Z `toolkitCallName` (qualified names),
  `toolkitGuards`/`withToolkitGuards` (pre_ guards, placed and collected as [C9], toolkit functions only).
  Unsupported functions (relation and function toolkits, `range`, `iter`, `squash`, `extract`) and `size`
  of a set: the marker `GENERATOR_ERROR_...` (generatorErrors.eol) at which Isabelle stops.
- Kind: manual incomplete; convention (the Isabelle terms).
- Background: generator issue AA (16 Nov 2026).
- Status: adopted for the sequence and set toolkits; the guards are under reconsideration (entry 11, entry 16).

## 8. (WITHDRAWN 17 Nov 2026) Appendix D and the sequence toolkit: no difference

- This entry said (16 Nov 2026) that Appendix D announces four toolkits but shows only set, relation and
  function. That was WRONG: the sequence toolkit is on p. 171 of the August 2022 PDF (a diagram turned by
  90 degrees, `diagrams/toolkit/sequence_toolkit.pdf` in the manual's source, included since 2019); the
  first reading stopped at p. 170 of the Aug 2022 PDF.
- Manual: Appendix D has the four toolkits, and they agree with the `.rct` files in RoboTool's jar
  (`lib/robochart/{set,relation,function,sequence}_toolkit.rct` of
  `circus.robocalc.robochart.textual_3.1.0.202610211110.jar`): the same functions in each toolkit, and the
  definitions of the 22 functions of the sequence toolkit read one by one against the diagram (17 Nov 2026).
- Kind: none. The entry is kept under its number so that the numbers of the other entries do not change.

## 9. tail and front on the empty sequence

- Manual: no translation (entry 7). The sequence toolkit (Appendix D, p. 171 of the Aug 2022 PDF, the same as
  `sequence_toolkit.rct`) defines `tail(<>) = <>` and `front(<>) = <>`, no precondition.
- CSP-M: [C10]: `sequence_toolkit_tail(<x>^xs) = xs`, `sequence_toolkit_front(xs^<x>) = xs`: no case for
  `<>`, so FDR stops with an evaluation error on `tail(<>)`; `pre_sequence_toolkit_tail(s) = true`, no
  guard.
- This generator: `tl`, `butlast` (Isabelle: `tl [] = []`, `butlast [] = []`), as the toolkit definition.
- Kind: semantic (CSP-M against the toolkit's definition; this generator follows the toolkit).
- Background: our reading of the sources (16 Nov 2026); relevant to Replan of Adaptation_Plan (`tail` of a
  one-element ranking gives `<>`, which is defined).
- Status: adopted (toolkit definition).

## 10. Uninitialised variables

- Manual: Chapter 4 Rule 35, p. 45 of the Aug 2022 PDF: `Memory(v)` starts with the initial value if there is one, else with an internal
  choice over the type: `⊓ i : [[v.type]] • Memory(i)`.
- CSP-M: [C11]: `initialValue` = the initial expression or `typeDefaultValue` (0 for int and nat, `true`
  for boolean, `<>`, `{}`, the first literal of an enumeration, ...): a fixed value, not a choice.
- This generator: `getInitialOrDefaultValueAsExpr` / `defaultValueAsZExpr` (RC2Z.eol): fixed defaults as the
  CSP-M (int 0, boolean true since issue L, empty sequence, empty set; other types 0).
- Kind: semantic (fixed default against nondeterministic choice).
- Background: the comment in RC2Z says the CSP-M default is an FDR workaround. For the deadlock-freedom
  proofs at the ncCore level there is no memory and every value read is arbitrary, so the proofs cover the
  manual's choice as well. Relevant to the inner MakePlan of the FULL Adaptation_Plan: `modelScore` has no
  initial value and no action extends it; with the CSP-M default `<>` the index guard of imageToCheck blocks
  (FDR prediction D.2a, `stm_nodes_and_parallel/dti-plan-deadlock-fdr/`, NOT RUN). CORRECTED 16 Nov 2026:
  this is not so for the flat MakePlan of D1.3, whose modelToCheck appends `<0>` to modelScore (see the proof
  repository, `stm_nodes_and_parallel/DEFINEDNESS_NOTES.md`). In the flat model an arbitrary initial value
  (Chapter 4 Rule 35) of modelRanking would make the unguarded indices in the conditions undefined; the default `<>`
  does not.
- Status: as the CSP-M; to decide whether to follow the manual (choice over the type) in the memory.

## 11. Guards for division and sequence indexing

- Manual: none (Chapter 4 Rules 65-69, pp. 55-56 of the Aug 2022 PDF, translate statements without guards).
- CSP-M: [C9]: `right != 0` for a division, `i >= 1` and `i <= length(s)` for a sequence index, in the
  guard of an assignment, call, communication or if statement; not for transition conditions.
- This generator: not generated on `composite-states`; generated as the CSP-M on branch
  `index-division-guards` (e3f812b, NOT merged): 8 conditions in 3 transition actions of MakePlan, no
  change for ex10, Legitimate, c1-c6.
- Kind: manual incomplete (definedness, entry 16); semantic between `composite-states` and the CSP-M.
- Background (CORRECTED 16 Nov 2026): with the default initial values the index guards of the flat MakePlan
  of D1.3 always hold (predicted, FDR scripts `stm_nodes_and_parallel/definedness-fdr/` of the proof
  repository, NOT RUN), so the MODEL does not need their absence; the PROOF METHOD does: at the ncCore level
  the values read are arbitrary, so a guarded branch would be blocked for some values and the existing
  method would fail without invariants on the memory. The earlier statement "modelScore is never initialised,
  so MakePlan would deadlock" holds for the full Adaptation_Plan only. FDR scripts and the analysis of guards
  against definedness obligations: proof repository `stm_nodes_and_parallel/DEFINEDNESS_NOTES.md`.
- Status: PENDING (Fang, 16 Nov 2026): the branch stays unmerged until entry 16 is decided with Ana.

## 12. Chapter 4 Rules 45 to 50: vars and usedVariables

- Manual: Chapter 4 Rule 45 (p. 48, Aug 2022 PDF) `usedVariables` "standard and omitted"; Chapter 4 Rule 49 (p. 49, Aug 2022 PDF) `vars(t) =
  vars(t.condition) ∪ vars(t.action) ∪ vars(t.deadline)`, but `vars` is defined only for nodes, transitions
  and statements (Chapter 4 Rules 46-51), not for expressions; Chapter 4 Rule 50 (p. 49, Aug 2022 PDF) has no case for an INPUT
  communication statement (it writes its parameter) and uses `usedVariables(s.left)` for the left-hand side
  of an assignment (an assignable, not an expression).
- CSP-M: computes the variable sets differently (entries 1, 2: all variables of the machine).
- This generator: `vars` (RC2Z.eol, Chapter 4 Rules 46-51), corrected 15 Nov 2026: Chapter 4 Rule 49 uses usedVariables for the
  condition and the deadline; Chapter 4 Rule 50 takes the variable of the left-hand side (`rootVarName`, also for an
  indexed one) and the parameter of an INPUT communication; Chapter 4 Rule 47 returns a set instance.
- Kind: manual incomplete.
- Status: corrections adopted (comments at the rules in RC2Z.eol).

## 13. Naming and declaration conventions

- Manual: `eventId(e).id(n).in`, `enter.id(n)`, ... as channels with node identifiers; no naming scheme for
  a theory.
- CSP-M: CSP-M modules per node container; channels `enter`, `entered`, `interrupt`, `internal__` declared
  per module; one pair `exit`/`exited` for the machine.
- This generator (conventions): fully qualified names, innermost first, joined by `_`
  (`enter_A_S1_S_C6_composite_c6`); a trigger channel per container `<e>_<container FQN>__in`
  (`triggerChan`, 14-15 Nov 2026); the channel declarations split per container (`createchanParaNC`:
  container part and machine part); one pair `exit`/`exited` per container (the inner pair is hidden inside
  `Behaviour`, so the outside sees one pair as in the CSP-M; equivalence NOT checked with FDR).
- Kind: convention (and form for the exit/exited pairs).
- Background: generator CLAUDE.md, decision of Fang of 14 Nov 2026 (scheme A); research notes 3.4, 4.1.
- Status: adopted on `trigger-fqn-naming` and `composite-states` (not yet on `main`).

## 14. Implementation forms of the CSP-M (no difference in meaning)

- D3: the node's `Inactive` is closed with the exception operator, `Inactive [| {terminate} |> SKIP`
  ([C7] 392), where the manual (and this generator) have `Termination = end → Skip` in a choice.
- D4: entry actions with a doubled `share__choice` and `true &`.
- D5 (17 Nov 2026): the During of a composite state (Chapter 4 Rule 60) has `RUN(enteredSS(s))` in the manual
  and `CRUN(enteredSS)` in the CSP-M ([C7] 389, 412). The untimed library defines `CRUN(x__) = RUN(x__)`
  ([C12], `state.csp` line 20, copied into `csp-gen/defs/state_defs.csp`), with FDR's built-in `RUN`: the same
  process. The timed library defines `CRUN(x__) = RUN(union(x__,{tock}))` (`state_timed.csp` line 21), which
  belongs to the timed semantics and is not compared here. Checked by reading the sources, not with FDR.
- Kind: form. Source: proof repository CLAUDE.md (FDR comparison on Legitimate, all assertions pass,
  checked with FDR).
- Status: no action.

## 15. Metamodel deviations from CZT

- `TupleSelExpr.arity` (15 Nov 2026, issue Y): the number of components of the selected tuple, so that the
  templates print the projection of an n-tuple; CZT keeps it in the product type.
- `ActionPara.generatorNote` (16 Nov 2026, entry 6): a warning of RC2Z about the paragraph, printed as a
  comment; no meaning in Circus.
- Kind: convention. Both are marked in `metamodels/circus_metamodel.ecore` by an annotation
  `http://www.robocalc.circus/Circus/deviation` (key `czt`) and documented by a GenModel annotation.
- Status: adopted.

## 16. Undefined expressions (definedness)

- Manual: nothing. Chapter 4 Rules 65-69 (pp. 55-56, Aug 2022 PDF) translate an assignment as `SStop △ set_x![[e]] → Skip`, a
  communication with `[[value]]`, an if statement with `[[s.expression]]`, without any guard; `[[e]]Expr` is
  used (e.g. Chapter 4 Rule 35) but not defined; the preconditions of functions appear only in the metamodel
  (Function.preconditions, p. 155 of the Aug 2022 PDF) and in the informal overview (p. 148, Aug 2022 PDF). No rule says what happens when an
  expression is undefined (head of an empty sequence, division by zero, an index out of range, a function
  applied outside its precondition). Checked by reading the manual, 16 Nov 2026.
- CSP-M: [C9]: guards `pre_<f>(args)`, `right != 0`, `1 <= i <= length(s)` before the statement, so that the
  branch with an undefined expression BLOCKS. This is needed for the encoding (FDR cannot evaluate an
  undefined expression; without the guard it stops with an error); its side effect is that undefinedness
  shows as deadlock.
- This generator: `composite-states`: pre_ guards for toolkit functions only (entry 7); no guards for
  division and indexing (entry 11 on a separate branch); for `x = f(args)` an older guard copies the
  precondition of f verbatim with the formal parameters NOT replaced by the arguments (generator issue CC,
  present since the initial commit a8177dc; not reached by the present models). In HOL functions are total:
  without a guard an undefined expression has an unspecified value (`hd []`, `x div 0`, `xs ! i` out of
  range) and the process goes on; a deadlock-freedom result is then more optimistic than the CSP-M's.
- Alternative under consideration: no guards in the HOL-CSP; the generator lists definedness proof
  obligations (for each expression with a precondition: whenever execution reaches it, the precondition
  holds), proved separately from deadlock freedom.
- Kind: manual incomplete; semantic between the CSP-M and the unguarded HOL-CSP.
- Status: PENDING, to be discussed with Ana (Fang, 16 Nov 2026). Until then: the existing pre_ guards of
  `composite-states` (head, last, min, max) stay as they are, `index-division-guards` stays unmerged, issue CC
  is not fixed.
