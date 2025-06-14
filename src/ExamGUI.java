import data.Assessment;

import data.AssessmentEditable;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
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
import java.util.*;
import java.util.prefs.Preferences;

import javafx.scene.effect.DropShadow;

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

    // Sort direction constants
    private static final String ASCENDING = "Ascending";
    private static final String DESCENDING = "Descending";

    private TextField examPathField;
    private TextField registrationPathField;
    private TextField collisionPathField;
    private TextField optionalYearField;
    private TreeTableView<CollisionEntry> collisionTreeTable;
    private ComboBox<String> sortExam1Box;
    private ComboBox<String> sortDirectionExam1Box;
    private ComboBox<String> sortExam2Box;
    private ComboBox<String> sortDirectionExam2Box;
    private TextField filterExam1Field;
    private TextField filterExam2Field;
    private TextField filterDistanceField;  // Add this field
    private CheckBox hideNullTimesCheckbox;  // Add this field
    private CheckBox showOnlyAssessmentsCheckbox; // Add this field
    private CheckBox showOnlyWithCollisionsCheckbox; // Add checkbox to show only assessments with collisions
    private Assessment[] assessments;
    private final Preferences prefs = Preferences.userRoot().node("/assessment_collision_detector");

    // Store expansion states for TreeItems
    private final Map<String, Boolean> expansionStates = new HashMap<>();

    // Dies speichert explizit den Zustand VOR dem Aktivieren von "Show only assessments"
    private final Map<String, Boolean> preCheckboxExpansionStates = new HashMap<>();

    // Page management
    private TabPane tabPane;
    private Tab inputTab;
    private Tab resultsTab;
    private Tab optimizedTab;
    private TreeTableView<CollisionEntry> optimizedTreeTable;

    // Flag to prevent infinite loops in sorting synchronization
    private boolean isUpdating = false;

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
            System.out.println(err);
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
        tabPane.getTabs().addAll(inputTab, resultsTab, createOptimizedTab(), createStatisticsTab(), createRoomPlansTab());

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

        // Setup column sorting behavior
        setupColumnSort();

        primaryStage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(20, 15, 20, 15));
        header.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");

        // Create left section for title
        HBox titleSection = new HBox();
        titleSection.setAlignment(Pos.CENTER_LEFT);
        titleSection.setHgrow(titleSection, Priority.ALWAYS);

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
                "Exam 1 Name", "Collision Count", "Begin", "End", "Avg. Distance", "Min. Distance"
        ));

        sortDirectionExam1Box = createStyledComboBox();
        sortDirectionExam1Box.setPromptText("Select Exam 1 sort direction...");
        sortDirectionExam1Box.setItems(FXCollections.observableArrayList(ASCENDING, DESCENDING));

        sortExam2Box = createStyledComboBox();
        sortExam2Box.setPromptText("Select Exam 2 sort...");
        sortExam2Box.setItems(FXCollections.observableArrayList(
                "Exam 2 Name", "Collision Count", "Begin", "End", "Distance"
        ));

        sortDirectionExam2Box = createStyledComboBox();
        sortDirectionExam2Box.setPromptText("Select Exam 2 sort direction...");
        sortDirectionExam2Box.setItems(FXCollections.observableArrayList(ASCENDING, DESCENDING));

        // Create and style field labels
        Label filterExam1Label = new Label("Filter Exam 1");
        filterExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label filterExam2Label = new Label("Filter Exam 2");
        filterExam2Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label filterDistanceLabel = new Label("Max Distance (hours)");
        filterDistanceLabel.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam1Label = new Label("Sort Exam 1");
        sortExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortDirectionExam1Label = new Label("Sort Direction Exam 1");
        sortDirectionExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam2Label = new Label("Sort Exam 2");
        sortExam2Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortDirectionExam2Label = new Label("Sort Direction Exam 2");
        sortDirectionExam2Label.setTextFill(Color.web(SECONDARY_COLOR));

        // Create distance filter field
        filterDistanceField = createStyledTextField("");
        filterDistanceField.setPromptText("Enter hours");
        filterDistanceField.setPrefWidth(150);

        // Add numeric validation and filtering
        filterDistanceField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Allow empty value for no filtering
            if (newVal.isEmpty()) {
                updateCollisionTreeTable();
                return;
            }

            // Allow only digits and one decimal point
            if (!newVal.matches("^\\d*\\.?\\d*$")) {
                filterDistanceField.setText(oldVal);
                return;
            }

            // Don't update on partial decimal (e.g., "." or "5.")
            if (!newVal.equals(".") && !newVal.endsWith(".")) {
                updateCollisionTreeTable();
            }
        });

        // Also update on focus lost to handle partial inputs
        filterDistanceField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                String text = filterDistanceField.getText();
                if (text.equals(".") || text.endsWith(".")) {
                    filterDistanceField.setText(text.replace(".", ""));
                }
                updateCollisionTreeTable();
            }
        });

        // Add components to grid
        controlGrid.add(filterExam1Label, 0, 0);
        controlGrid.add(filterExam1Field, 1, 0);
        controlGrid.add(filterExam2Label, 2, 0);
        controlGrid.add(filterExam2Field, 3, 0);

        controlGrid.add(sortExam1Label, 0, 1);
        controlGrid.add(sortExam1Box, 1, 1);
        controlGrid.add(sortDirectionExam1Label, 2, 1);
        controlGrid.add(sortDirectionExam1Box, 3, 1);

        controlGrid.add(sortExam2Label, 0, 2);
        controlGrid.add(sortExam2Box, 1, 2);
        controlGrid.add(sortDirectionExam2Label, 2, 2);
        controlGrid.add(sortDirectionExam2Box, 3, 2);

        // Place Max Distance filter side by side on the left
        HBox distanceBox = new HBox(10); // 10px spacing between label and field
        distanceBox.setAlignment(Pos.CENTER_LEFT);
        distanceBox.setPadding(new Insets(0, 0, 0, 0)); // Reset padding to align with other rows
        filterDistanceLabel.setPadding(new Insets(0, 10, 0, 0)); // Add some right padding to the label
        distanceBox.getChildren().addAll(filterDistanceLabel, filterDistanceField);

        // Add the HBox to the grid in the fourth row, spanning all columns
        controlGrid.add(distanceBox, 0, 3, 4, 1);

        // Configure the TextField width to be reasonable
        filterDistanceField.setPrefWidth(150); // Set a reasonable width for the field

        // Column constraints for even distribution
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(15);

        ColumnConstraints controlCol = new ColumnConstraints();
        controlCol.setPercentWidth(35);

        controlGrid.getColumnConstraints().addAll(labelCol, controlCol, labelCol, controlCol);

        section.getChildren().addAll(sectionTitle, separator, controlGrid);
        return section;
    }

    private void saveExpansionStatesToMap(TreeItem<CollisionEntry> item, Map<String, Boolean> stateMap) {
        if (item == null) return;

        // Nur speichern wenn Item einen Wert hat (nicht für Root)
        if (item.getValue() != null) {
            String key = getItemKey(item.getValue());
            if (key != null) {
                stateMap.put(key, item.isExpanded());
            }
        }

        // Für alle Kinder rekursiv
        for (TreeItem<CollisionEntry> child : item.getChildren()) {
            saveExpansionStatesToMap(child, stateMap);
        }
    }

    private void restoreExpansionStatesFromMap(TreeItem<CollisionEntry> item, Map<String, Boolean> stateMap) {
        if (item == null) return;

        // Status aus Map wiederherstellen für diesen Item
        if (item.getValue() != null) {
            String key = getItemKey(item.getValue());
            if (key != null) {
                Boolean wasExpanded = stateMap.get(key);
                if (wasExpanded != null) {
                    item.setExpanded(wasExpanded);
                } else if (item.getValue().isTitle()) {
                    // Wenn kein Status gespeichert ist und es ein Titel ist,
                    // default auf expanded für bessere Benutzbarkeit
                    item.setExpanded(true);
                }
            }
        }

        // Für alle Kinder rekursiv
        for (TreeItem<CollisionEntry> child : item.getChildren()) {
            restoreExpansionStatesFromMap(child, stateMap);
        }
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

        // Create controls container
        HBox controlsContainer = new HBox(20);
        controlsContainer.setAlignment(Pos.CENTER_LEFT);

        // Add checkboxes for filtering
        hideNullTimesCheckbox = new CheckBox("Hide entries with no times");
        hideNullTimesCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        showOnlyAssessmentsCheckbox = new CheckBox("Show only assessments");
        showOnlyAssessmentsCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        showOnlyWithCollisionsCheckbox = new CheckBox("Show only with collisions");
        showOnlyWithCollisionsCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        // Create columns menu button
        MenuButton columnsMenuButton = new MenuButton("Columns");
        columnsMenuButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"
        );

        controlsContainer.getChildren().addAll(hideNullTimesCheckbox, showOnlyAssessmentsCheckbox, showOnlyWithCollisionsCheckbox, columnsMenuButton);

        // Separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // Create tree table view instead of regular table
        TreeTableView<CollisionEntry> collisionTreeTable = new TreeTableView<>();
        collisionTreeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        collisionTreeTable.setShowRoot(false);
        VBox.setVgrow(collisionTreeTable, Priority.ALWAYS);

        // Set up the filtering
        hideNullTimesCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            // Vor dem Filtern oder Neuaufbauen immer die Expansion-States speichern
            if (collisionTreeTable.getRoot() != null) {
                saveExpansionStates(collisionTreeTable.getRoot());
            }

            if (!showOnlyAssessmentsCheckbox.isSelected() || !newValue) {
                updateCollisionTreeTable();

                // Nach dem Neuaufbau States wiederherstellen
                if (collisionTreeTable.getRoot() != null) {
                    restoreExpansionStates(collisionTreeTable.getRoot());

                    // Wenn "Show Only Assessment" aktiv ist, immer wieder einklappen
                    if (showOnlyAssessmentsCheckbox.isSelected()) {
                        for (TreeItem<CollisionEntry> titleItem : collisionTreeTable.getRoot().getChildren()) {
                            titleItem.setExpanded(false);
                        }
                    }
                }
            } else {
                TreeItem<CollisionEntry> root = collisionTreeTable.getRoot();
                if (root != null) {
                    filterTreeItems(root, newValue);
                }
            }
        });

        // Set up assessment-only view toggle
        showOnlyAssessmentsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            TreeItem<CollisionEntry> root = collisionTreeTable.getRoot();
            if (root == null) return;

            if (newValue) {  // Checkbox wurde aktiviert - speichern und einklappen
                // Speichere aktuellen Zustand explizit in die dedizierte Map
                preCheckboxExpansionStates.clear();
                saveExpansionStatesToMap(root, preCheckboxExpansionStates);

                // Alle einklappen, keine Kinder entfernen
                for (TreeItem<CollisionEntry> titleItem : root.getChildren()) {
                    titleItem.setExpanded(false);
                }
            } else {  // Checkbox wurde deaktiviert - wiederherstellen
                // Zuerst alle Einträge sichtbar machen falls nötig
                boolean wasHidingNullTimes = hideNullTimesCheckbox.isSelected();
                if (wasHidingNullTimes) {
                    // Deaktivieren ohne Listener auszulösen
                    hideNullTimesCheckbox.setSelected(false);
                    // Baum neu aufbauen mit allen Einträgen
                    updateCollisionTreeTable();
                }

                // Jetzt auf den neuen root anwenden (könnte sich nach updateCollisionTreeTable geändert haben)
                root = collisionTreeTable.getRoot();
                if (root != null) {
                    restoreExpansionStatesFromMap(root, preCheckboxExpansionStates);
                }

                // Filter ggf. wieder einschalten
                if (wasHidingNullTimes) {
                    hideNullTimesCheckbox.setSelected(true);
                }
            }
        });

        // Set up with-collisions-only view toggle
        showOnlyWithCollisionsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            // Save expansion states before updating
            if (collisionTreeTable.getRoot() != null) {
                saveExpansionStates(collisionTreeTable.getRoot());
            }

            // Update the table with the new filter
            updateCollisionTreeTable();

            // Restore expansion states
            if (collisionTreeTable.getRoot() != null) {
                restoreExpansionStates(collisionTreeTable.getRoot());

                // Keep items collapsed if "Show Only Assessments" is checked
                if (showOnlyAssessmentsCheckbox.isSelected()) {
                    for (TreeItem<CollisionEntry> titleItem : collisionTreeTable.getRoot().getChildren()) {
                        titleItem.setExpanded(false);
                    }
                }
            }
        });

        // Table columns
        TreeTableColumn<CollisionEntry, String> exam1Col = new TreeTableColumn<>("Exam 1");
        exam1Col.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().exam1QualifiedName)
        );
        exam1Col.setSortable(true);

        TreeTableColumn<CollisionEntry, String> exam2Col = new TreeTableColumn<>("Exam 2");
        exam2Col.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().exam2QualifiedName)
        );
        exam2Col.setSortable(true);

        TreeTableColumn<CollisionEntry, String> collisionCountCol = new TreeTableColumn<>("Collision Count");
        collisionCountCol.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().collisionCount)
        );
        collisionCountCol.setSortable(false);  // Disable sorting
        collisionCountCol.setComparator((s1, s2) -> {
            try {
                return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> beginCol = new TreeTableColumn<>("Begin");
        beginCol.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().beginTime)
        );
        beginCol.setSortable(false);  // Disable sorting

        TreeTableColumn<CollisionEntry, String> endCol = new TreeTableColumn<>("End");
        endCol.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().endTime)
        );
        endCol.setSortable(false);  // Disable sorting

        TreeTableColumn<CollisionEntry, String> distanceCol = new TreeTableColumn<>("Distance");
        distanceCol.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().distance)
        );
        distanceCol.setSortable(true);
        distanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> avgDistanceCol = new TreeTableColumn<>("Avg. Distance");
        avgDistanceCol.setCellValueFactory(param -> {
            CollisionEntry entry = param.getValue().getValue();
            if (entry.isTitle()) {
                return new SimpleStringProperty(entry.getAverageDistance());
            }
            return new SimpleStringProperty("");
        });
        avgDistanceCol.setSortable(true);
        avgDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> maxDistanceCol = new TreeTableColumn<>("Min. Distance");
        maxDistanceCol.setCellValueFactory(param -> {
            CollisionEntry entry = param.getValue().getValue();
            if (entry.isTitle()) {
                return new SimpleStringProperty(entry.getMinDistance());
            }
            return new SimpleStringProperty("");
        });
        maxDistanceCol.setSortable(true);
        maxDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        // Create column visibility menu items
        CheckMenuItem exam1Item = new CheckMenuItem("Exam 1");
        exam1Item.setSelected(true);
        exam1Item.setDisable(true);  // First column cannot be hidden

        CheckMenuItem exam2Item = new CheckMenuItem("Exam 2");
        exam2Item.setSelected(true);
        exam2Item.setDisable(true);

        CheckMenuItem collisionCountItem = new CheckMenuItem("Collision Count");
        collisionCountItem.setSelected(true);
        collisionCountItem.selectedProperty().addListener((obs, old, newValue) -> {
            collisionCountCol.setVisible(newValue);
            adjustColumnWidths();
        });

        CheckMenuItem beginItem = new CheckMenuItem("Begin");
        beginItem.setSelected(true);
        beginItem.selectedProperty().addListener((obs, old, newValue) -> {
            beginCol.setVisible(newValue);
            adjustColumnWidths();
        });

        CheckMenuItem endItem = new CheckMenuItem("End");
        endItem.setSelected(true);
        endItem.selectedProperty().addListener((obs, old, newValue) -> {
            endCol.setVisible(newValue);
            adjustColumnWidths();
        });

        CheckMenuItem distanceItem = new CheckMenuItem("Distance");
        distanceItem.setSelected(true);
        distanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            distanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        CheckMenuItem avgDistanceItem = new CheckMenuItem("Avg. Distance");
        avgDistanceItem.setSelected(true);
        avgDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            avgDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        CheckMenuItem maxDistanceItem = new CheckMenuItem("Min. Distance");
        maxDistanceItem.setSelected(true);
        maxDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            maxDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        // Add menu items to the columns menu
        columnsMenuButton.getItems().addAll(
                exam1Item, exam2Item, collisionCountItem,
                new SeparatorMenuItem(),
                beginItem, endItem, distanceItem,
                new SeparatorMenuItem(),
                avgDistanceItem, maxDistanceItem
        );

        // Set equal width constraints for all columns
        exam1Col.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.18));
        exam2Col.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.18));
        collisionCountCol.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.12));
        beginCol.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.13));
        endCol.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.13));
        distanceCol.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.13));
        avgDistanceCol.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.13));
        maxDistanceCol.prefWidthProperty().bind(collisionTreeTable.widthProperty().multiply(0.13));

        // Style the columns
        String columnStyle = "-fx-alignment: CENTER-LEFT; -fx-padding: 8px;";
        exam1Col.setStyle(columnStyle);
        exam2Col.setStyle(columnStyle);
        collisionCountCol.setStyle(columnStyle);
        beginCol.setStyle(columnStyle);
        endCol.setStyle(columnStyle);
        distanceCol.setStyle(columnStyle);
        avgDistanceCol.setStyle(columnStyle);
        maxDistanceCol.setStyle(columnStyle);

        // Make sort arrows always visible for all columns


        collisionTreeTable.getColumns().addAll(exam1Col, exam2Col, collisionCountCol,
                beginCol, endCol, distanceCol, avgDistanceCol, maxDistanceCol);

        // Set default sort on Exam 1 column (ascending)
        exam1Col.setSortType(TreeTableColumn.SortType.ASCENDING);
        collisionTreeTable.getSortOrder().add(exam1Col);

        // Prevent "no sort" state by forcing ascending when sort type becomes null
        for (TreeTableColumn<CollisionEntry, ?> column : collisionTreeTable.getColumns()) {
            column.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    column.setSortType(TreeTableColumn.SortType.ASCENDING);
                }
            });
        }

        // Store reference to tree table
        this.collisionTreeTable = collisionTreeTable;

        section.getChildren().addAll(
                sectionTitle,
                sectionDescription,
                controlsContainer,
                separator,
                collisionTreeTable
        );
        return section;
    }

    private void adjustColumnWidths() {
        // Ensure we're on the JavaFX Application Thread
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(this::adjustColumnWidths);
            return;
        }

        // First pass: Reset all column bindings and widths
        collisionTreeTable.getColumns().forEach(column -> {
            column.prefWidthProperty().unbind();
            column.setMinWidth(50);
            column.setPrefWidth(100);
        });

        // Force initial layout pass
        collisionTreeTable.layout();

        // Get current table width (excluding scrollbar if present)
        double tableWidth = collisionTreeTable.getWidth();
        double scrollbarWidth = 15;
        if (collisionTreeTable.lookup(".virtual-flow .scroll-bar:vertical") != null) {
            tableWidth -= scrollbarWidth;
        }

        final double finalTableWidth = tableWidth;

        // Count visible columns for proper width distribution
        long visibleColumns = collisionTreeTable.getColumns().stream()
                .filter(javafx.scene.control.TreeTableColumn::isVisible)
                .count();

        if (visibleColumns == 0) return;

        // Second pass: Apply proportional widths with proper bindings
        javafx.application.Platform.runLater(() -> {
            // Calculate total proportion to ensure we use 100% of width
            double totalProportion = 0;
            for (javafx.scene.control.TreeTableColumn<?, ?> column : collisionTreeTable.getColumns()) {
                if (column.isVisible()) {
                    if (column.getText().equals("Exam 1") || column.getText().equals("Exam 2")) {
                        totalProportion += 0.18;
                    } else if (column.getText().equals("Collision Count")) {
                        totalProportion += 0.12;
                    } else {
                        totalProportion += 0.13;
                    }
                }
            }

            // Scale factor to ensure proportions sum to 1.0
            final double scaleFactor = 1.0 / totalProportion;

            collisionTreeTable.getColumns().forEach(column -> {
                // Add visibility change listener to each column
                column.visibleProperty().addListener((obs, oldVal, newVal) -> {
                    // Use runLater to ensure layout is updated after visibility change
                    javafx.application.Platform.runLater(() -> {
                        adjustColumnWidths();
                        // Additional refresh after a short delay to ensure proper layout
                        javafx.application.Platform.runLater(() -> {
                            collisionTreeTable.refresh();
                            collisionTreeTable.layout();
                        });
                    });
                });

                if (column.isVisible()) {
                    // Calculate scaled proportion based on column type
                    double proportion;
                    if (column.getText().equals("Exam 1") || column.getText().equals("Exam 2")) {
                        proportion = 0.18 * scaleFactor;
                    } else if (column.getText().equals("Collision Count")) {
                        proportion = 0.12 * scaleFactor;
                    } else {
                        proportion = 0.13 * scaleFactor;
                    }

                    // Apply width binding with minimum width constraint
                    column.prefWidthProperty().bind(
                            collisionTreeTable.widthProperty().multiply(proportion)
                                    .subtract(scrollbarWidth / visibleColumns)
                    );

                    // Ensure minimum width is maintained
                    double minWidth = Math.max(50, finalTableWidth * proportion * 0.8);
                    column.setMinWidth(minWidth);
                }
            });

            // Force immediate layout update
            collisionTreeTable.refresh();
            collisionTreeTable.layout();
        });

        // Final layout pass after a short delay to ensure all changes are applied
        javafx.application.Platform.runLater(() -> {
            // Add a small delay to ensure all bindings are properly applied
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            collisionTreeTable.layout();
            collisionTreeTable.refresh();
        });
    }

    private void filterTreeItems(TreeItem<CollisionEntry> item, boolean hideNullTimes) {
        if (item == null) return;

        // Process children first (iterate over a copy to avoid concurrent modification)
        List<TreeItem<CollisionEntry>> children = new ArrayList<>(item.getChildren());
        for (TreeItem<CollisionEntry> child : children) {
            filterTreeItems(child, hideNullTimes);
        }

        // If it's not the root and should be hidden, remove it
        if (item.getValue() != null && item.getParent() != null) {
            boolean hasNullTimes = item.getValue().beginTime.isEmpty() && item.getValue().endTime.isEmpty();
            if (hideNullTimes && hasNullTimes) {
                item.getParent().getChildren().remove(item);
            }
        }

        // If this is a parent node with children, re-sort its children to maintain sort order
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            String sortCriteria = item == collisionTreeTable.getRoot() ?
                    sortExam1Box.getValue() : sortExam2Box.getValue();

            if (sortCriteria != null && !sortCriteria.isEmpty()) {
                item.getChildren().sort((item1, item2) ->
                        compareCollisionEntries(item1.getValue(), item2.getValue(), sortCriteria));
            }
        }
    }

    private void saveExpansionStates(TreeItem<CollisionEntry> item) {
        if (item == null) return;

        if (item.getValue() != null) {
            String key = getItemKey(item.getValue());
            if (key != null) {
                expansionStates.put(key, item.isExpanded());
            }
        }

        for (TreeItem<CollisionEntry> child : item.getChildren()) {
            saveExpansionStates(child);
        }
    }

    private void restoreExpansionStates(TreeItem<CollisionEntry> item) {
        if (item == null) return;

        if (item.getValue() != null) {
            String key = getItemKey(item.getValue());
            if (key != null) {
                Boolean wasExpanded = expansionStates.get(key);
                // When unchecking "Show Only Assessments", we want to restore previous
                // expansion states or expand by default for better usability
                if (wasExpanded != null) {
                    item.setExpanded(wasExpanded);
                } else if (item.getValue().isTitle()) {
                    // For assessment items without saved state, expand by default when
                    // showing children is allowed
                    item.setExpanded(true);
                }
            }
        }

        // Process children recursively
        for (TreeItem<CollisionEntry> child : item.getChildren()) {
            restoreExpansionStates(child);
        }

        // Force a layout refresh to ensure expansion state is visually updated
        collisionTreeTable.refresh();
    }

    private String getItemKey(CollisionEntry entry) {
        if (entry == null) return null;

        if (entry.isTitle()) {
            return "title:" + entry.exam1QualifiedName;
        } else {
            return entry.exam1QualifiedName + ":" + entry.exam2QualifiedName;
        }
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

        private Duration maxDuration;
        private Duration minDuration;
        private Duration totalDuration;
        private int validDurationsCount;

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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                this.beginTime = assessment.getBegin() != null ? assessment.getBegin().format(formatter) : "";
                this.endTime = assessment.getEnd() != null ? assessment.getEnd().format(formatter) : "";
            } else {
                this.beginTime = "";
                this.endTime = "";
            }
            this.distance = "";
            this.isTitle = true;

            // Initialize duration calculations
            this.maxDuration = null;
            this.minDuration = null;
            this.totalDuration = Duration.ZERO;
            this.validDurationsCount = 0;

            // Calculate distances for all collisions if this is a title row
            if (assessment != null) {
                for (Map.Entry<Assessment, Integer> detail : assessment.getCollisionCountByAssessment().entrySet()) {
                    Assessment collidingAssessment = detail.getKey();
                    if (assessment.getBegin() != null && assessment.getEnd() != null &&
                            collidingAssessment.getBegin() != null && collidingAssessment.getEnd() != null) {

                        Duration distance;
                        if (assessment.getBegin().isBefore(collidingAssessment.getBegin())) {
                            distance = Duration.between(assessment.getEnd(), collidingAssessment.getBegin());
                        } else {
                            distance = Duration.between(collidingAssessment.getEnd(), assessment.getBegin());
                        }

                        if (!distance.isNegative()) {
                            if (maxDuration == null || distance.compareTo(maxDuration) > 0) {
                                maxDuration = distance;
                            }
                            if (minDuration == null || distance.compareTo(minDuration) < 0) {
                                minDuration = distance;
                            }
                            totalDuration = totalDuration.plus(distance);
                            validDurationsCount++;
                        }
                    }
                }
            }
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
            this.beginTime = collidingAssessment.getBegin() != null ? collidingAssessment.getBegin().format(formatter) : "";
            this.endTime = collidingAssessment.getEnd() != null ? collidingAssessment.getEnd().format(formatter) : "";

            // Calculate distance if possible
            if (assessment.getBegin() != null && collidingAssessment.getBegin() != null) {
                Assessment first;
                Assessment last;
                if (assessment.getBegin().isBefore(collidingAssessment.getBegin())) {
                    first = assessment;
                    last = collidingAssessment;
                } else {
                    first = collidingAssessment;
                    last = assessment;
                }

                Duration distance = Duration.between(first.getEnd(), last.getBegin());
                long hours = distance.toHours();
                long minutes = distance.toMinutesPart();
                this.distance = String.format("%dh %dm", hours, minutes);
            } else {
                this.distance = "";
            }

            this.isTitle = false;
        }

        // Constructor for title rows with updated values after filtering
        public CollisionEntry(Assessment assessment, int visibleCollisionCount, Duration minDuration, Duration avgDuration) {
            this.assessment = assessment;
            this.collidingAssessment = null;
            this.exam1QualifiedName = assessment != null ? assessment.getQualifiedName() : "";
            this.exam2QualifiedName = "";

            // Use the passed-in visible collision count instead of calculating from all collisions
            this.collisionCount = String.valueOf(visibleCollisionCount);
            this.collisionCountValue = visibleCollisionCount;

            // Format date and time information if available
            if (assessment != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                this.beginTime = assessment.getBegin() != null ? assessment.getBegin().format(formatter) : "";
                this.endTime = assessment.getEnd() != null ? assessment.getEnd().format(formatter) : "";
            } else {
                this.beginTime = "";
                this.endTime = "";
            }

            this.distance = "";
            this.isTitle = true;

            // Set the provided min and avg duration values directly
            this.minDuration = minDuration;
            this.maxDuration = null; // We don't recalculate max distance for this feature

            // Only set total and count if we have an average
            if (avgDuration != null) {
                this.validDurationsCount = 1;
                this.totalDuration = avgDuration;
            } else {
                this.validDurationsCount = 0;
                this.totalDuration = Duration.ZERO;
            }
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

        public String getAverageDistance() {
            if (validDurationsCount == 0) return "";

            Duration avgDuration = totalDuration.dividedBy(validDurationsCount);
            return formatDuration(avgDuration);
        }

        public String getMinDistance() {
            if (minDuration == null) return "";
            return formatDuration(minDuration);
        }

        private String formatDuration(Duration duration) {
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            return String.format("%dh %dm", hours, minutes);
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
        HBox actionBox = (HBox) fileSection.getChildren().get(4);
        Button detectButton = (Button) actionBox.getChildren().get(0);
        Button saveButton = (Button) actionBox.getChildren().get(1);

        // Set action button handlers
        detectButton.setOnAction(e -> detectCollisions());
        saveButton.setOnAction(e -> saveCollisions());

        // Filter and sort handlers
        filterExam1Field.textProperty().addListener((obs, old, newValue) -> updateCollisionTable());
        filterExam2Field.textProperty().addListener((obs, old, newValue) -> updateCollisionTable());

        // Set default sort direction
        sortDirectionExam1Box.setValue(ASCENDING);
        sortDirectionExam2Box.setValue(ASCENDING);

        // Setup sort handlers
        sortExam1Box.setOnAction(e -> updateSort());
        sortDirectionExam1Box.setOnAction(e -> updateSort());
        sortExam2Box.setOnAction(e -> updateSort());
        sortDirectionExam2Box.setOnAction(e -> updateSort());

        // Double click handler for collision details and navigation
        collisionTreeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<CollisionEntry> selected = collisionTreeTable.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                CollisionEntry entry = selected.getValue();
                if (!entry.isTitle()) {
                    // This is a collision entry, find and select the corresponding assessment
                    Assessment targetAssessment = entry.getCollidingAssessment();
                    navigateToAssessment(targetAssessment);
                }
            }
        });
    }

    private void navigateToAssessment(Assessment targetAssessment) {
        if (targetAssessment == null || collisionTreeTable.getRoot() == null) return;

        // Search through the tree items to find the matching assessment
        String targetQualifiedName = targetAssessment.getQualifiedName();

        for (TreeItem<CollisionEntry> titleItem : collisionTreeTable.getRoot().getChildren()) {
            if (titleItem.getValue().isTitle() &&
                    titleItem.getValue().getAssessment().getQualifiedName().equals(targetQualifiedName)) {
                // Found the matching assessment, expand and select it
                titleItem.setExpanded(true);
                collisionTreeTable.getSelectionModel().select(titleItem);
                collisionTreeTable.scrollTo(collisionTreeTable.getRow(titleItem));
                break;
            }
        }
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

        updateCollisionTreeTable();
        updateSort();

        showAlert("Collisions detected successfully!", Alert.AlertType.INFORMATION);

        // Switch to results tab
        tabPane.getSelectionModel().select(RESULTS_PAGE);
    }

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

    private void updateCollisionTable() {
        if (assessments == null) return;

        updateCollisionTreeTable();
        setupColumnSort();
    }

    private void updateCollisionTreeTable() {
        if (assessments == null) return;

        // Store current expansion states before rebuilding tree
        if (collisionTreeTable.getRoot() != null) {
            saveExpansionStates(collisionTreeTable.getRoot());
        }

        // Store current column sorting information before rebuilding the tree
        List<TreeTableColumn<CollisionEntry, ?>> sortOrder = new ArrayList<>(collisionTreeTable.getSortOrder());

        // Create a root item to hold all entries
        TreeItem<CollisionEntry> root = new TreeItem<>(new CollisionEntry(null));

        // First, filter based on exam1 and exam2 filters
        String exam1Filter = filterExam1Field.getText().toLowerCase();
        String exam2Filter = filterExam2Field.getText().toLowerCase();
        String distanceFilter = filterDistanceField.getText();

        // Process and filter the collision data
        for (Assessment assessment : assessments) {
            // Check if this assessment matches exam1 filter (only for title rows)
            boolean matchesExam1 = exam1Filter.isEmpty() ||
                    assessment.getQualifiedName().toLowerCase().contains(exam1Filter);

            if (!matchesExam1) {
                continue; // Skip this assessment if it doesn't match exam1 filter
            }

            // Create a list of child items that match our filters
            List<TreeItem<CollisionEntry>> validChildren = new ArrayList<>();
            int validCollisionCount = 0;
            Duration minDistance = null;
            Duration totalDistance = Duration.ZERO;
            int validDistanceCount = 0;

            for (Map.Entry<Assessment, Integer> detail : assessment.getCollisionCountByAssessment().entrySet()) {
                Assessment collidingAssessment = detail.getKey();
                int collisionCount = detail.getValue();

                // Check if colliding assessment matches exam2 filter
                boolean matchesExam2 = exam2Filter.isEmpty() ||
                        collidingAssessment.getQualifiedName().toLowerCase().contains(exam2Filter);

                // Check if colliding assessment matches distance filter
                boolean matchesDistance = true;
                if (!distanceFilter.isEmpty()) {
                    try {
                        double maxHours = Double.parseDouble(distanceFilter);
                        if (assessment.getBegin() != null && assessment.getEnd() != null &&
                                collidingAssessment.getBegin() != null && collidingAssessment.getEnd() != null) {

                            Duration distance;
                            if (assessment.getBegin().isBefore(collidingAssessment.getBegin())) {
                                distance = Duration.between(assessment.getEnd(), collidingAssessment.getBegin());
                            } else {
                                distance = Duration.between(collidingAssessment.getEnd(), assessment.getBegin());
                            }

                            double distanceHours = distance.toMinutes() / 60.0;
                            matchesDistance = distanceHours <= maxHours && distanceHours >= 0;
                        }
                    } catch (NumberFormatException e) {
                        matchesDistance = true;
                    }
                }

                // Check if entry should be hidden due to missing times
                boolean hasValidTimes = !hideNullTimesCheckbox.isSelected() ||
                    (assessment.getBegin() != null && assessment.getEnd() != null &&
                     collidingAssessment.getBegin() != null && collidingAssessment.getEnd() != null);

                // If entry passes all filters, add it and update statistics
                if (matchesExam2 && matchesDistance && hasValidTimes) {
                    TreeItem<CollisionEntry> childItem = new TreeItem<>(
                            new CollisionEntry(assessment, collidingAssessment, collisionCount)
                    );
                    validChildren.add(childItem);
                    validCollisionCount += collisionCount;

                    // Update distance statistics if time info is available
                    if (assessment.getBegin() != null && assessment.getEnd() != null &&
                            collidingAssessment.getBegin() != null && collidingAssessment.getEnd() != null) {

                        Duration distance;
                        if (assessment.getBegin().isBefore(collidingAssessment.getBegin())) {
                            distance = Duration.between(assessment.getEnd(), collidingAssessment.getBegin());
                        } else {
                            distance = Duration.between(collidingAssessment.getEnd(), assessment.getBegin());
                        }

                        if (!distance.isNegative()) {
                            if (minDistance == null || distance.compareTo(minDistance) < 0) {
                                minDistance = distance;
                            }
                            totalDistance = totalDistance.plus(distance);
                            validDistanceCount++;
                        }
                    }
                }
            }

            // Skip the assessment if it has no valid collisions and "Show only with collisions" is enabled
            if (showOnlyWithCollisionsCheckbox.isSelected() && validCollisionCount == 0) {
                continue;
            }

            // Create a title item for this assessment with updated statistics
            TreeItem<CollisionEntry> titleItem = new TreeItem<>(
                new CollisionEntry(
                    assessment,
                    validCollisionCount,
                    minDistance,
                    validDistanceCount > 0 ? totalDistance.dividedBy(validDistanceCount) : null
                )
            );

            // Add the valid children
            titleItem.getChildren().addAll(validChildren);

            // Add the title item to the root if:
            // 1. No exam2 filter is active, or
            // 2. It has matching children, or
            // 3. We're showing only assessments
            if (exam2Filter.isEmpty() || !validChildren.isEmpty() || showOnlyAssessmentsCheckbox.isSelected()) {
                root.getChildren().add(titleItem);
            }
        }

        // Apply sorting
        sortTreeItems(root);

        // Update the tree table
        collisionTreeTable.setRoot(root);

        // Restore column-based sorting if it was active before
        if (!sortOrder.isEmpty()) {
            collisionTreeTable.getSortOrder().clear();
            collisionTreeTable.getSortOrder().addAll(sortOrder);
        }

        // Set expansion states based on saved states and checkbox settings
        for (TreeItem<CollisionEntry> titleItem : root.getChildren()) {
            String key = getItemKey(titleItem.getValue());
            Boolean wasExpanded = expansionStates.get(key);

            // If "Show Only Assessments" is selected, always collapse
            // Otherwise, restore previous expansion state if available, or expand by default
            if (showOnlyAssessmentsCheckbox.isSelected()) {
                titleItem.setExpanded(false);
            } else if (wasExpanded != null) {
                titleItem.setExpanded(wasExpanded);
            } else {
                titleItem.setExpanded(true); // Default to expanded if no saved state
            }
        }
    }

    private void recalculateAssessmentValues(TreeItem<CollisionEntry> root) {
        if (root == null) return;

        // Process each assessment (title item)
        for (TreeItem<CollisionEntry> titleItem : root.getChildren()) {
            if (titleItem.getValue().isTitle()) {
                // Get the assessment
                Assessment assessment = titleItem.getValue().getAssessment();
                if (assessment == null) continue;

                // Variables to track values for this assessment
                int visibleCollisionCount = 0;
                Duration minDistance = null;
                Duration totalDistance = Duration.ZERO;
                int validDistanceCount = 0;

                // Loop through visible child items to calculate new values
                for (TreeItem<CollisionEntry> childItem : titleItem.getChildren()) {
                    CollisionEntry childEntry = childItem.getValue();
                    if (childEntry == null) continue;

                    // Sum the CollisionCount values of all visible child Collision rows
                    visibleCollisionCount += childEntry.collisionCountValue;

                    // Calculate distance if time information is available
                    if (childEntry.distance != null && !childEntry.distance.isEmpty()) {
                        try {
                            // Parse the distance string (format: "Xh Ym")
                            String distStr = childEntry.distance;
                            int hours = Integer.parseInt(distStr.substring(0, distStr.indexOf('h')).trim());
                            int minutes = Integer.parseInt(distStr.substring(distStr.indexOf('h') + 1, distStr.indexOf('m')).trim());

                            // Create duration from hours and minutes
                            Duration distance = Duration.ofHours(hours).plusMinutes(minutes);

                            // Update min distance
                            if (minDistance == null || distance.compareTo(minDistance) < 0) {
                                minDistance = distance;
                            }

                            // Add to total for average calculation
                            totalDistance = totalDistance.plus(distance);
                            validDistanceCount++;
                        } catch (Exception e) {
                            // Skip this entry if distance format is invalid
                        }
                    }
                }

                // Create a new CollisionEntry with updated values for the title item
                CollisionEntry originalEntry = titleItem.getValue();
                CollisionEntry updatedEntry = new CollisionEntry(
                        assessment,
                        visibleCollisionCount,
                        minDistance,
                        validDistanceCount > 0 ? totalDistance.dividedBy(validDistanceCount) : null
                );

                // Replace the original entry with the updated one
                titleItem.setValue(updatedEntry);
            }
        }
    }

    private void sortTreeItems(TreeItem<CollisionEntry> root) {
        String exam1SortColumn = sortExam1Box.getValue();
        String exam1SortDirection = sortDirectionExam1Box.getValue();
        String exam2SortColumn = sortExam2Box.getValue();
        String exam2SortDirection = sortDirectionExam2Box.getValue();

        // Use ascending as default direction if none selected
        boolean exam1Ascending = exam1SortDirection == null || ASCENDING.equals(exam1SortDirection);
        boolean exam2Ascending = exam2SortDirection == null || ASCENDING.equals(exam2SortDirection);

        // Only sort if a column is selected
        if (exam1SortColumn != null) {
            // Sort the title rows (Exam 1) using primary sort
            root.getChildren().sort((item1, item2) -> {
                int result = compareCollisionEntries(item1.getValue(), item2.getValue(), exam1SortColumn);
                return exam1Ascending ? result : -result; // Reverse order for descending
            });
        }

        // Sort children of each title row (Exam 2) if a column is selected
        if (exam2SortColumn != null) {
            for (TreeItem<CollisionEntry> titleItem : root.getChildren()) {
                titleItem.getChildren().sort((item1, item2) -> {
                    int result = compareCollisionEntries(item1.getValue(), item2.getValue(), exam2SortColumn);
                    return exam2Ascending ? result : -result; // Reverse order for descending
                });
            }
        }
    }

    private int compareCollisionEntries(CollisionEntry entry1, CollisionEntry entry2, String property) {
        if (entry1 == null || entry2 == null) return 0;
        if (entry1.isTitle() && !entry2.isTitle()) return -1;
        if (!entry1.isTitle() && entry2.isTitle()) return 1;

        Assessment a1 = entry1.getAssessment();
        Assessment a2 = entry2.getAssessment();

        if (a1 == null || a2 == null) return 0;

        return switch (property) {
            case "Exam 1 Name", "Exam Name" -> {
                if (entry1.isTitle()) {
                    // For title rows (Exam 1), sort by primary exam
                    yield compareGermanStrings(a1.getQualifiedName(), a2.getQualifiedName());
                } else {
                    // For child rows (Exam 2), sort by the colliding assessment
                    Assessment c1 = entry1.getCollidingAssessment();
                    Assessment c2 = entry2.getCollidingAssessment();
                    if (c1 != null && c2 != null) {
                        yield compareGermanStrings(c1.getQualifiedName(), c2.getQualifiedName());
                    }
                    yield 0;
                }
            }
            case "Exam 2 Name" -> {
                // This is only for child rows (Exam 2)
                if (!entry1.isTitle() && !entry2.isTitle()) {
                    Assessment c1 = entry1.getCollidingAssessment();
                    Assessment c2 = entry2.getCollidingAssessment();
                    if (c1 != null && c2 != null) {
                        yield compareGermanStrings(c1.getQualifiedName(), c2.getQualifiedName());
                    }
                }
                yield 0;
            }
            case "Collision Count" -> Integer.compare(
                    entry1.getCollisionCountValue(),
                    entry2.getCollisionCountValue()
            );
            case "Start Time", "Begin" -> {
                Assessment compareA1 = entry1.isTitle() ? a1 : entry1.getCollidingAssessment();
                Assessment compareA2 = entry2.isTitle() ? a2 : entry2.getCollidingAssessment();

                if (compareA1.getBegin() == null && compareA2.getBegin() == null) yield 0;
                if (compareA1.getBegin() == null) yield -1;
                if (compareA2.getBegin() == null) yield 1;

                yield compareA1.getBegin().compareTo(compareA2.getBegin());
            }
            case "End Time", "End" -> {
                Assessment compareA1 = entry1.isTitle() ? a1 : entry1.getCollidingAssessment();
                Assessment compareA2 = entry2.isTitle() ? a2 : entry2.getCollidingAssessment();

                if (compareA1.getEnd() == null && compareA2.getEnd() == null) yield 0;
                if (compareA1.getEnd() == null) yield -1;
                if (compareA2.getEnd() == null) yield 1;

                yield compareA1.getEnd().compareTo(compareA2.getEnd());
            }
            case "Distance" -> {
                if (!entry1.isTitle() && !entry2.isTitle()) {
                    // Extract numeric values from distance strings (format: "Xh Ym")
                    String dist1 = entry1.distance;
                    String dist2 = entry2.distance;

                    if (dist1.isEmpty() && dist2.isEmpty()) yield 0;
                    if (dist1.isEmpty()) yield -1;
                    if (dist2.isEmpty()) yield 1;

                    // Parse hours and minutes to comparable value
                    try {
                        // Extract hours
                        int h1 = Integer.parseInt(dist1.substring(0, dist1.indexOf('h')).trim());
                        int h2 = Integer.parseInt(dist2.substring(0, dist2.indexOf('h')).trim());

                        if (h1 != h2) yield Integer.compare(h1, h2);

                        // Extract minutes
                        int m1 = Integer.parseInt(dist1.substring(dist1.indexOf('h') + 1, dist1.indexOf('m')).trim());
                        int m2 = Integer.parseInt(dist2.substring(dist2.indexOf('h') + 1, dist2.indexOf('m')).trim());

                        yield Integer.compare(m1, m2);
                    } catch (Exception e) {
                        yield dist1.compareTo(dist2);
                    }
                }
                yield 0;
            }
            case "Avg. Distance" -> {
                if (entry1.isTitle() && entry2.isTitle()) {
                    String avg1 = entry1.getAverageDistance();
                    String avg2 = entry2.getAverageDistance();

                    if (avg1.isEmpty() && avg2.isEmpty()) yield 0;
                    if (avg1.isEmpty()) yield -1;
                    if (avg2.isEmpty()) yield 1;

                    try {
                        // Convert to minutes for comparison
                        int h1 = Integer.parseInt(avg1.substring(0, avg1.indexOf('h')).trim());
                        int m1 = Integer.parseInt(avg1.substring(avg1.indexOf('h') + 1, avg1.indexOf('m')).trim());
                        int totalMinutes1 = h1 * 60 + m1;

                        int h2 = Integer.parseInt(avg2.substring(0, avg2.indexOf('h')).trim());
                        int m2 = Integer.parseInt(avg2.substring(avg2.indexOf('h') + 1, avg2.indexOf('m')).trim());
                        int totalMinutes2 = h2 * 60 + m2;

                        yield Integer.compare(totalMinutes1, totalMinutes2);
                    } catch (Exception e) {
                        yield avg1.compareTo(avg2);
                    }
                }
                yield 0;
            }
            case "Min. Distance" -> {
                if (entry1.isTitle() && entry2.isTitle()) {
                    String min1 = entry1.getMinDistance();
                    String min2 = entry2.getMinDistance();

                    if (min1.isEmpty() && min2.isEmpty()) yield 0;
                    if (min1.isEmpty()) yield -1;
                    if (min2.isEmpty()) yield 1;

                    try {
                        // Convert to minutes for comparison
                        int h1 = Integer.parseInt(min1.substring(0, min1.indexOf('h')).trim());
                        int m1 = Integer.parseInt(min1.substring(min1.indexOf('h') + 1, min1.indexOf('m')).trim());
                        int totalMinutes1 = h1 * 60 + m1;

                        int h2 = Integer.parseInt(min2.substring(0, min2.indexOf('h')).trim());
                        int m2 = Integer.parseInt(min2.substring(min2.indexOf('h') + 1, min2.indexOf('m')).trim());
                        int totalMinutes2 = h2 * 60 + m2;

                        yield Integer.compare(totalMinutes1, totalMinutes2);
                    } catch (Exception e) {
                        yield min1.compareTo(min2);
                    }
                }
                yield 0;
            }
            default -> 0;
        };
    }

    private void updateSort() {
        // If already updating, prevent infinite loop
        if (isUpdating) return;

        isUpdating = true;

        try {
            String exam1SortColumn = sortExam1Box.getValue();
            String exam1SortDirection = sortDirectionExam1Box.getValue();
            String exam2SortColumn = sortExam2Box.getValue();
            String exam2SortDirection = sortDirectionExam2Box.getValue();

            // Clear previous sorting
            collisionTreeTable.getSortOrder().clear();

            // Apply column sorting based on Exam1 dropdown selections
            if (exam1SortColumn != null && !exam1SortColumn.isEmpty()) {
                // Find the corresponding column
                TreeTableColumn<CollisionEntry, ?> selectedColumn = findColumnByName(mapDropdownValueToColumnName(exam1SortColumn));

                if (selectedColumn != null) {
                    // Set sort type based on direction
                    selectedColumn.setSortType(ASCENDING.equals(exam1SortDirection) ?
                            TreeTableColumn.SortType.ASCENDING :
                            TreeTableColumn.SortType.DESCENDING);

                    // Apply the sort to the table
                    collisionTreeTable.getSortOrder().add(selectedColumn);
                }
            }

            // Apply column sorting based on Exam2 dropdown selections
            if (exam2SortColumn != null && !exam2SortColumn.isEmpty()) {
                // Find the corresponding column
                TreeTableColumn<CollisionEntry, ?> selectedColumn = findColumnByName(mapDropdownValueToColumnName(exam2SortColumn));

                if (selectedColumn != null) {
                    // Set sort type based on direction
                    selectedColumn.setSortType(ASCENDING.equals(exam2SortDirection) ?
                            TreeTableColumn.SortType.ASCENDING :
                            TreeTableColumn.SortType.DESCENDING);

                    // Apply the sort to the table
                    collisionTreeTable.getSortOrder().add(selectedColumn);
                }
            }

            // Update the tree table with the current sort and filtering
            updateCollisionTreeTable();
            savePreferences();
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Maps dropdown values to column names in the table
     */
    private String mapDropdownValueToColumnName(String dropdownValue) {
        return switch (dropdownValue) {
            case "Exam 1 Name" -> "Exam 1";
            case "Exam 2 Name" -> "Exam 2";
            case "Collision Count" -> "Collision Count";
            case "Begin" -> "Begin";
            case "End" -> "End";
            case "Avg. Distance" -> "Avg. Distance";
            case "Min. Distance" -> "Min. Distance";
            case "Distance" -> "Distance";
            default -> dropdownValue;
        };
    }

    /**
     * Maps column names in the table to dropdown values
     */


    private TreeTableColumn<CollisionEntry, ?> findColumnByName(String columnName) {
        for (TreeTableColumn<CollisionEntry, ?> column : collisionTreeTable.getColumns()) {
            if (column.getText().equals(columnName)) {
                return column;
            }
        }
        return null;
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

    private void loadPreferences() {
        // Load existing name filters and distance filter
        filterExam1Field.setText(prefs.get("filterExam1", ""));
        filterExam2Field.setText(prefs.get("filterExam2", ""));
        filterDistanceField.setText(prefs.get("filterDistance", ""));

        // Load sort column selections
        sortExam1Box.setValue(prefs.get("sortExam1", "Exam 1 Name"));
        sortExam2Box.setValue(prefs.get("sortExam2", "Exam 2 Name"));

        // Load sort directions
        sortDirectionExam1Box.setValue(prefs.get("sortDirectionExam1", ASCENDING));
        sortDirectionExam2Box.setValue(prefs.get("sortDirectionExam2", ASCENDING));

        // Load filter checkbox states
        hideNullTimesCheckbox.setSelected(prefs.getBoolean("hideNullTimes", false));
        showOnlyWithCollisionsCheckbox.setSelected(prefs.getBoolean("showOnlyWithCollisions", false));
        showOnlyAssessmentsCheckbox.setSelected(prefs.getBoolean("showOnlyAssessments", false));
    }

    private void savePreferences() {
        // Save name filters and distance filter
        prefs.put("filterExam1", filterExam1Field.getText());
        prefs.put("filterExam2", filterExam2Field.getText());
        prefs.put("filterDistance", filterDistanceField.getText());

        // Save sort column selections
        prefs.put("sortExam1", sortExam1Box.getValue());
        prefs.put("sortExam2", sortExam2Box.getValue());

        // Save sort directions
        prefs.put("sortDirectionExam1", sortDirectionExam1Box.getValue());
        prefs.put("sortDirectionExam2", sortDirectionExam2Box.getValue());

        // Save filter checkbox states
        prefs.putBoolean("hideNullTimes", hideNullTimesCheckbox.isSelected());
        prefs.putBoolean("showOnlyWithCollisions", showOnlyWithCollisionsCheckbox.isSelected());
        prefs.putBoolean("showOnlyAssessments", showOnlyAssessmentsCheckbox.isSelected());
    }

    @Override
    public void stop() {
        savePreferences();
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
        selectStatistic.getItems().addAll("Fakultäten", "Studiengänge", "Zeitverteilung", "Kollisionsschwere");
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
        VBox chartBox = new VBox();
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setMinHeight(400);

        // When a course is selected, create and display the pie chart
        courseComboBox.setOnAction(e -> {
            String selectedCourse = courseComboBox.getValue();
            if (selectedCourse != null && assessments != null && assessments.length > 0) {
                // Clear previous chart
                chartBox.getChildren().clear();

                // Create new chart with the selected course
                PieChart pieChart = CollisionPieChartView.createCollisionPieChartByCourseOfStudy(selectedCourse, assessments);
                chartBox.getChildren().add(pieChart);
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
                        courseChartContainer.setVisible(false);
                        contentArea.getChildren().add(createFakContent());
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
                case "Zeitverteilung" -> {
                    if(assessments != null){
                        courseChartContainer.setVisible(false);
                        contentArea.getChildren().add(createTimeContent());
                    }else{
                        showAlert("Bitte laden Sie zuerst die Daten und erkennen Sie Kollisionen.", Alert.AlertType.WARNING);
                    }
                }
                case "Kollisionsschwere" -> {
                    if(assessments!=null){
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
    private Node createFakContent(){
        long facultyACount = 0;
        long facultyBCount = 0;
        long facultyCCount = 0;

        for(Assessment a : assessments) {
            if (!(a.getAssessmentEntries() == null)) {
                for (Assessment.AssessmentEntry assessmentEntry : a.getAssessmentEntries()) {
                    Map<Assessment, Integer> collisions = a.getCollisionCountByAssessment();
                    for (Map.Entry<Assessment, Integer> entry : collisions.entrySet()) {
                        Assessment collidingAssessment = entry.getKey();
                        int count = entry.getValue();
                        if (assessmentEntry.faculty().equals("A")) {
                            facultyACount += count;
                        } else if (assessmentEntry.faculty().equals("B")) {
                            facultyBCount += count;
                        } else {
                            facultyCCount += count;
                        }

                    }
                }
            }
        }
        CategoryAxis facultyAxis = new CategoryAxis();
        facultyAxis.setLabel("Fakultät");

        NumberAxis collisionByFacultyAxis = new NumberAxis();
        collisionByFacultyAxis.setLabel("Kollisionen");

        BarChart<String, Number> facultyBarChart = new BarChart<>(facultyAxis, collisionByFacultyAxis);
        facultyBarChart.setTitle("Fakultät Säulendiagramm");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Fakultät Daten");
        series.getData().add(new XYChart.Data<>("Fakultät A: "+facultyACount, facultyACount));
        series.getData().add(new XYChart.Data<>("Fakultät B: "+facultyBCount, facultyBCount));
        series.getData().add(new XYChart.Data<>("Fakultät C: "+facultyCCount, facultyCCount));

        facultyBarChart.getData().add(series);
        return facultyBarChart;
    }
    private Node createTimeContent() {
        return CollisionPieChartView.createCollisionPieChartByTime(assessments);
    }
    private Node createColSchwereContent(){
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label inProgressLabel = new Label("Kollisionsschwerestatistik - In Progress");
        inProgressLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        inProgressLabel.setTextFill(Color.web(SECONDARY_COLOR));

        Label descriptionLabel = new Label("This feature is currently under development.");
        descriptionLabel.setTextFill(Color.web(SECONDARY_COLOR));

        content.getChildren().addAll(inProgressLabel, descriptionLabel);
        return content;
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

    /**
     * Compares two strings with proper handling of German umlauts
     * Replaces "Ä" → "AE", "Ö" → "OE", "Ü" → "UE", and "ß" → "SS" before comparing
     */
    private int compareGermanStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;

        // Normalize German umlauts for comparison
        String normalized1 = normalizeGermanString(s1);
        String normalized2 = normalizeGermanString(s2);

        return normalized1.compareTo(normalized2);
    }

    /**
     * Normalizes German umlauts in a string for proper sorting
     */
    private String normalizeGermanString(String input) {
        if (input == null) return "";

        return input.replace("Ä", "AE")
                   .replace("ä", "ae")
                   .replace("Ö", "OE")
                   .replace("ö", "oe")
                   .replace("Ü", "UE")
                   .replace("ü", "ue")
                   .replace("ß", "SS");
    }

    private void setupColumnSort() {
        // Add listener for TreeTableView's sort order changes
        collisionTreeTable.getSortOrder().addListener((ListChangeListener<TreeTableColumn<CollisionEntry, ?>>) change -> {
            if (!isUpdating) {
                isUpdating = true;
                try {
                    // Get the first sorted column (if any)
                    if (!collisionTreeTable.getSortOrder().isEmpty()) {
                        TreeTableColumn<CollisionEntry, ?> firstSortColumn = collisionTreeTable.getSortOrder().get(0);
                        String columnText = firstSortColumn.getText();
                        TreeTableColumn.SortType sortType = firstSortColumn.getSortType();
                        String direction = sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING;

                        // Update corresponding dropdowns based on the column
                        if ("Exam 1".equals(columnText)) {
                            sortExam1Box.setValue("Exam 1 Name");
                            sortDirectionExam1Box.setValue(direction);
                        } else if ("Exam 2".equals(columnText)) {
                            sortExam2Box.setValue("Exam 2 Name");
                            sortDirectionExam2Box.setValue(direction);
                        } else if ("Distance".equals(columnText)) {
                            sortExam2Box.setValue("Distance");
                            sortDirectionExam2Box.setValue(direction);
                        } else if ("Min. Distance".equals(columnText)) {
                            sortExam1Box.setValue("Min. Distance");
                            sortDirectionExam1Box.setValue(direction);
                        } else if ("Avg. Distance".equals(columnText)) {
                            sortExam1Box.setValue("Avg. Distance");
                            sortDirectionExam1Box.setValue(direction);
                        }
                    }
                } finally {
                    isUpdating = false;
                }
            }
        });

        // Maintain sortType listeners for additional synchronization
        for (TreeTableColumn<CollisionEntry, ?> column : collisionTreeTable.getColumns()) {
            column.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
                if (!isUpdating && newValue != null) {
                    isUpdating = true;
                    try {
                        String columnText = column.getText();
                        String direction = newValue == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING;

                        // Use direct equality comparison for column text
                        if ("Exam 1".equals(columnText) && "Exam 1 Name".equals(sortExam1Box.getValue())) {
                            sortDirectionExam1Box.setValue(direction);
                        } else if ("Exam 2".equals(columnText) && "Exam 2 Name".equals(sortExam2Box.getValue())) {
                            sortDirectionExam2Box.setValue(direction);
                        } else if ("Distance".equals(columnText) && "Distance".equals(sortExam2Box.getValue())) {
                            sortDirectionExam2Box.setValue(direction);
                        } else if ("Min. Distance".equals(columnText) && "Min. Distance".equals(sortExam1Box.getValue())) {
                            sortDirectionExam1Box.setValue(direction);
                        } else if ("Avg. Distance".equals(columnText) && "Avg. Distance".equals(sortExam1Box.getValue())) {
                            sortDirectionExam1Box.setValue(direction);
                        }
                    } finally {
                        isUpdating = false;
                    }
                }
            });
        }
    }

    private Tab createOptimizedTab() {
        optimizedTab = new Tab("Optimized Exams");
        VBox optimizedContent = new VBox(20);
        optimizedContent.setPadding(new Insets(20));

        // Filter section - reuse the same structure
        VBox filterSection = createFilterSection();
        filterSection.setEffect(new DropShadow());

        // Table section with modified columns
        VBox tableSection = new VBox(15);
        tableSection.setPadding(new Insets(20));
        tableSection.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        tableSection.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        // Section title and description
        Label sectionTitle = new Label("Optimized Exam Schedule");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(SECONDARY_COLOR));

        Label sectionDescription = new Label("Optimized exam schedule to minimize collisions.");
        sectionDescription.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        // Create controls container for checkboxes and column visibility
        HBox controlsContainer = new HBox(20);
        controlsContainer.setAlignment(Pos.CENTER_LEFT);

        // Add checkboxes with the same functionality as Collision Results
        CheckBox optimHideNullTimesCheckbox = new CheckBox("Hide entries with no times");
        optimHideNullTimesCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        CheckBox optimShowOnlyAssessmentsCheckbox = new CheckBox("Show only assessments");
        optimShowOnlyAssessmentsCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        CheckBox optimShowOnlyWithCollisionsCheckbox = new CheckBox("Show only with collisions");
        optimShowOnlyWithCollisionsCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        // Create columns menu button
        MenuButton columnsMenuButton = new MenuButton("Columns");
        columnsMenuButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"
        );

        controlsContainer.getChildren().addAll(
            optimHideNullTimesCheckbox,
            optimShowOnlyAssessmentsCheckbox,
            optimShowOnlyWithCollisionsCheckbox,
            columnsMenuButton
        );

        // Create the TreeTableView
        optimizedTreeTable = new TreeTableView<>();
        optimizedTreeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        optimizedTreeTable.setShowRoot(false);
        VBox.setVgrow(optimizedTreeTable, Priority.ALWAYS);

        // Create columns
        TreeTableColumn<CollisionEntry, String> exam1Col = new TreeTableColumn<>("Exam 1");
        exam1Col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().exam1QualifiedName));

        TreeTableColumn<CollisionEntry, String> exam2Col = new TreeTableColumn<>("Exam 2");
        exam2Col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().exam2QualifiedName));

        TreeTableColumn<CollisionEntry, String> collisionCountCol = new TreeTableColumn<>("Collision Count");
        collisionCountCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().collisionCount));

        // Old timing columns
        TreeTableColumn<CollisionEntry, String> oldBeginCol = new TreeTableColumn<>("Old Begin");
        oldBeginCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().beginTime));

        TreeTableColumn<CollisionEntry, String> oldEndCol = new TreeTableColumn<>("Old End");
        oldEndCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().endTime));

        TreeTableColumn<CollisionEntry, String> oldDistanceCol = new TreeTableColumn<>("Old Distance");
        oldDistanceCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().distance));

        TreeTableColumn<CollisionEntry, String> oldAvgDistanceCol = new TreeTableColumn<>("Old Average Distance");
        oldAvgDistanceCol.setCellValueFactory(param -> {
            CollisionEntry entry = param.getValue().getValue();
            return entry.isTitle() ? new SimpleStringProperty(entry.getAverageDistance()) : new SimpleStringProperty("");
        });

        TreeTableColumn<CollisionEntry, String> oldMinDistanceCol = new TreeTableColumn<>("Old Min Distance");
        oldMinDistanceCol.setCellValueFactory(param -> {
            CollisionEntry entry = param.getValue().getValue();
            return entry.isTitle() ? new SimpleStringProperty(entry.getMinDistance()) : new SimpleStringProperty("");
        });

        // Optimized timing columns (placeholder data for now)
        TreeTableColumn<CollisionEntry, String> optimBeginCol = new TreeTableColumn<>("Optim Begin");
        optimBeginCol.setCellValueFactory(param -> new SimpleStringProperty(""));

        TreeTableColumn<CollisionEntry, String> optimEndCol = new TreeTableColumn<>("Optim End");
        optimEndCol.setCellValueFactory(param -> new SimpleStringProperty(""));

        TreeTableColumn<CollisionEntry, String> optimDistanceCol = new TreeTableColumn<>("Optim Distance");
        optimDistanceCol.setCellValueFactory(param -> new SimpleStringProperty(""));

        TreeTableColumn<CollisionEntry, String> optimAvgDistanceCol = new TreeTableColumn<>("Optim Average Distance");
        optimAvgDistanceCol.setCellValueFactory(param -> new SimpleStringProperty(""));

        TreeTableColumn<CollisionEntry, String> optimMinDistanceCol = new TreeTableColumn<>("Optim Min Distance");
        optimMinDistanceCol.setCellValueFactory(param -> new SimpleStringProperty(""));

        // Style all columns
        String columnStyle = "-fx-alignment: CENTER-LEFT; -fx-padding: 8px;";
        exam1Col.setStyle(columnStyle);
        exam2Col.setStyle(columnStyle);
        collisionCountCol.setStyle(columnStyle);
        oldBeginCol.setStyle(columnStyle);
        oldEndCol.setStyle(columnStyle);
        oldDistanceCol.setStyle(columnStyle);
        oldAvgDistanceCol.setStyle(columnStyle);
        oldMinDistanceCol.setStyle(columnStyle);
        optimBeginCol.setStyle(columnStyle);
        optimEndCol.setStyle(columnStyle);
        optimDistanceCol.setStyle(columnStyle);
        optimAvgDistanceCol.setStyle(columnStyle);
        optimMinDistanceCol.setStyle(columnStyle);

        // Create column visibility menu items
        CheckMenuItem exam1Item = new CheckMenuItem("Exam 1");
        exam1Item.setSelected(true);
        exam1Item.setDisable(true);  // First column cannot be hidden

        CheckMenuItem exam2Item = new CheckMenuItem("Exam 2");
        exam2Item.setSelected(true);
        exam2Item.setDisable(true);  // Second column cannot be hidden

        CheckMenuItem collisionCountItem = new CheckMenuItem("Collision Count");
        collisionCountItem.setSelected(true);

        // Old columns menu items (initially hidden)
        CheckMenuItem oldBeginItem = new CheckMenuItem("Old Begin");
        oldBeginItem.setSelected(false);

        CheckMenuItem oldEndItem = new CheckMenuItem("Old End");
        oldEndItem.setSelected(false);

        CheckMenuItem oldDistanceItem = new CheckMenuItem("Old Distance");
        oldDistanceItem.setSelected(false);

        CheckMenuItem oldAvgDistanceItem = new CheckMenuItem("Old Average Distance");
        oldAvgDistanceItem.setSelected(false);

        CheckMenuItem oldMinDistanceItem = new CheckMenuItem("Old Min Distance");
        oldMinDistanceItem.setSelected(false);

        // Optimized columns menu items (initially visible)
        CheckMenuItem optimBeginItem = new CheckMenuItem("Optim Begin");
        optimBeginItem.setSelected(true);

        CheckMenuItem optimEndItem = new CheckMenuItem("Optim End");
        optimEndItem.setSelected(true);

        CheckMenuItem optimDistanceItem = new CheckMenuItem("Optim Distance");
        optimDistanceItem.setSelected(true);

        CheckMenuItem optimAvgDistanceItem = new CheckMenuItem("Optim Average Distance");
        optimAvgDistanceItem.setSelected(true);

        CheckMenuItem optimMinDistanceItem = new CheckMenuItem("Optim Min Distance");
        optimMinDistanceItem.setSelected(true);

        // Add visibility listeners to all column menu items
        collisionCountItem.selectedProperty().addListener((obs, old, newValue) -> {
            collisionCountCol.setVisible(newValue);
            adjustColumnWidths();
        });

        // Old columns visibility listeners
        oldBeginItem.selectedProperty().addListener((obs, old, newValue) -> {
            oldBeginCol.setVisible(newValue);
            adjustColumnWidths();
        });

        oldEndItem.selectedProperty().addListener((obs, old, newValue) -> {
            oldEndCol.setVisible(newValue);
            adjustColumnWidths();
        });

        oldDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            oldDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        oldAvgDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            oldAvgDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        oldMinDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            oldMinDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        // Optimized columns visibility listeners
        optimBeginItem.selectedProperty().addListener((obs, old, newValue) -> {
            optimBeginCol.setVisible(newValue);
            adjustColumnWidths();
        });

        optimEndItem.selectedProperty().addListener((obs, old, newValue) -> {
            optimEndCol.setVisible(newValue);
            adjustColumnWidths();
        });

        optimDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            optimDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        optimAvgDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            optimAvgDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        optimMinDistanceItem.selectedProperty().addListener((obs, old, newValue) -> {
            optimMinDistanceCol.setVisible(newValue);
            adjustColumnWidths();
        });

        // Add menu items to the columns menu with separators for better organization
        columnsMenuButton.getItems().addAll(
                exam1Item, exam2Item, collisionCountItem,
                new SeparatorMenuItem(),
                new CustomMenuItem(new Label("Original Times:")) {{ setHideOnClick(false); }},
                oldBeginItem, oldEndItem, oldDistanceItem, oldAvgDistanceItem, oldMinDistanceItem,
                new SeparatorMenuItem(),
                new CustomMenuItem(new Label("Optimized Times:")) {{ setHideOnClick(false); }},
                optimBeginItem, optimEndItem, optimDistanceItem, optimAvgDistanceItem, optimMinDistanceItem
        );

        // Add all columns to the table
        optimizedTreeTable.getColumns().addAll(
            exam1Col, exam2Col, collisionCountCol,
            oldBeginCol, optimBeginCol,
            oldEndCol, optimEndCol,
            oldDistanceCol, optimDistanceCol,
            oldAvgDistanceCol, optimAvgDistanceCol,
            oldMinDistanceCol, optimMinDistanceCol
        );

        // Set initial visibility for old columns (hidden)
        oldBeginCol.setVisible(false);
        oldEndCol.setVisible(false);
        oldDistanceCol.setVisible(false);
        oldAvgDistanceCol.setVisible(false);
        oldMinDistanceCol.setVisible(false);

        // Add separator before table
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // Add components to the table section
        tableSection.getChildren().addAll(
            sectionTitle,
            sectionDescription,
            controlsContainer,
            separator,
            optimizedTreeTable
        );

        // Add sections to the content
        optimizedContent.getChildren().addAll(filterSection, tableSection);

        // Add scroll pane
        ScrollPane optimizedScrollPane = new ScrollPane(optimizedContent);
        optimizedScrollPane.setFitToWidth(true);
        optimizedScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        optimizedTab.setContent(optimizedScrollPane);
        return optimizedTab;
    }

    public static void run(){
        launch();
    }
}

