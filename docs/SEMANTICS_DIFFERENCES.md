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
  Rule numbers are unchanged in the source of June 2026 (master 04e7b96, which adds only Appendix F); a build
  of that source on the Linux workstation (17 Nov 2026) gives the same rule numbers and the same pages as the
  August 2022 PDF (List of rules and Chapter 4 compared page by page); pages are cited for that PDF. Annotated copy:
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
  - [C13] [CStateMachineGenerator.xtend 83-162, `channelDeclarations`](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStateMachineGenerator.xtend#L83-L162);
    node modules [CStateGenerator.xtend 176-336](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStateGenerator.xtend#L176-L336);
    `NIDS` [CTransitionGenerator.xtend 39-47](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CTransitionGenerator.xtend#L39-L47)
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
- This generator: the same structure, the same as the manual and the CSP-M. From 16 to 23 Nov 2026 (branch
  `composite-states`) a check `exitTriggerConflicts` (RC2Z.eol, at Chapter 4 Rule 60) warned when an exit action of
  a state inside S (any depth) read or wrote the variable bound by the input trigger of a transition leaving S
  (dialog, log, comment `(* GENERATOR WARNING: ... *)`). REMOVED on 23 Nov 2026 (Fang): RoboTool's CSP-M has the
  same unordered structure (read in robochart-csp-gen: CTransitionGenerator.xtend 57-80 `compileTransition`,
  189-203 `compileTriggerForMemory`: `e__!id.in?x -> share__choice(set_x!x -> SKIP) ; share__choice(exit -> SKIP)
  ; ...`; CStateGenerator.xtend 482-503 `tidsR`: `S::interrupt` renamed to the trigger event; 358-414 `Node_rule`:
  the inner states run `exit ; exit action ; exited` inside Behaviour after the interrupt), and RoboTool gives no
  hint (no check in the RoboChart validator, circus.robocalc.robochart.textual 3.1.0.202610211110; none in the
  CSP generator). The warning mechanism (`generatorNote`, entry 15) stays; the name clash of GG uses it. Test
  model `robochart-models/composite-tests/c8_exit_input.rct` (proof repository) stays: no warning, loads.
- Kind: manual incomplete (a known problem of the semantics itself, the same in the manual and the CSP-M).
- INTERNAL NOTE (23 Nov 2026): this is a known problem of the RoboChart semantics (Pedro, reply of 16 Nov 2026):
  the value of x seen or left by such an exit action depends on the interleaving. It is no longer reported by the
  generator. The models of the proofs are not affected: Adaptation_Plan has no exit action and no trigger with a
  parameter (proof repository, `stm_nodes_and_parallel/COMPOSITE_NOTES.md`, Section 2). Not covered by the
  former check either: x used inside an operation called by the exit action (entry 1, hazard).
- Status: same as the manual and the CSP-M; the check was removed on 23 Nov 2026.

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
- The pre_ guards in detail (checked by reading the code, 21 Nov 2026; `toolkitFunctions.eol`, RC2Z
  `toolkitGuards`, `toolkitGuardsOf`, `withToolkitGuards`, `createStmtAsAction`, `createStmtInContextAsAction`
  on `composite-states` 251834f, against [C9] and `lib/semantics/{sequence,function}_toolkit.csp` of the
  CSP-M generator; nothing generated or run):
  - Which functions. Of all four toolkits, only six functions declare a precondition, all in the sequence
    toolkit (`lib/robochart/*.rct` of `circus.robocalc.robochart.textual_3.1.0.202610211110.jar`):
    `head`, `last` (`isNonEmpty(s)`), `min` (`hasMin(a)`), `max` (`hasMax(a)`), `size` (`isFinite(a)`),
    `squash` (`isFiniteFunction(f)`). This generator guards four of them: `head`, `last` (`s ≠ []`), `min`,
    `max` (the existence of a least / greatest member). `size`: the table entry `pre_sequence_toolkit_size`
    is `""`, so `toolkitGuardsOf` adds no guard; `size` is supported for sequences only, for which
    `isFinite` holds. The CSP-M writes `pre_sequence_toolkit_size(s)`, defined as
    `function_toolkit_isFinite(s)`, which the CSP-M library defines as `true` for every argument (sets and
    sequences; its comment: "sets in our encoding in CSPM are always finite"): the same process. `squash`:
    not supported (`toolkitCallName` gives the marker
    `GENERATOR_ERROR_unsupported_toolkit_sequence_toolkit_squash`, and `toolkitGuardsOf` adds no guard for a
    marker); Isabelle stops at the marker, so there is no output to compare. In the CSP-M its guard
    `pre_sequence_toolkit_squash(f)` = `function_toolkit_isFiniteFunction(f)` = `function_toolkit_isFinite(f)`
    = `true`. So of the six, the CSP-M guards that can be false are those of `head`, `last`, `min`, `max`,
    exactly the four guarded here. Also different: the CSP-M guards the preconditions of EVERY function with
    a precondition (`pre_<id>(args)` also for user-defined functions); this generator, by AA, only those of toolkit
    functions (`toolkitGuardsOf` requires a toolkit call name); user-defined functions only for
    `x = f(args)` (CC, below).
  - Where. In both generators only in the four kinds of statement (assignment, operation call,
    communication statement, if statement), as a guard of the statement placed after the reads of the
    statement and before it (CSP-M `readState(vars, guard & stmt)`; here `createStmtInContextAsAction`:
    `readState(usedVariables(self), ...)` around `withToolkitGuards(...)`). The calls collected are those in
    the statement's own expressions (for a communication its value, for an if statement its condition, for
    an assignment its right-hand side and the index of an indexed left-hand side), descending ONLY through
    divisions, sequence indexing (`s[i]`: base and index) and the arguments of calls, in both. NOT guarded,
    in both: transition conditions (CSP-M `compileTransitionCondition`, CTransitionGenerator.xtend 50, prints
    the condition alone; here conditions do not go through `createStmtAsAction`); calls below any other
    operator, e.g. `x = 1 + head(s)`, `b = head(s) > 0`, in the value or body of a `let`, in a conditional
    expression, under a tuple selection (`(head(s))[1]` is not a sequence index). Entry, during and exit
    actions consist of statements and are guarded as above. So the positions are the same as in the CSP-M;
    the differences are the set of functions (above) and the index and division guards of [C9], which are not
    generated on this branch (entry 11). For an unguarded call the meanings differ: the CSP-M has only
    `sequence_toolkit_head(<x>^xs) = x`, so an unguarded `head(<>)` (e.g. in `x = 1 + head(s)`) makes FDR's
    evaluation fail (as `tail(<>)`, entry 9); in Isabelle `hd []` is an unspecified value and the process
    goes on.
  - A second, older guard (generator issue CC). Before 22 Nov 2026: in `createAssignmentStmtAsAction`, an
    assignment whose right-hand side is itself a call of a function with preconditions was guarded by the
    precondition expressions of the function, with the FORMAL parameters not replaced by the arguments; for a
    toolkit function this came in addition to the guard above. Generated on 22 Nov 2026 with the rules of
    8c1a37c (dev-tools GenAll): `entry x = head(s)` in the toy models d1 and d2 got `s ≠ []` twice (the
    second with the bare formal `s`, which is not the machine's variable `s_D1_...`); `x = head(q)` in test
    model c9 got `q_C9_... ≠ []` and `s ≠ []`; `y = small(a + 1)` got `n * 2 < 10` with the formal `n`; a
    precondition with a quantifier stopped the generation ("Called method 'createMemPredEquality' on
    undefined object").
  - FIXED 22 Nov 2026 (`composite-states`; decision of Fang, 22 Nov 2026: this release keeps the guards for
    preconditions, as the CSP-M, PROVISIONALLY, and only CC is fixed). `userPreconditionGuard`,
    `substitutedPrecondition`: only USER-DEFINED functions (a toolkit function is skipped, AA guards it);
    the arguments replace the formal parameters by position (on a copy of the precondition; the printers
    print every compound argument in parentheses); a bound name of the precondition (quantifier, lambda,
    set comprehension, let) that occurs in an argument is renamed first (`g` to `g_1`); a precondition that
    `createExpr` cannot translate (quantifiers: the generator translates none, in any position) is printed
    as the guard marker `GENERATOR_ERROR_untranslated_precondition_<f>` instead of stopping the generation.
    Machine variables cannot be captured: RC2Z gives them the suffix of their container before
    translating, so only names such as a function name can clash. Test models c9, c9b (proof repository,
    `robochart-models/composite-tests/`), results in `docs/COMPOSITE_RELEASE_TEST.md`.
  - Remaining difference to the CSP-M (NOT changed, decision of Fang, 22 Nov 2026: record, do not extend):
    the guard of a user-defined function is added ONLY for an assignment whose right-hand side is the call
    itself (`x = f(args)`), as before the fix. The CSP-M guards user-defined functions as toolkit
    functions: in all four kinds of statement and in the positions described above (`y = 1 + f(a)` is not
    guarded in either; `y = g(f(a))`, `send!f(a)`, `if f(a) > 0 then ...` and an operation call with
    argument `f(a)` are guarded in the CSP-M, not here). Order where both apply: the guards of AA outside,
    the guard of CC inside, both after the reads and before the statement.
  - Status: the guards are PROVISIONAL. The conclusion of Ana and Alvaro (January 2026) is that the right
    semantics is that of the manual, which does not treat a precondition as a guard, and that the guards of
    the CSP-M are a pragmatic choice; the ITP 2026 paper uses the semantics with guards. When preconditions
    become definedness proof obligations (entry 16), the guards of AA and of CC are removed in the same
    change.

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
- FDR (checked with FDR, 19 Nov 2026, Windows, FDR 4.2.7; proof repository, branch `trigger-fqn-naming`,
  `stm_nodes_and_parallel/definedness-fdr/`, toy models d1-d3 with `s = tail(s)` in a transition action):
  "Compilation Failed: Pattern match failure whilst attempting to evaluate: sequence_toolkit_tail(<>)"
  for every assertion. So in the CSP-M `tail(<>)` neither BLOCKS (as a guarded statement does: the index and
  division guards of entry 11 and the `pre_` guards of head and last make the statement STOP) nor takes
  SOME VALUE: it makes FDR's evaluation fail, and the model cannot be checked at all. FDR stopped also in two
  models in which the call is not reachable with `<>` in the composed machine: it evaluates each sequential
  component on its own, every input over its whole type (inferred from these results), so a guard
  protects a call only if it comes after the last read of the variable; the CSP-M's statements read their
  variables again (`get_x?x` before each statement), so a condition or an `if` on an earlier read does not
  protect them (Replan of Adaptation_Plan: `if size(modelRanking) > 0 then modelRanking = tail(modelRanking)`).
  Against this generator: no guard either, but Isabelle's `tl [] = []` is DEFINED (it is `hd []` that is
  unspecified), so the HOL-CSP follows the toolkit and the process goes on. RoboTool emits `pre_` guards only
  for toolkit functions with a precondition (CStatementGenerator.xtend 535-546: head, last, size, min, max,
  squash); tail and front have none. Analysis and changed predictions: proof repository
  `stm_nodes_and_parallel/DEFINEDNESS_NOTES.md`, Section 5.

## 10. Uninitialised variables

- Manual: Chapter 4 Rule 35, p. 45 of the Aug 2022 PDF: `Memory(v)` starts with the initial value if there is one, else with an internal
  choice over the type: `⊓ i : [[v.type]] • Memory(i)`.
- CSP-M: [C11]: `initialValue` = the initial expression or `typeDefaultValue` (0 for int and nat, `true`
  for boolean, `<>`, `{}`, the first literal of an enumeration, ...): a fixed value, not a choice.
- This generator: `getInitialOrDefaultValueAsExpr` / `defaultValueAsZExpr` (RC2Z.eol): fixed defaults as the
  CSP-M. Since 23 Nov 2026 (fix of generator issue KK) the same values as `typeDefaultValue` of the CSP-M
  (robochart-csp-gen, untimed/GeneratorUtils.xtend 285-326): int, nat, real and a primitive type declared in the
  model (`type T`, declared `type_synonym T = nat`) 0; boolean `True`; string `''''`; an enumeration its first
  literal, applied to the defaults of its parameters (`Circle (0)`); a record the record of the defaults of its
  fields (`\<lparr>Point.x = 0, Point.y = 0\<rparr>`; the templates now also declare `char :: discrete_cpo`, so that a
  string variable can be the parameter of its Memory fixrec); a sequence `[]`; a set `{}`; a tuple the tuple of the
  defaults; a function type the constant function `(\<lambda>_. d)` (CSP-M `funcToSet(\ x_ @ d, A)`). Relation,
  vector and matrix types are not supported by this generator: their type and their default are the markers
  `GENERATOR_ERROR_type_<kind>` and `GENERATOR_ERROR_default_value_<kind>` (Isabelle stops there; the CSP-M gives
  `{}` and tuples). Before (also on main 2976313): 0 for every type other than int and boolean, a type error in
  Isabelle for enumerations, records and strings (KK).
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
- Status: the default values are those of the CSP-M (since 23 Nov 2026 for every type, issue KK), DIFFERENT FROM
  THE MANUAL (Chapter 4 Rule 35: a choice over the type); to decide whether to follow the manual in the memory.

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
  division and indexing (entry 11 on a separate branch); for `x = f(args)` with f user-defined, a guard by
  the preconditions of f with the arguments in place of the formal parameters (generator issue CC, fixed
  22 Nov 2026; before, the preconditions were copied with the formal parameters, also for toolkit
  functions; entry 7). In HOL functions are total:
  without a guard an undefined expression has an unspecified value (`hd []`, `x div 0`, `xs ! i` out of
  range) and the process goes on; a deadlock-freedom result is then more optimistic than the CSP-M's.
- Alternative under consideration: no guards in the HOL-CSP; the generator lists definedness proof
  obligations (for each expression with a precondition: whenever execution reaches it, the precondition
  holds), proved separately from deadlock freedom.
- Kind: manual incomplete; semantic between the CSP-M and the unguarded HOL-CSP.
- Status: PENDING, to be discussed with Ana (Fang, 16 Nov 2026). DECISION for the release of
  `composite-states` (Fang, 22 Nov 2026): the guards for preconditions stay, as in the CSP-M, PROVISIONALLY
  (pre_ guards of toolkit functions, AA; guards of user-defined functions for `x = f(args)`, CC, now with the
  arguments); `index-division-guards` stays unmerged. When preconditions become definedness proof obligations,
  the guards are removed in the same change.

## 17. Channel declarations (17 Nov 2026)

- Manual: the untimed rules of Chapter 4 (Rules 1 to 73) use channels and events (`enter`, `entered`,
  `exit`, `exited`, `interrupt`, `internal`, `end`, `share`, `get_x`, `set_x`, `set_EXT_x` / `set_Ext_x`,
  `setL(v)`/`setR(v)` of Definition C.2.2, `eventId(e).in/out`, `eventId(e)__.id(src).in`, `o.nameCall`) and the
  identifiers `vid`, `eventId`, `id` without declaring them: parameters, their types and the place of the
  declaration are not given. Inconsistencies inside the manual: `set_EXT_` (Chapter 4 Rules 3, 25, 37)
  against `set_Ext_` (Chapter 4 Rules 12, 19, 20; a typo, confirmed by Alvaro on 26 Nov 2026); `shared` in Chapter 4 Rule 25 where `share` is meant (the
  CSP-M has `share__`); the introduction of Chapter 4 says that RoboTool realises the identifiers as qualified
  names, but the CSP-M uses plain names inside CSP-M modules.
- CSP-M: declared per CSP-M module: the state machine module declares `internal__ : NIDS`, `interrupt`,
  `exit`, `exited`, `terminate`, `get_x, set_x, setL_x, setR_x : T` for every variable, `set_EXT_x` for the
  required ones, `e__ : NIDS.InOut[.T]` and `e : InOut[.T]` for every event, `oCall` for operations without
  definition ([C13] lines 83-162); each node is a module with `enter`, `entered`, `interrupt` (and
  `enteredL`, `enteredR`, declared but unused), so `enter.id(n)` is `n::enter`; a composite state's module
  declares its own `NIDS`, `internal__` and `e__`; `datatype NIDS = NID_<node> | ...` per node container;
  `share__` and `datatype InOut = in | out` in the library. Names differ: `end` is `terminate`, `share` is
  `share__`, `internal` is `internal__`.
- This generator: its own conventions (entry 13), not part of the manual.
- Kind: manual incomplete.
- Manual annotation: Appendix G "Channel declarations in RoboTool's CSP-M" of the annotated manual
  (`laila-fangyan/robochart-reference`, branch `csp-m-differences`, shown with the notes switched on), with
  the tables of declarations and of correspondence, and a note at the start of Chapter 4.
- Status: recorded.

## 18. If statement without else (22 Nov 2026)

- Manual: Chapter 4 Rule 68 translates an if statement with its then and else statements; the case of a statement
  `if e then s end` without else is not written as a rule here (not checked further against the manual's
  grammar section).
- CSP-M: [CStatementGenerator.xtend 272-274](https://github.com/UoY-RoboStar/robochart-csp-gen/blob/a6412b40c83ded4ab2c18c2b9bd28575ec270983/circus.robocalc.robochart.generator.csp/src/circus/robocalc/robochart/generator/csp/comp/untimed/CStatementGenerator.xtend#L272-L274):
  `if e then (s) else SKIP` when there is no else.
- This generator: until 22 Nov 2026 a one-branch guarded command, printed `if b then P` without else, which
  Isabelle does not parse (and, in a state action, a stack overflow before that: generator issue FF). Since
  22 Nov 2026 (`createIfStmtAsAction`, RC2Z.eol): the missing else is `Skip`, as the CSP-M.
- Kind: form (now the same meaning as the CSP-M). Status: adopted (fix of issue FF, Fang's task of 22 Nov 2026).

## 19. Records, enumerations with parameters, and the names of the generator (23 Nov 2026)

- Manual: records (`datatype`/`record`) and enumerations with parameters are types of the model; the semantics of
  their expressions (field access `r.f`, record literal `R(| f = e |)`, field assignment `r.f = e`, literal with
  arguments `E::L(e)`) is that of the underlying data language and not given in Chapter 4.
- CSP-M: the default value of a record is the tuple of the defaults of its fields, that of an enumeration the first
  literal with the defaults of its parameters, `L.d` (GeneratorUtils.xtend 285-326, `typeDefaultValue`); the
  translation of field access and record literals in the CSP-M was not read for this entry.
- This generator, since 23 Nov 2026 (fixes of generator issues LL, MM, NN): a record is an Isabelle `record`; field
  access `r.f` is `(R.f r)`, a record literal is `\<lparr>R.f = e, ...\<rparr>` (fields in the order of the
  declaration), a field assignment is `set_r!(r\<lparr>R.f := e\<rparr>)` after reading r. Field names are QUALIFIED
  by the record (`R.f`): with two records that have a field of the same name the unqualified name refers to the
  record declared last (checked in Isabelle: `x p0` is a type error for `p0 :: Point` after `record Pixel = x ::
  int`), and an unqualified field name clashes with any other constant of the theory. An enumeration literal with
  parameters is a constructor with arguments (`datatype Shape = Circle "real" | Square`, `Shape::Circle(1.0)` is
  `(Circle (1.0))`). The variables of the theory's own definitions are named so that they cannot be the name of a
  constant declared for the model: `x__a`, `y__a` (discrete_cpo instances), `n__id` (parameter of `Trans`),
  `value__v` (parameter of the `Memory` processes), `b__a`, `Q__a`, `P__a` (`assume`, `guar`); before, `x`, `y`,
  `n`, `value`, `b`, `Q`, `P` were captured by a record field, enumeration literal or function of that name
  (NN). Names ending in `_` cannot be used (Isabelle: "Bad name"). Record field names and datatype constructor
  names (enumeration literals, node identifiers) are printed QUOTED in their declarations (`"value" :: "int"`,
  `datatype Letter = "b" | ...`): a name that is an Isabelle keyword (e.g. `value`, `where`) cannot be written bare
  there (checked: outer syntax error for a field `value`; quoted, it loads). Still reserved: the generated constants
  `share`, `terminate`, `aviol`, `gviol`, `SSTOP`, `assume`, `guar` and the channel and process names; a model
  element with one of these names clashes (not checked by the generator).
- Kind: form (translation of the data language), convention (names).
- Status: adopted (23 Nov 2026). Test models: proof repository, `robochart-models/regression-tests/`.
  Known limits: the templates declare all enumerations before all records, so an enumeration literal with a
  parameter of record type does not load; RoboTool's validator (circus.robocalc.robochart.textual
  3.1.0.202610211110) throws a ClassCastException on a literal with arguments (`constantConstructionWellTyped`),
  the editor shows "Error executing EValidator", the model is nevertheless valid.

## 20. Channel names and the correspondence with the CSP-M events (25 Nov 2026)

- Manual: `vid`, `eventId` and `id` "calculate unique identifiers" for variables, events and nodes; one possible
  implementation is the qualified name (chapter4.tex 14-18). Nothing more is required of the names.
- CSP-M: CSP-M modules are name spaces; the same short name is used in several modules (`m1::set_EXT_obs`,
  `m2::set_EXT_obs`, `ctrl::set_obs`), and the composition renames between them (`ObsD::set_obs <- set_obs`).
- This generator: HOL-CSP and IsaCircus channels are global (one datatype `chan_event`, one `chantype`).
  PRINCIPLE (Fang, 25 Nov 2026): the channel names are NOT required to agree literally with the CSP-M. They are
  required (1) to be unique, as the manual requires of `vid`, `eventId`, `id`, and (2) to correspond injectively to
  the events of the CSP-M, such that under this correspondence the behaviour is the same. A comparison with
  RoboTool's CSP-M by FDR renames by this correspondence first and then checks the refinements in both directions.
- Kind: convention.
- Status: principle adopted 25 Nov 2026. How it is applied at the controller level (names printed inside the
  machine paragraphs, or the machine paragraphs as for a machine alone and a renaming at the composition) is open:
  CONTROLLER_PLAN.md 1.1 and 7.10.

## 21. Constants at the controller and the machine level (25 Nov 2026)

- Manual: Chapter 4 Rule 13 `constInit` (upstream 04e7b96 chapter4.tex 265; branch csp-m-differences 282) initialises
  the constants of a controller or module by events `set_vid(c)!value` or `set_vid(c)?name(c)`; Rule 19 (upstream
  376-412, branch 393-429) has `lconsts` and `rconsts` (`set_vid(c)` of the local and required constants) in its
  synchronisation set and `lconsts` in its hidden set; Rule 20 (upstream 416-440, branch 433-457) calls `constInit`.
  The corresponding rule for machines, `constInitSTM`, is commented out in the source (upstream 1672, branch 1845).
- CSP-M: at both levels the constants are PARAMETERS of the processes (`STM(id__, const_ctrl_ObsD_dest)`,
  `D__(id__, const_ctrl_dest)`, `Memory(id__, obs, dest)`); no `set_` event of a constant exists.
- This generator: at both levels the constants are Isabelle `consts` (or `definition` with an initial value), which
  is the meaning of the CSP-M's parameters (a fixed value for the whole process, arbitrary when not initialised).
  No `set_` event of a constant, no constant as a parameter of the controller memory (decision 7 of Fang,
  25 Nov 2026). Before, RC2Z printed the controller's constants as memory parameters, `set_<c>` (never declared) in
  both sets of Rule 19 and a renaming `set_c <- set_c` in Rule 22, with the comments "TBC need to confirm with Pedro,
  should we always use const's name or we can use the value of it if exists?" (Rule 19) and "TBC, set_ + v.name
  should be replace by vid(v,c)??" (Rule 22): it followed Rules 13, 19, 20 of the manual at the controller level,
  while the machine level already used `consts`.
- Kind: form (a constant as a logical constant instead of an initialising event).
- Status: adopted 25 Nov 2026 (controller-layer).

## 22. Rule 22 and Rule 19: the controller memory's writes into the machines' copies (25 Nov 2026)

- Manual: Rule 20 (upstream 420, 424; branch 437, 441) writes a controller variable v into the copy of each machine m
  that requires it, `set_Ext_vid(v,m)!x`. Rule 22 (renamingMachine, upstream 474, branch 491) renames the machine's
  constants `set_vid(v) <- set_vid(v,c)` but has NO pair for the variables, so the machine's input `set_EXT_vid(v)`
  (Rule 25) is never connected to `set_Ext_vid(v,m)`; Rule 19's synchronisation and hidden sets (upstream 390,
  394-396; branch 407, 411-413) do not contain `set_Ext_vid(v,m)` either. Read literally the copies are open to the
  environment of the controller (FDR experiment of 25 Nov 2026, proof repository `robochart-models/controller-fdr/
  rule19-sync/`: distinguishable from the CSP-M in traces, failures, deadlock and divergence).
- CSP-M: the memory writes `m::set_EXT_v!x`, the event of m's memory itself (no renaming needed, the module is the
  name space); `m::set_EXT_v` is in the synchronisation set between the machines and the memory and hidden
  (CControllerGenerator `compileAux`; CMemoryGenerator 214, 219).
- This generator (variant (ii) of CONTROLLER_PLAN 7.10): the machine paragraphs are those of the machine alone
  (`set_EXT_v`); `renamingMachine_<m>` adds the pair `set_EXT_v <- set_EXT_v_<FQN m>` (the CSP-M's `m::set_EXT_v`
  in the global name space), as Rule 22 does for constants: ADDED BY US after Rule 22's treatment of constants. The
  copies `set_EXT_v_<FQN m>` are in the synchronisation set of Rule 19 and hidden: ADDED BY US after the CSP-M.
  Both are an OMISSION OF THE MANUAL. Spelling `set_EXT_` throughout (the manual writes `set_Ext_` in
  Rules 15, 20 and `set_EXT_` in Rule 25).
- Answer of Alvaro (26 Nov 2026): `set_EXT_` / `set_Ext_` is a typo (one name). The definition of `rvars` in Rule 19
  is wrong; it should be `rvars = {| v : requiredVariables(c); m : ms @ set_Ext_vid(v,m) |}`, used in both the
  synchronisation set and the hidden set. Follow-up question of Fang (26 Nov 2026, OPEN): should it cover the
  controller's LOCAL variables too, i.e. `{| m : ms; v : requiredVariables(m) @ set_Ext_vid(v,m) |}`? That is the
  CSP-M (`{| stm::set_EXT_x |}` for every machine that requires x) and the present implementation; with Alvaro's
  formula over `requiredVariables(c)` only, the copies of a controller-local variable (ctl2, the running example)
  would again be neither synchronised nor hidden.
- Kind: error of the manual (confirmed by Alvaro, 26 Nov 2026).
- Status: adopted 25 Nov 2026 (controller-layer); confirmed in principle by Alvaro on 26 Nov 2026; the extent of
  `rvars` (required or all variables of the machines) still open, the implementation stays as the CSP-M.

## 23. Events of the machines in a controller: unconnected and internal connections hidden (25 Nov 2026)

- Manual: Rules 21-23 neither hide the events of a machine that are not connected nor the events of a connection
  between two machines; they stay visible events of the controller.
- CSP-M: `RenamingStateMachineChanset` hides the unconnected events of each machine (`\ {| stm::e |}`,
  CControllerGenerator 408-429); `composeStateMachines` hides the events of the connections between the machines
  after the composition (`\ diff(intersection, {|terminate|})`).
- This generator: as the CSP-M. The unconnected events are hidden in `renamingMachine_<m>` (before the renaming, which
  does not touch them; equivalent to the CSP-M's order); the internal connection events, `e_<FQN source>_in/out`,
  are hidden in `composeMachines_<c>`. Hiding the unconnected events: decision 2 of Fang (25 Nov 2026), TO ASK PEDRO.
  Hiding the internal connections: needed for the FDR comparison with the CSP-M (otherwise extra visible events);
  confirmed by Fang on 26 Nov 2026. Both (unconnected and internal connections): TO ASK PEDRO.
- Kind: difference between the manual and the CSP-M; this generator follows the CSP-M.
- Status: adopted 25 Nov 2026, confirmed 26 Nov 2026 (controller-layer); question to Pedro open (unconnected events
  and internal connections). The effect of hiding an unconnected event
  that triggers a self-loop (divergence): `robochart-models/controller-fdr/termination-unconnected/`.

## 24. Rule 19: the controller's own required variables are not hidden (25 Nov 2026)

- Manual: Rule 19 hides `rvars`, the `set_Ext_vid(v)` of the variables the controller requires (upstream 394-396,
  406-407; branch 411-413, 423-424).
- CSP-M: hides only `set_v`, `get_v` of the local variables and the machines' copies; the controller's
  `set_EXT_v` stays visible (it is the module's write into the controller, Rule 12/1 at the module level).
- This generator: as the CSP-M (before 25 Nov 2026 RC2Z hid `rvars` as the manual). Not exercised by a test model
  (no controller of the tests requires a variable); relevant at the module level (step 4).
- Kind: difference between the manual and the CSP-M (the manual's hiding would disconnect the module memory).
- Answer of Alvaro (26 Nov 2026): with his corrected `rvars` (entry 22), the controller's own `set_Ext_vid(v)` is no
  longer in the hidden set. So the manual, once corrected, agrees with the CSP-M and with this generator.
- Status: adopted 25 Nov 2026, confirmed by Fang 26 Nov 2026; agrees with Alvaro's correction of Rule 19
  (26 Nov 2026).

## 25. Initial value of a machine's copy of a controller variable (25 Nov 2026)

- Manual: not read for this point.
- CSP-M: the machine's copy starts with the default of the type (`sharedVarMemory(id__) = Memory_obs(0)`), not with
  the initial value of the controller's variable; the controller memory starts with that value (`Memory(id__, 5)`).
  Checked with FDR (25 Nov 2026, proof repository `robochart-models/controller-fdr/shared-init/`): with
  `var obs : int = 5` in the controller and a machine that reads obs before any write, the machine reads 0.
- This generator: the same (`sharedVarMemory_<m> = sharedMemory_obs⋅0`).
- Answer of Alvaro (26 Nov 2026): a by-product of the propagation mechanism (the copies receive the value only through
  the memory's forwarding of a write), and intended behaviour. Kept as it is; to be discussed in a GitHub issue
  (draft: CONTROLLER_PLAN Section 8, "To do").
- Kind: intended behaviour of both generators (Alvaro, 26 Nov 2026); open for discussion.
- Status: kept (Fang, 26 Nov 2026); nothing changed; GitHub issue drafted, to be filed by Fang.

## 26. Termination of the controller: option 2 instead of the exception (25 Nov 2026)

- Manual: Rule 19 ends with the exception `\<Theta>_{end} Skip`; the memory has no `end` branch.
- CSP-M: `[|{|terminate|}|> SKIP`; `terminate` is not in the synchronisation set with the memory.
- This generator (option 2, decision 1 of Fang, 25 Nov 2026; as RC2Z did before): `terminate` is in the
  synchronisation sets of composeMachines and of the controller, and the memory offers `terminate -> Skip`; no
  exception is printed (the Z metamodel has none). Comparison of the two forms on ctl2:
  `robochart-models/controller-fdr/termination-unconnected/`.
- Kind: form; equivalence see the FDR package.
- Status: adopted.

## 27. setL and setR of the required variables (26 Nov 2026)

(Written for the fix release of main, generator issue OO, 26 Nov 2026; on main entries 20-26 are absent.)

- Manual: Chapter 4 Rules 41 and 60 use `setL_v`, `setR_v` for the write synchronisation (Definition C.2.2); the
  declarations are not given there.
- CSP-M: declares `get_v, set_v, setL_v, setR_v` for every variable of every machine, required variables included
  (`set_EXT_v` separately), whether or not they are used.
- This generator: declares `setL_v`, `setR_v` of a REQUIRED variable only when the machine has a composite state,
  the only place where they are used (Behaviour, Rule 60, over all variables of the machine); local variables as
  before. Fang (26 Nov 2026): keep the conditional declaration. By the naming principle (controller-layer entry 20:
  unique names, injective correspondence with the CSP-M events) the events of the two correspond one to one; the
  CSP-M's extra channels are not used by any process, so no event of either side is affected.
- Kind: form (declarations).
- Status: adopted 26 Nov 2026 (fix of generator issue OO).
