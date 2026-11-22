# Release notes (DRAFT): composite states

22 Nov 2026. Draft for the release of branch `composite-states` into `main`. NOT released: `main` is still
2976313 (published plugin 1.0.0.202611071400). The test results behind this draft are in
`docs/COMPOSITE_RELEASE_TEST.md`.

The plugin writes three theories per state machine: the HOL-CSP semantics (`HOLCSP_<model>_<time>.thy`), the
HOL-CSP Trans core (`HOLCSP_trans_core_<model>_<time>.thy`) and the IsaCircus semantics
(`IsaCircus_enriched_<model>_<time>.thy`). Where an item below changes only one of them it is said.

## New: composite states

States that contain a state machine of their own (any depth of nesting), with entry, during and exit
actions at every level, transitions into and out of the composite state, junctions and final states inside
it. Translated as RoboTool's CSP-M writes them (CSP generator 3.1.0.202608041330), following Chapter 4
Rule 60 of the RoboChart reference manual; where the CSP-M differs from the manual the generator follows
the CSP-M, and each difference is listed in `docs/SEMANTICS_DIFFERENCES.md`.

- Each composite state S gives `Behaviour_S` (the inner machine, Rule 40 with S as node container, in
  parallel with the during action of S, with the two-sided write synchronisation over all variables of the
  machine; `enteredSS` and `terminate` hidden), `During_S` and `RunEntered_S`.
- The paragraphs of each node container are printed together, innermost container first.
- Warning (not an error) when an exit action of an inner state reads or writes a variable bound by the
  input trigger of a transition leaving the composite state: the two are not ordered. Shown as a dialog,
  written to `~/.isacircus/compile.log`, and printed as a comment `(* GENERATOR WARNING: ... *)` at S in
  the HOL-CSP and IsaCircus theories; the generation goes on.

## Changed: names

- Trigger channels are named `<event>_<FQN of the node container>__in` instead of `<event>__in`
  (ex10: `a__in` becomes `a_stm3_ex10__in`). Needed because every node container, now also every
  composite state, has its own node identifiers. The event channels `<event>_in`, `<event>_out` are
  unchanged. Affects all three theories; list of places: `docs/ISACIRCUS_CHANGES_trigger-fqn-naming.md`.
- Theories that copied generated names (proof theories) must be updated; the order of elements of
  generated sets (channel declarations, synchronisation sets, renaming relations) may differ between runs.

## Fixes

- W. Record and enumeration types of imported packages are generated (the types of the selected file's
  package and of other non-toolkit packages it reaches, dependencies first). A clash of two type names
  from different packages stops the generation with an error naming both packages.
- X. A variable that occurs only in the value of a `let` is now read before the statement (before, it
  was free in the generated equation).
- Y. Selection of component n of a tuple with more than two components is printed correctly (before,
  component 2 was always printed as `snd`).
- Z. The event datatype `chan_event` with more than 80 constructors is declared without the datatype
  plugins `code`, `transfer` and the `quickcheck_*` plugins (a comment says so); loading the theory of a
  large model no longer runs out of memory. HOL-CSP theories only.
- AA. Functions of the RoboChart toolkits are translated to their Isabelle counterparts (`head` to `hd`,
  `tail` to `tl`, ...; table in `erules/toolkitFunctions.eol`), and statements that call a toolkit
  function with a precondition (`head`, `last`, `min`, `max`) are guarded by it, as in the CSP-M.
- EE. The write synchronisation of a composite state also covers the variables of required interfaces
  (no change for the models tested).
- CC. A statement `x = f(args)` with f a user-defined function with preconditions is guarded by the
  preconditions with the ARGUMENTS in place of the formal parameters (bound names of the precondition
  renamed where they would capture a name of an argument). Before, the preconditions were copied with
  the formal parameters, and toolkit functions got a second guard. Consequence for released output: the
  chemical detector's `anl = location(gs)` was guarded by `size(gs) > 0` with the formal `gs`, a free
  variable since variables are printed with the suffix of their container (`gs_GasAnalysis`).
- Partial output. When a template fails, the theory is still written up to the last complete command,
  with `(* INCOMPLETE: ... *)` at the top and `(* GENERATOR ERROR: ... *)` and an `ML` error command at
  the end, so that Isabelle stops there; the other theories are still written. Known unsupported input is
  printed as an identifier `GENERATOR_ERROR_<kind>_<name>` (e.g. an unsupported toolkit function, a
  precondition with a quantifier), at which Isabelle stops ("Extra variables on rhs"). A dialog names the
  failed templates; details in `~/.isacircus/compile.log`.

## Known limitations

- Controllers are not supported by the HOL-CSP output (issue DD): operation definitions are translated
  only inside controllers, and the HOL-CSP template does not handle them. A model with a controller gives
  the marker `GENERATOR_ERROR_missing_action_crtlMemory_...`; Isabelle stops earlier (duplicate event
  constructor).
- Set-typed variables (`var a : Set(int)`) are not supported (issue BB): the generation fails at the
  channel declaration. Hence the set toolkit functions cannot be used on a model yet.
- Toolkit functions `range`, `iter`, `squash`, `extract`, and `size` of a set, are not supported; they are
  printed as markers `GENERATOR_ERROR_unsupported_toolkit_<package>_<function>` and
  `GENERATOR_ERROR_size_of_set`. Supported functions and their translation: `erules/toolkitFunctions.eol`.
- Quantifiers (`exists`, `forall`) are not translated in any position; in a precondition of a function
  called as `x = f(args)` they give the guard marker `GENERATOR_ERROR_untranslated_precondition_<f>`.
- Found while testing this release, not fixed, all present since the generator of the ITP 2026 paper and NOT
  introduced by this release (docs/COMPOSITE_RELEASE_TEST.md, Section 11):
  - an `if` without `else` in an entry, during or exit action stops the model-to-model step with a stack
    overflow (FF);
  - functions of an imported package are not declared in the HOL-CSP theory; Isabelle reports them as extra
    variables (GG);
  - a required interface declared in another file whose variables have no initial value stops the
    model-to-model step ("Called feature 'name' on undefined object") (HH);
  - a function with three or more parameters gets a parameter type that does not match its calls; Isabelle
    reports a type error (II);
  - a state machine without controller does not declare the constants of the interfaces it requires (JJ).
- Guards for preconditions are PROVISIONAL. A function applied outside its precondition makes the statement
  block, so it shows up as a DEADLOCK in the HOL-CSP (as in RoboTool's CSP-M), not as an error. The
  reference manual does not treat preconditions as guards; this will change to definedness proof
  obligations (for each expression with a precondition: whenever execution reaches it, the precondition
  holds), proved separately from deadlock freedom, and the guards will then be removed. The guard of a
  user-defined function is added only for `x = f(args)`, while RoboTool's CSP-M also guards it in the
  other kinds of statement (`docs/SEMANTICS_DIFFERENCES.md`, entry 7). The CSP-M's guards for division by
  zero and for an index outside a sequence are not generated.
- The Trans-core deadlock lemma of a composite state's inner machine fails when the theory is loaded
  outside the plugin (expected, see issue T).
