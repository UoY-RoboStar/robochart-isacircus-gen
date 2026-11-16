# Changes of the IsaCircus output on branch trigger-fqn-naming

14 Nov 2026. For the notification to Arjun (NOT sent). Branch `trigger-fqn-naming`, commit 844768b,
against `main` 2976313 (= published plugin 1.0.0.202611071400). Only `RC2Z.eol` affects the IsaCircus
output (`circusM2T.egl` is unchanged; the template changes of this branch are in the two HOL-CSP
templates).

## The one change

A trigger channel, i.e. the channel of an event indexed by the node identifiers of a node container,
used for the triggers of transitions, was named `<event>__in`. It is now named

    <event>_<FQN of the node container>__in

where the FQN is the path from the container up to the package, innermost first, separated by `_`, as
in every other generated name (`NIDS_<FQN>`, `enter_<node>_<FQN>`, ...). For a state machine `stm3` in
package `ex10`: `a__in` becomes `a_stm3_ex10__in`. Reason: the channel is typed with the node
identifiers of one container; with composite states (planned) every container has its own.

Not changed: the event channels `<event>_in` and `<event>_out` (connections match them by name), all
other channels, all process names, the structure of every action.

## Where the name occurs in a generated IsaCircus theory

| Place | Example (ex10), before | after |
|---|---|---|
| channel declaration, one per event of the machine, including the events of the interfaces it uses | `a__in :: "NIDS_stm3_ex10"` | `a_stm3_ex10__in :: "NIDS_stm3_ex10"` |
| trigger of a transition in `Trans_<stm>` and in `Transc_<stm>` | `a__in\<^bold>.NID_s0_stm3_ex10 \<rightarrow> ...` | `a_stm3_ex10__in\<^bold>.NID_s0_stm3_ex10 \<rightarrow> ...` |
| interrupt renaming in `ncCoreBehaviour_<stm>` | `csbasic (interrupt_s0_stm3_ex10) \<times> csbasic (a__in\<^bold>.NID_s0_stm3_ex10)` | `... csbasic (a_stm3_ex10__in\<^bold>.NID_s0_stm3_ex10)` |
| synchronisation set of `ncCoreBehaviour_<stm>` | `a__in\<^bold>.NID_s0_stm3_ex10` | `a_stm3_ex10__in\<^bold>.NID_s0_stm3_ex10` |
| trigger renaming in `ncBehaviour_<stm>` | `csbasic (a__in\<^bold>.x) \<times> csbasic (a_in)` | `csbasic (a_stm3_ex10__in\<^bold>.x) \<times> csbasic (a_in)` |

Counts in the two models checked (dev-tools `GenAll ... rcpkg alltemplates`, before and after):

| Model | occurrences | distinct channels |
|---|---|---|
| ex10 (`example10.rct`) | 6 | 1: `a` |
| Legitimate (`dti/enriched/Adaptation_Legitimate.rct`) | 21 | 12: `newModel` (6 occurrences), `verifyPlan` (6), and 10 channels that are only declared (`planAccepted`, `planRejected`, `get_newModel`, `newConfidences`, `get_newConfidences`, `set_newConfidences`, `imagesWithLight`, `get_imagesWithLight`, `imagesWithPoorLight`, `get_imagesWithPoorLight`), each with the suffix `_Adaptation_Legitimate_Adaptation_Legitimate__in` |

## What was compared

For both models: the IsaCircus theory generated with the rules of `main`, with every `<e>__in`
replaced by the new name, has exactly the same tokens (same multiset of identifiers and symbols) as
the theory generated on this branch. They are not identical line by line: the order of the elements
of sets (channel declarations, synchronisation sets, renaming pairs) differs between two runs of the
generator in any case, also with unchanged rules, and a changed name changes that order where sets of
strings are involved. No line-by-line identical pair could therefore be produced.

Not checked: whether the new IsaCircus theories are accepted by IsaCircus (the old ones were not
checked here either); models other than these two; models with controllers or modules.

## Branch composite-states: types of imported packages (generator issue W, 15 Nov 2026)

`RC2Z.eol` now also translates the record, enumeration and primitive types of imported packages (not
the toolkits) that the selected file reaches, dependencies first; before, only the types of the
selected file's package were translated, although the imported files were loaded (issue C). The
names are not qualified; two types of the same name stop the generation with an error.

