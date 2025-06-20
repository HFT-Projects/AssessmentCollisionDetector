package gui;

import data.Assessment;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.prefs.Preferences;

/**
 * Tab for optimization functionalities in the exam scheduling application.
 */
public class CollisionsTab {

    @SuppressWarnings("unused")
    private static final String PRIMARY_COLOR = "#3498db";
    private static final String SECONDARY_COLOR = "#2c3e50";

    private final VBox content;
    private final Tab tab;

    public CollisionsTab() {
        tab = new Tab("Collision Results");
        tab.setClosable(false);
        tab.setDisable(true);

        // Create basic layout
        content = new VBox(10);
        content.setPadding(new Insets(20));

        // Create a ScrollPane to wrap the content
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        VBox.setVgrow(section, Priority.ALWAYS);


        // Section title
        Label sectionTitle = new Label("Collision Results");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web(SECONDARY_COLOR));

        // Section description
        Label sectionDescription = new Label("Detected exam collisions based on student registrations.");
        sectionDescription.setStyle("-fx-text-fill: " + SECONDARY_COLOR + ";");

        // Separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 10, 0));



        // Add components to section
        section.getChildren().addAll(
                sectionTitle,
                sectionDescription,
                separator
        );

        // Add all sections to the content
        content.getChildren().addAll(section);

        // Set the ScrollPane as the tab content
        tab.setContent(scrollPane);
    }

    public void enable_tab(Assessment[] assessments, Preferences prefs) {
        AssessmentTable assessmentTable = new AssessmentTable(assessments, prefs);
        content.getChildren().add(assessmentTable);
        tab.setDisable(false);
    }

    public Tab getTab() {
        return tab;
    }
}
