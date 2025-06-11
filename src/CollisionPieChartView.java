import data.Assessment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

import java.time.Duration;
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
     * @param assessments Array of assessments to analyze
     * @return The generated PieChart
     */
    public static PieChart createCollisionPieChartByCourseOfStudy(String courseOfStudy, Assessment[] assessments) {
        // Categories for time distances
        int greenCollisions = 0; // > 48 hours
        int yellowCollisions = 0; // > 24 hours but <= 48 hours
        int redCollisions = 0; // <= 24 hours

        // Process all assessments for the specified course
        for (Assessment assessment : assessments) {
            // Only process assessments for the selected course
            if (assessment != null && courseOfStudy.equals(assessment.getCourseOfStudy())) {
                // Process collisions for this assessment
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
                            if (hours > 48) {
                                greenCollisions += count;
                            } else if (hours > 24) {
                                yellowCollisions += count;
                            } else {
                                redCollisions += count;
                            }
                        } else {
                            // Overlapping exams (negative distance) - these are hard collisions
                            redCollisions += count;
                        }
                    } else {
                        // If timing information is missing, count as green
                        greenCollisions += count;
                    }
                }
            }
        }
        return visualizePieChart(greenCollisions, yellowCollisions, redCollisions, courseOfStudy);
    }
    public static PieChart createCollisionPieChartByTime(Assessment[] assessments){
        // Categories for time distances
        int greenCollisions = 0; // > 48 hours
        int yellowCollisions = 0; // > 24 hours but <= 48 hours
        int redCollisions = 0; // <= 24 hours

        // Process all assessments
        for (Assessment assessment : assessments) {
            if (assessment != null) {
                // Process collisions for this assessment
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
                            if (hours > 48) {
                                greenCollisions += count;
                            } else if (hours > 24) {
                                yellowCollisions += count;
                            } else {
                                redCollisions += count;
                            }
                        } else {
                            // Overlapping exams (negative distance) - these are hard collisions
                            redCollisions += count;
                        }
                    } else {
                        // If timing information is missing, count as green
                        greenCollisions += count;
                    }
                }
            }
        }
        return visualizePieChart(greenCollisions, yellowCollisions, redCollisions, "alle Prüfungen");
    }
    public static PieChart visualizePieChart(int greenCollisions, int yellowCollisions, int redCollisions, String info){

        // Create chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        // Only add slices for categories that have collisions
        if (greenCollisions > 0) {
            pieChartData.add(new PieChart.Data("Grün (>48h) Kollisionen: "+greenCollisions, greenCollisions));
        }
        if (yellowCollisions > 0) {
            pieChartData.add(new PieChart.Data("Gelb (>24h, ≤48h) Kollisionen: "+yellowCollisions, yellowCollisions));
        }
        if (redCollisions > 0) {
            pieChartData.add(new PieChart.Data("Rot (≤24h) Kollisionen: "+redCollisions, redCollisions));
        }

        if (pieChartData.isEmpty()) {
            // Add a placeholder if no collisions were found
            pieChartData.add(new PieChart.Data("Keine Kollisionen", 1));
        }

        // Create and configure the chart
        final PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Kollisionsverteilung für " + info);
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(20);
        chart.setPrefHeight(400);

        // Apply colors to the chart after it's been displayed
        javafx.application.Platform.runLater(() -> {
            for (PieChart.Data data : pieChartData) {
                if (data.getName().startsWith("Grün")) {
                    data.getNode().setStyle("-fx-pie-color: #5cb85c;"); // Green
                } else if (data.getName().startsWith("Gelb")) {
                    data.getNode().setStyle("-fx-pie-color: #f0ad4e;"); // Yellow
                } else if (data.getName().startsWith("Rot")) {
                    data.getNode().setStyle("-fx-pie-color: #d9534f;"); // Red
                } else {
                    data.getNode().setStyle("-fx-pie-color: #6c757d;"); // Gray for "No collisions" case
                }
            }
        });

        return chart;
    }
}


