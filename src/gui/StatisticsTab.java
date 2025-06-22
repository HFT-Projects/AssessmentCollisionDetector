package gui;

import data.Assessment;
import data.MergedAssessment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Set;
import java.util.TreeSet;

public class StatisticsTab {
    private final Tab tab;
    private final VBox contentArea;
    private final ComboBox<String> selectStatistic;
    private final VBox courseChartContainer;
    private final ComboBox<String> courseComboBox;
    private final HBox chartBox;

    private Assessment[] assessments = null;
    private MergedAssessment[] optimizedAssessments = null;

    private static final String STR_OVERVIEW_STATS = "overview";
    private static final String STR_BY_FACULTY_STATS = "by faculties";
    private static final String STR_BY_COURSE_OF_STUDY_STATS = "by course of study";

    public StatisticsTab() {
        tab = new Tab("Statistics");

        // Create main container with BorderPane to allow positioning
        BorderPane mainContainer = new BorderPane();
        mainContainer.setPadding(new Insets(20));

        // Create HBox for the dropdown at top left
        HBox selectorContainer = new HBox(10);
        selectorContainer.setAlignment(Pos.TOP_LEFT);

        // Create the statistics selector
        selectStatistic = new ComboBox<>();
        selectStatistic.getItems().addAll(STR_OVERVIEW_STATS, STR_BY_COURSE_OF_STUDY_STATS, STR_BY_FACULTY_STATS);
        selectStatistic.valueProperty().setValue(selectStatistic.getItems().get(0));

        selectorContainer.getChildren().add(selectStatistic);
        mainContainer.setTop(selectorContainer);

        // Content area where statistics will be displayed
        contentArea = new VBox(20);
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setPadding(new Insets(20, 0, 0, 0));
        mainContainer.setCenter(contentArea);

        // Create a container for course selection and pie chart
        courseChartContainer = new VBox(15);
        courseChartContainer.setPadding(new Insets(20));
        courseChartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        courseChartContainer.setVisible(false); // Initially hidden

        // Course selection
        courseComboBox = new ComboBox<>();
        courseComboBox.setPromptText("choose course of study");
        courseComboBox.setMaxWidth(300);

        // Container for the pie chart
        chartBox = new HBox();
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setMinHeight(400);

        // When a course is selected, create and display the pie chart
        courseComboBox.valueProperty().addListener((o, oldVal, newVal) -> loadCollisionByAssessmentContent());

        // Add components to the course chart container
        Label courseHeading = new Label("Collisions by course of study");
        courseHeading.setFont(Font.font("System", FontWeight.BOLD, 16));
        courseHeading.setTextFill(Color.web(MainGUI.SECONDARY_COLOR));

        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));

        courseChartContainer.getChildren().addAll(
                courseHeading,
                separator,
                courseComboBox,
                chartBox
        );


        // Action handler for the statistics selector
        selectStatistic.setOnAction(event -> loadMainContent());

        tab.setContent(mainContainer);
    }

    private void loadMainContent() {
        // Clear current content
        contentArea.getChildren().clear();

        // Handle the selected statistic
        switch (selectStatistic.getValue()) {
            case STR_OVERVIEW_STATS -> {
                if (assessments == null)
                    throw new AssertionError();
                courseChartContainer.setVisible(false);
                HBox collisionBox = new HBox();
                collisionBox.setAlignment(Pos.CENTER);
                collisionBox.setMinHeight(400);
                Node collisionChart = CollisionPieChartView.createCollisionPieChartByTime(assessments);
                Node collisionChartOptimized = optimizedAssessments == null ? new VBox() : CollisionPieChartView.createOptimizedCollisionPieChartByTime(optimizedAssessments);
                collisionBox.getChildren().addAll(collisionChart, collisionChartOptimized);
                contentArea.getChildren().add(collisionBox);
            }
            case STR_BY_FACULTY_STATS -> {
                if (assessments == null)
                    throw new AssertionError();
                HBox facultyContent = new HBox();
                facultyContent.setAlignment(Pos.CENTER);
                facultyContent.setMinHeight(800);
                Node facultyChart = CollisionPieChartView.createCollisionPieChartByFaculty(assessments);
                Node optimizedFacultyChart = optimizedAssessments == null ? new VBox() : CollisionPieChartView.createOptimizedCollisionPieChartByFaculty(optimizedAssessments);
                facultyContent.getChildren().addAll(facultyChart, optimizedFacultyChart);

                ScrollPane sp = new ScrollPane();
                sp.setMinWidth(500);
                sp.setContent(facultyContent);
                courseChartContainer.setVisible(false);
                contentArea.getChildren().add(sp);
            }
            case STR_BY_COURSE_OF_STUDY_STATS -> {
                // Update course selection box first
                if (assessments == null)
                    throw new AssertionError();

                Set<String> courses = new TreeSet<>();
                for (Assessment a : assessments) {
                    if (a.getCourseOfStudy() != null && !a.getCourseOfStudy().isEmpty()) {
                        courses.add(a.getCourseOfStudy());
                    }
                }
                courseComboBox.getItems().clear();
                courseComboBox.getItems().addAll(courses);
                if (!courseComboBox.getItems().isEmpty())
                    courseComboBox.valueProperty().setValue(courseComboBox.getItems().get(0));

                // Show the course selection and chart container
                courseChartContainer.setVisible(true);
                contentArea.getChildren().add(courseChartContainer);
            }
            default -> System.out.println("No statistic selected");
        }
    }

    private void loadCollisionByAssessmentContent() {
        if (courseComboBox.getValue() == null)
            return;

        String selectedCourse = courseComboBox.getValue();
        if (selectedCourse != null && assessments != null && assessments.length > 0) {
            // Clear previous chart
            chartBox.getChildren().clear();

            // Create new chart with the selected course
            Node pieChart = CollisionPieChartView.createCollisionPieChartByCourseOfStudy(selectedCourse, assessments);
            Node pieChartOptimized = optimizedAssessments == null ? new VBox() : CollisionPieChartView.createOptimizedCollisionPieChartByCourseOfStudy(selectedCourse, optimizedAssessments);

            chartBox.getChildren().addAll(pieChart, pieChartOptimized);
        }
    }

    public void setAssessments(Assessment[] assessments) {
        this.assessments = assessments;
        loadMainContent();
    }

    public void setOptimizedAssessments(MergedAssessment[] optimizedAssessments) {
        this.optimizedAssessments = optimizedAssessments;
        loadMainContent();
    }

    public Tab getTab() {
        return tab;
    }
}
