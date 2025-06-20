package gui;

import data.Assessment;
import data.MergedAssessment;
import manager.optimizer.AssessmentOptimizer;
import manager.AssessmentsManager;
import manager.SaveManager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.effect.DropShadow;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

public class ExamGUI extends Application {
    // Style constants
    private static final String PRIMARY_COLOR = "#3498db";
    private static final String SECONDARY_COLOR = "#2c3e50";
    private static final String SUCCESS_COLOR = "#2ecc71";
    private static final String DARK_COLOR = "#34495e";

    // Application constants
    private static final String APP_NAME = "Exam Collision Detector";
    private static final String APP_VERSION = "1.5.0";
    private static final String[] APP_AUTHORS = {
            "Johannes Kerger",
            "Joshua Bedford",
            "Razvan Grumaz",
            "Azat Özden"
    };
    private static final String COPYRIGHT_YEAR = "2025";

    // Page constants
    private static final int INPUT_PAGE = 0;
    private static final int RESULTS_PAGE = 1;

    private CollisionsTab collisionsTab;
    private TextField examPathField;
    private TextField registrationPathField;
    private TextField collisionPathField;
    private TextField optionalYearField;
    private Assessment[] assessments;
    private MergedAssessment[] optimizedStatAssessments;
    private final Preferences prefs = Preferences.userRoot().node("/assessment_collision_detector");
    private final Preferences preferencesCollisionsTab = Preferences.userRoot().node("/assessment_collision_detector/collisions_tab/collisions_table");
    private final Preferences preferencesOptimizerTab = Preferences.userRoot().node("/assessment_collision_detector/optimizer_tab/collisions_table");

