# dev-tools

Command-line tools to run the generator without the RoboTool user interface, for regressions and
diagnosis. They compile against the jars of an installed RoboTool and use the same loader class as the
plugin (`circus.robocalc.robochart.epsilon.isacircus.loader.RctLoader`, in the plugin's `src/`).

    dev-tools/build.sh [RoboTool dir]      # default ~/tools/robotool-1.3.2026102101; output in dev-tools/build/
    dev-tools/run.sh <Tool> <arguments>

| Tool | Arguments | What it does |
|---|---|---|
| `LoadRct` | `<file.rct> <project root> [closure\|all]` | loads the model with `RctLoader`; prints how the files were chosen (closure, folder or project fallback), the files read, the event references of the selected file, and ambiguity warnings |
| `GenAll` | `<file.rct> <project root> <erules dir> <circus_metamodel.ecore> <out dir> <tag> [closure\|all\|single] [extra=<dir>] [rcpkg] [alltemplates]` | runs `RC2Z.eol` and `circusM2T_HOLCSP.egl` as the plugin does; writes `<out>/<base>_circus.model` and `<out>/HOLCSP_<base>_<tag>.thy` (theory name `HOLCSP_<base>_<tag>`). `single` loads the selected file alone (the plugin's former behaviour) |
| `ParseCheck` | `<file.eol\|file.egl>...` | parses the files with the Epsilon engine and prints the parse problems |

`GenAll` options: `extra=<dir>` registers every `.ecore` of `<dir>` in the global EPackage registry before running
(RoboTool registers all its metamodels; several, e.g. PRISM, physmod, Circus, have classes with RoboChart's
names); `rcpkg` creates the RC model with the RoboChart package, as the plugin does since 5 Nov 2026; without it
the RC model looks classes up in the global registry. Test of 5 Nov 2026 (Legitimate item 2, 8 extra metamodels
from RoboTool's jars): all variants run; the extra metamodels change only the order of commutative operands.

`closure` (default) is what the plugin does: the selected file, the files of its package and, recursively,
of the imported packages (text scan of the sources under the project root, generated folders such as
`csp-gen/`, `hol-csp_gen/`, `src-gen/` excluded); fallback to the folder, then the project, if references
remain unresolved. `all` reads all source files under the project root.

Example (Legitimate, templates and RC2Z.eol of the working tree):

    G=circus.robocalc.robochart.epsilon.isacircus
    E=~/Git/RoboChart-Deadlock-HOLCSP-AG-dev/robochart-models/dti/enriched
    dev-tools/run.sh GenAll $E/Adaptation_Legitimate.rct $E $G/erules $G/metamodels/circus_metamodel.ecore /tmp/out dev

The generated theory can then be checked with `isabelle process_theories -l RC_HOL-CSP -f <file>.thy`.
By default only the HOL-CSP template is run; with `alltemplates` also `circusM2T.egl`
(`IsaCircus_enriched_<base>_<tag>.thy`) and `circusM2T_HOLCSP_trans_core.egl` (`HOLCSP_trans_core_<base>_<tag>.thy`),
on the same Z model, as `CircusM2T.egx` does.
Each template is caught on its own, as in the plugin (`PartialOutput.java` in the plugin's package
`...isacircus.m2t`, compiled by `build.sh`): a template that throws writes the text generated so far, with
`INCOMPLETE` at the beginning and `GENERATOR ERROR` and an `ML` error command at the end, GenAll prints
`INCOMPLETE <file>: <reason>` and ends with exit code 2 after all templates; the other templates still run.
A failure of `RC2Z.eol` stops GenAll as before (no theory).
Warnings of `RC2Z.eol` (attribute `generatorNote` of the Z model, read by `GeneratorNotes.java`; at present the
name clash of functions of imported packages, GG; the exit-action check of composite states was removed on
23 Nov 2026) are printed as lines `GENERATOR WARNING: ...` after the model-to-model step;
they do not stop GenAll.
