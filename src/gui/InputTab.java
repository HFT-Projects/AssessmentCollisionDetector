package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.prefs.Preferences;

public class InputTab {
    private final Tab tab;
    private final TextField examsPathField;
    private final TextField registrationsPathField;
    private final TextField collisionsPathField;
    private final TextField optionalYearField;

    public InputTab(MainGUI mainGUI, Preferences prefs) {
        tab = new Tab("Data Input");
        VBox inputContent = new VBox(20);
        inputContent.setPadding(new Insets(20));

        // Files section
        // Create container for file input section
        VBox fileSection = new VBox(15);
        fileSection.setPadding(new Insets(20));
        fileSection.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Section header
        Label sectionTitle = new Label("Import Files");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // File input grid layout
        GridPane fileGrid = new GridPane();
        fileGrid.setHgap(10);
        fileGrid.setVgap(15);

        // Initialize text fields with saved preferences
        examsPathField = createStyledTextField(prefs.get("examsPath", ""));
        registrationsPathField = createStyledTextField(prefs.get("registrationsPath", ""));
        collisionsPathField = createStyledTextField(prefs.get("collisionsPath", ""));

        // Create browse buttons
        Button examBrowseButton = createStyledButton("Browse", MainGUI.PRIMARY_COLOR);
        Button registrationBrowseButton = createStyledButton("Browse", MainGUI.PRIMARY_COLOR);
        Button collisionBrowseButton = createStyledButton("Browse", MainGUI.PRIMARY_COLOR);

        examBrowseButton.setOnAction(_ -> mainGUI.selectFile(examsPathField, false));
        registrationBrowseButton.setOnAction(_ -> mainGUI.selectFile(registrationsPathField, false));
        collisionBrowseButton.setOnAction(_ -> mainGUI.selectFile(collisionsPathField, true));

        // Create and style field labels with dark text color for visibility
        Label examLabel = new Label("Exam File");
        examLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        examLabel.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        Label registrationLabel = new Label("Registration File");
        registrationLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        registrationLabel.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        Label collisionLabel = new Label("Output File");
        collisionLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        collisionLabel.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        // Add components to grid with proper layout
        fileGrid.add(examLabel, 0, 0);
        fileGrid.add(examsPathField, 1, 0);
        fileGrid.add(examBrowseButton, 2, 0);

        fileGrid.add(registrationLabel, 0, 1);
        fileGrid.add(registrationsPathField, 1, 1);
        fileGrid.add(registrationBrowseButton, 2, 1);

        fileGrid.add(collisionLabel, 0, 2);
        fileGrid.add(collisionsPathField, 1, 2);
        fileGrid.add(collisionBrowseButton, 2, 2);

        // Column constraints
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPercentWidth(20);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setPercentWidth(65);

        ColumnConstraints buttonColumn = new ColumnConstraints();
        buttonColumn.setPercentWidth(15);

        fileGrid.getColumnConstraints().addAll(labelColumn, fieldColumn, buttonColumn);

        // Optional Year section
        HBox optionalYearBox = new HBox(10);
        optionalYearBox.setAlignment(Pos.CENTER_LEFT);
        optionalYearBox.setPadding(new Insets(15, 0, 0, 0));

        Label optionalYearLabel = new Label("Year (Optional):");
        optionalYearLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        optionalYearLabel.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        optionalYearField = createStyledTextField(prefs.get("yearInput", ""));
        optionalYearField.setPromptText("YYYY");
        optionalYearField.setPrefWidth(100);

        optionalYearBox.getChildren().addAll(optionalYearLabel, optionalYearField);

        // buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(20, 0, 0, 0));

        Button detectButton = createStyledButton("Detect Collisions", MainGUI.SUCCESS_COLOR);
        Button saveButton = createStyledButton("Save Collisions", MainGUI.PRIMARY_COLOR);
        // only active after collision detection
        saveButton.setDisable(true);

        detectButton.setOnAction(_ -> {
            String examsPath = examsPathField.getText();
            String registrationsPath = registrationsPathField.getText();
            String yearInput = optionalYearField.getText();

            if (!new File(examsPath).exists()) {
                MainGUI.showAlert("Invalid exams file path!", Alert.AlertType.ERROR);
                return;
            }

            if (!new File(registrationsPath).exists()) {
                MainGUI.showAlert("Invalid registrations file path!", Alert.AlertType.ERROR);
                return;
            }

            Integer year;
            // validate year
            if (yearInput == null || yearInput.isEmpty()) {
                year = null;
            } else if (!(yearInput.matches("\\d{4}"))) {
                MainGUI.showAlert("Invalid year!", Alert.AlertType.ERROR);
                return;
            } else {
                year = Integer.parseInt(yearInput.trim());
            }

            // save paths to preferences
            prefs.put("examsPath", examsPath);
            prefs.put("registrationsPath", registrationsPath);
            prefs.put("yearInput", year == null ? "" : String.valueOf(year));

            mainGUI.detectCollisions(examsPath, registrationsPath, year);

            // disable collision detection (because it can only run once)
            detectButton.setDisable(true);
            // enable save collisions button
            saveButton.setDisable(false);
        });

        saveButton.setOnAction(_ -> {
            String collisionsPath = collisionsPathField.getText();

            // Check if path is null or empty
            if (collisionsPath == null || collisionsPath.trim().isEmpty()) {
                MainGUI.showAlert("Please specify a file path for saving collisions.", Alert.AlertType.ERROR);
                return;
            }

            File collisionFile = new File(collisionsPath);

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
            prefs.put("collisionsPath", collisionsPath);

            mainGUI.saveCollisions(collisionsPath);
        });

        actionBox.getChildren().addAll(detectButton, saveButton);

        fileSection.getChildren().addAll(sectionTitle, separator, fileGrid, optionalYearBox, actionBox);

        inputContent.getChildren().add(fileSection);

        ScrollPane inputScrollPane = new ScrollPane(inputContent);
        inputScrollPane.setFitToWidth(true);
        inputScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        tab.setContent(inputScrollPane);
    }

    private static TextField createStyledTextField(String initialText) {
        TextField textField = new TextField(initialText);
        textField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-padding: 8px;" +
                        "-fx-font-size: 12px;"
        );
        textField.setPromptText("Enter path..."); // Add placeholder text
        return textField;
    }

    private static Button createStyledButton(String text, String bgColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"
        );

        // Hover effect
        button.setOnMouseEntered(_ ->
                button.setStyle(
                        "-fx-background-color: derive(" + bgColor + ", -20%);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-cursor: hand;" +
                                "-fx-border-radius: 4px;"
                )
        );

        button.setOnMouseExited(_ ->
                button.setStyle(
                        "-fx-background-color: " + bgColor + ";" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-cursor: hand;" +
                                "-fx-border-radius: 4px;"
                )
        );

        return button;
    }

    public Tab getTab() {
        return tab;
    }
}
