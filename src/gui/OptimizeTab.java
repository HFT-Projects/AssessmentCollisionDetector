package gui;

import data.MergedAssessment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.prefs.Preferences;

public class OptimizeTab {
    private final VBox section;
    private final Tab tab;

    public OptimizeTab(MainGUI mainGUI, Preferences optimizerPreferences, Preferences collisionTablePreferences) {
        tab = new Tab("Optimization");
        tab.setClosable(false);

        // Create basic layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Create a ScrollPane to wrap the content
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Create control section (perform optimization & save to file)
        HBox controlSection = new HBox(20);
        controlSection.setPadding(new Insets(0, 0, 20, 0));
        controlSection.setAlignment(Pos.CENTER_LEFT);
        controlSection.setPrefWidth(Double.MAX_VALUE);


        // === Left side ===
        // button created already here because it is needed for event handlers below
        // other properties are set further down
        Button saveOptimizedButton = new Button("Save Optimized");

        VBox leftSide = new VBox(10);
        leftSide.setAlignment(Pos.TOP_LEFT);

        Button optimizeButton = new Button("Run Optimization");
        optimizeButton.setStyle(
                "-fx-background-color: #2ecc71;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"
        );

        // CheckBoxes for optimization options
        CheckBox considerRoomCheckbox = new CheckBox("Consider Rooms in Optimization");
        considerRoomCheckbox.setStyle("-fx-text-fill: #2c3e50;");

        CheckBox considerSupervisorCheckbox = new CheckBox("Consider Supervisors in Optimization");
        considerSupervisorCheckbox.setStyle("-fx-text-fill: #2c3e50;");

        leftSide.getChildren().addAll(optimizeButton, considerRoomCheckbox, considerSupervisorCheckbox);
        optimizeButton.setOnAction(_ -> {
            MergedAssessment[] optimizedAssessments = mainGUI.optimizeStart(considerRoomCheckbox.isSelected(), considerSupervisorCheckbox.isSelected());
            if (optimizedAssessments == null)
                return;
            setAssessments(optimizedAssessments, collisionTablePreferences);
            // activate save optimized button
            saveOptimizedButton.setDisable(false);
        });

        // === Right side ===
        VBox rightSide = new VBox(10);
        rightSide.setAlignment(Pos.TOP_RIGHT);

        HBox saveSection = new HBox(15);  // Increased spacing between elements
        saveSection.setAlignment(Pos.CENTER_LEFT);

        Label saveFileLabel = new Label("Save Optimized File:");
        saveFileLabel.setStyle("-fx-text-fill: #2c3e50;");

        TextField folderPathField = new TextField();
        folderPathField.setText(optimizerPreferences.get("optimizedAssessmentsPath", ""));
        folderPathField.setEditable(false);
        folderPathField.setPromptText("No folder selected");

        folderPathField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-padding: 8px;" +
                        "-fx-font-size: 12px;" +
                        "-fx-pref-width: 600px;"
        );

        Button browseButton = new Button("Browse...");
        browseButton.setStyle(
                "-fx-background-color: " + MainGUI.PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"
        );

        browseButton.setOnAction(_ -> mainGUI.selectFile(folderPathField, true));

        // only active after optimization
        saveOptimizedButton.setDisable(true);
        saveOptimizedButton.setStyle(
                "-fx-background-color: " + MainGUI.PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"
        );

        saveOptimizedButton.setOnAction(_ -> {
            String path = folderPathField.getText();

            // Check if path is null or empty
            if (path == null || path.trim().isEmpty()) {
                MainGUI.showAlert("Please specify a file path for saving collisions.", Alert.AlertType.ERROR);
                return;
            }

            File collisionFile = new File(path);

            // Check if parent directory exists and is accessible
            File parentDir = collisionFile.getParentFile();
            if (parentDir == null) {
                MainGUI.showAlert("Invalid file path. Please specify a valid directory path.", Alert.AlertType.ERROR);
                return;
            }

            if (!parentDir.exists()) {
                MainGUI.showAlert("Directory does not exist: " + parentDir.getPath(), Alert.AlertType.ERROR);
                return;
            }

            if (!parentDir.canWrite()) {
                MainGUI.showAlert("Cannot write to directory: " + parentDir.getPath() + "\nPlease check directory permissions.", Alert.AlertType.ERROR);
                return;
            }

            // Check if file exists and is writable
            if (collisionFile.exists() && !collisionFile.canWrite()) {
                MainGUI.showAlert("Cannot write to file: " + collisionFile.getPath() + "\nPlease check file permissions.", Alert.AlertType.ERROR);
                return;
            }

            // Save paths to preferences
            optimizerPreferences.put("optimizedAssessmentsPath", path);

            mainGUI.saveOptimizedAssessments(path);
        });

        saveSection.getChildren().addAll(saveFileLabel, folderPathField, browseButton, saveOptimizedButton);
        rightSide.getChildren().add(saveSection);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add left and right sides to the control section
        controlSection.getChildren().addAll(leftSide, spacer, rightSide);

        section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        VBox.setVgrow(section, Priority.ALWAYS);


        // Section title
        Label sectionTitle = new Label("Optimization Results");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        // Section description
        Label sectionDescription = new Label("Results of exam schedule optimization.");
        sectionDescription.setStyle("-fx-text-fill: " + MainGUI.SECONDARY_COLOR + ";");

        // Separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));


        // Add components to section
        section.getChildren().addAll(
                sectionTitle,
                sectionDescription,
                separator
        );

        // Add all sections to the content
        content.getChildren().addAll(controlSection, section);
        VBox.setVgrow(section, Priority.ALWAYS);
        scrollPane.setFitToHeight(true);

        // Set the ScrollPane as the tab content
        tab.setContent(scrollPane);
    }

    public void setAssessments(MergedAssessment[] assessments, Preferences prefs) {
        AssessmentTable assessmentTable = new AssessmentTable(assessments, prefs, true);
        VBox.setVgrow(assessmentTable, Priority.ALWAYS);
        section.getChildren().add(assessmentTable);
    }

    public Tab getTab() {
        return tab;
    }
}
