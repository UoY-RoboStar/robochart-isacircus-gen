# README

## RoboChart2IsaCircus

An Eclipse plugin for translating RoboChart models to Isabelle/Circus theory files and verifying them using Isabelle.

### Features
- Compile .rct files to Isabelle/Circus (.thy) theory files
- Verify .thy files using Isabelle (console output)
- Verification log saved to isabelle_log folder

### Prerequisites
- Eclipse 2021-12 or later
- RoboTool (https://robostar.cs.york.ac.uk/robotool/)
- Epsilon 2.4 (https://download.eclipse.org/epsilon/updates/2.4/)
- Isabelle 2025-2 (for verification, Linux/macos only, must be in system PATH)

### Installation
*(这里需要补上具体步骤，例如：下载插件包 -> 放入Eclipse的dropins文件夹 -> 重启Eclipse等)*

### Usage
#### Compile RoboChart to IsaCircus
Right-click a .rct file > IsaCircus > Compile

#### Verify theory file
Right-click a .thy file > Isabelle > Verify (console)
Or use the toolbar buttons (IsaCircus toolbar) after selecting the file.
