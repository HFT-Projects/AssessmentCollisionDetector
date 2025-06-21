package gui;

import data.Assessment;
import data.MergedAssessment;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import manager.AssessmentsManager;
import manager.SaveManager;
import manager.optimizer.AssessmentOptimizer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class MainGUI extends Application {
    static final String PRIMARY_COLOR = "#3498db";
    static final String SECONDARY_COLOR = "#2c3e50";
    static final String SUCCESS_COLOR = "#2ecc71";
    static final String DARK_COLOR = "#34495e";

    private static final String APP_NAME = "Exam Collision Detector";
    private static final String APP_VERSION = "1.5.0";
    private static final String[] APP_AUTHORS = {
            "Johannes Wilhelm Hermann Kerger",
            "Joshua Bedford",
            "Razvan Grumaz",
            "Azat Özden"
    };
    private static final String COPYRIGHT_YEAR = "2025";

    private final Preferences preferencesCollisionsTab = Preferences.userRoot().node("/assessment_collision_detector/collisions_tab/collisions_table");
    private final Preferences preferencesOptimizerTab = Preferences.userRoot().node("/assessment_collision_detector/optimizer_tab/collisions_table");
    private final Preferences preferencesInputTab = Preferences.userRoot().node("/assessment_collision_detector/input_tab");

    private FileChooser fileChooser;
    private Stage primaryStage;
    private TabPane tabPane;

    @SuppressWarnings("FieldCanBeLocal")
    private InputTab inputTab;
    private CollisionsTab collisionsTab;
    private OptimizeTab optimizeTab;
    private StatisticsTab statisticsTab;

    private Assessment[] assessments;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
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
            //noinspection CallToPrintStackTrace
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
        optimizeTab = new OptimizeTab(this, preferencesOptimizerTab);
        optimizeTab.getTab().setDisable(true);

        inputTab = new InputTab(this, preferencesInputTab);

        collisionsTab = new CollisionsTab();

        statisticsTab = new StatisticsTab();
        statisticsTab.getTab().setDisable(true);

        // Add tabs to tab pane
        tabPane.getTabs().addAll(inputTab.getTab(), collisionsTab.getTab(), optimizeTab.getTab(), statisticsTab.getTab());

        // Set initial tab to input
        tabPane.getSelectionModel().select(inputTab.getTab());

        root.setCenter(tabPane);

        // Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Create and show scene
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);

        // Setup file choosers and event handlers
        setupEventHandlers();

        primaryStage.show();

        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));

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

    // TODO
    private void setupEventHandlers() {
        // Create directory chooser for output
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
    }

    void selectFile(TextField field, boolean save) {
        File file;
        if (!save)
            file = fileChooser.showOpenDialog(primaryStage);
        else
            file = fileChooser.showSaveDialog(primaryStage);

        if (file != null) {
            field.setText(file.getAbsolutePath());
            if (file.getParentFile() != null)
                fileChooser.setInitialDirectory(file.getParentFile());
        }
    }

    void detectCollisions(String examsPath, String registrationsPath, Integer year) {
        assessments = AssessmentsManager.loadAllAssessments(examsPath, registrationsPath, year);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, registrationsPath);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        collisionsTab.enable_tab(assessments, preferencesCollisionsTab);
        optimizeTab.getTab().setDisable(false);
        statisticsTab.setAssessments(assessments);
        statisticsTab.getTab().setDisable(false);

        showAlert("Collisions detected successfully!", Alert.AlertType.INFORMATION);

        // Switch to results tab
        tabPane.getSelectionModel().select(collisionsTab.getTab());
    }

    void saveCollisions(String collisionsPath) {
        // Check if we have collision data to save
        if (assessments == null || assessments.length == 0) {
            MainGUI.showAlert("No collisions to save! Please detect collisions first.", Alert.AlertType.ERROR);
            return;
        }

        // Proceed with saving
        try {
            SaveManager.saveCollisions(collisionsPath, assessments);
            showAlert("Collisions saved successfully to " + collisionsPath, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error saving collisions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // TODO
    MergedAssessment[] optimizeStart(boolean room, Boolean supervisor) {
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

        statisticsTab.setOptimizedAssessments(optimizedAssessments);

        return optimizedAssessments;
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

    public static void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(1000);

        alert.showAndWait();
    }

    public static void run() {
        launch();
    }
}

