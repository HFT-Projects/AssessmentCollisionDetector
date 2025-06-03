import data.Assessment;

import data.AssessmentEditable;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Stream;
import javafx.scene.effect.DropShadow;

public class ExamGUI extends Application {
    // Style constants
    private static final String PRIMARY_COLOR = "#3498db";
    private static final String SECONDARY_COLOR = "#2c3e50";
    private static final String SUCCESS_COLOR = "#2ecc71";
    private static final String LIGHT_COLOR = "#ecf0f1";
    private static final String DARK_COLOR = "#34495e";

    // Page constants
    private static final int INPUT_PAGE = 0;
    private static final int RESULTS_PAGE = 1;

    private TextField examPathField;
    private TextField registrationPathField;
    private TextField collisionPathField;
    private TreeTableView<CollisionEntry> collisionTreeTable;
    private ComboBox<String> sortExam1Box;
    private ComboBox<String> sortExam2Box;
    private TextField filterExam1Field;
    private TextField filterExam2Field;
    private ObservableList<Map.Entry<Assessment, CollisionDetector.ReturnTuple>> collisions;
    private final Preferences prefs = Preferences.userNodeForPackage(ExamGUI.class);

    // Page management
    private TabPane tabPane;
    private Tab inputTab;
    private Tab resultsTab;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Exam Collision Detector");

