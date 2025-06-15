import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import data.MergedAssessment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import javafx.collections.ListChangeListener;

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


    private MergedAssessment [] optimizedAssessments;

    // Checkboxes for filtering
    private CheckBox hideNullTimesCheckbox;
    private CheckBox showOnlyAssessmentsCheckbox;
    private CheckBox showOnlyWithCollisionsCheckbox;

    // Column visibility controls
    private MenuButton columnsMenuButton;
    private Map<String, CheckMenuItem> columnMenuItems = new HashMap<>();
    private Map<String, Boolean> columnVisibilityStates = new HashMap<>();

    // Map to store expansion states before "Show Only Assessments" is activated
    private final Map<String, Boolean> preCheckboxExpansionStates = new HashMap<>();

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

    public OptimizeTab(ExamGUI examGUI) {
        this.examGUI = examGUI;
        tab = new Tab("Optimize");
        tab.setClosable(false);

        // Create basic layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Create optimize button section
        HBox buttonSection = new HBox();
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.setPadding(new Insets(0, 0, 10, 0));

        Button optimizeButton = new Button("Run Optimization");
        optimizeButton.setStyle(
            "-fx-background-color: #2ecc71;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10px 20px;" +
            "-fx-cursor: hand;" +
            "-fx-border-radius: 4px;"
        );

        optimizeButton.setOnAction(e -> onOptimizeButtonClicked());

        buttonSection.getChildren().add(optimizeButton);

        // Create filter section
        VBox filterSection = createFilterSection();
        filterSection.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Create and configure TreeTableView in its container
        VBox tableSection = createTableSection();

        // Add all sections to the content
        content.getChildren().addAll(buttonSection, filterSection, tableSection);

        tab.setContent(content);

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
        Label maxDistanceLabel = new Label("Max Distance (hours)");

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
        hideNullTimesCheckbox.selectedProperty().addListener((obs, old, newValue) -> updateTable());

        showOnlyAssessmentsCheckbox = new CheckBox("Show Only Assessments");
        showOnlyAssessmentsCheckbox.setStyle("-fx-text-fill: #2c3e50;");
        showOnlyAssessmentsCheckbox.selectedProperty().addListener((obs, old, newValue) -> updateTable());

        showOnlyWithCollisionsCheckbox = new CheckBox("Show Only with Collisions");
        showOnlyWithCollisionsCheckbox.setStyle("-fx-text-fill: #2c3e50;");
        showOnlyWithCollisionsCheckbox.selectedProperty().addListener((obs, old, newValue) -> updateTable());

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

            if (!newVal.equals(".") && !newVal.endsWith(".")) {
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
    }

    private void updateTable() {
        // Save expansion states before updating
        Map<String, Boolean> expansionStates = saveExpansionStates();

        // Get root and filter based on checkboxes
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root == null) return;

        // Create a filtered copy of the root's children
        List<TreeItem<CollisionEntry>> filteredItems = new ArrayList<>(root.getChildren());

        // Apply Hide Entries with No Times filter
        if (hideNullTimesCheckbox.isSelected()) {
            filteredItems.removeIf(item -> {
                CollisionEntry entry = item.getValue();
                boolean hasNoOldTimes = entry.getOldBeginTime().isEmpty() && entry.getOldEndTime().isEmpty();
                boolean hasNoOptimizedTimes = entry.getOptimizedBeginTime().isEmpty() && entry.getOptimizedEndTime().isEmpty();
                return hasNoOldTimes && hasNoOptimizedTimes;
            });
        }

        // Apply Show Only with Collisions filter
        if (showOnlyWithCollisionsCheckbox.isSelected()) {
            filteredItems.removeIf(item -> item.getValue().getCollisionCount() == 0);
        }

        // Apply Show Only Assessments filter
        if (showOnlyAssessmentsCheckbox.isSelected()) {
            // Save current expansion states before collapsing
            saveExpansionStatesToMap(root, preCheckboxExpansionStates);

            // Collapse all items
            filteredItems.forEach(item -> item.setExpanded(false));
        } else {
            // Restore previous expansion states
            restoreExpansionStatesFromMap(root, preCheckboxExpansionStates);
        }

        // Apply text filters
        String exam1Filter = filterExam1Field.getText().toLowerCase();
        String exam2Filter = filterExam2Field.getText().toLowerCase();

        if (!exam1Filter.isEmpty() || !exam2Filter.isEmpty()) {
            filteredItems.removeIf(item -> {
                CollisionEntry entry = item.getValue();
                boolean matchesExam1 = exam1Filter.isEmpty() ||
                    entry.getExam1QualifiedName().toLowerCase().contains(exam1Filter);
                boolean matchesExam2 = exam2Filter.isEmpty() ||
                    entry.getExam2QualifiedName().toLowerCase().contains(exam2Filter);
                return !matchesExam1 || !matchesExam2;
            });
        }

        // Apply max distance filter if specified
        String maxDistanceText = maxDistanceField.getText();
        if (!maxDistanceText.isEmpty()) {
            try {
                double maxDistance = Double.parseDouble(maxDistanceText);
                filteredItems.removeIf(item -> {
                    String distanceStr = item.getValue().getOptimizedDistance()
                        .replaceAll("[^0-9.]", ""); // Remove non-numeric chars
                    if (distanceStr.isEmpty()) return false;
                    double distance = Double.parseDouble(distanceStr);
                    return distance > maxDistance;
                });
            } catch (NumberFormatException e) {
                // Invalid number format, skip filtering
            }
        }

        // Update root with filtered items
        root.getChildren().setAll(filteredItems);

        // Apply sorting
        String sortCriteria = sortExam1Box.getValue();
        String sortDirection = sortDirectionExam1Box.getValue();

        if (sortCriteria != null && !sortCriteria.isEmpty()) {
            sortItems(root.getChildren(), sortCriteria, sortDirection);
        }

        // Restore expansion states
        restoreExpansionStates(expansionStates);

        // Update parent aggregates
        updateAllParentAggregates();
    }

    private Map<String, Boolean> saveExpansionStates() {
        Map<String, Boolean> states = new HashMap<>();
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root != null) {
            saveExpansionStatesToMap(root, states);
        }
        return states;
    }

    private void saveExpansionStatesToMap(TreeItem<CollisionEntry> item, Map<String, Boolean> states) {
        if (item == null || item.getValue() == null) return;

        String key = getItemKey(item.getValue());
        if (key != null) {
            states.put(key, item.isExpanded());
        }

        for (TreeItem<CollisionEntry> child : item.getChildren()) {
            saveExpansionStatesToMap(child, states);
        }
    }

    private void restoreExpansionStates(Map<String, Boolean> states) {
        TreeItem<CollisionEntry> root = optimizedTreeTable.getRoot();
        if (root != null) {
            restoreExpansionStatesFromMap(root, states);
        }
    }

    private void restoreExpansionStatesFromMap(TreeItem<CollisionEntry> item, Map<String, Boolean> states) {
        if (item == null || item.getValue() == null) return;

        String key = getItemKey(item.getValue());
        if (key != null) {
            Boolean wasExpanded = states.get(key);
            if (wasExpanded != null) {
                item.setExpanded(wasExpanded);
            }
        }

        for (TreeItem<CollisionEntry> child : item.getChildren()) {
            restoreExpansionStatesFromMap(child, states);
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
        switch (criteria) {
            case "Exam1 Name":
                return e1.getExam1QualifiedName().compareTo(e2.getExam1QualifiedName());
            case "Collision Count":
                return Integer.compare(e1.getCollisionCount(), e2.getCollisionCount());
            case "Old Begin":
                return compareTimes(e1.getOldBeginTime(), e2.getOldBeginTime());
            case "Old End":
                return compareTimes(e1.getOldEndTime(), e2.getOldEndTime());
            case "Optimized Begin":
                return compareTimes(e1.getOptimizedBeginTime(), e2.getOptimizedBeginTime());
            case "Optimized End":
                return compareTimes(e1.getOptimizedEndTime(), e2.getOptimizedEndTime());
            default:
                return 0;
        }
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
        optimizedTreeTable.getColumns().addAll(
            exam1Col, exam2Col, collisionCountCol,
            oldBeginCol, optimizedBeginCol,
            oldEndCol, optimizedEndCol,
            oldDistanceCol, optimizedDistanceCol,
            oldAvgDistanceCol, optimizedAvgDistanceCol,
            oldMinDistanceCol, optimizedMinDistanceCol
        );

        // Add listener to handle column width adjustments when visibility changes
        optimizedTreeTable.getColumns().addListener((ListChangeListener<TreeTableColumn<CollisionEntry, ?>>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved()) {
                    adjustColumnWidths();
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
                if (minOldDistance == null || oldDistance.compareTo(minOldDistance) < 0) {
                    minOldDistance = oldDistance;
                }
                if (minOptimizedDistance == null || optimizedDistance.compareTo(minOptimizedDistance) < 0) {
                    minOptimizedDistance = optimizedDistance;
                }

                // Add to totals for average calculation
                totalOldDistance = totalOldDistance.plus(oldDistance);
                totalOptimizedDistance = totalOptimizedDistance.plus(optimizedDistance);
                validDistanceCount++;
            }
        }

        // Calculate averages
        Duration avgOldDistance = validDistanceCount > 0 ?
            totalOldDistance.dividedBy(validDistanceCount) : Duration.ZERO;
        Duration avgOptimizedDistance = validDistanceCount > 0 ?
            totalOptimizedDistance.dividedBy(validDistanceCount) : Duration.ZERO;

        return new DistanceStats(
            avgOldDistance,
            avgOptimizedDistance,
            minOldDistance != null ? minOldDistance : Duration.ZERO,
            minOptimizedDistance != null ? minOptimizedDistance : Duration.ZERO
        );
    }

    private static Duration calculateTimeDifference(MergedAssessment exam1, MergedAssessment exam2) {
        if (exam1 == null || exam2 == null ||
            exam1.getBegin() == null || exam1.getEnd() == null ||
            exam2.getBegin() == null || exam2.getEnd() == null) {
            return Duration.ZERO;
        }

        if (exam1.getBegin().isBefore(exam2.getBegin())) {
            return Duration.between(exam1.getEnd(), exam2.getBegin());
        } else {
            return Duration.between(exam2.getEnd(), exam1.getBegin());
        }
    }

    private static Duration calculateOptimizedTimeDifference(MergedAssessment exam1, MergedAssessment exam2) {
        if (exam1 == null || exam2 == null ||
            exam1.getOptimizedBegin() == null || exam1.getOptimizedEnd() == null ||
            exam2.getOptimizedBegin() == null || exam2.getOptimizedEnd() == null) {
            return Duration.ZERO;
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
        int validDistances = 0;

        for (TreeItem<CollisionEntry> child : children) {
            CollisionEntry entry = child.getValue();
            if (entry == null) continue;

            // Sum up collisions
            totalCollisions += entry.getCollisionCount();

            // Calculate distances
            Duration oldDist = calculateTimeDifference(entry.parentAssessment, entry.childAssessment);
            Duration optDist = calculateOptimizedTimeDifference(entry.parentAssessment, entry.childAssessment);

            if (oldDist != null) {
                totalOldDistance = totalOldDistance.plus(oldDist);
                if (minOldDistance == null || oldDist.compareTo(minOldDistance) < 0) {
                    minOldDistance = oldDist;
                }
            }

            if (optDist != null) {
                totalOptDistance = totalOptDistance.plus(optDist);
                if (minOptDistance == null || optDist.compareTo(minOptDistance) < 0) {
                    minOptDistance = optDist;
                }
            }

            validDistances++;
        }

        // Update parent row values
        CollisionEntry parentEntry = parentItem.getValue();
        parentEntry.setDynamicStats(
            new DistanceStats(
                validDistances > 0 ? totalOldDistance.dividedBy(validDistances) : Duration.ZERO,
                validDistances > 0 ? totalOptDistance.dividedBy(validDistances) : Duration.ZERO,
                minOldDistance != null ? minOldDistance : Duration.ZERO,
                minOptDistance != null ? minOptDistance : Duration.ZERO
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
        }

        // Update the table display
        updateTable();
    }

    private void onOptimizeButtonClicked() {
        optimizedAssessments = examGUI.optimizeStart();
        if (optimizedAssessments != null) {
            updateOptimizedTreeTable(optimizedAssessments);
        }
    }
}
