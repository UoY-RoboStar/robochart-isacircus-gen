# Release notes: composite states

Released 23 Nov 2026: `main` f242344 (merge of composite-states 23d181e), plugin 1.0.0.202611232203, update site
https://robostar.cs.york.ac.uk/robotool/robochart-isacircus-gen/update/. The test results behind these notes are in
`docs/COMPOSITE_RELEASE_TEST.md`. A fix release followed on 26 Nov 2026 (section "Fix release of 26 Nov 2026"
below).

The plugin writes three theories per state machine: the HOL-CSP semantics (`HOLCSP_<model>_<time>.thy`), the
HOL-CSP Trans core (`HOLCSP_trans_core_<model>_<time>.thy`) and the IsaCircus semantics
(`IsaCircus_enriched_<model>_<time>.thy`). Where an item below changes only one of them it is said.

Loading the generated HOL-CSP theories needs the latest RC_HOL-CSP of RoboChart-Deadlock-HOLCSP-AG-dev (the
generated theories import `RC_HOL-CSP.RoboChart_Library`; the relational renaming and other definitions they use
exist there only since November 2026).

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

## Changed: names

- Trigger channels are named `<event>_<FQN of the node container>__in` instead of `<event>__in`
  (ex10: `a__in` becomes `a_stm3_ex10__in`). Needed because every node container, now also every
  composite state, has its own node identifiers. The event channels `<event>_in`, `<event>_out` are
  unchanged. Affects all three theories; list of places: `docs/ISACIRCUS_CHANGES_trigger-fqn-naming.md`.
- Theories that copied generated names (proof theories) must be updated; the order of elements of
  generated sets (channel declarations, synchronisation sets, renaming relations) may differ between runs.

## Fix release of 26 Nov 2026

- OO. A state machine with a composite state that writes a REQUIRED variable `v` (a variable of a required
  interface) inside the composite state: the write synchronisation of `Behaviour_<S>` (Chapter 4 Rule 60, over all
  variables of the machine, since fix EE) uses `setL_v` and `setR_v`, which were declared only for local variables.
  The HOL-CSP theory did not load ("Extra variables on rhs: setR_v, setL_v"). Now `setL_v` and `setR_v` are declared
  for the required variables of a state machine that has a composite state; the output of all other models is
  unchanged (checked on the composite-state, definedness, DTI, ITP and regression test models: the same tokens).
  RoboTool's CSP-M declares them for every variable of every machine (docs/SEMANTICS_DIFFERENCES.md entry 27).
  Test model: RoboChart-Deadlock-HOLCSP-AG-dev `robochart-models/regression-tests/oo_required_composite/`.

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
- FF. An `if` statement without `else` in an entry, during or exit action no longer stops the
  model-to-model step with a stack overflow, and an `if` without `else` anywhere is translated with the else
  branch `Skip`, as RoboTool's CSP-M (before, the theory contained `if b then P` without else, which Isabelle does
  not parse). Both present since the generator of the ITP 2026 paper.
- GG. Functions of imported packages that the model calls (directly, or through the pre- and postconditions of
  the functions it calls) are declared in the HOL-CSP theory; before, Isabelle reported them as extra variables.
  Toolkit functions are not affected (see AA). Names are not qualified: if a called function of an imported
  package has the name of a function of the model's own package, only the latter is declared and a warning is
  shown (dialog, `~/.isacircus/compile.log`, comment in the IsaCircus theory).
- II. A function with three or more parameters gets the parameter type `(A×B×C) ⇒ R`, matching its calls
  `f(a, b, c)`; before, the type was nested to the left (since 9 Oct 2026) or not parenthesised (before), and
  Isabelle reported a type error at the first call.
- KK. A variable without initial value gets the default value of RoboTool's CSP-M: 0 for int, nat, real and types
  declared with `type T`; `True`; `''''` for string; the first literal of an enumeration (with the defaults of its
  parameters); the record of the defaults of its fields; `[]`; `{}`; the tuple of the defaults; the constant
  function for a function type. Before, every type other than int and boolean got `0`, and Isabelle reported a
  type error for enumerations, records and strings. Relation, vector and matrix types are not supported: their
  type and default are `GENERATOR_ERROR_` markers (before, the generation stopped). Different from the manual
  (Chapter 4 Rule 35 leaves the value arbitrary); `docs/SEMANTICS_DIFFERENCES.md`, entry 10.