        // Create a drop shadow effect for cards
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.color(0.4, 0.4, 0.4, 0.3));

        // Main container
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f7fa;");

        // Header section
        HBox header = createHeader();
        root.setTop(header);

        // Create tab pane for pages
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create input page
        inputTab = new Tab("Data Input");
        VBox inputContent = new VBox(20);
        inputContent.setPadding(new Insets(20));

        // Files section
        VBox fileSection = createFileSection();
        fileSection.setEffect(dropShadow);

        inputContent.getChildren().add(fileSection);

        ScrollPane inputScrollPane = new ScrollPane(inputContent);
        inputScrollPane.setFitToWidth(true);
        inputScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        inputTab.setContent(inputScrollPane);

        // Create results page
        resultsTab = new Tab("Collision Results");
        VBox resultsContent = new VBox(20);
        resultsContent.setPadding(new Insets(20));

        // Filter section
        VBox filterSection = createFilterSection();
        filterSection.setEffect(dropShadow);

        // Table section
        VBox tableSection = createTableSection();
        tableSection.setEffect(dropShadow);

        resultsContent.getChildren().addAll(filterSection, tableSection);

        ScrollPane resultsScrollPane = new ScrollPane(resultsContent);
        resultsScrollPane.setFitToWidth(true);
        resultsScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        resultsTab.setContent(resultsScrollPane);

        // Add tabs to tab pane
        tabPane.getTabs().addAll(inputTab, resultsTab);

        // Set initial tab to input
        tabPane.getSelectionModel().select(INPUT_PAGE);

        root.setCenter(tabPane);

        // Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Create and show scene
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);

        // Load saved preferences
        loadPreferences();

        // Setup file choosers and event handlers
        setupEventHandlers(primaryStage);

        primaryStage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 15, 20, 15));
        header.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");

        Label title = new Label("Exam Collision Detector");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        header.getChildren().add(title);
        return header;
    }

    private VBox createFileSection() {
        // Create container for file input section
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Section header
        Label sectionTitle = new Label("Import Files");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(SECONDARY_COLOR));

        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // File input grid layout
        GridPane fileGrid = new GridPane();
        fileGrid.setHgap(10);
        fileGrid.setVgap(15);

        // Initialize text fields with saved preferences
        examPathField = createStyledTextField(prefs.get("examsPath", ""));
        registrationPathField = createStyledTextField(prefs.get("registrationsPath", ""));
        collisionPathField = createStyledTextField(prefs.get("collisionsPath", ""));

        // Create browse buttons
        Button examBrowseButton = createStyledButton("Browse", PRIMARY_COLOR);
        Button registrationBrowseButton = createStyledButton("Browse", PRIMARY_COLOR);
        Button collisionBrowseButton = createStyledButton("Browse", PRIMARY_COLOR);

        // Create and style field labels with dark text color for visibility
        Label examLabel = new Label("Exam File");
        examLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        examLabel.setTextFill(Color.web(SECONDARY_COLOR));

        Label registrationLabel = new Label("Registration File");
        registrationLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        registrationLabel.setTextFill(Color.web(SECONDARY_COLOR));

        Label collisionLabel = new Label("Output File");
        collisionLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        collisionLabel.setTextFill(Color.web(SECONDARY_COLOR));

        // Add components to grid with proper layout
        fileGrid.add(examLabel, 0, 0);
        fileGrid.add(examPathField, 1, 0);
        fileGrid.add(examBrowseButton, 2, 0);

        fileGrid.add(registrationLabel, 0, 1);
        fileGrid.add(registrationPathField, 1, 1);
        fileGrid.add(registrationBrowseButton, 2, 1);

        fileGrid.add(collisionLabel, 0, 2);
        fileGrid.add(collisionPathField, 1, 2);
        fileGrid.add(collisionBrowseButton, 2, 2);

        // Column constraints
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPercentWidth(20);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setPercentWidth(65);

        ColumnConstraints buttonColumn = new ColumnConstraints();
        buttonColumn.setPercentWidth(15);

        fileGrid.getColumnConstraints().addAll(labelColumn, fieldColumn, buttonColumn);

        // Action buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(20, 0, 0, 0));

        Button detectButton = createStyledButton("Detect Collisions", SUCCESS_COLOR);
        Button saveButton = createStyledButton("Save Collisions", PRIMARY_COLOR);

        actionBox.getChildren().addAll(detectButton, saveButton);

        section.getChildren().addAll(sectionTitle, separator, fileGrid, actionBox);
        return section;
    }

    private VBox createFilterSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Section header
        Label sectionTitle = new Label("Filter and Sort");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(SECONDARY_COLOR));

        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // Filter and sort grid
        GridPane controlGrid = new GridPane();
        controlGrid.setHgap(20);
        controlGrid.setVgap(15);

        // Create filter fields with placeholder text
        filterExam1Field = createStyledTextField("");
        filterExam1Field.setPromptText("Filter exam 1...");
        filterExam2Field = createStyledTextField("");
        filterExam2Field.setPromptText("Filter exam 2...");

        // Create sort comboboxes
        sortExam1Box = createStyledComboBox();
        sortExam1Box.setPromptText("Select Exam 1 sort...");
        sortExam1Box.setItems(FXCollections.observableArrayList(
            "Exam Name", "Date", "Time", "Room", "Collision Count"
        ));

        sortExam2Box = createStyledComboBox();
        sortExam2Box.setPromptText("Select Exam 2 sort...");
        sortExam2Box.setItems(FXCollections.observableArrayList(
            "Exam Name", "Date", "Time", "Room", "Collision Count"
        ));

        // Create and style field labels
        Label filterExam1Label = new Label("Filter Exam 1");
        filterExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label filterExam2Label = new Label("Filter Exam 2");
        filterExam2Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam1Label = new Label("Sort Exam 1");
        sortExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam2Label = new Label("Sort Exam 2");
        sortExam2Label.setTextFill(Color.web(SECONDARY_COLOR));

        // Add components to grid
        controlGrid.add(filterExam1Label, 0, 0);
        controlGrid.add(filterExam1Field, 1, 0);
        controlGrid.add(filterExam2Label, 2, 0);
        controlGrid.add(filterExam2Field, 3, 0);
        controlGrid.add(sortExam1Label, 0, 1);
        controlGrid.add(sortExam1Box, 1, 1);
        controlGrid.add(sortExam2Label, 2, 1);
        controlGrid.add(sortExam2Box, 3, 1);

        // Column constraints for even distribution
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(15);

        ColumnConstraints controlCol = new ColumnConstraints();
        controlCol.setPercentWidth(35);

        controlGrid.getColumnConstraints().addAll(labelCol, controlCol, labelCol, controlCol);

        section.getChildren().addAll(sectionTitle, separator, controlGrid);
        return section;
    }

    private VBox createTableSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        section.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(section, Priority.ALWAYS);

        // Section title
        Label sectionTitle = new Label("Collision Results");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(SECONDARY_COLOR));

        // Section description
        Label sectionDescription = new Label("Detected exam collisions based on student registrations.");
        sectionDescription.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        // Separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // Create tree table view instead of regular table
        TreeTableView<CollisionEntry> collisionTreeTable = new TreeTableView<>();
        collisionTreeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        collisionTreeTable.setPrefHeight(300);
        collisionTreeTable.setShowRoot(false);
        VBox.setVgrow(collisionTreeTable, Priority.ALWAYS);

        // Apply table styles
        collisionTreeTable.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 4px;"
        );

        // Table columns
        TreeTableColumn<CollisionEntry, String> exam1Col =
            new TreeTableColumn<>("Exam 1");
        exam1Col.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getValue().exam1QualifiedName)
        );

        TreeTableColumn<CollisionEntry, String> exam2Col =
            new TreeTableColumn<>("Exam 2");
        exam2Col.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getValue().exam2QualifiedName)
        );

        TreeTableColumn<CollisionEntry, String> collisionCountCol =
            new TreeTableColumn<>("Collision Count");
        collisionCountCol.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getValue().collisionCount)
        );

        TreeTableColumn<CollisionEntry, String> beginCol =
            new TreeTableColumn<>("Begin");
        beginCol.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getValue().beginTime)
        );

        TreeTableColumn<CollisionEntry, String> endCol =
            new TreeTableColumn<>("End");
        endCol.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getValue().endTime)
        );

        TreeTableColumn<CollisionEntry, String> distanceCol =
            new TreeTableColumn<>("Distance");
        distanceCol.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getValue().distance)
        );

        // Style the columns
        String columnStyle = "-fx-alignment: CENTER-LEFT; -fx-padding: 8px;";
        exam1Col.setStyle(columnStyle);
        exam2Col.setStyle(columnStyle);
        collisionCountCol.setStyle(columnStyle);
        beginCol.setStyle(columnStyle);
        endCol.setStyle(columnStyle);
        distanceCol.setStyle(columnStyle);

        collisionTreeTable.getColumns().addAll(exam1Col, exam2Col, collisionCountCol, beginCol, endCol, distanceCol);

        // Store reference to tree table
        this.collisionTreeTable = collisionTreeTable;

        section.getChildren().addAll(sectionTitle, sectionDescription, separator, collisionTreeTable);
        return section;
    }

    // Inner class to hold collision entry data for the tree table
    private static class CollisionEntry {
        private final String exam1QualifiedName;
        private final String exam2QualifiedName;
        private final String collisionCount;
        private final String beginTime;
        private final String endTime;
        private final String distance;
        private final boolean isTitle;
        private final Assessment assessment;
        private final Assessment collidingAssessment;
        private final int collisionCountValue;

        // Constructor for title rows - includes all collisions in sum
        public CollisionEntry(Assessment assessment) {
            this.assessment = assessment;
            this.collidingAssessment = null;
            this.exam1QualifiedName = assessment != null ? assessment.getQualifiedName() : "";
            this.exam2QualifiedName = "";

            // Calculate total collisions including exams without dates
            Map<Assessment, Integer> collisions = assessment != null ?
                assessment.getCollisionCountByAssessment() :
                Map.of();

            int totalCount = collisions.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

            this.collisionCount = String.valueOf(totalCount);
            this.collisionCountValue = totalCount;

            // Format date and time information if available
            if (assessment != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
                this.beginTime = assessment.getBegin() != null ? assessment.getBegin().format(formatter) : "";
                this.endTime = assessment.getEnd() != null ? assessment.getEnd().format(formatter) : "";
            } else {
                this.beginTime = "";
                this.endTime = "";
            }
            this.distance = "";
            this.isTitle = true;
        }

        // Constructor for collision detail rows
        public CollisionEntry(Assessment assessment, Assessment collidingAssessment, int collisionCount) {
            this.assessment = assessment;
            this.collidingAssessment = collidingAssessment;
            this.exam1QualifiedName = assessment.getQualifiedName();
            this.exam2QualifiedName = collidingAssessment.getQualifiedName();
            this.collisionCount = String.valueOf(collisionCount);
            this.collisionCountValue = collisionCount;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
            this.beginTime = assessment.getBegin() != null ? assessment.getBegin().format(formatter) : "";
            this.endTime = assessment.getEnd() != null ? assessment.getEnd().format(formatter) : "";

            // Calculate distance if possible
            if (assessment.getBegin() != null && collidingAssessment.getBegin() != null) {
                Duration duration = Duration.between(assessment.getBegin(), collidingAssessment.getBegin());
                long hours = Math.abs(duration.toHours());
                long minutes = Math.abs(duration.toMinutesPart());
                this.distance = String.format("%dh %dm", hours, minutes);
            } else {
                this.distance = "";
            }

            this.isTitle = false;
        }

        public boolean isTitle() {
            return isTitle;
        }

        public Assessment getAssessment() {
            return assessment;
        }

        public Assessment getCollidingAssessment() {
            return collidingAssessment;
        }

        public int getCollisionCountValue() {
            return collisionCountValue;
        }
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15));
        footer.setStyle("-fx-background-color: " + DARK_COLOR + ";");

        Label copyright = new Label("Exam Collision Detector © 2025");
        copyright.setTextFill(Color.WHITE);

        footer.getChildren().add(copyright);
        return footer;
    }

    private TextField createStyledTextField(String initialText) {
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

    private ComboBox<String> createStyledComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 4px;" +
            "-fx-padding: 8px;" +
            "-fx-font-size: 12px;"
        );
        return comboBox;
    }

    private Button createStyledButton(String text, String bgColor) {
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
        button.setOnMouseEntered(e ->
            button.setStyle(
                "-fx-background-color: derive(" + bgColor + ", -20%);" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 15px;" +
                "-fx-cursor: hand;" +
                "-fx-border-radius: 4px;"
            )
        );

        button.setOnMouseExited(e ->
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

    private void setupEventHandlers(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));

        // Get the file section from the input tab
        ScrollPane inputScrollPane = (ScrollPane) inputTab.getContent();
        VBox inputContent = (VBox) inputScrollPane.getContent();
        VBox fileSection = (VBox) inputContent.getChildren().get(0);

        // Get the GridPane from the file section (third child after title and separator)
        GridPane fileGrid = (GridPane) fileSection.getChildren().get(2);

        // Get the browse buttons from the grid
        Button examBrowse = (Button) fileGrid.getChildren().stream()
            .filter(node -> node instanceof Button)
            .findFirst()
            .orElseThrow();
        Button registrationBrowse = (Button) fileGrid.getChildren().stream()
            .filter(node -> node instanceof Button)
            .skip(1)
            .findFirst()
            .orElseThrow();
        Button collisionBrowse = (Button) fileGrid.getChildren().stream()
            .filter(node -> node instanceof Button)
            .skip(2)
            .findFirst()
            .orElseThrow();

        // Set button actions
        examBrowse.setOnAction(e -> selectFile(fileChooser, examPathField, primaryStage, false));
        registrationBrowse.setOnAction(e -> selectFile(fileChooser, registrationPathField, primaryStage, false));
        collisionBrowse.setOnAction(e -> selectFile(fileChooser, collisionPathField, primaryStage, true));

        // Get the action buttons from the file section (fourth child)
        HBox actionBox = (HBox) fileSection.getChildren().get(3);
        Button detectButton = (Button) actionBox.getChildren().get(0);
        Button saveButton = (Button) actionBox.getChildren().get(1);

        // Set action button handlers
        detectButton.setOnAction(e -> detectCollisions());
        saveButton.setOnAction(e -> saveCollisions());

        // Filter and sort handlers
        filterExam1Field.textProperty().addListener((obs, old, newValue) -> updateCollisionTable());
        filterExam2Field.textProperty().addListener((obs, old, newValue) -> updateCollisionTable());
        sortExam1Box.setOnAction(e -> updateSort());
        sortExam2Box.setOnAction(e -> updateSort());

        // Double click handler for collision details
        collisionTreeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<CollisionEntry> selected =
                    collisionTreeTable.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.getValue().isTitle()) {
                    showCollisionDetails(selected.getValue());
                }
            }
        });
    }

    private void selectFile(FileChooser fileChooser, TextField field, Stage stage, boolean save) {
        File file;
        if (!save)
            file = fileChooser.showOpenDialog(stage);
        else
            file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            field.setText(file.getAbsolutePath());
            if (file.getParentFile() != null)
                fileChooser.setInitialDirectory(file.getParentFile());
        }
    }

    private void detectCollisions() {
        try {
            if (!validatePaths()) {
                showAlert("Invalid file paths! Please ensure both Exam and Registration files exist.", Alert.AlertType.ERROR);
                return;
            }

            String examsPath = examPathField.getText();
            String registrationsPath = registrationPathField.getText();

            // save paths to preferences
            prefs.put("examsPath", examsPath);
            prefs.put("registrationsPath", registrationsPath);

            Assessment[] assessments1 = LoadManager.loadExams(examPathField.getText(), null);
            Assessment[] assessments2 = LoadManager.loadMissingAssessments(registrationPathField.getText(), assessments1);
            Assessment[] assessments = Stream.of(assessments1, assessments2).flatMap(Arrays::stream).toArray(Assessment[]::new);

            Map<String, Set<String>> registrationsByAssessmentsQualifiedName = LoadManager.loadRegistrations(registrationPathField.getText(), assessments);
            // save registrations into Assessment objects
            for (Assessment p : assessments) {
                // the following exception should never occur (-> internal logic error -> bug)
                if (p.getRegisteredStudents() != null)
                    throw new AssertionError("The registration of the assessment " + p + " was already loaded.");

                ((AssessmentEditable)p).setRegisteredStudents(registrationsByAssessmentsQualifiedName.get(p.getQualifiedName()));
            }

            Map<Assessment, CollisionDetector.ReturnTuple> collisions = CollisionDetector.detectCollisions(assessments);
            // save collisions into Assessment objects
            for (Assessment p : assessments) {
                // the following exception should never occur (-> internal logic error -> bug)
                if (p.getCollisionSum() != null || p.getCollisionCountByAssessment() != null)
                    throw new AssertionError("The collisions of the assessment " + p + " was already loaded.");

                CollisionDetector.ReturnTuple collision = collisions.get(p);
                ((AssessmentEditable)p).setCollisionSum(collision.collisionSum());
                ((AssessmentEditable)p).setCollisionCountByAssessment(collision.collisionCountByAssessment());
            }

            Map<Assessment, CollisionDetector.ReturnTuple> collisionMap =
                CollisionDetector.detectCollisions(assessments);

            this.collisions = FXCollections.observableArrayList(collisionMap.entrySet());
            updateCollisionTreeTable();
            updateSort();

            showAlert("Collisions detected successfully!", Alert.AlertType.INFORMATION);

            // Switch to results tab
            tabPane.getSelectionModel().select(RESULTS_PAGE);

        } catch (Exception e) {
            showAlert("Error detecting collisions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void saveCollisions() {
        try {
            String collisionsPath = collisionPathField.getText();

            // save paths to preferences
            prefs.put("collisionsPath", collisionsPath);

            if (collisions == null || collisions.isEmpty()) {
                showAlert("No collisions to save! Please detect collisions first.", Alert.AlertType.ERROR);
                return;
            }

            Assessment[] assessmentsArray = collisions.stream()
                .map(Map.Entry::getKey)
                .toArray(Assessment[]::new);

            SaveManager.saveCollisions(collisionsPath, assessmentsArray);
            showAlert("Collisions saved successfully to " + collisionsPath, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error saving collisions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validatePaths() {
        return new File(examPathField.getText()).exists() &&
               new File(registrationPathField.getText()).exists() &&
               (!new File(collisionPathField.getText()).exists() ||
                new File(collisionPathField.getText()).canWrite());
    }

    private void updateCollisionTable() {
        if (collisions == null) return;

        updateCollisionTreeTable();
    }

    private void updateCollisionTreeTable() {
        if (collisions == null) return;

        // Create a root item to hold all entries
        TreeItem<CollisionEntry> root = new TreeItem<>(new CollisionEntry(null));

        // First, filter based on exam1 and exam2 filters
        String exam1Filter = filterExam1Field.getText().toLowerCase();
        String exam2Filter = filterExam2Field.getText().toLowerCase();

        // Process and filter the collision data
        for (Map.Entry<Assessment, CollisionDetector.ReturnTuple> entry : collisions) {
            Assessment assessment = entry.getKey();
            CollisionDetector.ReturnTuple tuple = entry.getValue();

            // Check if this assessment matches exam1 filter (only for title rows)
            boolean matchesExam1 = exam1Filter.isEmpty() ||
                assessment.getQualifiedName().toLowerCase().contains(exam1Filter);

            if (!matchesExam1) {
                continue; // Skip this assessment if it doesn't match exam1 filter
            }

            // Create a title item for this assessment
            TreeItem<CollisionEntry> titleItem = new TreeItem<>(
                new CollisionEntry(assessment)
            );
            titleItem.setExpanded(true);

            boolean hasMatchingChild = false;

            // Add child items for each colliding assessment
            for (Map.Entry<Assessment, Integer> detail : tuple.collisionCountByAssessment().entrySet()) {
                Assessment collidingAssessment = detail.getKey();
                int collisionCount = detail.getValue();

                // Check if colliding assessment matches exam2 filter
                boolean matchesExam2 = exam2Filter.isEmpty() ||
                    collidingAssessment.getQualifiedName().toLowerCase().contains(exam2Filter);

                if (matchesExam2) {
                    TreeItem<CollisionEntry> childItem = new TreeItem<>(
                        new CollisionEntry(assessment, collidingAssessment, collisionCount)
                    );
                    titleItem.getChildren().add(childItem);
                    hasMatchingChild = true;
                }
            }

            // Add title item if it either:
            // 1. Has no exam2 filter (so we show all title rows)
            // 2. Has matching children (so we show title rows with matching children)
            if (exam2Filter.isEmpty() || hasMatchingChild) {
                root.getChildren().add(titleItem);
            }
        }

        // Sort the data
        sortTreeItems(root);

        // Update the tree table
        collisionTreeTable.setRoot(root);
    }

    private void sortTreeItems(TreeItem<CollisionEntry> root) {
        String exam1Sort = sortExam1Box.getValue();
        String exam2Sort = sortExam2Box.getValue();

        // Sort the title rows (Exam 1) using primary sort
        root.getChildren().sort((item1, item2) ->
            compareCollisionEntries(item1.getValue(), item2.getValue(), exam1Sort));

        // Sort children of each title row (Exam 2) using secondary sort
        for (TreeItem<CollisionEntry> titleItem : root.getChildren()) {
            titleItem.getChildren().sort((item1, item2) ->
                compareCollisionEntries(item1.getValue(), item2.getValue(), exam2Sort));
        }
    }

    private int compareCollisionEntries(CollisionEntry entry1, CollisionEntry entry2, String property) {
        if (entry1.isTitle() && !entry2.isTitle()) return -1;
        if (!entry1.isTitle() && entry2.isTitle()) return 1;

        Assessment a1 = entry1.getAssessment();
        Assessment a2 = entry2.getAssessment();

        if (a1 == null || a2 == null) return 0;

        return switch (property) {
            case "Exam Name" -> {
                if (entry1.isTitle()) {
                    // For title rows (Exam 1), sort by primary exam
                    yield a1.getQualifiedName().compareTo(a2.getQualifiedName());
                } else {
                    // For child rows (Exam 2), sort by the colliding assessment
                    Assessment c1 = entry1.getCollidingAssessment();
                    Assessment c2 = entry2.getCollidingAssessment();
                    if (c1 != null && c2 != null) {
                        yield c1.getQualifiedName().compareTo(c2.getQualifiedName());
                    }
                    yield 0;
                }
            }
            case "Date" -> {
                Assessment compareA1 = entry1.isTitle() ? a1 : entry1.getCollidingAssessment();
                Assessment compareA2 = entry2.isTitle() ? a2 : entry2.getCollidingAssessment();

                if (compareA1.getBegin() == null || compareA2.getBegin() == null) {
                    yield (compareA1.getBegin() == null) ? -1 : 1;
                }
                yield compareA1.getBegin().toLocalDate().compareTo(compareA2.getBegin().toLocalDate());
            }
            case "Time" -> {
                Assessment compareA1 = entry1.isTitle() ? a1 : entry1.getCollidingAssessment();
                Assessment compareA2 = entry2.isTitle() ? a2 : entry2.getCollidingAssessment();

                if (compareA1.getBegin() == null || compareA2.getBegin() == null) {
                    yield (compareA1.getBegin() == null) ? -1 : 1;
                }
                yield compareA1.getBegin().toLocalTime().compareTo(compareA2.getBegin().toLocalTime());
            }
            case "Room" -> {
                Assessment compareA1 = entry1.isTitle() ? a1 : entry1.getCollidingAssessment();
                Assessment compareA2 = entry2.isTitle() ? a2 : entry2.getCollidingAssessment();
                yield compareA1.getQualifiedName().compareTo(compareA2.getQualifiedName());
            }
            case "Collision Count" -> Integer.compare(
                entry2.getCollisionCountValue(),  // Descending order
                entry1.getCollisionCountValue()
            );
            default -> 0;
        };
    }

    private void updateSort() {
        updateCollisionTreeTable();
        savePreferences();
    }

    private void showCollisionDetails(CollisionEntry collision) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Collision Details");
        dialog.setHeaderText("Detailed collisions for: " + collision.getAssessment().getQualifiedName());

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Add exam time information
        Assessment exam = collision.getAssessment();
        if (exam.getBegin() != null && exam.getEnd() != null) {
            HBox timeInfo = new HBox(10);
            timeInfo.setAlignment(Pos.CENTER_LEFT);
            timeInfo.setStyle(
                "-fx-background-color: " + LIGHT_COLOR + ";" +
                "-fx-padding: 10px;" +
                "-fx-border-radius: 5px;" +
                "-fx-background-radius: 5px;"
            );

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
            Label timeLabel = new Label("Scheduled: " +
                exam.getBegin().format(formatter) + " to " +
                exam.getEnd().format(formatter));
            timeLabel.setStyle("-fx-font-weight: bold;");

            timeInfo.getChildren().add(timeLabel);
            content.getChildren().add(timeInfo);
        }

        // Add colliding assessment details
        Assessment collidingAssessment = collision.getCollidingAssessment();
        if (collidingAssessment != null) {
            HBox collisionInfo = new HBox(10);
            collisionInfo.setAlignment(Pos.CENTER_LEFT);
            collisionInfo.setStyle(
                "-fx-background-color: " + LIGHT_COLOR + ";" +
                "-fx-padding: 10px;" +
                "-fx-border-radius: 5px;" +
                "-fx-background-radius: 5px;"
            );

            VBox examInfo = new VBox(5);

            Label nameLabel = new Label("Colliding Assessment: " + collidingAssessment.getQualifiedName());
            nameLabel.setStyle("-fx-font-weight: bold;");
            examInfo.getChildren().add(nameLabel);

            // Add time information if available
            if (collidingAssessment.getBegin() != null && collidingAssessment.getEnd() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
                Label timeLabel = new Label("Scheduled: " +
                    collidingAssessment.getBegin().format(formatter) + " to " +
                    collidingAssessment.getEnd().format(formatter));
                timeLabel.setStyle("-fx-font-size: 12px;");
                examInfo.getChildren().add(timeLabel);
            }

            collisionInfo.getChildren().add(examInfo);
            content.getChildren().add(collisionInfo);
        }

        // Add collision count
        Label countLabel = new Label("Collision Count: " + collision.getCollisionCountValue() + " student(s)");
        countLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 15px 0 5px 0;");
        content.getChildren().add(countLabel);

        // Add distance information if available
        if (collision.getCollidingAssessment() != null &&
            exam.getBegin() != null &&
            collision.getCollidingAssessment().getBegin() != null) {

            Duration duration = Duration.between(exam.getBegin(), collision.getCollidingAssessment().getBegin());
            long hours = Math.abs(duration.toHours());
            long minutes = Math.abs(duration.toMinutesPart());

            Label distanceLabel = new Label(String.format("Time Distance: %d hours %d minutes", hours, minutes));
            distanceLabel.setStyle("-fx-font-size: 14px; -fx-padding: 5px 0;");
            content.getChildren().add(distanceLabel);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Style the dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.setPrefWidth(600);

        dialog.showAndWait();
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void loadPreferences() {
        sortExam1Box.setValue(prefs.get("sortExam1", "Exam Name"));
        sortExam2Box.setValue(prefs.get("sortExam2", "Date"));
        filterExam1Field.setText(prefs.get("filterExam1", ""));
        filterExam2Field.setText(prefs.get("filterExam2", ""));
    }

    private void savePreferences() {
        prefs.put("sortExam1", sortExam1Box.getValue());
        prefs.put("sortExam2", sortExam2Box.getValue());
        prefs.put("filterExam1", filterExam1Field.getText());
        prefs.put("filterExam2", filterExam2Field.getText());
    }

    @Override
    public void stop() {
        savePreferences();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
