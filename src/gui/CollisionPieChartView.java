package gui;

import data.AssessmentBase;
import data.Assessment;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides a pie chart visualization for assessment collisions,
 * showing the distribution of collisions based on the time distance between exams.
 */
public class CollisionPieChartView {

    /**
     * Creates a pie chart for collisions within a specific course of study.
     *
     * @param courseOfStudy Course of study to analyze
     * @param assessments   Array of assessments to analyze
     * @return The generated PieChart
     */
    public static Node createCollisionPieChartByCourseOfStudy(String courseOfStudy, Assessment[] assessments) {
        List<Assessment> assesmentList = new ArrayList<>();
        for (Assessment assessment : assessments) {
            if (assessment != null && assessment.getCourseOfStudy().equals(courseOfStudy)) {
                assesmentList.add(assessment);
            }
        }
        Assessment[] assessmentsByCourseOfStudy = assesmentList.toArray(new Assessment[0]);
        return visualizePieChart(assessmentsByCourseOfStudy, "Studiengänge");
    }

    public static Node createCollisionPieChartByTime(Assessment[] assessments) {
        return visualizePieChart(assessments, "alle Prüfungen");
    }
    public static Node createCollisionPieChartByFaculty(Assessment[] assessments){
        VBox facultyStatistics = new VBox(20);
        facultyStatistics.setPadding(new Insets(20));
        facultyStatistics.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        facultyStatistics.setAlignment(Pos.CENTER);
        facultyStatistics.setMinHeight(1500);
        facultyStatistics.setMinWidth(1000);

        List<Assessment> facultyAAssessments = new ArrayList<>();
        List<Assessment> facultyBAssessments = new ArrayList<>();
        List<Assessment> facultyCAssessments = new ArrayList<>();

        for(Assessment a : assessments){
            if (!(a.getAssessmentEntries() == null)) {
                for (AssessmentBase.AssessmentEntry assessmentEntry : a.getAssessmentEntries()) {
                    Map<Assessment, Integer> collisions = a.getCollisionCountByAssessment();
                    for (Map.Entry<Assessment, Integer> entry : collisions.entrySet()) {
                        if (assessmentEntry.faculty().equals("A")) {
                            facultyAAssessments.add(a);
                        } else if (assessmentEntry.faculty().equals("B")) {
                            facultyBAssessments.add(a);
                        } else {
                            facultyCAssessments.add(a);
                        }

                    }
                }
            }
        }
        Assessment[] facultyAArray = facultyAAssessments.toArray(new Assessment[0]);
        Assessment[] facultyBArray = facultyBAssessments.toArray(new Assessment[0]);
        Assessment[] facultyCArray = facultyCAssessments.toArray(new Assessment[0]);

        Node facultyAPieChart = visualizePieChart(facultyAArray, "Fakultät A:");
        Node facultyBPieChart = visualizePieChart(facultyBArray, "Fakultät B:");
        Node facultyCPieChart = visualizePieChart(facultyCArray, "Fakultät C:");

        facultyStatistics.getChildren().addAll(facultyAPieChart, facultyBPieChart, facultyCPieChart);

        return facultyStatistics;
    }

    public static Node visualizePieChart(Assessment[] assessments, String info) {
        // Categories for time distances
        int greenCollisions = 0; // > 36 hours
        int yellowCollisions = 0; // > 12 hours but <= 36 hours
        int orangeCollisions = 0; // > 3 hours but <= 12 hours
        int redCollisions = 0; // > 1 hour but <= 3 hours
        int blackCollisions = 0; // <= 1 hour
        double totalHours = 0.0; // Total amount of hours, used to calculate average time between assessments
        long minDistance = 100;


        for (Assessment assessment : assessments) {

            if (assessment != null) {
                Map<Assessment, Integer> collisions = assessment.getCollisionCountByAssessment();
                for (Map.Entry<Assessment, Integer> entry : collisions.entrySet()) {
                    Assessment collidingAssessment = entry.getKey();
                    int count = entry.getValue();

                    // Calculate distance between assessments
                    if (assessment.getBegin() != null && assessment.getEnd() != null &&
                            collidingAssessment.getBegin() != null && collidingAssessment.getEnd() != null) {

                        Duration distance;
                        if (assessment.getBegin().isBefore(collidingAssessment.getBegin())) {
                            distance = Duration.between(assessment.getEnd(), collidingAssessment.getBegin());
                        } else {
                            distance = Duration.between(collidingAssessment.getEnd(), assessment.getBegin());
                        }

                        // Only count if the distance is valid (not negative)
                        if (!distance.isNegative()) {
                            long hours = distance.toHours();
                            totalHours+=hours;

                            // Get the minimum distance between 2 Assessments
                            if(hours<minDistance){
                                minDistance = hours;
                            }

                            // Add the count of colliding students to the appropriate category
                            if (hours > 36) {
                                greenCollisions += count;
                            } else if (hours > 12) {
                                yellowCollisions += count;
                            } else if (hours > 3) {
                                orangeCollisions += count;
                            } else if (hours > 1) {
                                redCollisions += count;
                            } else {
                                blackCollisions += count;
                            }
                        } else {
                            // Overlapping exams (negative distance) - these are hard collisions
                            blackCollisions += count;
                        }
                    } else {
                        // If timing information is missing, count as green
                        greenCollisions += count;
                    }
                }
            }
        }

        // Create chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        // Add slices for all categories
        pieChartData.add(new PieChart.Data("(>36h): " + greenCollisions, greenCollisions));
        pieChartData.add(new PieChart.Data("(>12h, ≤36h): " + yellowCollisions, yellowCollisions));
        pieChartData.add(new PieChart.Data("(>3h, ≤12h): " + orangeCollisions, orangeCollisions));
        pieChartData.add(new PieChart.Data("(>1h, ≤3h): " + redCollisions, redCollisions));
        pieChartData.add(new PieChart.Data("(≤1h): " + blackCollisions, blackCollisions));

        //Calculate the average time between assessments
        DecimalFormat formatter = new DecimalFormat("#0.00");
        double avgHoursBetweenAssessments = totalHours/(greenCollisions+yellowCollisions+orangeCollisions+redCollisions+blackCollisions);

        //Create Labels for additional Info
        Label avgHoursLabel = new Label("Durchschnittlicher Abstand zwischen Prüfungen: "+formatter.format(avgHoursBetweenAssessments)+" Stunden.");
        avgHoursLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));

        Label minDistanceLabel = new Label("Kleinster Abstand zwischen zwei Prüfungen: " +minDistance+" Stunden.");
        minDistanceLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));


        // Create and configure the chart
        final PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Kollisionsverteilung für " + info);
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(20);
        chart.setPrefHeight(400);
        chart.setLegendVisible(true);

        try {
            String cssResource = CollisionPieChartView.class.getResource("/chartcolors.css").toExternalForm();
            chart.getStylesheets().add(cssResource);
        } catch (Exception e) {
            System.err.println("Could not load CSS file: " + e.getMessage());
        }

        //Create Container for the Chart and Info
        VBox contentBox = new VBox(20);
        contentBox.setMinHeight(400);
        contentBox.getChildren().addAll(chart, avgHoursLabel, minDistanceLabel);
        minDistanceLabel.setAlignment(Pos.CENTER);
        avgHoursLabel.setAlignment(Pos.CENTER);


        return contentBox;
    }
}


