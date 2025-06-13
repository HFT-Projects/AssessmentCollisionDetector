# AssessmentCollisionDetector

## Setting up the development environment

1. Download JavaFX sdk [here](https://gluonhq.com/products/javafx/)

2. Create run configuration

   1. Create a new run configuration
   
   2. Add the following to the VM-options: \
      `--module-path "%javafx-sdk%/lib" --add-modules javafx.controls,javafx.fxml` \
      replace `%javafx-sdk%` with the path of the unarchived javafx sdk.


## [Features](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/work_items/1)

The [features document](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/work_items/1) shows all features and by whom and when they were implemented.


## Additional Branches

- ### [gui-basic-swing-poc](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/gui-basic-swing-poc)
  This branch contains the initial Swing GUI, developed before we switched to JavaFX.

- ### [optimizer-manual-poc](https://gitlab.rz.hft-stuttgart.de/Abschlussprojekt_SWP1_Gruppe_02/Abschlussprojekt_SWP1_Gruppe_02/-/tree/optimizer-manual-poc)
   This branch contains an incomplete attempt of a manual optimizer for the assessments without solving libraries.
