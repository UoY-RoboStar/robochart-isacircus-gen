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
