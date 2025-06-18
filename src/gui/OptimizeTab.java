package gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import data.MergedAssessment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.Collator;
import java.util.*;

/**
 * Tab for optimization functionalities in the exam scheduling application.
 */
public class OptimizeTab {

    private static final String PRIMARY_COLOR = "#3498db";
    private static final String SECONDARY_COLOR = "#2c3e50";


    private final Tab tab;
    private TreeTableView<CollisionEntry> optimizedTreeTable;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final ExamGUI examGUI;

    // Filter and sort controls
    private TextField filterExam1Field;
    private TextField filterExam2Field;
    private ComboBox<String> sortExam1Box;
    private ComboBox<String> sortDirectionExam1Box;
    private ComboBox<String> sortExam2Box;
    private ComboBox<String> sortDirectionExam2Box;
    private TextField maxDistanceField;


    private MergedAssessment[] optimizedAssessments;

    // Map to store expansion states before "Show Only Assessments" is activated//not working correctly-> need to be fix
    //Wichtig, kann mit ExamGUI in andere seperate Klasse verelegt werden. REFACTOR
    private final Map<String, Boolean> preCheckboxExpansionStates = new HashMap<>();
    private final Map<String, Boolean> expansionStates = new HashMap<>();

    // Checkboxes for filtering
    private CheckBox hideNullTimesCheckbox;
    private CheckBox showOnlyAssessmentsCheckbox;
    private CheckBox showOnlyWithCollisionsCheckbox;

    // Column visibility controls
    private MenuButton columnsMenuButton;
    private final Map<String, CheckMenuItem> columnMenuItems = new HashMap<>();
    private final Map<String, Boolean> columnVisibilityStates = new HashMap<>();

    // Constants for column groups

    // Map to associate dropdown options with column names for sorting
    private final Map<String, String> sortExam1ColumnMap = new HashMap<>();
    private final Map<String, String> sortExam2ColumnMap = new HashMap<>();

    // Sort direction constants
    private static final String ASCENDING = "Ascending";
    private static final String DESCENDING = "Descending";

    private final Collator germanCollator = Collator.getInstance(Locale.GERMAN);
    private boolean isUpdating;

    public OptimizeTab(ExamGUI examGUI) {
        this.examGUI = examGUI;
        tab = new Tab("Optimize");
        tab.setClosable(false);

        // Initialize the column mapping for sort dropdowns
        initializeSortColumnMappings();

        // Create basic layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Create a ScrollPane to wrap the content
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Create control section with left and right sides in an HBox
        HBox controlSection = new HBox(20);
        controlSection.setPadding(new Insets(0, 0, 20, 0));
        controlSection.setAlignment(Pos.CENTER_LEFT);


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
        optimizeButton.setOnAction(e -> onOptimizeButtonClicked(considerSupervisorCheckbox.isSelected(),considerRoomCheckbox.isSelected()));

        // === Right side ===
        VBox rightSide = new VBox(10);
        rightSide.setAlignment(Pos.TOP_LEFT);

        HBox saveSection = new HBox(15);  // Increased spacing between elements
        saveSection.setAlignment(Pos.CENTER_LEFT);

        Label saveFileLabel = new Label("Save Optimized File:");
        saveFileLabel.setStyle("-fx-text-fill: #2c3e50;");

        TextField folderPathField = new TextField();
        folderPathField.setEditable(false);
        folderPathField.setPromptText("No folder selected");
        
        folderPathField.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 4px;" +
                "-fx-padding: 8px;" +
                "-fx-font-size: 12px;"
        );