Effect on the IsaCircus output: lines are ADDED, none removed or changed. Checked on ex10,
Legitimate, c1 to c6 and the flat MakePlan (dev-tools, before and after): only Legitimate
(`dti/enriched/Adaptation_Legitimate.rct`, which imports `Types`) changes, by 13 lines, the
declarations of the three records it reaches:

    record EstimatedPosition =
    	topLeft :: "(nat\<times>nat)"
    	bottomRight :: "(nat\<times>nat)"
    	confidence :: "real"
    record ImageData =
    	pixels :: "(((nat\<times>nat\<times>nat) list) list)"
    	height :: "nat"
    	width :: "nat"
    record ScrewImageData =
    	position :: "(real\<times>real\<times>real)"
    	orientation :: "(real\<times>real\<times>real)"
    	image :: "ImageData"
    	screwsDetected :: "(EstimatedPosition list)"

Before, the IsaCircus theory of Legitimate used `ScrewImageData` in its event channel declarations
without declaring it. Any model whose state machine uses types declared in another file changes in
the same way. Not checked: whether IsaCircus accepts the new theory.

## Branch composite-states: projection of n-tuples (generator issue Y, 15 Nov 2026)

`TupleSelExpr` has a new optional attribute `arity` (circus_metamodel.ecore; default 0 = unknown;
marked as a deviation from CZT by an annotation). RC2Z sets it to the number of components of the
RoboChart product type; `circusM2T.egl` prints the selection through the shared operation
`tupleSelString` (`erules/tupleSelection.eol`, imported at the first line of the template), as the two
HOL-CSP templates do. Component k of an n-tuple is printed `fst (snd^(k-1) t)` for k < n and
`snd^(n-1) t` for k = n; for arity 0 the printing is the one used before.

Effect on the IsaCircus output: only selections from tuples with three or more components change, and
only when the component is not the first one. Pairs are printed exactly as before. Checked: ex10,
Legitimate, c1 to c6 and the flat MakePlan unchanged; the full `dti/enriched/Adaptation_Plan.rct`
(`imagesWithLight_var : Seq(ScrewImageData * real * nat)`, component 2) changes in 2 places (Trans and
Transc):

    before:  snd ((imagesWithLight_var ! ...))
    after:   fst (snd ((imagesWithLight_var ! ...)))

Before, the IsaCircus theory selected the pair `(real, nat)` where the real number is meant. Not
checked: whether IsaCircus accepts the new theory.

## Branch composite-states: toolkit functions and their preconditions (generator issue AA, 16 Nov 2026)

RC2Z writes a call of a function of `sequence_toolkit` or `set_toolkit` with the qualified name
`<package>_<function>` (as RoboTool's CSP-M generator does) and guards an assignment, a communication, a
call and an if statement by the preconditions of the toolkit functions they call, placed as in the CSP-M
generator (after the reads, before the statement). The three templates print both by the shared table
`erules/toolkitFunctions.eol` (`hd`, `tl`, `butlast`, `rev`, `@`, `distinct`, set operators, ...). A
toolkit function that is not in the table (all of `relation_toolkit` and `function_toolkit`, and
`range`, `iter`, `squash`, `extract`) and `size` of a set now STOP the generation with an error naming
the function; before, the name was printed unchanged.

Effect on the IsaCircus output, checked with dev-tools: ex10, Legitimate, c1 to c6 and the flat MakePlan
unchanged (`size` of a sequence is printed exactly as before, `size((s))`). The full
`dti/enriched/Adaptation_Plan.rct` changes in two places:

    entry of PlanMade, before:  set_newModel_out!head((modelRanking)) -> Skip
                       after:   ((modelRanking) \<noteq> []) \<^bold>& (... set_newModel_out!(hd (modelRanking)) -> Skip ...)
    entry of Replan (then branch of the if), before:  set_modelRanking!tail((modelRanking)) -> Skip
                                             after:   set_modelRanking!(tl (modelRanking)) -> Skip

Before, `head` and `tail` were names that IsaCircus/Isabelle does not define. Not checked: whether
IsaCircus accepts the new theory.
