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
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import javafx.collections.ListChangeListener;
import java.text.Collator;
import java.util.*;

/**
 * Tab for optimization functionalities in the exam scheduling application.
 */
public class OptimizeTab {
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
    private static final List<String> LOCKED_COLUMNS = Arrays.asList("Exam 1", "Exam 2");
    private static final List<String> OLD_COLUMNS = Arrays.asList(
        "Old Begin", "Old End", "Old Distance", "Old Avg. Distance", "Old Min. Distance"
    );
    private static final List<String> ALL_COLUMNS = Arrays.asList(
        "Exam 1", "Exam 2", "Collision Count",
        "Old Begin", "Old End", "Old Distance", "Old Avg. Distance", "Old Min. Distance",
        "Opt Begin", "Opt End", "Opt Distance", "Opt Avg Distance", "Opt Min Distance"
    );

    // Sort direction constants
    private static final String ASCENDING = "Ascending";
    private static final String DESCENDING = "Descending";

    private final Collator germanCollator = Collator.getInstance(Locale.GERMAN);

    public OptimizeTab(ExamGUI examGUI) {
        this.examGUI = examGUI;
        tab = new Tab("Optimize");
        tab.setClosable(false);

        // Create basic layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Create a ScrollPane to wrap the content
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Create optimize button section
        HBox buttonSection = new HBox();
        buttonSection.setAlignment(Pos.BASELINE_LEFT);
        buttonSection.setPadding(new Insets(0, 0, 10, 0));

        Button optimizeButton = new Button("Run Optimization");
        optimizeButton.setStyle(
                "-fx-background-color:  #2ecc71 ;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"

        );

        optimizeButton.setOnAction(e -> onOptimizeButtonClicked());

        CheckBox considerRoom = new CheckBox("Consider Rooms in Optimization");
        considerRoom.setStyle("-fx-text-fill: #2c3e50;");

        CheckBox considerSupervisor = new CheckBox("Consider Supervisor in Optimization");
        considerSupervisor.setStyle("-fx-text-fill: #2c3e50;");

        Label optiLabelSave = new Label();
        optiLabelSave.setText("Save the Optimized File");

        TextField optimizedSavePath = new TextField();
        optimizedSavePath.setPrefWidth(300);
        optimizedSavePath.setPrefHeight(28);

        Button optSave = new Button("Save Optimized");
        optSave.setStyle(
                        "-fx-background-color:  #3498db ;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-radius: 4px;"

        );
        optSave.setOnAction(e -> {

        });






        //Distance between button and checkboxes
        buttonSection.getChildren().addAll(optimizeButton, considerRoom, considerSupervisor, optiLabelSave, optimizedSavePath, optSave);
        HBox.setMargin(considerRoom, new Insets(0, 0, 0, 20));
        HBox.setMargin(considerSupervisor, new Insets(0, 0, 0, 10));
        HBox.setMargin(optiLabelSave, new Insets(0, 0, 0, 30));
        HBox.setMargin(optimizedSavePath, new Insets(0, 0, 0, 10));
        HBox.setMargin(optSave, new Insets(0, 0, 0, 10));


        // Create filter section
        VBox filterSection = createFilterSection();
        filterSection.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Create and configure TreeTableView in its container
        VBox tableSection = createTableSection();

        // Add all sections to the content
        content.getChildren().addAll(buttonSection, filterSection, tableSection);

        // Set the ScrollPane as the tab content
        tab.setContent(scrollPane);

        // Initialize listeners for filters
        setupFilterListeners();
    }

    private VBox createFilterSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        // Section header
        Label sectionTitle = new Label("Filter and Sort");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

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

