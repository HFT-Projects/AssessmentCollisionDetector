# AssessmentCollisionDetector

## Overview

The AssessmentCollisionDetector is a software tool developed as part of an academic assignment at [HFT Stuttgart](https://hft-stuttgart.com/). It is designed to manage and optimize assessment schedules while detecting and resolving potential collisions. The project provides a graphical user interface (GUI) for input, collision detection, optimization, and statistical analysis. The software is built using Java 24 and JavaFX 24.0.1, ensuring a modern and responsive user experience.

### Key Features

- **Input Parsing**: Reads and parses input files to extract assessment data.
- **Collision Detection**: Identifies scheduling conflicts between assessments.
- **Optimization**: Utilizes OptaPlanner to optimize assessment schedules.
- **Output Generation**: Creates CSV files summarizing detected collisions.
- **Graphical User Interface**: Provides an intuitive GUI for managing input, viewing collisions, optimizing schedules, and analyzing statistics.

## Project Details

### Input Files

- **pruefungen.csv**: Contains data about planned assessments.
- **anmeldungen.csv**: Contains student registrations for the respective assessments.

### Output Files

- **kollisionen.csv**: Provides an overview of all pairs of assessments where at least one student is registered for both.

## Setting up the Development Environment

1. **Use Java 24 (V. 24.0.1)**

2. **Download JavaFX SDK (V. 24.0.1)**
   - [Download JavaFX SDK](https://gluonhq.com/products/javafx/)

3. **Create Run Configuration**
   - Create a new run configuration in your IDE.
   - Add the following to the VM options:
     ```
     --module-path "%javafx-sdk%/lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics
     ```
     Replace `%javafx-sdk%` with the path to the unarchived JavaFX SDK.

## Additional Branches

- **[gui-basic-swing-poc](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/gui-basic-swing-poc)**
  - By @42oeaz1bif
  - This branch contains the initial Swing GUI, developed before we switched to JavaFX.

- **[optimizer-manual-poc](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/optimizer-manual-poc)**
  - By @42grra1bif
  - This branch contains an incomplete attempt of a manual optimizer for the assessments without solving libraries.

- **[old-gui-before-refactoring](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/old-gui-before-refactoring)**
  - By @42oeaz1bif
  - This branch contains the GUI before the refactoring (by @42kejo1bif).

## Attribution

- **Icon**: [Icon by zero_wing](https://www.freepik.com/icon/data-visualization_10397166)
