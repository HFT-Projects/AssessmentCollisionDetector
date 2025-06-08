import data.Assessment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentCollisionDetectorTest {
    private final static String PATH_INPUT_ASSESSMENTS = "resources/pruefungen.csv";
    private final static String PATH_INPUT_REGISTRATIONS = "resources/anmeldungen.csv";
    public final static String PATH_OUTPUT_COLLISIONS = "target/collisions.csv";
    private final static String PATH_INPUT_COLLISIONS_SAMPLE = "resources/kollisionen_v2.csv";

    @Test
    void testMainOutputMatchesSample() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_ASSESSMENTS, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);
        SaveManager.saveCollision(PATH_OUTPUT_COLLISIONS, assessments);

        String f1 = Files.readString(Paths.get(PATH_INPUT_COLLISIONS_SAMPLE));
        String f2 = Files.readString(Paths.get(PATH_OUTPUT_COLLISIONS));

        f1 = f1.replaceAll("\r\n", "\n");
        f2 = f2.replaceAll("\r\n", "\n");

        assertEquals(f1, f2,
                "The output file doesn't match the sample file!");
    }
}