    // Page management
    private TabPane tabPane;
    private Tab inputTab;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Exam Collision Detector");
        primaryStage.setMaximized(true);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            throwable.printStackTrace(pw);
            String[] stackTrace = sw.getBuffer().toString().replace("\r", "").split("\n");
            String err;
            if (stackTrace.length <= 20) {
                err = Arrays.stream(stackTrace).reduce("", (s1, s2) -> s1 + "\n" + s2);
            } else {
                err = Arrays.stream(stackTrace).limit(19).reduce("", (s1, s2) -> s1 + "\n" + s2);
                err += "\n... and " + (stackTrace.length - 19) + " more lines.";
            }
            throwable.printStackTrace();
            showAlert(err, Alert.AlertType.ERROR);
        });

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

        // Initialize OptimizeTab
        OptimizeTab optimizeTab = new OptimizeTab(this::optimizeStart, preferencesOptimizerTab);

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

        collisionsTab = new CollisionsTab();

        // Add tabs to tab pane
        tabPane.getTabs().addAll(inputTab, collisionsTab.getTab(), optimizeTab.getTab(), createStatisticsTab(), createRoomPlansTab());

        // Set initial tab to input
        tabPane.getSelectionModel().select(INPUT_PAGE);

        root.setCenter(tabPane);

        // Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Create and show scene
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);

        // Setup file choosers and event handlers
        setupEventHandlers(primaryStage);

        primaryStage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(20, 15, 20, 15));
        header.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");

        // Create left section for title
        HBox titleSection = new HBox();
        titleSection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleSection, Priority.ALWAYS);

        Label title = new Label(APP_NAME);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        titleSection.getChildren().add(title);

        // Create right section for About button
        HBox buttonSection = new HBox();
        buttonSection.setAlignment(Pos.CENTER_RIGHT);

        Button aboutButton = new Button("About");
        aboutButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: white;" +
                        "-fx-border-radius: 3px;"
        );
        aboutButton.setOnAction(e -> showAboutDialog());
        buttonSection.getChildren().add(aboutButton);

        // Add both sections to header
        header.getChildren().addAll(titleSection, buttonSection);
        HBox.setHgrow(titleSection, Priority.ALWAYS);

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

        // Optional Year section
        HBox optionalYearBox = new HBox(10);
        optionalYearBox.setAlignment(Pos.CENTER_LEFT);
        optionalYearBox.setPadding(new Insets(15, 0, 0, 0));

        Label optionalYearLabel = new Label("Year (Optional):");
        optionalYearLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        optionalYearLabel.setTextFill(Color.web(SECONDARY_COLOR));

        optionalYearField = createStyledTextField("");
        optionalYearField.setPromptText("YYYY");
        //optionalYearField.setPadding(new Insets(0,1,0,0));
        optionalYearField.setPrefWidth(100);

        optionalYearBox.getChildren().addAll(optionalYearLabel, optionalYearField);

        // Action buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(20, 0, 0, 0));

        Button detectButton = createStyledButton("Detect Collisions", SUCCESS_COLOR);
        Button saveButton = createStyledButton("Save Collisions", PRIMARY_COLOR);

        actionBox.getChildren().addAll(detectButton, saveButton);

        section.getChildren().addAll(sectionTitle, separator, fileGrid, optionalYearBox, actionBox);
        return section;
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

        // Create directory chooser for output
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir")));

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
        HBox actionBox = (HBox) fileSection.getChildren().get(4);
        Button detectButton = (Button) actionBox.getChildren().get(0);
        Button saveButton = (Button) actionBox.getChildren().get(1);

        // Set action button handlers
        detectButton.setOnAction(e -> detectCollisions());
        saveButton.setOnAction(e -> saveCollisions());
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
        String examsPath = examPathField.getText();
        String registrationsPath = registrationPathField.getText();

        if (!new File(examsPath).exists()) {
            showAlert("Invalid exams file path!", Alert.AlertType.ERROR);
            return;
        }

        if (!new File(registrationsPath).exists()) {
            showAlert("Invalid registrations file path!", Alert.AlertType.ERROR);
            return;
        }


        String yearInput = optionalYearField.getText();
        Integer year;
        if (yearInput == null || yearInput.isEmpty()) {
            year = null;
        }
        else if (!(yearInput.matches("\\d{4}"))) {
            showAlert("Invalid year!", Alert.AlertType.ERROR);
            return;
        } else {
            // Parse year, store it and update table
            year = Integer.parseInt(yearInput.trim());
        }

        // save paths to preferences
        prefs.put("examsPath", examsPath);
        prefs.put("registrationsPath", registrationsPath);

        assessments = AssessmentsManager.loadAllAssessments(examsPath, registrationsPath, year);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, registrationsPath);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        collisionsTab.enable_tab(assessments, preferencesCollisionsTab);

        showAlert("Collisions detected successfully!", Alert.AlertType.INFORMATION);

        // Switch to results tab
        tabPane.getSelectionModel().select(RESULTS_PAGE);
    }

    //Wenn man einen Speicherpfad angibt und auf dem Button Save klickt
    private void saveCollisions() {
        String collisionsPath = collisionPathField.getText();

        // Check if path is null or empty
        if (collisionsPath == null || collisionsPath.trim().isEmpty()) {
            showAlert("Please specify a file path for saving collisions.", Alert.AlertType.ERROR);
            return;
        }

        File collisionFile = new File(collisionsPath);

        // Check if parent directory exists and is accessible
        File parentDir = collisionFile.getParentFile();
        if (parentDir == null) {
            showAlert("Invalid file path. Please specify a valid directory path.", Alert.AlertType.ERROR);
            return;
        }

        if (!parentDir.exists()) {
            showAlert("Directory does not exist: " + parentDir.getPath(), Alert.AlertType.ERROR);
            return;
        }

        if (!parentDir.canWrite()) {
            showAlert("Cannot write to directory: " + parentDir.getPath() + "\nPlease check directory permissions.", Alert.AlertType.ERROR);
            return;
        }

        // Check if file exists and is writable
        if (collisionFile.exists() && !collisionFile.canWrite()) {
            showAlert("Cannot write to file: " + collisionFile.getPath() + "\nPlease check file permissions.", Alert.AlertType.ERROR);
            return;
        }

        // Check if we have collision data to save
        if (assessments == null || assessments.length == 0) {
            showAlert("No collisions to save! Please detect collisions first.", Alert.AlertType.ERROR);
            return;
        }

        // Save paths to preferences
        prefs.put("collisionsPath", collisionsPath);

        // Proceed with saving
        try {
            SaveManager.saveCollisions(collisionsPath, assessments);
            showAlert("Collisions saved successfully to " + collisionsPath, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error saving collisions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(1000);

        alert.showAndWait();
    }

    private Tab createStatisticsTab() {
        Tab statisticsTab = new Tab("Statistics");

        // Create main container with BorderPane to allow positioning
        BorderPane mainContainer = new BorderPane();
        mainContainer.setPadding(new Insets(20));

        // Create HBox for the dropdown at top left
        HBox selectorContainer = new HBox(10);
        selectorContainer.setAlignment(Pos.TOP_LEFT);

        // Create the statistics selector
        ComboBox<String> selectStatistic = new ComboBox<>();
        selectStatistic.getItems().addAll("Fakultäten", "Studiengänge", "Kollisionsschwere");
        selectStatistic.setPromptText("Wählen Sie eine Statistik.");

        selectorContainer.getChildren().add(selectStatistic);
        mainContainer.setTop(selectorContainer);

        // Content area where statistics will be displayed
        VBox contentArea = new VBox(20);
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setPadding(new Insets(20, 0, 0, 0));
        mainContainer.setCenter(contentArea);


        // Create a container for course selection and pie chart
        VBox courseChartContainer = new VBox(15);
        courseChartContainer.setPadding(new Insets(20));
        courseChartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        courseChartContainer.setVisible(false); // Initially hidden

        // Course selection
        ComboBox<String> courseComboBox = new ComboBox<>();
        courseComboBox.setPromptText("Wählen Sie einen Studiengang.");
        courseComboBox.setMaxWidth(300);
        // Container for the pie chart
        HBox chartBox = new HBox();
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setMinHeight(400);

        // When a course is selected, create and display the pie chart
        courseComboBox.setOnAction(e -> {
            String selectedCourse = courseComboBox.getValue();
            if (selectedCourse != null && assessments != null && assessments.length > 0) {
                // Clear previous chart
                chartBox.getChildren().clear();

                // Create new chart with the selected course
                Node pieChart = CollisionPieChartView.createCollisionPieChartByCourseOfStudy(selectedCourse, assessments);
                Node pieChartOptimized = optimizedStatAssessments == null? new VBox() : CollisionPieChartView.createOptimizedCollisionPieChartByCourseOfStudy(selectedCourse, optimizedStatAssessments);

                chartBox.getChildren().addAll(pieChart, pieChartOptimized);

            }
        });
        // Add components to the course chart container
        Label courseHeading = new Label("Kollisionen nach Studiengang");
        courseHeading.setFont(Font.font("System", FontWeight.BOLD, 16));
        courseHeading.setTextFill(Color.web(SECONDARY_COLOR));

        Label courseDescription = new Label("Zeigt die Verteilung der Kollisionen nach Zeitabständen zwischen Prüfungen.");
        courseDescription.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        courseChartContainer.getChildren().addAll(
                courseHeading,
                courseDescription,
                separator,
                courseComboBox,
                chartBox
        );




        // Action handler for the statistics selector
        selectStatistic.setOnAction(event -> {
            String selectedStatistic = selectStatistic.getValue();

            // Clear current content
            contentArea.getChildren().clear();

            // Handle the selected statistic
            switch(selectedStatistic) {
                case "Fakultäten" -> {
                    if(assessments != null){
                        HBox facultyContent = new HBox();
                        facultyContent.setAlignment(Pos.CENTER);
                        facultyContent.setMinHeight(800);
                        Node facultyChart = CollisionPieChartView.createCollisionPieChartByFaculty(assessments);
                        Node optimizedFacultyChart = optimizedStatAssessments == null? new VBox() : CollisionPieChartView.createOptimizedCollisionPieChartByFaculty(optimizedStatAssessments);
                        facultyContent.getChildren().addAll(facultyChart, optimizedFacultyChart);

                        ScrollPane sp = new ScrollPane();
                        sp.setMinWidth(500);
                        sp.setContent(facultyContent);
                        courseChartContainer.setVisible(false);
                        contentArea.getChildren().add(sp);
                    }else{
                        showAlert("Bitte laden Sie zuerst die Daten und erkennen Sie Kollisionen.", Alert.AlertType.WARNING);
                    }
                }
                case "Studiengänge" -> {
                    // Update course selection box first
                    if (assessments != null && assessments.length > 0) {
                        Set<String> courses = new TreeSet<>();
                        for (Assessment a : assessments) {
                            if (a.getCourseOfStudy() != null && !a.getCourseOfStudy().isEmpty()) {
                                courses.add(a.getCourseOfStudy());
                            }
                        }
                        courseComboBox.getItems().clear();
                        courseComboBox.getItems().addAll(courses);
                    } else {
                        courseComboBox.getItems().clear();
                        showAlert("Bitte laden Sie zuerst die Daten und erkennen Sie Kollisionen.", Alert.AlertType.WARNING);
                    }

                    // Show the course selection and chart container
                    courseChartContainer.setVisible(true);
                    contentArea.getChildren().add(courseChartContainer);
                }
                case "Kollisionsschwere" -> {
                    if(assessments != null){
                        courseChartContainer.setVisible(false);
                        contentArea.getChildren().add(createColSchwereContent());
                    }else{
                        showAlert("Bitte laden Sie zuerst die Daten und erkennen Sie Kollisionen.", Alert.AlertType.WARNING);
                    }
                }
                default -> System.out.println("No statistic selected");
            }
        });

        statisticsTab.setContent(mainContainer);
        return statisticsTab;
    }

    private Node createColSchwereContent() {
        HBox collisionBox = new HBox();
        collisionBox.setAlignment(Pos.CENTER);
        collisionBox.setMinHeight(400);
        Node collisionChart = CollisionPieChartView.createCollisionPieChartByTime(assessments);
        Node collisionChartOptimized = optimizedStatAssessments == null? new VBox() : CollisionPieChartView.createOptimizedCollisionPieChartByTime(optimizedStatAssessments);
        collisionBox.getChildren().addAll(collisionChart, collisionChartOptimized);
        return collisionBox;
    }


    private Tab createRoomPlansTab() {
        Tab roomPlansTab = new Tab("Room Occupancy Plans");
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label inProgressLabel = new Label("Room Occupancy Plans - In Progress");
        inProgressLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        inProgressLabel.setTextFill(Color.web(SECONDARY_COLOR));

        Label descriptionLabel = new Label("This feature is currently under development.");
        descriptionLabel.setTextFill(Color.web(SECONDARY_COLOR));

        content.getChildren().addAll(inProgressLabel, descriptionLabel);
        roomPlansTab.setContent(content);
        return roomPlansTab;
    }

    private void showAboutDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("About " + APP_NAME);
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // App name and version
        Label nameLabel = new Label(APP_NAME);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        nameLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Label versionLabel = new Label("Version " + APP_VERSION);
        versionLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        versionLabel.setTextFill(Color.web(SECONDARY_COLOR));

        // Authors section
        Label authorsTitle = new Label("Authors:");
        authorsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        authorsTitle.setTextFill(Color.web(SECONDARY_COLOR));

        VBox authorsBox = new VBox(5);
        for (String author : APP_AUTHORS) {
            Label authorLabel = new Label("• " + author);
            authorLabel.setTextFill(Color.web(SECONDARY_COLOR));
            authorsBox.getChildren().add(authorLabel);
        }

        // Copyright
        Label copyrightLabel = new Label("© " + COPYRIGHT_YEAR + " All rights reserved.");
        copyrightLabel.setTextFill(Color.web(SECONDARY_COLOR));

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));

        content.getChildren().addAll(
                nameLabel,
                versionLabel,
                separator,
                authorsTitle,
                authorsBox,
                new Separator(),
                copyrightLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(400);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        dialog.showAndWait();
    }

    // TODO
    public MergedAssessment [] optimizeStart(boolean room, Boolean supervisor) {
        if (assessments == null || assessments.length == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Assessments");
            alert.setHeaderText(null);
            alert.setContentText("No assessments found. Please run Detect Collisions first.");
            alert.showAndWait();
            return null;
        }

        MergedAssessment[] mergedAssessments = AssessmentOptimizer.mergeAssessments(assessments);

        MergedAssessment[][] assessmentGroups = AssessmentOptimizer.getAssessmentGroups(mergedAssessments);
        MergedAssessment[] optimizedAssessments = AssessmentOptimizer.optimizeAssessmentGroups(assessmentGroups, supervisor, room);

        this.optimizedStatAssessments = optimizedAssessments;

        return optimizedAssessments;
    }



    public static void run(){
        launch();
    }
}

