import data.Assessment;

import data.AssessmentEditable;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import java.util.*;
import java.util.prefs.Preferences;

import javafx.scene.effect.DropShadow;

public class ExamGUI extends Application {
    // Style constants
    private static final String PRIMARY_COLOR = "#3498db";
    private static final String SECONDARY_COLOR = "#2c3e50";
    private static final String SUCCESS_COLOR = "#2ecc71";
    private static final String LIGHT_COLOR = "#ecf0f1";
    private static final String DARK_COLOR = "#34495e";

    // Application constants
    private static final String APP_NAME = "Exam Collision Detector";
    private static final String APP_VERSION = "1.0.0";
    private static final String[] APP_AUTHORS = {
            "Author 1",
            "Author 2",
            "Author 3",
            "Author 4"
    };
    private static final String COPYRIGHT_YEAR = "2025";

    // Page constants
    private static final int INPUT_PAGE = 0;
    private static final int RESULTS_PAGE = 1;
    private static final int STATISTICS_PAGE = 2;
    private static final int ROOM_PLANS_PAGE = 3;

    private TextField examPathField;
    private TextField registrationPathField;
    private TextField collisionPathField;
    private TreeTableView<CollisionEntry> collisionTreeTable;
    private ComboBox<String> sortExam1Box;
    private ComboBox<String> sortExam2Box;
    private TextField filterExam1Field;
    private TextField filterExam2Field;
    private TextField filterDistanceField;  // Add this field
    private CheckBox hideNullTimesCheckbox;  // Add this field
    private CheckBox showOnlyAssessmentsCheckbox; // Add this field
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
        tabPane.getTabs().addAll(inputTab, resultsTab, createStatisticsTab(), createRoomPlansTab());

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
                "Exam Name", "Date", "Time", "Room", "Collision Count", "Avg. Distance", "Max. Distance"
        ));

        sortExam2Box = createStyledComboBox();
        sortExam2Box.setPromptText("Select Exam 2 sort...");
        sortExam2Box.setItems(FXCollections.observableArrayList(
                "Exam Name", "Date", "Time", "Room", "Collision Count", "Avg. Distance", "Max. Distance"
        ));

        // Create and style field labels
        Label filterExam1Label = new Label("Filter Exam 1");
        filterExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label filterExam2Label = new Label("Filter Exam 2");
        filterExam2Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label filterDistanceLabel = new Label("Max Distance (hours)");
        filterDistanceLabel.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam1Label = new Label("Sort Exam 1");
        sortExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam2Label = new Label("Sort Exam 2");
        sortExam2Label.setTextFill(Color.web(SECONDARY_COLOR));

        // Create distance filter field
        filterDistanceField = createStyledTextField("");
        filterDistanceField.setPromptText("Enter max distance (hours)...");
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
        controlGrid.add(sortExam2Label, 2, 1);
        controlGrid.add(sortExam2Box, 3, 1);

        // Place Max Distance filter side by side on the left
        HBox distanceBox = new HBox(10); // 10px spacing between label and field
        distanceBox.setAlignment(Pos.CENTER_LEFT);
        distanceBox.setPadding(new Insets(0, 0, 0, 0)); // Reset padding to align with other rows
        filterDistanceLabel.setPadding(new Insets(0, 10, 0, 0)); // Add some right padding to the label
        distanceBox.getChildren().addAll(filterDistanceLabel, filterDistanceField);

        // Add the HBox to the grid in the third row, spanning all columns
        controlGrid.add(distanceBox, 0, 2, 4, 1);

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

        controlsContainer.getChildren().addAll(hideNullTimesCheckbox, showOnlyAssessmentsCheckbox, columnsMenuButton);

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
            // Only update if "Show Only Assessments" is not checked, or we're unchecking "Hide Entries with No Times"
            // This prevents disrupting the collapsed state when both filters are active
            /*if (!showOnlyAssessmentsCheckbox.isSelected() || !newValue) {
                updateCollisionTreeTable();
            } else {
                // Just apply the filtering without rebuilding the entire tree
                TreeItem<CollisionEntry> root = collisionTreeTable.getRoot();
                if (root != null) {
                    filterTreeItems(root, true);
                }
            }*/



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
            }
            else {  // Checkbox wurde deaktiviert - wiederherstellen
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
        collisionCountCol.setSortable(true);
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
        beginCol.setSortable(true);

        TreeTableColumn<CollisionEntry, String> endCol = new TreeTableColumn<>("End");
        endCol.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getValue().endTime)
        );
        endCol.setSortable(true);

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

        TreeTableColumn<CollisionEntry, String> maxDistanceCol = new TreeTableColumn<>("Max. Distance");
        maxDistanceCol.setCellValueFactory(param -> {
            CollisionEntry entry = param.getValue().getValue();
            if (entry.isTitle()) {
                return new SimpleStringProperty(entry.getMaxDistance());
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

        CheckMenuItem maxDistanceItem = new CheckMenuItem("Max. Distance");
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

        collisionTreeTable.getColumns().addAll(exam1Col, exam2Col, collisionCountCol,
                beginCol, endCol, distanceCol, avgDistanceCol, maxDistanceCol);

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

    private boolean hasTimesData(CollisionEntry entry) {
        if (entry == null) return false;

        boolean hasBeginTime = entry.beginTime != null && !entry.beginTime.trim().isEmpty();
        boolean hasEndTime = entry.endTime != null && !entry.endTime.trim().isEmpty();

        return hasBeginTime && hasEndTime;
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
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

        public String getMaxDistance() {
            if (maxDuration == null) return "";
            return formatDuration(maxDuration);
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

        // save paths to preferences
        prefs.put("examsPath", examsPath);
        prefs.put("registrationsPath", registrationsPath);

        assessments = AssessmentsManager.loadAllAssessments(examsPath, registrationsPath, null);
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
    }

    private void updateCollisionTreeTable() {
        if (assessments == null) return;

        // Store current expansion states before rebuilding tree
        if (collisionTreeTable.getRoot() != null) {
            saveExpansionStates(collisionTreeTable.getRoot());
        }

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

            // Create a title item for this assessment
            TreeItem<CollisionEntry> titleItem = new TreeItem<>(
                    new CollisionEntry(assessment)
            );

            // Don't expand automatically - we'll restore expansion states later
            boolean hasMatchingChild = false;

            // Create child items regardless of showOnlyAssessmentsCheckbox setting
            // This ensures the tree structure is complete even when only showing assessments
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

                            // Convert the distance to hours with decimal places for more precise filtering
                            double distanceHours = distance.toMinutes() / 60.0;
                            matchesDistance = !distance.isNegative() && distanceHours <= maxHours;
                        } else {
                            matchesDistance = false; // No times available, don't show in filtered results
                        }
                    } catch (NumberFormatException e) {
                        matchesDistance = false;
                    }
                }

                if (matchesExam2 && matchesDistance) {
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
            // 3. We're showing only assessments (so we show all title rows regardless of children)
            if (exam2Filter.isEmpty() || hasMatchingChild || showOnlyAssessmentsCheckbox.isSelected()) {
                root.getChildren().add(titleItem);
            }
        }

        // Sort the data
        sortTreeItems(root);

        // Apply time-based filtering if checkbox is selected
        if (hideNullTimesCheckbox.isSelected()) {
            filterTreeItems(root, true);
        }

        // Update the tree table
        collisionTreeTable.setRoot(root);

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
            case "Avg. Distance" -> {
                if (!entry1.isTitle() || !entry2.isTitle()) {
                    yield entry1.isTitle() ? -1 : 1;
                }
                if (entry1.validDurationsCount == 0 || entry2.validDurationsCount == 0) {
                    yield entry1.validDurationsCount == 0 ? 1 : -1;
                }
                yield entry1.totalDuration.dividedBy(entry1.validDurationsCount)
                        .compareTo(entry2.totalDuration.dividedBy(entry2.validDurationsCount));
            }
            case "Max. Distance" -> {
                if (!entry1.isTitle() || !entry2.isTitle()) {
                    yield entry1.isTitle() ? -1 : 1;
                }
                if (entry1.maxDuration == null || entry2.maxDuration == null) {
                    yield entry1.maxDuration == null ? 1 : -1;
                }
                yield entry1.maxDuration.compareTo(entry2.maxDuration);
            }
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

            Label distanceLabel = new Label(String.format("Time Distance: " + collision.distance));
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
        filterDistanceField.setText(prefs.get("filterDistance", ""));
    }

    private void savePreferences() {
        prefs.put("sortExam1", sortExam1Box.getValue());
        prefs.put("sortExam2", sortExam2Box.getValue());
        prefs.put("filterExam1", filterExam1Field.getText());
        prefs.put("filterExam2", filterExam2Field.getText());
        prefs.put("filterDistance", filterDistanceField.getText());
    }

    @Override
    public void stop() {
        savePreferences();
    }

    private Tab createStatisticsTab() {
        Tab statisticsTab = new Tab("Statistics");
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label inProgressLabel = new Label("Statistics View - In Progress");
        inProgressLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        inProgressLabel.setTextFill(Color.web(SECONDARY_COLOR));

        Label descriptionLabel = new Label("This feature is currently under development.");
        descriptionLabel.setTextFill(Color.web(SECONDARY_COLOR));

        content.getChildren().addAll(inProgressLabel, descriptionLabel);
        statisticsTab.setContent(content);
        return statisticsTab;
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

    public static void run() {
        launch();
    }
}