- LL. Records: field access `r.f`, record literals `R(| f = e |)` and field assignments `r.f = e` in guards,
  conditions, actions and communications are translated (`(R.f r)`, `⦇R.f = e, ...⦈`, `r⦇R.f := e⦈`); field names are
  qualified by the record, so two records may have a field of the same name. Before, field access and record
  literals stopped the generation ("The 'no null' constraint is violated"). An assignment to a field or an element
  (`r.f = e`, `x[i] = e`) now also reads the variable it updates (before, only the right-hand side was read).
- MM. Enumeration literals with parameters, `enumeration Shape { Circle(real) Square }`, are declared with them
  (`datatype Shape = Circle "real" | Square`, before `Circle | Square`), and `Shape::Circle(1.0)` is translated
  to `(Circle (1.0))` (before, the generation stopped).
- NN. Names of the model can no longer be captured by the variables of the generated definitions: these are now
  `x__a`, `y__a`, `n__id` (parameter of `Trans`), `value__v` (parameter of the `Memory` processes), `b__a`, `Q__a`,
  `P__a` (before `x`, `y`, `n`, `value`, `b`, `Q`, `P`: a record field `x` gave a type error in the record's
  discrete_cpo instance). Record field and constructor names are printed quoted, so that a name that is an
  Isabelle keyword (e.g. a field `value`) can be declared. Changes the text of every generated theory (these
  names), not its meaning.
- Function-typed variables: the parameter type of their Memory fixrec is parenthesised, `(int ⇒ int)→...`
  (before, a type error).
- Partial output. When a template fails, the theory is still written up to the last complete command,
  with `(* INCOMPLETE: ... *)` at the top and `(* GENERATOR ERROR: ... *)` and an `ML` error command at
  the end, so that Isabelle stops there; the other theories are still written. Known unsupported input is
  printed as an identifier `GENERATOR_ERROR_<kind>_<name>` (e.g. an unsupported toolkit function, a
  precondition with a quantifier), at which Isabelle stops ("Extra variables on rhs"). A dialog names the
  failed templates; details in `~/.isacircus/compile.log`.

## Known limitations

- An enumeration literal whose parameter is a record type does not load (the theory declares all enumerations
  before all records). RoboTool's RoboChart validator throws an exception on a literal with arguments
  (`Shape::Circle(1.0)`: "Error executing EValidator" in the editor); the model is valid and the generation works.
- Names reserved by the generated theories: `share`, `terminate`, `aviol`, `gviol`, `SSTOP`, `assume`, `guar` and
  the generated channel and process names; a model element with one of these names clashes (not checked).
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
- A required interface declared in ANOTHER FILE whose variables have no initial value stops the model-to-model
  step ("Called feature 'name' on undefined object") (HH). Cause: the loader gives the transformation a copy of
  the selected file in a resource of its own, so the objects of the other files are not part of the transformed
  model and the transformation cannot ask for their types. Workarounds: give the variables an initial value, or
  declare the interface in the same file as the state machine. Present since the dependency loading of
  5 Nov 2026; before it, such references were not resolved at all.
- A state machine without controller does not declare the constants of the interfaces it requires; Isabelle
  reports them as extra variables (JJ). Workaround: declare the constant in the state machine. With a
  controller, the variables of the interface are declared twice (controllers are not supported, DD).
- Deadlock-freedom proofs of the Trans core (the lemma in `HOLCSP_trans_core_*.thy`): where an `assume` is
  followed by the rest of a transition in a sequential composition, its fallback `aviol → Trans⋅n` is followed
  by that rest, and the proof method leaves goals it cannot close. The proofs of the ITP 2026 paper moved the
  sequential composition into the scope of the `assume` by hand (docs/COMPOSITE_RELEASE_TEST.md, Section 12).
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
