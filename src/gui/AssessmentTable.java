package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

public class AssessmentTable extends BorderPane {
    // TreeTableView statt TableView
    private TreeTableView<AssessmentEntry> treeTableView;
    private ObservableList<AssessmentEntry> masterData;

    public AssessmentTable() {
        // Beispielhafte harte Daten: {Fach, Note, Kategorie}
        masterData = FXCollections.observableArrayList(
                new AssessmentEntry("Mathematik", 1.3, "Naturwissenschaften"),
                new AssessmentEntry("Physik", 1.7, "Naturwissenschaften"),
                new AssessmentEntry("Informatik", 2.0, "Informatik"),
                new AssessmentEntry("Englisch", 2.3, "Sprachen")
        );

        // TreeTableView und Spalten
        treeTableView = new TreeTableView<>();
        TreeTableColumn<AssessmentEntry, String> subjectCol = new TreeTableColumn<>("Fach");
        subjectCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("subject"));
        subjectCol.setSortable(false);

        TreeTableColumn<AssessmentEntry, Double> gradeCol = new TreeTableColumn<>("Note");
        gradeCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("grade"));
        gradeCol.setSortable(false);

        treeTableView.getColumns().addAll(subjectCol, gradeCol);

        // Kategorien als TreeItems
        TreeItem<AssessmentEntry> root = new TreeItem<>(new AssessmentEntry("Alle Kategorien", null, null));
        root.setExpanded(true);

        // Kategorien sammeln
        ObservableList<String> categories = FXCollections.observableArrayList();
        for (AssessmentEntry entry : masterData) {
            if (!categories.contains(entry.getCategory())) {
                categories.add(entry.getCategory());
            }
        }

        for (String category : categories) {
            TreeItem<AssessmentEntry> categoryItem = new TreeItem<>(new AssessmentEntry(category, null, category));
            for (AssessmentEntry entry : masterData) {
                if (category.equals(entry.getCategory())) {
                    categoryItem.getChildren().add(new TreeItem<>(entry));
                }
            }
            categoryItem.setExpanded(true);
            root.getChildren().add(categoryItem);
        }

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);

        // ...bestehende Zeile 3: Buttons, Dropdown, Checkboxen...
        Button collapseAllBtn = new Button("collapse all");
        collapseAllBtn.setOnAction(e -> {
            for (TreeItem<AssessmentEntry> cat : root.getChildren()) {
                cat.setExpanded(false);
            }
        });

        MenuButton columnsMenu = new MenuButton("columns");
        CheckMenuItem subjectColItem = new CheckMenuItem("Fach");
        subjectColItem.setSelected(true);
        subjectColItem.selectedProperty().addListener((obs, oldVal, newVal) -> subjectCol.setVisible(newVal));
        CheckMenuItem gradeColItem = new CheckMenuItem("Note");
        gradeColItem.setSelected(true);
        gradeColItem.selectedProperty().addListener((obs, oldVal, newVal) -> gradeCol.setVisible(newVal));
        columnsMenu.getItems().addAll(subjectColItem, gradeColItem);

        CheckBox hideNoTimes = new CheckBox("hide entries with no times");
        CheckBox showOnlyCollisions = new CheckBox("show only entries with collisions");

        // ...bestehende Zeile 1: Suchfelder und Inputbox...
        TextField searchExam1 = new TextField();
        Label searchExam1Label = new Label("Filter Exam 1");
        searchExam1Label.setLabelFor(searchExam1);
        searchExam1.setPromptText("Suchen...");

        TextField searchExam2 = new TextField();
        Label searchExam2Label = new Label("Filter Exam 2");
        searchExam2Label.setLabelFor(searchExam2);
        searchExam2.setPromptText("Suchen...");

        TextField maxDistance = new TextField();
        Label maxDistanceLabel = new Label("Max distance (hours)");
        maxDistanceLabel.setLabelFor(maxDistance);
        maxDistance.setPromptText("z.B. 2");

        // ...bestehende Zeile 2: Sortierfelder...
        ComboBox<String> sortExam1 = new ComboBox<>();
        sortExam1.getItems().addAll("Fach", "Note");
        sortExam1.getSelectionModel().selectFirst();
        Label sortExam1Label = new Label("Sort Exam 1");
        sortExam1Label.setLabelFor(sortExam1);

        ComboBox<String> sortDirExam1 = new ComboBox<>();
        sortDirExam1.getItems().addAll("Aufsteigend", "Absteigend");
        sortDirExam1.getSelectionModel().selectFirst();
        Label sortDirExam1Label = new Label("Sort direction Exam 1");
        sortDirExam1Label.setLabelFor(sortDirExam1);

        ComboBox<String> sortExam2 = new ComboBox<>();
        sortExam2.getItems().addAll("Fach", "Note");
        sortExam2.getSelectionModel().selectFirst();
        Label sortExam2Label = new Label("Sort Exam 2");
        sortExam2Label.setLabelFor(sortExam2);

        ComboBox<String> sortDirExam2 = new ComboBox<>();
        sortDirExam2.getItems().addAll("Aufsteigend", "Absteigend");
        sortDirExam2.getSelectionModel().selectFirst();
        Label sortDirExam2Label = new Label("Sort direction Exam 2");
        sortDirExam2Label.setLabelFor(sortDirExam2);

        // ...bestehendes Layout...
        HBox controls = new HBox(15,
                collapseAllBtn,
                columnsMenu,
                hideNoTimes,
                showOnlyCollisions,
                new VBox(searchExam1Label, searchExam1),
                new VBox(searchExam2Label, searchExam2),
                new VBox(maxDistanceLabel, maxDistance),
                new VBox(sortExam1Label, sortExam1),
                new VBox(sortDirExam1Label, sortDirExam1),
                new VBox(sortExam2Label, sortExam2),
                new VBox(sortDirExam2Label, sortDirExam2)
        );
        controls.setPadding(new Insets(10, 10, 10, 10));
        controls.setAlignment(Pos.BOTTOM_LEFT);

        setTop(controls);
        setCenter(treeTableView);
    }

    // Hilfsklasse für TreeTableView
    public static class AssessmentEntry {
        private final SimpleStringProperty subject;
        private final SimpleDoubleProperty grade;
        private final String category;

        public AssessmentEntry(String subject, Double grade, String category) {
            this.subject = new SimpleStringProperty(subject);
            this.grade = grade != null ? new SimpleDoubleProperty(grade) : null;
            this.category = category;
        }

        public String getSubject() {
            return subject.get();
        }

        public SimpleStringProperty subjectProperty() {
            return subject;
        }

        public Double getGrade() {
            return grade != null ? grade.get() : null;
        }

        public SimpleDoubleProperty gradeProperty() {
            return grade;
        }

        public String getCategory() {
            return category;
        }
    }
}
