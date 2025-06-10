import data.Assessment;

import data.MergedAssessment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS, null);
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
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS, null);
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

    @Test
    @Order(3)
    void testMergingAssessments() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS, null);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        MergedAssessment[] mergedAssessments = AssessmentOptimizer.mergeAssessments(assessments);

        List<Assessment> assessmentList1 = new LinkedList<>(Arrays.asList(assessments));
        List<Assessment> assessmentList2 = new LinkedList<>();

        for (MergedAssessment ma : mergedAssessments) {
            assessmentList2.addAll(Arrays.asList(ma.getAssessments()));
        }

        assertEquals(assessmentList1.size(), assessmentList2.size(), "assessmentLists must be of same size!");

        for (Assessment a : assessmentList1) {
            assertEquals(Collections.frequency(assessmentList1, a), Collections.frequency(assessmentList2, a), "Assessments must appear equally often in both lists");
        }
    }

    @Test
    @Order(4)
    void testAssessmentToMergedAssessmentMap() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS, null);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        AssessmentOptimizer.mergeAssessments(assessments);

        List<Assessment> assessmentList1 = new LinkedList<>(Arrays.asList(assessments));
        List<Assessment> assessmentList2 = new LinkedList<>(MergedAssessment.getAssessmentToMergedAssessmentMap().keySet());

        assessmentList1.sort(Comparator.comparing(Assessment::getName));
        assessmentList2.sort(Comparator.comparing(Assessment::getName));

        assertEquals(assessmentList1.size(), assessmentList2.size(), "assessmentLists must be of same size!");

        for (Assessment a : assessmentList1) {
            assertEquals(Collections.frequency(assessmentList1, a), Collections.frequency(assessmentList2, a), "Assessments must appear equally often in both lists");
        }
    }

    @Test
    @Order(5)
    void testMergedAssessmentsFromAssessments2() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS, null);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        AssessmentOptimizer.mergeAssessments(assessments);

        for (Assessment a : MergedAssessment.getAssessmentToMergedAssessmentMap().keySet()) {
            assertTrue(Arrays.asList(MergedAssessment.getAssessmentToMergedAssessmentMap().get(a).getAssessments()).contains(a));
        }
    }

    @Test
    @Order(6)
    void testAssessmentGroups() throws Exception {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_EXAMS, PATH_INPUT_REGISTRATIONS, null);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        MergedAssessment[] mergedAssessments = AssessmentOptimizer.mergeAssessments(assessments);

        MergedAssessment[][] assessmentGroups = AssessmentOptimizer.getAssessmentGroups(mergedAssessments);

        List<Assessment> assessmentList1 = new LinkedList<>(Arrays.asList(assessments));
        List<Assessment> assessmentList2 = new LinkedList<>();

        for (MergedAssessment[] mas : assessmentGroups) {
            for (MergedAssessment ma : mas) {
                assessmentList2.addAll(Arrays.asList(ma.getAssessments()));
            }
        }

        assertEquals(assessmentList1.size(), assessmentList2.size(), "assessmentLists must be of same size!");

        for (Assessment a : assessmentList1) {
            assertEquals(Collections.frequency(assessmentList1, a), Collections.frequency(assessmentList2, a), "Assessments must appear equally often in both lists");
        }
    }
}

