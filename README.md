# AssessmentCollisionDetector

## Setting up the development environment

1. use **Java 24 (V. 24.0.1)**

2. Download JavaFX sdk (V. 24.0.1) [here](https://gluonhq.com/products/javafx/)

3. Create run configuration

   1. Create a new run configuration
   
   2. Add the following to the VM-options: \
      `--module-path "%javafx-sdk%/lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics` \
      replace `%javafx-sdk%` with the path of the unarchived javafx sdk.


## Work Distribution

### Features

- read & parse input files: @42kejo1bif, @42bejo1bif, @42oeaz1bif & @42grra1bif
    - adjust to new specifications: @42kejo1bif
- detect collisions: @42kejo1bif, @42bejo1bif, @42oeaz1bif & @42grra1bif
- create collisions output file (csv): @42kejo1bif, @42bejo1bif, @42oeaz1bif & @42grra1bif
- add basic tests: @42kejo1bif
- GUI: @42oeaz1bif, @42bejo1bif (& @42kejo1bif)
  - GUI setup, Input, Collisions & Optimization view: @42oeaz1bif
  - Statistics: @42bejo1bif
  - Refactoring: @42kejo1bif
- Optimization: @42grra1bif & @42kejo1bif
  - basic optimization & optaplanner setup: @42grra1bif
  - bug fixing & further features: @42kejo1bif
  
### Other Tasks
- project management, planning, etc.: @42kejo1bif (~3h)
- bug-fixing & technical support: @42kejo1bif (>7h)
- refactoring: @42kejo1bif (>13h)
- git merge conflict resolving: @42kejo1bif (~2.5h (but felt like an eternity))


## Additional Branches

- ### [gui-basic-swing-poc](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/gui-basic-swing-poc)
  by @42oeaz1bif \
  This branch contains the initial Swing GUI, developed before we switched to JavaFX.

- ### [optimizer-manual-poc](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/optimizer-manual-poc)
  by @42grra1bif \
  This branch contains an incomplete attempt of a manual optimizer for the assessments without solving libraries.

- ### [old-gui-before-refactoring](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/old-gui-before-refactoring)
  by @42oeaz1bif \
  This branch contains the gui before the refactoring (by @42kejo1bif).


### Attribution

[Icon by zero_wing](https://www.freepik.com/icon/data-visualization_10397166)