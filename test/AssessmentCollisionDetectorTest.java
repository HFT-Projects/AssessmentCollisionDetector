import data.Assessment;

import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssessmentCollisionDetectorTest {
    private final static String PATH_INPUT_EXAMS = "resources/pruefungen.csv";
    private final static String PATH_INPUT_REGISTRATIONS = "resources/anmeldungen.csv";
    public final static String PATH_OUTPUT_COLLISIONS = "target/collisions.csv";
    private final static String PATH_INPUT_COLLISIONS_SAMPLE = "resources/kollisionen_v2.csv";
    private final static String PATH_OUTPUT_ASSESSMENTS = "target/assessments.csv";

    @Test
    @Order(1)
    void testCollisionDetectionAndSaving() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);
        SaveManager.saveCollisions(PATH_OUTPUT_COLLISIONS, assessments);

        String f1 = Files.readString(Paths.get(PATH_INPUT_COLLISIONS_SAMPLE));
        String f2 = Files.readString(Paths.get(PATH_OUTPUT_COLLISIONS));

        f1 = f1.replaceAll("\r\n", "\n");
        f2 = f2.replaceAll("\r\n", "\n");

        assertEquals(f1, f2,
                "The output file doesn't match the sample file!");
    }

    @Test
    @Order(2)
    void testSavingAssessments() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        SaveManager.saveAssessments(PATH_OUTPUT_ASSESSMENTS, assessments);

        String f1 = Files.readString(Paths.get(PATH_INPUT_EXAMS));
        String f2 = Files.readString(Paths.get(PATH_OUTPUT_ASSESSMENTS));

        f1 = f1.replaceAll("\r\n", "\n").replaceAll("\uFEFF", "");
        f2 = f2.replaceAll("\r\n", "\n").replaceAll("\uFEFF", "");

        Set<String> f1Lines = new HashSet<>(Arrays.asList(f1.split("\n")));
        Set<String> f2Lines = new HashSet<>(Arrays.asList(f2.split("\n")));

        assertTrue(f2Lines.containsAll(f1Lines));
    }
}