        Button browseButton = new Button("Browse...");
        browseButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 15px;" +
                "-fx-cursor: hand;" +
                "-fx-border-radius: 4px;"
        );

        browseButton.setOnAction(e -> {
            javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
            directoryChooser.setTitle("Select Output Directory");
            java.io.File directory = directoryChooser.showDialog(tab.getTabPane().getScene().getWindow());
            if (directory != null) {
                folderPathField.setText(directory.getAbsolutePath());
            }
        });

        Button saveOptimizedButton = new Button("Save Optimized");
        saveOptimizedButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 15px;" +
                "-fx-cursor: hand;" +
                "-fx-border-radius: 4px;"
        );

        saveOptimizedButton.setOnAction(e -> {
            // Placeholder for save action
            // Implementation will be added later
        });

        saveSection.getChildren().addAll(saveFileLabel, folderPathField, browseButton, saveOptimizedButton);
        rightSide.getChildren().add(saveSection);

        // Add left and right sides to the control section
        controlSection.getChildren().addAll(leftSide, rightSide);

        // Create filter section
        VBox filterSection = createFilterSection();
        filterSection.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Create and configure TreeTableView in its container
        VBox tableSection = createTableSection();

        // Add all sections to the content
        content.getChildren().addAll(controlSection, filterSection, tableSection);

        // Set the ScrollPane as the tab content
        tab.setContent(scrollPane);

        // Initialize listeners for filters
        setupFilterListeners();

        // Initialize event handlers (this will setup our sorting listeners)
        setupEventHandlers();

        // Set default sort direction
        sortDirectionExam1Box.setValue(ASCENDING);
        sortDirectionExam2Box.setValue(ASCENDING);

        // Set initial default sort columns
        sortExam1Box.setValue("Exam1 Name");
        sortExam2Box.setValue("Exam2 Name");

        // Apply initial sorting to the TreeTableView
        if (optimizedTreeTable != null) {
            // Sort Exam 1 table
            applySortToTreeTable(optimizedTreeTable, "Exam1 Name", TreeTableColumn.SortType.ASCENDING);

            // Sort each parent's children by Exam 2 name
            TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
            if (root != null) {
                for (TreeItem<CollisionEntry> parent : root.getChildren()) {
                    List<TreeItem<CollisionEntry>> children = new ArrayList<>(parent.getChildren());
                    sortItems(children, "Exam2 Name", ASCENDING);
                    parent.getChildren().clear();
                    parent.getChildren().addAll(children);
                }
            }
        }
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

        // Create filter fields
        filterExam1Field = createStyledTextField();
        filterExam1Field.setPromptText("Filter exam 1...");
        filterExam2Field = createStyledTextField();
        filterExam2Field.setPromptText("Filter exam 2...");
        maxDistanceField = createStyledTextField();
        maxDistanceField.setPromptText("Enter max hours");
        maxDistanceField.setPrefWidth(50);

        // Create sort comboboxes
        sortExam1Box = createStyledComboBox();
        sortExam1Box.setPromptText("Select Exam 1 sort...");
        sortExam1Box.setItems(FXCollections.observableArrayList(
                "Exam1 Name","Collision Count", "Old Begin", "Old End", "Old Average Distance", "Old Min. Distance",
                "Optimize Begin", "Optimize End", "Optimize Average Distance", "Optimize Min. Distance"
        ));

        sortDirectionExam1Box = createStyledComboBox();
        sortDirectionExam1Box.setPromptText("Select Exam 1 sort direction...");
        sortDirectionExam1Box.setItems(FXCollections.observableArrayList(ASCENDING, DESCENDING));

        sortExam2Box = createStyledComboBox();
        sortExam2Box.setPromptText("Select Exam 2 sort...");
        sortExam2Box.setItems(FXCollections.observableArrayList(
                "Exam2 Name", "Collision Count", "Old Begin", "Old End", "Old Distance",
                "Optimize Begin", "Optimize End", "Optimize Distance"
        ));

        sortDirectionExam2Box = createStyledComboBox();
        sortDirectionExam2Box.setPromptText("Select Exam 2 sort direction...");
        sortDirectionExam2Box.setItems(FXCollections.observableArrayList(ASCENDING, DESCENDING));

        Label filterExam1Label = new Label("Filter Exam 1");
        filterExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label filterExam2Label = new Label("Filter Exam 2");
        filterExam2Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label maxDistanceLabel = new Label("Max Distance (hours)");
        maxDistanceLabel.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam1Label = new Label("Sort Exam 1");
        sortExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortDirectionExam1Label = new Label("Sort Direction Exam 1");
        sortDirectionExam1Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortExam2Label = new Label("Sort Exam 2");
        sortExam2Label.setTextFill(Color.web(SECONDARY_COLOR));
        Label sortDirectionExam2Label = new Label("Sort Direction Exam 2");
        sortDirectionExam2Label.setTextFill(Color.web(SECONDARY_COLOR));


        maxDistanceField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Allow empty value for no filtering
            if (newVal.isEmpty()) {
                updateTable();
                return;
            }

            // Allow only digits and one decimal point
            if (!newVal.matches("^\\d*\\.?\\d*$")) {
                maxDistanceField.setText(oldVal);
                return;
            }

            // Don't update on partial decimal (e.g., "." or "5.")
            if (!newVal.equals(".") && !newVal.endsWith(".")) {
                updateTable();
            }
        });

        // Also update on focus lost to handle partial inputs
        maxDistanceField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                String text = maxDistanceField.getText();
                if (text.equals(".") || text.endsWith(".")) {
                    maxDistanceField.setText(text.replace(".", ""));
                }
                updateTable();
            }
        });

        // Add components to grid
        int row = 0;
        controlGrid.add(filterExam1Label, 0, row);
        controlGrid.add(filterExam1Field, 1, row);
        controlGrid.add(filterExam2Label, 2, row);
        controlGrid.add(filterExam2Field, 3, row);

        row++;
        controlGrid.add(sortExam1Label, 0, row);
        controlGrid.add(sortExam1Box, 1, row);
        controlGrid.add(sortDirectionExam1Label, 2, row);
        controlGrid.add(sortDirectionExam1Box, 3, row);

        row++;
        controlGrid.add(sortExam2Label, 0, row);
        controlGrid.add(sortExam2Box, 1, row);
        controlGrid.add(sortDirectionExam2Label, 2, row);
        controlGrid.add(sortDirectionExam2Box, 3, row);

        row++;
        controlGrid.add(maxDistanceLabel, 0, row);
        controlGrid.add(maxDistanceField, 1, row);

        // Set column constraints
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(15);
        ColumnConstraints controlCol = new ColumnConstraints();
        controlCol.setPercentWidth(35);
        controlGrid.getColumnConstraints().addAll(labelCol, controlCol, labelCol, controlCol);

        section.getChildren().addAll(sectionTitle, separator, controlGrid);
        return section;
    }
    //REFACTOR
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
    //REFACTOR
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
        VBox.setVgrow(section, Priority.ALWAYS);


        // Section title
        Label sectionTitle = new Label("Optimization Results");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(SECONDARY_COLOR));

        // Section description
        Label sectionDescription = new Label("Results of exam schedule optimization.");
        sectionDescription.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        // Create controls container
        HBox controlsContainer = new HBox(20);
        controlsContainer.setAlignment(Pos.CENTER_LEFT);

        // Add checkboxes for filtering
        hideNullTimesCheckbox = new CheckBox("Hide entries with no times");
        hideNullTimesCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");
        hideNullTimesCheckbox.selectedProperty().addListener((obs, old, newValue) -> {// Vor dem Filtern oder Neuaufbauen immer die Expansion-States speichern
            if (optimizedTreeTable.getRoot() != null) {
                saveExpansionStates(optimizedTreeTable.getRoot());
            }

            if (!showOnlyAssessmentsCheckbox.isSelected() || !newValue) {
                updateTable();

                // Nach dem Neuaufbau States wiederherstellen
                if (optimizedTreeTable.getRoot() != null) {
                    restoreExpansionStates(optimizedTreeTable.getRoot());

                    // Wenn "Show Only Assessment" aktiv ist, immer wieder einklappen
                    if (showOnlyAssessmentsCheckbox.isSelected()) {
                        for (TreeItem<CollisionEntry> titleItem : optimizedTreeTable.getRoot().getChildren()) {
                            titleItem.setExpanded(false);
                        }
                    }
                }
            } else {
                updateTable();
                TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
                if (root != null) {
                    updateTable();
                }
            }
        });

        showOnlyAssessmentsCheckbox = new CheckBox("Show only assessments");
        showOnlyAssessmentsCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");
        showOnlyAssessmentsCheckbox.selectedProperty().addListener((obs, old, newValue) -> {
                    TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
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
                            updateTable();
                        }

                        // Jetzt auf den neuen root anwenden (könnte sich nach updateCollisionTreeTable geändert haben)
                        root = optimizedTreeTable.getRoot();
                        if (root != null) {
                            restoreExpansionStatesFromMap(root, preCheckboxExpansionStates);
                        }

                        // Filter ggf. wieder einschalten
                        if (wasHidingNullTimes) {
                            hideNullTimesCheckbox.setSelected(true);
                        }
                    }
                }
        );

        showOnlyWithCollisionsCheckbox = new CheckBox("Show only with collisions");
        showOnlyWithCollisionsCheckbox.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");
        showOnlyWithCollisionsCheckbox.selectedProperty().addListener((obs, old, newValue) -> { // Save expansion states before updating
            if (optimizedTreeTable.getRoot() != null) {
                saveExpansionStates(optimizedTreeTable.getRoot());
            }

            // Update the table with the new filter
            updateTable();

            // Restore expansion states
            if (optimizedTreeTable.getRoot() != null) {
                restoreExpansionStates(optimizedTreeTable.getRoot());

                // Keep items collapsed if "Show Only Assessments" is checked
                if (showOnlyAssessmentsCheckbox.isSelected()) {
                    for (TreeItem<CollisionEntry> titleItem : optimizedTreeTable.getRoot().getChildren()) {
                        titleItem.setExpanded(false);
                    }
                }
            }});

        // Create columns menu button
        columnsMenuButton = new MenuButton("Columns");
        columnsMenuButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 5px 10px;" +
                "-fx-cursor: hand;" +
                "-fx-border-radius: 4px;"
        );




        // Initialize and setup the TreeTableView
        this.optimizedTreeTable = setupTreeTableView();
        VBox.setVgrow(optimizedTreeTable, Priority.ALWAYS);

        // Initialize column menu items
        setupColumnMenuItems();


        // Add all controls to container
        controlsContainer.getChildren().addAll(
            hideNullTimesCheckbox,
            showOnlyAssessmentsCheckbox,
            showOnlyWithCollisionsCheckbox,
            columnsMenuButton
        );

        // Separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        // Add components to section
        section.getChildren().addAll(
            sectionTitle,
            sectionDescription,
            controlsContainer,
            separator,
            optimizedTreeTable
        );

        return section;
    }

    //
    private void bindColumnToCheckItem(String columnName, CheckMenuItem item, boolean defaultVisible) {
        // Store the check menu item in the map for later reference
        columnMenuItems.put(columnName, item);
        columnVisibilityStates.put(columnName, defaultVisible);

        // Set initial selected state
        item.setSelected(defaultVisible);

        // Find and set initial visibility of the column
        TreeTableColumn<CollisionEntry, ?> col = findColumnByName(columnName);
        if (col != null) {
            col.setVisible(defaultVisible);

            // Two-way binding between column visibility and menu item
            item.selectedProperty().addListener((obs, old, newVal) -> {
                columnVisibilityStates.put(columnName, newVal);
                TreeTableColumn<CollisionEntry, ?> c = findColumnByName(columnName);
                if (c != null) {
                    c.setVisible(newVal);
                    adjustColumnWidths();
                }
            });

            // Update menu item if column visibility changes externally
            col.visibleProperty().addListener((obs, old, newVal) -> {
                if (item.isSelected() != newVal) {
                    item.setSelected(newVal);
                    columnVisibilityStates.put(columnName, newVal);
                }
            });
        }
    }


    private void setupColumnMenuItems() {

        CheckMenuItem exam1Item = new CheckMenuItem("Exam 1");
        exam1Item.setSelected(true);
        exam1Item.setDisable(true);  // First column cannot be hidden

        CheckMenuItem exam2Item = new CheckMenuItem("Exam 2");
        exam2Item.setSelected(true);
        exam2Item.setDisable(true);

        CheckMenuItem collisionCountItem = new CheckMenuItem("Collision Count");
        bindColumnToCheckItem("Collision Count", collisionCountItem, true);

        CheckMenuItem oldBeginItem = new CheckMenuItem("Old Begin");
        bindColumnToCheckItem("Old Begin", oldBeginItem, false);

        CheckMenuItem oldEndItem = new CheckMenuItem("Old End");
        bindColumnToCheckItem("Old End", oldEndItem, false);

        CheckMenuItem optBeginItem = new CheckMenuItem("Opt Begin");
        bindColumnToCheckItem("Opt Begin", optBeginItem, true);

        CheckMenuItem optEndItem = new CheckMenuItem("Opt End");
        bindColumnToCheckItem("Opt End", optEndItem, true);

        CheckMenuItem oldDistanceItem = new CheckMenuItem("Old Distance");
        bindColumnToCheckItem("Old Distance", oldDistanceItem, false);

        CheckMenuItem optDistanceItem = new CheckMenuItem("Opt Distance");
        bindColumnToCheckItem("Opt Distance", optDistanceItem, true);

        CheckMenuItem oldAvgDistanceItem = new CheckMenuItem("Old Avg. Distance");
        bindColumnToCheckItem("Old Avg. Distance", oldAvgDistanceItem, false);

        CheckMenuItem oldMinDistanceItem = new CheckMenuItem("Old Min. Distance");
        bindColumnToCheckItem("Old Min. Distance", oldMinDistanceItem, false);

        CheckMenuItem optAvgDistanceItem = new CheckMenuItem("Opt Avg. Distance");
        bindColumnToCheckItem("Opt Avg. Distance", optAvgDistanceItem, true);

        CheckMenuItem optMinDistanceItem = new CheckMenuItem("Opt Min. Distance");
        bindColumnToCheckItem("Opt Min. Distance", optMinDistanceItem, true);

        // Add menu items to the columns menu
        columnsMenuButton.getItems().addAll(
                exam1Item, exam2Item, collisionCountItem,
                new SeparatorMenuItem(),
                oldBeginItem, oldEndItem, oldDistanceItem, oldAvgDistanceItem, oldMinDistanceItem,
                new SeparatorMenuItem(),
                optBeginItem, optEndItem, optDistanceItem, optAvgDistanceItem, optMinDistanceItem
        );


    }

    private TextField createStyledTextField() {
        TextField field = new TextField();
        field.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 4px;" +
                "-fx-padding: 8px;"
        );
        return field;
    }

    private ComboBox<String> createStyledComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 4px;" +
                "-fx-padding: 8px;"
        );
        return comboBox;
    }

    private void setupFilterListeners() {
        // Text field listeners
        filterExam1Field.textProperty().addListener((obs, oldVal, newVal) -> updateTable());
        filterExam2Field.textProperty().addListener((obs, oldVal, newVal) -> updateTable());

        // Add numeric validation and filtering for maxDistanceField
        maxDistanceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                updateTable();
                return;
            }

            if (!newVal.matches("^\\d*\\.?\\d*$")) {
                maxDistanceField.setText(oldVal);
                return;
            }

            if (!newVal.endsWith(".")) {
                updateTable();
            }
        });

        // Sort box listeners
        sortExam1Box.setOnAction(e -> updateTable());
        sortDirectionExam1Box.setOnAction(e -> updateTable());
        sortExam2Box.setOnAction(e -> updateTable());
        sortDirectionExam2Box.setOnAction(e -> updateTable());

        // Set default sort direction
        sortDirectionExam1Box.setValue(ASCENDING);
        sortDirectionExam2Box.setValue(ASCENDING);

        // Add listeners to sort dropdowns to update column sorting
        sortExam1Box.valueProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                TreeTableColumn<CollisionEntry, ?> column = findColumnByName("Exam 1");
                if (column != null) {
                    column.setSortType(sortDirectionExam1Box.getValue().equals(ASCENDING) ?
                        TreeTableColumn.SortType.ASCENDING : TreeTableColumn.SortType.DESCENDING);
                    //noinspection unchecked
                    optimizedTreeTable.getSortOrder().setAll(column);
                }
            }
        });

        sortExam2Box.valueProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                TreeTableColumn<CollisionEntry, ?> column = findColumnByName("Exam 2");
                if (column != null) {
                    column.setSortType(sortDirectionExam2Box.getValue().equals(ASCENDING) ?
                        TreeTableColumn.SortType.ASCENDING : TreeTableColumn.SortType.DESCENDING);
                    //noinspection unchecked
                    optimizedTreeTable.getSortOrder().setAll(column);
                }
            }
        });

        // Add listeners for sort direction changes
        sortDirectionExam1Box.valueProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && sortExam1Box.getValue() != null) {
                TreeTableColumn<CollisionEntry, ?> column = findColumnByName("Exam 1");
                if (column != null) {
                    column.setSortType(newValue.equals(ASCENDING) ?
                        TreeTableColumn.SortType.ASCENDING : TreeTableColumn.SortType.DESCENDING);
                }
            }
        });

        sortDirectionExam2Box.valueProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && sortExam2Box.getValue() != null) {
                TreeTableColumn<CollisionEntry, ?> column = findColumnByName("Exam 2");
                if (column != null) {
                    column.setSortType(newValue.equals(ASCENDING) ?
                        TreeTableColumn.SortType.ASCENDING : TreeTableColumn.SortType.DESCENDING);
                }
            }
        });
    }

    private void updateTable() {

        // Get root and check for null assessments
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root == null || optimizedAssessments == null) return;

        // Store current expansion states differently based on checkbox state change
        if (optimizedTreeTable.getRoot() != null) {
            saveExpansionStates(optimizedTreeTable.getRoot());
        }

        List<TreeTableColumn<CollisionEntry, ?>> sortOrder = new ArrayList<>(optimizedTreeTable.getSortOrder());

        // Create a new root item
        TreeItem<CollisionEntry> newRoot = new TreeItem<>(new CollisionEntry(null));

        // Get filter values
        String exam1Filter = filterExam1Field.getText().toLowerCase();
        String exam2Filter = filterExam2Field.getText().toLowerCase();
        String maxDistance = maxDistanceField.getText();

        // Process each assessment
        for (MergedAssessment assessment : optimizedAssessments) {
            if (assessment == null) continue;

            // Check if this assessment matches exam1 filter
            boolean matchesExam1 = exam1Filter.isEmpty() ||
                assessment.getQualifiedName().toLowerCase().contains(exam1Filter);

            if (!matchesExam1) continue;

            // Create a list of child items that match our filters
            List<TreeItem<CollisionEntry>> validChildren = new ArrayList<>();
            int validCollisionCount = 0;
            Duration minOldDistance = null;
            Duration minOptDistance = null;
            Duration totalOldDistance = Duration.ZERO;
            Duration totalOptDistance = Duration.ZERO;
            int validDistanceCount = 0;

            // Process each colliding assessment//Same as the one in ExamGUI.java
            Map<MergedAssessment, Integer> collisions = assessment.getCollisionCountByAssessment();
            if (collisions != null) {
                for (Map.Entry<MergedAssessment, Integer> collision : collisions.entrySet()) {
                    MergedAssessment childAssessment = collision.getKey();
                    int collisionCount = collision.getValue();

                    // Check if colliding assessment matches exam2 filter
                    boolean matchesExam2 = exam2Filter.isEmpty() ||
                        childAssessment.getQualifiedName().toLowerCase().contains(exam2Filter);

                    // Calculate distances for filtering and aggregation
                    Duration optDistance = calculateOptimizedTimeDifference(assessment, childAssessment);
                    Duration oldDistance = calculateTimeDifference(assessment, childAssessment);

                    // Check distance filter
                    boolean matchesDistance = true;
                    if (!maxDistance.isEmpty()) {
                        try {
                            double maxHours = Double.parseDouble(maxDistance);
                            // Only filter by OptDistance, and only if it's valid (non-zero and non-negative)
                            if (optDistance != null && !optDistance.isZero() && !optDistance.isNegative()) {
                                double distanceHours = optDistance.toMinutes() / 60.0;
                                matchesDistance = distanceHours <= maxHours;
                            }
                            // If OptDistance is null, zero, or negative, include the item (treated as no distance)
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    // Check if entry should be hidden due to missing times
                    boolean hasValidTimes = !hideNullTimesCheckbox.isSelected() ||
                            (assessment.getBegin() != null && assessment.getEnd() != null &&
                                    childAssessment.getBegin() != null && childAssessment.getEnd() != null &&
                                    assessment.getOptimizedBegin() != null && assessment.getOptimizedEnd() != null &&
                                    childAssessment.getOptimizedBegin() != null && childAssessment.getOptimizedEnd() != null);


                    // If entry passes all filters, add it and update statistics
                    if (matchesExam2 && matchesDistance && hasValidTimes) {
                        TreeItem<CollisionEntry> childItem = new TreeItem<>(
                            new CollisionEntry(assessment, childAssessment, collisionCount)
                        );
                        validChildren.add(childItem);
                        validCollisionCount += collisionCount;

                        // Update distance statistics only for valid distances
                        if (oldDistance != null && !oldDistance.isNegative() && !oldDistance.isZero()) {
                            if (minOldDistance == null || oldDistance.compareTo(minOldDistance) < 0) {
                                minOldDistance = oldDistance;
                            }
                            totalOldDistance = totalOldDistance.plus(oldDistance);
                            validDistanceCount++;
                        }

                        if (optDistance != null && !optDistance.isNegative() && !optDistance.isZero()) {
                            if (minOptDistance == null || optDistance.compareTo(minOptDistance) < 0) {
                                minOptDistance = optDistance;
                            }
                            totalOptDistance = totalOptDistance.plus(optDistance);
                        }
                    }
                }
            }

            // Skip if no valid collisions and filter is active
            if (showOnlyWithCollisionsCheckbox.isSelected() && validCollisionCount == 0) {
                continue;
            }

            // Create parent item with updated statistics, excluding zero distances from averages
            TreeItem<CollisionEntry> parentItem = new TreeItem<>(new CollisionEntry(assessment));
            parentItem.getValue().setDynamicStats(new DistanceStats(
                validDistanceCount > 0 ? totalOldDistance.dividedBy(validDistanceCount) : null,
                validDistanceCount > 0 ? totalOptDistance.dividedBy(validDistanceCount) : null,
                    minOldDistance,
                    minOptDistance
            ));
            parentItem.getValue().setDynamicCollisionCount(validCollisionCount);

            // Add children and check visibility
            parentItem.getChildren().addAll(validChildren);

            // Add parent if it matches criteria
            // Add the title item to the root if:
            // 1. No exam2 filter is active, or
            // 2. It has matching children, or
            // 3. We're showing only assessments
            if (exam2Filter.isEmpty() || !validChildren.isEmpty() || showOnlyAssessmentsCheckbox.isSelected()) {
                newRoot.getChildren().add(parentItem);
            }
        }


        sortTreeItems(newRoot);

        // Update table with new data
        optimizedTreeTable.setRoot(newRoot);

        // Restore sort order if any was active
        if (!sortOrder.isEmpty()) {
            optimizedTreeTable.getSortOrder().clear();
            optimizedTreeTable.getSortOrder().addAll(sortOrder);
        }

        // Set expansion states based on saved states and settings
        for (TreeItem<CollisionEntry> titleItem : newRoot.getChildren()) {
            String key = getItemKey(titleItem.getValue());
            Boolean wasExpanded = expansionStates.get(key);

            if (showOnlyAssessmentsCheckbox.isSelected()) {
                titleItem.setExpanded(false);
            } else
                titleItem.setExpanded(Objects.requireNonNullElseGet(wasExpanded, () -> !showOnlyAssessmentsCheckbox.isSelected()));
        }

        // Update parent aggregates
        updateAllParentAggregates();
    }

    private void sortTreeItems(TreeItem<CollisionEntry> root) {
        // Skip if we're in the middle of an update to prevent infinite loops
        if (isUpdating) return;
        isUpdating = true;

        try {
            String exam1SortCriteria = sortExam1Box.getValue();
            String exam1SortDirection = sortDirectionExam1Box.getValue();
            String exam2SortCriteria = sortExam2Box.getValue();
            String exam2SortDirection = sortDirectionExam2Box.getValue();

            // Sort parent rows if a sort criteria is selected
            if (exam1SortCriteria != null && !exam1SortCriteria.isEmpty()) {
                sortItems(root.getChildren(), exam1SortCriteria, exam1SortDirection != null ? exam1SortDirection : ASCENDING);
            }

            // Sort child rows for each parent if a sort criteria is selected
            if (exam2SortCriteria != null && !exam2SortCriteria.isEmpty()) {
                for (TreeItem<CollisionEntry> parentItem : root.getChildren()) {
                    sortItems(parentItem.getChildren(), exam2SortCriteria, exam2SortDirection != null ? exam2SortDirection : ASCENDING);
                }
            }
        } finally {
            isUpdating = false;
        }
    }

    //REFACTOR
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


    //REFACTOR
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
        optimizedTreeTable.refresh();
    }


    private String getItemKey(CollisionEntry entry) {
        if (entry == null) return null;
        return entry.getExam1QualifiedName() + "|" + entry.getExam2QualifiedName();
    }

    private void sortItems(List<TreeItem<CollisionEntry>> items, String criteria, String direction) {
        items.sort((item1, item2) -> {
            // Get comparable values based on the selected column/criteria
            Comparable<?> value1 = getValueForColumn(item1.getValue(), criteria);
            Comparable<?> value2 = getValueForColumn(item2.getValue(), criteria);

            int result;
            // Handle null values - nulls should be sorted first when ascending
            if (value1 == null && value2 == null) {
                result = 0;
            } else if (value1 == null) {
                result = -1; // Null values are sorted first
            } else if (value2 == null) {
                result = 1;
            } else if (value1 instanceof String && value2 instanceof String) {
                // Use German collator for String comparison (case insensitive)
                result = germanCollator.compare(value1.toString(), value2.toString());
            } else {
                // For non-string types, use natural ordering
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable1 = (Comparable<Object>) value1;
                result = comparable1.compareTo(value2);
            }

            // Apply sort direction
            return direction.equals(ASCENDING) ? result : -result;
        });
    }

    /**
     * Gets the value from a CollisionEntry for a specific column/criteria for sorting.
     * @param entry The CollisionEntry to extract the value from
     * @param columnName The column/criteria name to get the value for
     * @return A Comparable value for sorting, or null if not applicable
     */
    private Comparable<?> getValueForColumn(CollisionEntry entry, String columnName) {
        if (entry == null) return null;

        // For title rows (parent rows), properly handle column selection
        if (entry.isTitle()) {
            // Get the appropriate assessment for comparison (parent assessment for title rows)
            MergedAssessment assessment = entry.getParentAssessment();

            // Return the value based on the column/criteria
            return switch (columnName) {
                case "Exam1 Name", "Exam 1 Name" -> entry.getExam1QualifiedName();
                case "Exam2 Name", "Exam 2 Name" -> entry.getExam2QualifiedName();
                case "Collision Count" -> entry.getCollisionCount();
                case "Old Begin" -> assessment != null ? assessment.getBegin() : null;
                case "Old End" -> assessment != null ? assessment.getEnd() : null;
                case "Optimize Begin", "Optimized Begin" -> assessment != null ? assessment.getOptimizedBegin() : null;
                case "Optimize End", "Optimized End" -> assessment != null ? assessment.getOptimizedEnd() : null;
                // Handle all variations of distance column names for consistency
                case "Old Average Distance", "Old Avg. Distance" ->
                    entry.dynamicStats != null ? entry.dynamicStats.getAverageOldDistance() : null;
                case "Optimize Average Distance", "Optimized Average Distance", "Opt Avg. Distance" ->
                    entry.dynamicStats != null ? entry.dynamicStats.getAverageOptimizedDistance() : null;
                case "Old Min Distance", "Old Min. Distance" ->
                    entry.dynamicStats != null ? entry.dynamicStats.getMinOldDistance() : null;
                case "Optimize Min. Distance", "Optimized Min Distance", "Opt Min. Distance" ->
                    entry.dynamicStats != null ? entry.dynamicStats.getMinOptimizedDistance() : null;
                default -> entry.getExam1QualifiedName(); //default
            };
        }

        // Check if we're comparing parent or child contexts
        boolean isParentContext = entry.getChildAssessment() == null;

        // Get the appropriate assessment for comparison
        MergedAssessment assessment = isParentContext ?
                entry.getParentAssessment() : entry.getChildAssessment();

        // Return the appropriate value based on the column/criteria
        return switch (columnName) {
            case "Exam1 Name", "Exam 1 Name" -> entry.getExam1QualifiedName();
            case "Exam2 Name", "Exam 2 Name" -> entry.getExam2QualifiedName();
            case "Collision Count" -> entry.getCollisionCount();
            case "Old Begin" -> assessment != null ? assessment.getBegin() : null;
            case "Old End" -> assessment != null ? assessment.getEnd() : null;
            case "Optimize Begin", "Optimized Begin" -> assessment != null ? assessment.getOptimizedBegin() : null;
            case "Optimize End", "Optimized End" -> assessment != null ? assessment.getOptimizedEnd() : null;
            case "Old Distance" -> {
                if (!isParentContext) {
                    yield calculateTimeDifference(entry.getParentAssessment(), entry.getChildAssessment());
                }
                yield null;
            }
            case "Optimize Distance", "Optimized Distance" -> {
                if (!isParentContext) {
                    yield calculateOptimizedTimeDifference(entry.getParentAssessment(), entry.getChildAssessment());
                }
                yield null;
            }
            case "Old Average Distance", "Old Avg. Distance" -> {
                if (isParentContext && entry.dynamicStats != null) {
                    yield entry.dynamicStats.getAverageOldDistance();
                }
                yield null;
            }
            case "Old Min Distance", "Old Min. Distance" -> {
                if (isParentContext && entry.dynamicStats != null) {
                    yield entry.dynamicStats.getMinOldDistance();
                }
                yield null;
            }
            case "Optimize Average Distance", "Optimized Average Distance", "Opt Avg. Distance" -> {
                if (isParentContext && entry.dynamicStats != null) {
                    yield entry.dynamicStats.getAverageOptimizedDistance();
                }
                yield null;
            }
            case "Optimize Min Distance", "Optimized Min Distance", "Opt Min. Distance" -> {
                if (isParentContext && entry.dynamicStats != null) {
                    yield entry.dynamicStats.getMinOptimizedDistance();
                }
                yield null;
            }
            default -> null;
        };
    }
    //For synchro
    private String mapDropdownValueToColumnName(String dropdownValue) {
        return switch (dropdownValue) {
            case "Exam 1 Name" -> "Exam 1";
            case "Exam 2 Name" -> "Exam 2";
            case "Collision Count" -> "Collision Count";
            case "Old Begin" -> "Old Begin";
            case "Old End" -> "Old End";
            case "Optimize Begin" -> "Opt Begin";
            case "Optimize End" -> "Opt End";
            case "Optimize  Avg. Distance" -> "Opt Avg. Distance";
            case "Optimize Min. Distance" -> "Opt Min. Distance";
            case "Optimize Distance" -> "Opt Distance";
            case "Old Avg. Distance" -> "Old Avg. Distance";
            case "Old Min. Distance" -> "Old Min. Distance";
            case "Old Distance" -> "Old Distance";
            default -> dropdownValue;
        };
    }

    private void updateSort() {
        if (isUpdating) return;
        isUpdating = true;

        try {
            String exam1SortColumn = sortExam1Box.getValue();
            String exam1SortDirection = sortDirectionExam1Box.getValue();
            String exam2SortColumn = sortExam2Box.getValue();
            String exam2SortDirection = sortDirectionExam2Box.getValue();

            TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
            if (root == null) return;

            // First, sort all parent nodes (Exam1 level)
            if (exam1SortColumn != null && !exam1SortColumn.isEmpty()) {
                List<TreeItem<CollisionEntry>> parentItems = new ArrayList<>(root.getChildren());
                sortItems(parentItems, exam1SortColumn, exam1SortDirection);

                // Clear and re-add sorted items
                root.getChildren().clear();
                root.getChildren().addAll(parentItems);
            }

            // Then, sort children within each parent (Exam2 level)
            if (exam2SortColumn != null && !exam2SortColumn.isEmpty()) {
                for (TreeItem<CollisionEntry> parent : root.getChildren()) {
                    List<TreeItem<CollisionEntry>> childItems = new ArrayList<>(parent.getChildren());
                    sortItems(childItems, exam2SortColumn, exam2SortDirection);

                    // Clear and re-add sorted items
                    parent.getChildren().clear();
                    parent.getChildren().addAll(childItems);
                }
            }

            // Clear the TreeTableView's sort order since we're handling sorting manually
            optimizedTreeTable.getSortOrder().clear();

            // Update the table to reflect changes
            optimizedTreeTable.refresh();

        } finally {
            isUpdating = false;
        }
    }

    private void setupEventHandlers() {
        // Filter and sort handlers
        filterExam1Field.textProperty().addListener((obs, old, newValue) -> updateTable());
        filterExam2Field.textProperty().addListener((obs, old, newValue) -> updateTable());

        // Set default sort direction
        sortDirectionExam1Box.setValue(ASCENDING);
        sortDirectionExam2Box.setValue(ASCENDING);

        // Setup sort handlers - these will trigger our updateSort() method
        sortExam1Box.setOnAction(e -> updateSort());
        sortDirectionExam1Box.setOnAction(e -> updateSort());
        sortExam2Box.setOnAction(e -> updateSort());
        sortDirectionExam2Box.setOnAction(e -> updateSort());

        optimizedTreeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Check for double-click
                TreeItem<CollisionEntry> selectedItem = optimizedTreeTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.getValue().isTitle() &&
                        selectedItem.getParent() != null && selectedItem.getParent() != optimizedTreeTable.getRoot()) {

                    // Get the qualified name from the selected child row
                    String selectedQualifiedName = selectedItem.getValue().getExam2QualifiedName();

                    // Find and select the corresponding parent row
                    TreeItem<CollisionEntry> parentToSelect = findParentByQualifiedName(selectedQualifiedName);
                    if (parentToSelect != null) {
                        // Expand the parent row and scroll to it
                        parentToSelect.setExpanded(true);
                        optimizedTreeTable.scrollTo(optimizedTreeTable.getRow(parentToSelect));
                        optimizedTreeTable.getSelectionModel().select(parentToSelect);
                    }
                }
            }
        });
    }


    private TreeTableView<CollisionEntry> setupTreeTableView() {
        TreeTableView<CollisionEntry> optimizedTreeTable = new TreeTableView<>();
        optimizedTreeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        optimizedTreeTable.setShowRoot(false);

        // Set preferred height to match CollisionResults tab
        optimizedTreeTable.setPrefHeight(600);
        optimizedTreeTable.setMinHeight(400);
        VBox.setVgrow(optimizedTreeTable, Priority.ALWAYS);

        // Add double-click handler for parent row navigation
        optimizedTreeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Check for double-click
                TreeItem<CollisionEntry> selectedItem = optimizedTreeTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.getValue().isTitle() &&
                    selectedItem.getParent() != null && selectedItem.getParent() != optimizedTreeTable.getRoot()) {

                    // Get the qualified name from the selected child row
                    String selectedQualifiedName = selectedItem.getValue().getExam2QualifiedName();

                    // Find and select the corresponding parent row
                    TreeItem<CollisionEntry> parentToSelect = findParentByQualifiedName(selectedQualifiedName);
                    if (parentToSelect != null) {
                        // Expand the parent row and scroll to it
                        parentToSelect.setExpanded(true);
                        optimizedTreeTable.scrollTo(optimizedTreeTable.getRow(parentToSelect));
                        optimizedTreeTable.getSelectionModel().select(parentToSelect);
                    }
                }
            }
        });

        // Create all columns
        TreeTableColumn<CollisionEntry, String> exam1Col = new TreeTableColumn<>("Exam 1");
        exam1Col.setCellValueFactory(param -> {
            TreeItem<CollisionEntry> item = param.getValue();
            if (item == null || item.getValue() == null) {
                return new SimpleStringProperty("");
            }

            // Get whether this is a parent row by checking parent reference
            boolean isParentRow = item.getParent() == null || item.getParent() == optimizedTreeTable.getRoot();

            if (isParentRow) {
                // Parent row - show its own qualified name
                MergedAssessment assessment = item.getValue().parentAssessment;
                return new SimpleStringProperty(assessment != null ? assessment.getQualifiedName() : "");
            } else {
                // Child row - show parent's qualified name
                MergedAssessment parentAssessment = item.getParent().getValue().parentAssessment;
                return new SimpleStringProperty(parentAssessment != null ? parentAssessment.getQualifiedName() : "");
            }
        });
        exam1Col.setSortable(true);


        TreeTableColumn<CollisionEntry, String> exam2Col = new TreeTableColumn<>("Exam 2");
        exam2Col.setCellValueFactory(param -> {
            TreeItem<CollisionEntry> item = param.getValue();
            if (item == null || item.getValue() == null) {
                return new SimpleStringProperty("");
            }

            // Get whether this is a parent row by checking parent reference
            boolean isParentRow = item.getParent() == null || item.getParent() == optimizedTreeTable.getRoot();

            if (isParentRow) {
                // Parent row - show empty
                return new SimpleStringProperty("");
            } else {
                // Child row - show its own qualified name
                MergedAssessment childAssessment = item.getValue().childAssessment;
                return new SimpleStringProperty(childAssessment != null ? childAssessment.getQualifiedName() : "");
            }
        });
        exam2Col.setSortable(true);


        TreeTableColumn<CollisionEntry, String> collisionCountCol = new TreeTableColumn<>("Collision Count");
        collisionCountCol.setCellValueFactory(param -> {
            if (param.getValue() == null || param.getValue().getValue() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(String.valueOf(param.getValue().getValue().getCollisionCount()));
        });
        collisionCountCol.setSortable(false);  // Disable sorting
        collisionCountCol.setComparator((s1, s2) -> {
            try {
                return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });


        // Create and configure time-related columns
        TreeTableColumn<CollisionEntry, String> oldBeginCol = new TreeTableColumn<>("Old Begin");
        oldBeginCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldBeginTime() : ""));
        oldBeginCol.setSortable(false);  // Disable sorting


        TreeTableColumn<CollisionEntry, String> optimizedBeginCol = new TreeTableColumn<>("Opt Begin");
        optimizedBeginCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedBeginTime() : ""));
        optimizedBeginCol.setSortable(false);  // Disable sorting

        TreeTableColumn<CollisionEntry, String> oldEndCol = new TreeTableColumn<>("Old End");
        oldEndCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldEndTime() : ""));
        oldEndCol.setSortable(false);  // Disable sorting

        TreeTableColumn<CollisionEntry, String> optimizedEndCol = new TreeTableColumn<>("Opt End");
        optimizedEndCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedEndTime() : ""));
        optimizedEndCol.setSortable(false);  // Disable sorting

        TreeTableColumn<CollisionEntry, String> oldDistanceCol = new TreeTableColumn<>("Old Distance");
        oldDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldDistance() : ""));
        oldDistanceCol.setSortable(true);
        oldDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> optimizedDistanceCol = new TreeTableColumn<>("Opt Distance");
        optimizedDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedDistance() : ""));
        optimizedDistanceCol.setSortable(true);
        optimizedDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> oldAvgDistanceCol = new TreeTableColumn<>("Old Avg. Distance");
        oldAvgDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldAverageDistance() : ""));
        oldAvgDistanceCol.setSortable(true);
        oldAvgDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> optimizedAvgDistanceCol = new TreeTableColumn<>("Opt Avg. Distance");
        optimizedAvgDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedAverageDistance() : ""));
        optimizedAvgDistanceCol.setSortable(true);
        optimizedAvgDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> oldMinDistanceCol = new TreeTableColumn<>("Old Min. Distance");
        oldMinDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldMinDistance() : ""));
        oldMinDistanceCol.setSortable(true);
        oldMinDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        TreeTableColumn<CollisionEntry, String> optimizedMinDistanceCol = new TreeTableColumn<>("Opt Min. Distance");
        optimizedMinDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedMinDistance() : ""));
        optimizedMinDistanceCol.setSortable(true);
        optimizedMinDistanceCol.setComparator((s1, s2) -> {
            try {
                return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        // Add all columns to the TreeTableView
        //noinspection unchecked
        optimizedTreeTable.getColumns().addAll(
            exam1Col, exam2Col, collisionCountCol,
            oldBeginCol, optimizedBeginCol,
            oldEndCol, optimizedEndCol,
            oldDistanceCol, optimizedDistanceCol,
            oldAvgDistanceCol, optimizedAvgDistanceCol,
            oldMinDistanceCol, optimizedMinDistanceCol
        );

        exam1Col.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.18));
        exam2Col.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.18));
        collisionCountCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.12));
        oldBeginCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        oldEndCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        oldDistanceCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        oldAvgDistanceCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        oldMinDistanceCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        optimizedBeginCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        optimizedEndCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        optimizedDistanceCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        optimizedAvgDistanceCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));
        optimizedMinDistanceCol.prefWidthProperty().bind(optimizedTreeTable.widthProperty().multiply(0.13));

        // Style the columns
        String columnStyle = "-fx-alignment: CENTER-LEFT; -fx-padding: 8px;";
        exam1Col.setStyle(columnStyle);
        exam2Col.setStyle(columnStyle);
        collisionCountCol.setStyle(columnStyle);
        oldBeginCol.setStyle(columnStyle);
        oldEndCol.setStyle(columnStyle);
        oldDistanceCol.setStyle(columnStyle);
        oldAvgDistanceCol.setStyle(columnStyle);
        oldMinDistanceCol.setStyle(columnStyle);
        optimizedBeginCol.setStyle(columnStyle);
        optimizedEndCol.setStyle(columnStyle);
        optimizedDistanceCol.setStyle(columnStyle);
        optimizedAvgDistanceCol.setStyle(columnStyle);
        optimizedMinDistanceCol.setStyle(columnStyle);

        // Implement custom sorting for exam columns
        exam1Col.setComparator(germanCollator::compare);
        exam2Col.setComparator(germanCollator::compare);

        // Set default sort on Exam 1 column (ascending)
        exam1Col.setSortType(TreeTableColumn.SortType.ASCENDING);
        optimizedTreeTable.getSortOrder().add(exam1Col);

        // Prevent "no sort" state by forcing ascending when sort type becomes null
        for (TreeTableColumn<CollisionEntry, ?> column : optimizedTreeTable.getColumns()) {
            column.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    column.setSortType(TreeTableColumn.SortType.ASCENDING);
                }
            });
        }

        // Add listener to update dropdowns when column sorting changes
        optimizedTreeTable.getSortOrder().addListener((javafx.collections.ListChangeListener<TreeTableColumn<CollisionEntry, ?>>) change -> {
            if (isUpdating) return;

            while (change.next()) {
                if (change.wasAdded() && !change.getAddedSubList().isEmpty()) {
                    TreeTableColumn<CollisionEntry, ?> column = change.getAddedSubList().get(0);
                    updateSortDropdownsFromColumnHeader(column);
                }
            }
        });

        // Add listeners to each column's sortType property
        for (TreeTableColumn<CollisionEntry, ?> column : optimizedTreeTable.getColumns()) {
            column.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
                if (!isUpdating && newValue != null) {
                    updateSortDropdownsFromColumnHeader(column);
                }
            });
        }

        return optimizedTreeTable;

    }

    private void adjustColumnWidths() {
        // Ensure we're on the JavaFX Application Thread
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(this::adjustColumnWidths);
            return;
        }

        // First pass: Reset all column bindings and widths
        optimizedTreeTable.getColumns().forEach(column -> {
            column.prefWidthProperty().unbind();
            column.setMinWidth(50);
            column.setPrefWidth(100);
        });

        // Force initial layout pass
        optimizedTreeTable.layout();

        // Get current table width (excluding scrollbar if present)
        double tableWidth = optimizedTreeTable.getWidth();
        double scrollbarWidth = 15;
        if (optimizedTreeTable.lookup(".virtual-flow .scroll-bar:vertical") != null) {
            tableWidth -= scrollbarWidth;
        }

        final double finalTableWidth = tableWidth;

        // Count visible columns for proper width distribution
        long visibleColumns = optimizedTreeTable.getColumns().stream()
                .filter(javafx.scene.control.TreeTableColumn::isVisible)
                .count();

        if (visibleColumns == 0) return;

        // Second pass: Apply proportional widths with proper bindings
        javafx.application.Platform.runLater(() -> {
            // Calculate total proportion to ensure we use 100% of width
            double totalProportion = 0;
            for (javafx.scene.control.TreeTableColumn<?, ?> column : optimizedTreeTable.getColumns()) {
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

            optimizedTreeTable.getColumns().forEach(column -> {
                // Add visibility change listener to each column
                column.visibleProperty().addListener((obs, oldVal, newVal) -> {
                    // Use runLater to ensure layout is updated after visibility change
                    javafx.application.Platform.runLater(() -> {
                        adjustColumnWidths();
                        // Additional refresh after a short delay to ensure proper layout
                        javafx.application.Platform.runLater(() -> {
                            optimizedTreeTable.refresh();
                            optimizedTreeTable.layout();
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
                            optimizedTreeTable.widthProperty().multiply(proportion)
                                    .subtract(scrollbarWidth / visibleColumns)
                    );

                    // Ensure minimum width is maintained
                    double minWidth = Math.max(50, finalTableWidth * proportion * 0.8);
                    column.setMinWidth(minWidth);
                }
            });

            // Force immediate layout update
            optimizedTreeTable.refresh();
            optimizedTreeTable.layout();
        });

        // Final layout pass after a short delay to ensure all changes are applied
        javafx.application.Platform.runLater(() -> {
            // Add a small delay to ensure all bindings are properly applied
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            optimizedTreeTable.layout();
            optimizedTreeTable.refresh();
        });
    }


    /**
     * Returns the optimize tab instance.
     * @return The JavaFX Tab instance
     */
    public Tab getTab() {
        return tab;
    }

    /**
     * Helper class to compute distance statistics for parent assessments
     */
    private static class DistanceStats {
        private final Duration averageOldDistance;
        private final Duration averageOptimizedDistance;
        private final Duration minOldDistance;
        private final Duration minOptimizedDistance;

        public DistanceStats(Duration avgOld, Duration avgOpt, Duration minOld, Duration minOpt) {
            this.averageOldDistance = avgOld;
            this.averageOptimizedDistance = avgOpt;
            this.minOldDistance = minOld;
            this.minOptimizedDistance = minOpt;
        }

        public Duration getAverageOldDistance() { return averageOldDistance; }
        public Duration getAverageOptimizedDistance() { return averageOptimizedDistance; }
        public Duration getMinOldDistance() { return minOldDistance; }
        public Duration getMinOptimizedDistance() { return minOptimizedDistance; }
    }

    /**
     * Calculates distance statistics for a parent assessment based on its children
     */
    private static DistanceStats calculateDistanceStats(MergedAssessment parent) {
        if (parent == null) return null;

        Duration minOldDistance = null;
        Duration minOptimizedDistance = null;
        Duration totalOldDistance = Duration.ZERO;
        Duration totalOptimizedDistance = Duration.ZERO;
        int validDistanceCount = 0;

        Map<MergedAssessment, Integer> childCollisions = parent.getCollisionCountByAssessment();
        if (childCollisions != null && !childCollisions.isEmpty()) {
            for (MergedAssessment child : childCollisions.keySet()) {
                if (child == null) continue;

                Duration oldDistance = calculateTimeDifference(parent, child);
                Duration optimizedDistance = calculateOptimizedTimeDifference(parent, child);

                // Update minimum distances
                if (oldDistance != null && !oldDistance.isNegative()) {
                    if (minOldDistance == null || oldDistance.compareTo(minOldDistance) < 0) {
                        minOldDistance = oldDistance;
                    }
                    totalOldDistance = totalOldDistance.plus(oldDistance);
                    validDistanceCount++;
                }

                if (optimizedDistance != null && !optimizedDistance.isNegative()) {
                    if (minOptimizedDistance == null || optimizedDistance.compareTo(minOptimizedDistance) < 0) {
                        minOptimizedDistance = optimizedDistance;
                    }
                    totalOptimizedDistance = totalOptimizedDistance.plus(optimizedDistance);
                }
            }
        }

        // Calculate averages
        Duration avgOldDistance = validDistanceCount > 0 ?
            totalOldDistance.dividedBy(validDistanceCount) : null;
        Duration avgOptimizedDistance = validDistanceCount > 0 ?
            totalOptimizedDistance.dividedBy(validDistanceCount) : null;

        return new DistanceStats(
            avgOldDistance,
            avgOptimizedDistance,
            minOldDistance,
            minOptimizedDistance
        );
    }

    private static Duration calculateTimeDifference(MergedAssessment exam1, MergedAssessment exam2) {
        // Return null if any required time is missing, so we can distinguish between
        // "no distance" (null) and "zero distance" (Duration.ZERO)
        if (exam1 == null || exam2 == null ||
            exam1.getBegin() == null || exam1.getEnd() == null ||
            exam2.getBegin() == null || exam2.getEnd() == null) {
            return null;
        }

        if (exam1.getBegin().isBefore(exam2.getBegin())) {
            return Duration.between(exam1.getEnd(), exam2.getBegin());
        } else {
            return Duration.between(exam2.getEnd(), exam1.getBegin());
        }
    }

    private static Duration calculateOptimizedTimeDifference(MergedAssessment exam1, MergedAssessment exam2) {
        // Return null if any required time is missing
        if (exam1 == null || exam2 == null ||
            exam1.getOptimizedBegin() == null || exam1.getOptimizedEnd() == null ||
            exam2.getOptimizedBegin() == null || exam2.getOptimizedEnd() == null) {
            return null;
        }

        if (exam1.getOptimizedBegin().isBefore(exam2.getOptimizedBegin())) {
            return Duration.between(exam1.getOptimizedEnd(), exam2.getOptimizedBegin());
        } else {
            return Duration.between(exam2.getOptimizedEnd(), exam1.getOptimizedBegin());
        }
    }

    /**
     * Updates the aggregate values for a parent row based on its visible children.
     * @param parentItem The TreeItem representing a parent row
     */
    private void updateParentAggregates(TreeItem<CollisionEntry> parentItem) {
        if (parentItem == null || parentItem.getValue() == null || !parentItem.getValue().isTitle()) {
            return;
        }

        List<TreeItem<CollisionEntry>> children = parentItem.getChildren();
        if (children.isEmpty()) {
            return;
        }

        // Aggregate values
        int totalCollisions = 0;
        Duration totalOldDistance = Duration.ZERO;
        Duration totalOptDistance = Duration.ZERO;
        Duration minOldDistance = null;
        Duration minOptDistance = null;
        int validOldDistances = 0;
        int validOptDistances = 0;

        for (TreeItem<CollisionEntry> child : children) {
            CollisionEntry entry = child.getValue();
            if (entry == null) continue;

            // Sum up collisions
            totalCollisions += entry.getCollisionCount();

            // Calculate distances
            Duration oldDist = calculateTimeDifference(entry.parentAssessment, entry.childAssessment);
            Duration optDist = calculateOptimizedTimeDifference(entry.parentAssessment, entry.childAssessment);

            // Only process valid (non-null) distances that aren't negative (overlaps)
            if (oldDist != null && !oldDist.isNegative()) {
                totalOldDistance = totalOldDistance.plus(oldDist);
                if (minOldDistance == null || oldDist.compareTo(minOldDistance) < 0) {
                    minOldDistance = oldDist;
                }
                validOldDistances++;
            }

            if (optDist != null && !optDist.isNegative()) {
                totalOptDistance = totalOptDistance.plus(optDist);
                if (minOptDistance == null || optDist.compareTo(minOptDistance) < 0) {
                    minOptDistance = optDist;
                }
                validOptDistances++;
            }
        }

        // Update parent row values - only calculate averages if we have valid distances
        CollisionEntry parentEntry = parentItem.getValue();
        parentEntry.setDynamicStats(
            new DistanceStats(
                validOldDistances > 0 ? totalOldDistance.dividedBy(validOldDistances) : null,
                validOptDistances > 0 ? totalOptDistance.dividedBy(validOptDistances) : null,
                minOldDistance,
                minOptDistance
            )
        );
        parentEntry.setDynamicCollisionCount(totalCollisions);

        // Refresh the TreeTableView to show updated values
        optimizedTreeTable.refresh();
    }

    /**
     * Updates all parent rows in the TreeTableView
     */
    private void updateAllParentAggregates() {
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root == null) return;

        root.getChildren().forEach(this::updateParentAggregates);
    }

    /**
     * Inner class to hold collision entry data for the optimization tree table.
     */
    private static class CollisionEntry {
        private final String exam1QualifiedName;
        private final String exam2QualifiedName;
        private final int collisionCount;
        private final MergedAssessment parentAssessment;
        private final MergedAssessment childAssessment;
        private final boolean isTitle;
        private final DistanceStats distanceStats;
        private DistanceStats dynamicStats;
        private int dynamicCollisionCount;

        // Constructor for title rows (parent assessments)
        public CollisionEntry(MergedAssessment assessment) {
            this.parentAssessment = assessment;
            this.childAssessment = null;
            this.exam1QualifiedName = assessment != null ? assessment.getQualifiedName() : "";
            this.exam2QualifiedName = "";
            this.collisionCount = 0;
            this.isTitle = true;
            this.distanceStats = assessment != null ? calculateDistanceStats(assessment) : null;
        }

        // Constructor for collision rows (child assessments)
        public CollisionEntry(MergedAssessment parentAssessment, MergedAssessment childAssessment, int collisionCount) {
            this.parentAssessment = parentAssessment;
            this.childAssessment = childAssessment;
            this.exam1QualifiedName = parentAssessment.getQualifiedName();
            this.exam2QualifiedName = childAssessment.getQualifiedName();
            this.collisionCount = collisionCount;
            this.isTitle = false;
            this.distanceStats = null; // No distance stats for child rows
        }

        public MergedAssessment getParentAssessment() {return parentAssessment;}
        public MergedAssessment getChildAssessment(){ return childAssessment;}
        public String getExam1QualifiedName() {
            return exam1QualifiedName;
        }

        public String getExam2QualifiedName() {
            return exam2QualifiedName;
        }

        public int getCollisionCount() {
            return isTitle ? dynamicCollisionCount : collisionCount;
        }

        public String getOldBeginTime() {
            if (isTitle) {
                return parentAssessment != null && parentAssessment.getBegin() != null ?
                    parentAssessment.getBegin().format(DATE_TIME_FORMATTER) : "";
            }
            return childAssessment != null && childAssessment.getBegin() != null ?
                childAssessment.getBegin().format(DATE_TIME_FORMATTER) : "";
        }

        public String getOldEndTime() {
            if (isTitle) {
                return parentAssessment != null && parentAssessment.getEnd() != null ?
                    parentAssessment.getEnd().format(DATE_TIME_FORMATTER) : "";
            }
            return childAssessment != null && childAssessment.getEnd() != null ?
                childAssessment.getEnd().format(DATE_TIME_FORMATTER) : "";
        }

        public String getOptimizedBeginTime() {
            if (isTitle) {
                return parentAssessment != null && parentAssessment.getOptimizedBegin() != null ?
                    parentAssessment.getOptimizedBegin().format(DATE_TIME_FORMATTER) : "";
            }
            return childAssessment != null && childAssessment.getOptimizedBegin() != null ?
                childAssessment.getOptimizedBegin().format(DATE_TIME_FORMATTER) : "";
        }

        public String getOptimizedEndTime() {
            if (isTitle) {
                return parentAssessment != null && parentAssessment.getOptimizedEnd() != null ?
                    parentAssessment.getOptimizedEnd().format(DATE_TIME_FORMATTER) : "";
            }
            return childAssessment != null && childAssessment.getOptimizedEnd() != null ?
                childAssessment.getOptimizedEnd().format(DATE_TIME_FORMATTER) : "";
        }

        public String getOldDistance() {
            if (!isTitle && parentAssessment != null && childAssessment != null) {
                Duration distance = calculateTimeDifference(parentAssessment, childAssessment);
                return formatDuration(distance);
            }
            return "";
        }

        public String getOptimizedDistance() {
            if (!isTitle && parentAssessment != null && childAssessment != null) {
                Duration distance = calculateOptimizedTimeDifference(parentAssessment, childAssessment);
                return formatDuration(distance);
            }
            return "";
        }

        public String getOldAverageDistance() {
            if (isTitle && dynamicStats != null) {
                return formatDuration(dynamicStats.getAverageOldDistance());
            }
            return "";
        }

        public String getOptimizedAverageDistance() {
            if (isTitle && dynamicStats != null) {
                return formatDuration(dynamicStats.getAverageOptimizedDistance());
            }
            return "";
        }

        public String getOldMinDistance() {
            if (isTitle && dynamicStats != null) {
                return formatDuration(dynamicStats.getMinOldDistance());
            }
            return "";
        }

        public String getOptimizedMinDistance() {
            if (isTitle && dynamicStats != null) {
                return formatDuration(dynamicStats.getMinOptimizedDistance());
            }
            return "";
        }

        private String formatDuration(Duration duration) {
            if (duration == null) return "";
            long totalMinutes = duration.toMinutes();
            if (totalMinutes < 0) return "Overlap";
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (hours == 0 && minutes == 0) {
                return "0h";
            }
            return minutes == 0 ? String.format("%dh", hours) : String.format("%dh %dm", hours, minutes);
        }

        public boolean isTitle() {
            return isTitle;
        }

        public void setDynamicStats(DistanceStats stats) {
            this.dynamicStats = stats;
        }

        public void setDynamicCollisionCount(int count) {
            this.dynamicCollisionCount = count;
        }
    }

    /**
     * Finds a parent TreeItem by matching the qualified name
     */
    private TreeItem<CollisionEntry> findParentByQualifiedName(String qualifiedName) {
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root == null || qualifiedName == null) return null;

        // Search through all parent items
        for (TreeItem<CollisionEntry> parentItem : root.getChildren()) {
            if (parentItem.getValue() != null &&
                parentItem.getValue().isTitle() &&
                qualifiedName.equals(parentItem.getValue().getExam1QualifiedName())) {
                return parentItem;
            }
        }
        return null;
    }


    public void updateOptimizedTreeTable(MergedAssessment[] assessments) {
        if (assessments == null || assessments.length == 0) {
            return;
        }

        // Create root item
        TreeItem<CollisionEntry> root = new TreeItem<>(new CollisionEntry(null));
        optimizedTreeTable.setRoot(root);

        // Create entries for each assessment
        for (MergedAssessment assessment : assessments) {
            if (assessment == null) continue;

            // Create parent entry
            TreeItem<CollisionEntry> parentItem = new TreeItem<>(new CollisionEntry(assessment));
            root.getChildren().add(parentItem);

            // Get collision data for this assessment
            Map<MergedAssessment, Integer> collisions = assessment.getCollisionCountByAssessment();
            if (collisions != null) {
                // Add child entries for each collision
                for (Map.Entry<MergedAssessment, Integer> collision : collisions.entrySet()) {
                    MergedAssessment childAssessment = collision.getKey();
                    int collisionCount = collision.getValue();

                    // Create child entry with collision count
                    TreeItem<CollisionEntry> childItem = new TreeItem<>(
                        new CollisionEntry(assessment, childAssessment, collisionCount)
                    );
                    parentItem.getChildren().add(childItem);
                }
            }

            // Expand parent items by default unless "Show Only Assessments" is checked
            parentItem.setExpanded(!showOnlyAssessmentsCheckbox.isSelected());
        }

        // Update the table display
        updateTable();
    }

    /**
     * Updates the sort dropdowns based on the current column sort state
     */
    private void updateSortDropdownsFromColumnHeader(TreeTableColumn<CollisionEntry, ?> column) {
        if (column == null) return;

        String columnName = column.getText();
        TreeTableColumn.SortType sortType = column.getSortType();

        // Convert sort type to direction string
        String direction = (sortType == TreeTableColumn.SortType.ASCENDING) ? ASCENDING : DESCENDING;

        // Determine which dropdown to update based on column type
        if (isExam1Column(columnName)) {
            // Update Exam 1 dropdowns
            isUpdating = true;
            try {
                sortExam1Box.setValue(mapColumnNameToDropdownValue(columnName));
                sortDirectionExam1Box.setValue(direction);
            } finally {
                isUpdating = false;
            }
        } else if (isExam2Column(columnName)) {
            // Update Exam 2 dropdowns
            isUpdating = true;
            try {
                sortExam2Box.setValue(mapColumnNameToDropdownValue(columnName));
                sortDirectionExam2Box.setValue(direction);
            } finally {
                isUpdating = false;
            }
        }
    }

    /**
     * Finds a column by its name
     */
    private TreeTableColumn<CollisionEntry, ?> findColumnByName(String name) {
        for (TreeTableColumn<CollisionEntry, ?> column : optimizedTreeTable.getColumns()) {
            if (column.getText().equals(name)) {
                return column;
            }
        }
        return null;
    }

    private void onOptimizeButtonClicked(Boolean supervisor, boolean room) {
        optimizedAssessments = examGUI.optimizeStart(supervisor, room);
        if (optimizedAssessments != null) {
            updateOptimizedTreeTable(optimizedAssessments);
        } else {
            System.out.println("Da stimmt doch irgendwas nicht");
        }
    }

    /**
     * Applies sorting to the tree table based on column name and sort type.
     *
     * @param treeTable The tree table to sort
     * @param columnName The name of the column to sort by
     * @param sortType The sort direction (ascending or descending)
     */
    private void applySortToTreeTable(TreeTableView<CollisionEntry> treeTable, String columnName, TreeTableColumn.SortType sortType) {
        TreeTableColumn<CollisionEntry, ?> column = findColumnByName(columnName);
        if (column != null) {
            column.setSortType(sortType);
            treeTable.getSortOrder().setAll(column);
            treeTable.sort();
        }
    }

    /**
     * Checks if the given column name is an Exam 1 column.
     *
     * @param columnName The column name to check
     * @return true if the column is an Exam 1 column, false otherwise
     */
    private boolean isExam1Column(String columnName) {
        return columnName.startsWith("Exam1") ||
               columnName.equals("Collision Count") ||
               columnName.equals("Old Begin") ||
               columnName.equals("Old End") ||
               columnName.equals("Old Average Distance") ||
               columnName.equals("Old Min. Distance") ||
               columnName.equals("Optimize Begin") ||
               columnName.equals("Optimize End") ||
               columnName.equals("Optimize Average Distance") ||
               columnName.equals("Optimize Min. Distance");
    }

    /**
     * Checks if the given column name is an Exam 2 column.
     *
     * @param columnName The column name to check
     * @return true if the column is an Exam 2 column, false otherwise
     */
    private boolean isExam2Column(String columnName) {
        return columnName.startsWith("Exam2") ||
               columnName.equals("Collision Count") ||
               columnName.equals("Old Begin") ||
               columnName.equals("Old End") ||
               columnName.equals("Old Distance") ||
               columnName.equals("Optimize Begin") ||
               columnName.equals("Optimize End") ||
               columnName.equals("Optimize Distance");
    }

    /**
     * Maps a column name to the corresponding dropdown value.
     *
     * @param columnName The column name to map
     * @return The dropdown value corresponding to the column name
     */
    private String mapColumnNameToDropdownValue(String columnName) {
        // Direct mapping for most column names
        if (columnName.equals("Exam 1")) return "Exam1 Name";
        if (columnName.equals("Exam 2")) return "Exam2 Name";

        // For other columns, use the same name as in the dropdown
        return columnName;
    }

    /**
     * Initialize the mapping between dropdown options and their corresponding column names.
     * This is used to associate dropdown selections with the actual table columns for sorting.
     */
    private void initializeSortColumnMappings() {
        // Map SortExam1 dropdown options to column names
        sortExam1ColumnMap.put("Exam1 Name", "Exam 1");
        sortExam1ColumnMap.put("Old Average Distance", "Old Avg. Distance");
        sortExam1ColumnMap.put("Old Min. Distance", "Old Min. Distance");
        sortExam1ColumnMap.put("Optimize Average Distance", "Opt Avg. Distance");
        sortExam1ColumnMap.put("Optimize Min. Distance", "Opt Min. Distance");

        // Map SortExam2 dropdown options to column names
        sortExam2ColumnMap.put("Exam2 Name", "Exam 2");
        sortExam2ColumnMap.put("Old Distance", "Old Distance");
        sortExam2ColumnMap.put("Optimize Distance", "Opt Distance");
    }
}