        // Create sort comboboxes
        sortExam1Box = createStyledComboBox();
        sortExam1Box.setPromptText("Select Exam 1 sort...");
        sortExam1Box.setItems(FXCollections.observableArrayList(
                "Exam1 Name", "Old Begin", "Old End", "Old Average Distance", "Old Min Distance",
                "Optimize Begin", "Optimize End", "Optimize Average Distance", "Optimize Min Distance"
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

        // Create and style labels
        Label filterExam1Label = new Label("Filter Exam 1");
        Label filterExam2Label = new Label("Filter Exam 2");
        Label sortExam1Label = new Label("Sort Exam 1");
        Label sortDirectionExam1Label = new Label("Sort Direction Exam 1");
        Label sortExam2Label = new Label("Sort Exam 2");
        Label sortDirectionExam2Label = new Label("Sort Direction Exam 2");
        Label maxDistanceLabel = new Label("Max Opt Distance (hours)");

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

    private VBox createTableSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        VBox.setVgrow(section, Priority.ALWAYS);

        // Initialize and setup the TreeTableView
        setupTreeTableView();
        setupTableSizeListener();
        VBox.setVgrow(optimizedTreeTable, Priority.ALWAYS);

        // Section title
        Label sectionTitle = new Label("Optimization Results");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        // Section description
        Label sectionDescription = new Label("Results of exam schedule optimization.");
        sectionDescription.setStyle("-fx-text-fill: #2c3e50;");

        // Create controls container
        HBox controlsContainer = new HBox(20);
        controlsContainer.setAlignment(Pos.CENTER_LEFT);

        // Add checkboxes for filtering
        hideNullTimesCheckbox = new CheckBox("Hide Entries with No Times");
        hideNullTimesCheckbox.setStyle("-fx-text-fill: #2c3e50;");
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
                TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
                if (root != null) {
                    updateTable();
                }
            }
        });

        showOnlyAssessmentsCheckbox = new CheckBox("Show Only Assessments");
        showOnlyAssessmentsCheckbox.setStyle("-fx-text-fill: #2c3e50;");
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

        showOnlyWithCollisionsCheckbox = new CheckBox("Show Only with Collisions");
        showOnlyWithCollisionsCheckbox.setStyle("-fx-text-fill: #2c3e50;");
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
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 5px 10px;" +
            "-fx-cursor: hand;" +
            "-fx-border-radius: 4px;"
        );

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

    private void setupColumnMenuItems() {
        // Clear existing items
        columnMenuItems.clear();
        columnsMenuButton.getItems().clear();

        // Create menu items for each column
        for (String columnName : ALL_COLUMNS) {
            CheckMenuItem item = new CheckMenuItem(columnName);

            // Set initial state
            boolean isLocked = LOCKED_COLUMNS.contains(columnName);
            boolean isOldColumn = OLD_COLUMNS.contains(columnName);

            // Set initial visibility state
            item.setSelected(!isOldColumn); // All columns visible by default except "Old" columns
            item.setDisable(isLocked); // Lock "Exam 1" and "Exam 2" columns

            // Store initial visibility state
            columnVisibilityStates.put(columnName, !isOldColumn);

            // Add listener for visibility changes
            item.selectedProperty().addListener((obs, old, newValue) -> {
                if (!isLocked) { // Only update if the column isn't locked
                    columnVisibilityStates.put(columnName, newValue);
                    updateColumnVisibility();
                }
            });

            // Store menu item reference
            columnMenuItems.put(columnName, item);
            columnsMenuButton.getItems().add(item);
        }

        // Add separator after locked columns and old columns
        List<MenuItem> items = new ArrayList<>(columnsMenuButton.getItems());
        columnsMenuButton.getItems().clear();

        int i = 0;
        for (MenuItem item : items) {
            columnsMenuButton.getItems().add(item);
            // Add separators after exam columns (index 1) and after old columns (index 7)
            if (i == 1 || i == 7) {
                columnsMenuButton.getItems().add(new SeparatorMenuItem());
            }
            i++;
        }

        // Initial update of column visibility
        updateColumnVisibility();
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
        // Update table with new data
        optimizedTreeTable.setRoot(newRoot);

        sortTreeItems(newRoot);


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

        MergedAssessment a1 = entry1.getParentAssessment();
        MergedAssessment a2 = entry2.getParentAssessment();

        if (a1 == null || a2 == null) return 0;

        return switch (property) {
            case "Exam 1 Name", "Exam Name" -> {
                if (entry1.isTitle()) {
                    // For title rows (Exam 1), sort by primary exam
                    yield compareGermanStrings(a1.getQualifiedName(), a2.getQualifiedName());
                } else {
                    // For child rows (Exam 2), sort by the colliding assessment
                    MergedAssessment c1 = entry1.getParentAssessment();
                    MergedAssessment c2 = entry1.getParentAssessment();
                    if (c2 != null) {
                        yield compareGermanStrings(c1.getQualifiedName(), c2.getQualifiedName());
                    }
                    yield 0;
                }
            }
            case "Exam 2 Name" -> {
                // This is only for child rows (Exam 2)
                if (!entry1.isTitle()) {
                    MergedAssessment c1 = entry1.getChildAssessment();
                    MergedAssessment c2 = entry2.getChildAssessment();
                    if (c1 != null && c2 != null) {
                        yield compareGermanStrings(c1.getQualifiedName(), c2.getQualifiedName());
                    }
                }
                yield 0;
            }
            case "Collision Count" -> Integer.compare(
                    entry1.getCollisionCount(),
                    entry2.getCollisionCount()
            );
            case "Old Begin" -> {
                MergedAssessment compareA1 = entry1.isTitle() ? a1 : entry1.getChildAssessment();
                MergedAssessment compareA2 = entry2.isTitle() ? a2 : entry2.getChildAssessment();

                if (compareA1.getBegin() == null && compareA2.getBegin() == null) yield 0;
                if (compareA1.getBegin() == null) yield -1;
                if (compareA2.getBegin() == null) yield 1;

                yield compareA1.getBegin().compareTo(compareA2.getBegin());
            }
            case "Old End" -> {
                MergedAssessment compareA1 = entry1.isTitle() ? a1 : entry1.getChildAssessment();
                MergedAssessment compareA2 = entry2.isTitle() ? a2 : entry2.getChildAssessment();

                if (compareA1.getEnd() == null && compareA2.getEnd() == null) yield 0;
                if (compareA1.getEnd() == null) yield -1;
                if (compareA2.getEnd() == null) yield 1;

                yield compareA1.getEnd().compareTo(compareA2.getEnd());
            }
            case "Opt Begin" -> {
                MergedAssessment compareA1 = entry1.isTitle() ? a1 : entry1.getChildAssessment();
                MergedAssessment compareA2 = entry2.isTitle() ? a2 : entry2.getChildAssessment();

                if (compareA1.getOptimizedBegin() == null && compareA2.getOptimizedBegin() == null) yield 0;
                if (compareA1.getOptimizedBegin() == null) yield -1;
                if (compareA2.getOptimizedBegin() == null) yield 1;

                yield compareA1.getOptimizedBegin().compareTo(compareA2.getOptimizedBegin());
            }
            case "Opt End" -> {
                MergedAssessment compareA1 = entry1.isTitle() ? a1 : entry1.getChildAssessment();
                MergedAssessment compareA2 = entry2.isTitle() ? a2 : entry2.getChildAssessment();

                if (compareA1.getOptimizedEnd() == null && compareA2.getOptimizedEnd() == null) yield 0;
                if (compareA1.getOptimizedEnd() == null) yield -1;
                if (compareA2.getOptimizedEnd() == null) yield 1;

                yield compareA1.getOptimizedEnd().compareTo(compareA2.getOptimizedEnd());
            }
            case "Old Distance" -> {
                if (!entry1.isTitle()) {
                    // Extract numeric values from distance strings (format: "Xh Ym")
                    String dist1 = entry1.getOldDistance();
                    String dist2 = entry2.getOldDistance();

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
            case "Opt Distance" -> {
                if (!entry1.isTitle()) {
                    // Extract numeric values from distance strings (format: "Xh Ym")
                    String dist1 = entry1.getOptimizedDistance();
                    String dist2 = entry2.getOptimizedDistance();

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
            case "Old Avg. Distance" -> {
                if (entry1.isTitle()) {
                    String avg1 = entry1.getOldAverageDistance();
                    String avg2 = entry2.getOldAverageDistance();

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
            case "Opt Avg. Distance" -> {
                if (entry1.isTitle()) {
                    String avg1 = entry1.getOptimizedAverageDistance();
                    String avg2 = entry2.getOptimizedAverageDistance();

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
            case "Old Min. Distance" -> {
                if (entry1.isTitle()) {
                    String min1 = entry1.getOldMinDistance();
                    String min2 = entry2.getOldMinDistance();

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
            case "Opt Min. Distance" -> {
                if (entry1.isTitle()) {
                    String min1 = entry1.getOptimizedMinDistance();
                    String min2 = entry2.getOptimizedMinDistance();

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

    /*private void restoreExpansionStates(Map<String, Boolean> states) {
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root != null) {
            restoreExpansionStatesFromMap(root, states);
        }
    }*/

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

    private String getItemKey(CollisionEntry entry) {
        if (entry == null) return null;
        return entry.getExam1QualifiedName() + "|" + entry.getExam2QualifiedName();
    }

    private void sortItems(List<TreeItem<CollisionEntry>> items, String criteria, String direction) {
        items.sort((item1, item2) -> {
            int result = compareItems(item1.getValue(), item2.getValue(), criteria);
            return direction.equals(ASCENDING) ? result : -result;
        });
    }

    private int compareItems(CollisionEntry e1, CollisionEntry e2, String criteria) {
        return switch (criteria) {
            case "Exam1 Name" -> germanCollator.compare(e1.getExam1QualifiedName(), e2.getExam1QualifiedName());
            case "Collision Count" -> Integer.compare(e1.getCollisionCount(), e2.getCollisionCount());
            case "Old Begin" -> compareTimes(e1.getOldBeginTime(), e2.getOldBeginTime());
            case "Old End" -> compareTimes(e1.getOldEndTime(), e2.getOldEndTime());
            case "Optimized Begin" -> compareTimes(e1.getOptimizedBeginTime(), e2.getOptimizedBeginTime());
            case "Optimized End" -> compareTimes(e1.getOptimizedEndTime(), e2.getOptimizedEndTime());
            default -> 0;
        };
    }

    private int compareTimes(String time1, String time2) {
        if (time1.isEmpty() && time2.isEmpty()) return 0;
        if (time1.isEmpty()) return 1;
        if (time2.isEmpty()) return -1;
        return time1.compareTo(time2);
    }

    private void setupTreeTableView() {
        optimizedTreeTable = new TreeTableView<>();
        optimizedTreeTable.setShowRoot(false);
        optimizedTreeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

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

        // Configure base column properties
        double examColumnMaxWidth = 350.0;
        double examColumnMinWidth = 200.0;
        double defaultColumnWidth = 120.0;
        double minColumnWidth = 100.0;

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
        exam1Col.setReorderable(false);
        exam1Col.setMaxWidth(examColumnMaxWidth);
        exam1Col.setMinWidth(examColumnMinWidth);
        exam1Col.setPrefWidth(examColumnMinWidth);

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
        exam2Col.setReorderable(false);
        exam2Col.setMaxWidth(examColumnMaxWidth);
        exam2Col.setMinWidth(examColumnMinWidth);
        exam2Col.setPrefWidth(examColumnMinWidth);

        TreeTableColumn<CollisionEntry, String> collisionCountCol = new TreeTableColumn<>("Collision Count");
        collisionCountCol.setCellValueFactory(param -> {
            if (param.getValue() == null || param.getValue().getValue() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(String.valueOf(param.getValue().getValue().getCollisionCount()));
        });
        collisionCountCol.setReorderable(false);
        collisionCountCol.setPrefWidth(100);
        collisionCountCol.setMinWidth(90);

        // Time columns setup function with null safety
        BiConsumer<TreeTableColumn<CollisionEntry, String>, Boolean> setupTimeColumn = (column, isVisible) -> {
            column.setReorderable(false);
            column.setPrefWidth(defaultColumnWidth);
            column.setMinWidth(minColumnWidth);
            column.setVisible(isVisible);
        };

        // Create and configure time-related columns
        TreeTableColumn<CollisionEntry, String> oldBeginCol = new TreeTableColumn<>("Old Begin");
        oldBeginCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldBeginTime() : ""));
        setupTimeColumn.accept(oldBeginCol, false);

        TreeTableColumn<CollisionEntry, String> optimizedBeginCol = new TreeTableColumn<>("Opt Begin");
        optimizedBeginCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedBeginTime() : ""));
        setupTimeColumn.accept(optimizedBeginCol, true);

        TreeTableColumn<CollisionEntry, String> oldEndCol = new TreeTableColumn<>("Old End");
        oldEndCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldEndTime() : ""));
        setupTimeColumn.accept(oldEndCol, false);

        TreeTableColumn<CollisionEntry, String> optimizedEndCol = new TreeTableColumn<>("Opt End");
        optimizedEndCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedEndTime() : ""));
        setupTimeColumn.accept(optimizedEndCol, true);

        TreeTableColumn<CollisionEntry, String> oldDistanceCol = new TreeTableColumn<>("Old Distance");
        oldDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldDistance() : ""));
        setupTimeColumn.accept(oldDistanceCol, false);

        TreeTableColumn<CollisionEntry, String> optimizedDistanceCol = new TreeTableColumn<>("Opt Distance");
        optimizedDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedDistance() : ""));
        setupTimeColumn.accept(optimizedDistanceCol, true);

        TreeTableColumn<CollisionEntry, String> oldAvgDistanceCol = new TreeTableColumn<>("Old Avg. Distance");
        oldAvgDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldAverageDistance() : ""));
        setupTimeColumn.accept(oldAvgDistanceCol, false);

        TreeTableColumn<CollisionEntry, String> optimizedAvgDistanceCol = new TreeTableColumn<>("Opt Avg Distance");
        optimizedAvgDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedAverageDistance() : ""));
        setupTimeColumn.accept(optimizedAvgDistanceCol, true);

        TreeTableColumn<CollisionEntry, String> oldMinDistanceCol = new TreeTableColumn<>("Old Min. Distance");
        oldMinDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOldMinDistance() : ""));
        setupTimeColumn.accept(oldMinDistanceCol, false);

        TreeTableColumn<CollisionEntry, String> optimizedMinDistanceCol = new TreeTableColumn<>("Opt Min Distance");
        optimizedMinDistanceCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getValue() != null ? param.getValue().getValue().getOptimizedMinDistance() : ""));
        setupTimeColumn.accept(optimizedMinDistanceCol, true);

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

        // Disable sorting for specific columns
        collisionCountCol.setSortable(false);
        oldBeginCol.setSortable(false);
        optimizedBeginCol.setSortable(false);
        oldEndCol.setSortable(false);
        optimizedEndCol.setSortable(false);

        // Implement custom sorting for exam columns
        exam1Col.setComparator(germanCollator::compare);
        exam2Col.setComparator(germanCollator::compare);

        // Add listener to handle column width adjustments when visibility changes
        optimizedTreeTable.getColumns().addListener((ListChangeListener<TreeTableColumn<CollisionEntry, ?>>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved()) {
                    adjustColumnWidths();
                }
            }
        });

        // Add sort listeners to columns to sync with dropdowns
        exam1Col.sortTypeProperty().addListener((obs, oldSort, newSort) -> {
            if (newSort != null) {
                sortExam1Box.setValue("Exam1 Name");
                sortDirectionExam1Box.setValue(newSort == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
            }
        });

        exam2Col.sortTypeProperty().addListener((obs, oldSort, newSort) -> {
            if (newSort != null) {
                sortExam2Box.setValue("Exam2 Name");
                sortDirectionExam2Box.setValue(newSort == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
            }
        });

        // Add sort order listener to the table
        optimizedTreeTable.getSortOrder().addListener((ListChangeListener<TreeTableColumn<CollisionEntry, ?>>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (TreeTableColumn<CollisionEntry, ?> column : c.getAddedSubList()) {
                        updateSortDropdowns(column);
                    }
                }
            }
        });
    }

    private void adjustColumnWidths() {
        // Count visible columns (excluding Exam 1, Exam 2, and Collision Count)
        long visibleColumns = optimizedTreeTable.getColumns().stream()
            .filter(col -> col.isVisible() &&
                   !col.getText().equals("Exam 1") &&
                   !col.getText().equals("Exam 2") &&
                   !col.getText().equals("Collision Count"))
            .count();

        if (visibleColumns > 0) {
            // Calculate available width for dynamic columns
            double tableWidth = optimizedTreeTable.getWidth();
            double reservedWidth = 500; // Space for Exam columns and collision count
            double availableWidth = Math.max(tableWidth - reservedWidth, 0);
            double columnWidth = Math.max(availableWidth / visibleColumns, 100);

            // Apply the calculated width to visible columns
            optimizedTreeTable.getColumns().stream()
                .filter(TreeTableColumn::isVisible)
                .forEach(col -> {
                    String colName = col.getText();
                    if (!colName.equals("Exam 1") && !colName.equals("Exam 2") && !colName.equals("Collision Count")) {
                        col.setPrefWidth(columnWidth);
                    }
                });
        }
    }

    private void setupTableSizeListener() {
        optimizedTreeTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                adjustColumnWidths();
            }
        });
    }

    private void updateColumnVisibility() {
        for (TreeTableColumn<CollisionEntry, ?> column : optimizedTreeTable.getColumns()) {
            String columnName = column.getText();
            column.setVisible(columnVisibilityStates.getOrDefault(columnName, true));
        }
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

    /**
     * Updates the TreeTableView after optimization
     */
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
    private void updateSortDropdowns(TreeTableColumn<CollisionEntry, ?> column) {
        String columnName = column.getText();
        TreeTableColumn.SortType sortType = column.getSortType();

        if ("Exam 1".equals(columnName)) {
            sortExam1Box.setValue("Exam1 Name");
            sortDirectionExam1Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        } else if ("Exam 2".equals(columnName)) {
            sortExam2Box.setValue("Exam2 Name");
            sortDirectionExam2Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        }  else if ("Old Distance".equals(columnName)) {
            sortExam2Box.setValue("Old Distance");
            sortDirectionExam2Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        }else if ("Opt Distance".equals(columnName)) {
            sortExam2Box.setValue("Opt Distance");
            sortDirectionExam2Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        } else if ("Old Avg Distance".equals(columnName)) {
            sortExam1Box.setValue("Old Avg. Distance");
            sortDirectionExam1Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        } else if ("Opt Avg Distance".equals(columnName)) {
            sortExam1Box.setValue("Opt Avg Distance");
            sortDirectionExam1Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        } else if ("Old Min Distance".equals(columnName)) {
            sortExam1Box.setValue("Old Min Distance");
            sortDirectionExam1Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
        } else if ("Opt Min Distance".equals(columnName)) {
            sortExam1Box.setValue("Opt Min Distance");
            sortDirectionExam1Box.setValue(sortType == TreeTableColumn.SortType.ASCENDING ? ASCENDING : DESCENDING);
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

    private void onOptimizeButtonClicked() {
        optimizedAssessments = examGUI.optimizeStart();
        if (optimizedAssessments != null) {
            updateOptimizedTreeTable(optimizedAssessments);
        } else {
            System.out.println("Da stimmt doch irgendwas nicht");
        }
    }
}
