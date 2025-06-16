package gui;

import data.Assessment;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public static PieChart createCollisionPieChartByCourseOfStudy(String courseOfStudy, Assessment[] assessments) {
        List<Assessment> assesmentList = new ArrayList<>();
        for (Assessment assessment : assessments) {
            if (assessment != null && assessment.getCourseOfStudy().equals(courseOfStudy)) {
                assesmentList.add(assessment);
            }
        }
        Assessment[] assessmentsByCourseOfStudy = assesmentList.toArray(new Assessment[0]);
        return visualizePieChart(assessmentsByCourseOfStudy, "Studiengänge");
    }

    public static PieChart createCollisionPieChartByTime(Assessment[] assessments) {
        return visualizePieChart(assessments, "alle Prüfungen");
    }

    public static PieChart visualizePieChart(Assessment[] assessments, String info) {
        // Categories for time distances
        int greenCollisions = 0; // > 36 hours
        int yellowCollisions = 0; // > 12 hours but <= 36 hours
        int orangeCollisions = 0; // > 3 hours but <= 12 hours
        int redCollisions = 0; // > 1 hour but <= 3 hours
        int blackCollisions = 0; // <= 1 hour


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

        // Only add slices for categories that have collisions
        pieChartData.add(new PieChart.Data("(>36h): " + greenCollisions, greenCollisions));
        pieChartData.add(new PieChart.Data("(>12h, ≤36h): " + yellowCollisions, yellowCollisions));
        pieChartData.add(new PieChart.Data("(>3h, ≤12h): " + orangeCollisions, orangeCollisions));
        pieChartData.add(new PieChart.Data("(>1h, ≤3h): " + redCollisions, redCollisions));
        pieChartData.add(new PieChart.Data("(≤1h): " + blackCollisions, blackCollisions));


        // Create and configure the chart
        final PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Kollisionsverteilung für " + info);
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(20);
        chart.setPrefHeight(400);
        chart.setLegendVisible(true);  // Set legend visible first
        try {
            String cssResource = CollisionPieChartView.class.getResource("/chartcolors.css").toExternalForm();
            chart.getStylesheets().add(cssResource);
        } catch (Exception e) {
            System.err.println("Could not load CSS file: " + e.getMessage());
        }

        return chart;
    }
}


