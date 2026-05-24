\# RoboChart2IsaCircus



An Eclipse plugin for translating RoboChart models to Isabelle/Circus theory files and verifying them using Isabelle.



\## Features



\- Compile .rct files to Isabelle/Circus (.thy) theory files

\- Verify .thy files using Isabelle (console output)

\- Verification log saved to isabelle\_log folder



\## Prerequisites



\- Eclipse 2021-12 or later

\- RoboTool (https://robostar.cs.york.ac.uk/robotool/)

\- Epsilon 2.4 (https://download.eclipse.org/epsilon/updates/2.4/)

\- Isabelle 2025-2 (for verification, Linux/macOS only, must be in system PATH)



\## Installation



\### Option 1: Update Site URL



In Eclipse: Help > Install New Software > Add > enter URL:



&#x20;   https://uoy-robostar.github.io/robochart-isacircus-gen/



\### Option 2: Download ZIP



Download the update site ZIP from the \[Releases page](https://github.com/UoY-RoboStar/robochart-isacircus-gen/releases), then in Eclipse: Help > Install New Software > Add > Archive > select the ZIP file.



\## Usage



\### Compile RoboChart to IsaCircus



Right-click a .rct file > IsaCircus > Compile



\### Verify theory file



Right-click a .thy file > Isabelle > Verify (console)



Or use the toolbar buttons (IsaCircus toolbar) after selecting the file.

