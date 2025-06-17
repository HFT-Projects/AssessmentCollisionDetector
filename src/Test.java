import data.*;
import manager.*;
import manager.optimizer.*;
import java.util.*;

class Test {
    public final static String PATH_OUTPUT_COLLISIONS = "target/collisions.csv";
    private final static String PATH_INPUT_ASSESSMENTS = "resources/pruefungen.csv";
    private final static String PATH_INPUT_REGISTRATIONS = "resources/anmeldungen.csv";

    public static void main(String[] args) {
        Assessment[] assessments = AssessmentsManager.loadAllAssessments(PATH_INPUT_ASSESSMENTS, PATH_INPUT_REGISTRATIONS, null);
        AssessmentsManager.loadRegistrationsIntoAssessments(assessments, PATH_INPUT_REGISTRATIONS);
        AssessmentsManager.loadCollisionsIntoAssessments(assessments);

        MergedAssessment[] mergedAssessments = AssessmentOptimizer.mergeAssessments(assessments);

        MergedAssessment[][] assessmentGroups = AssessmentOptimizer.getAssessmentGroups(mergedAssessments);

        // print assessmentGroups
        // Arrays.stream(assessmentGroups).map(Arrays::stream).forEach(as -> {System.out.println("\n\n"); as.forEach(System.out::println);});

        //show overview
        AssessmentOptimizer.logGroupStatistics(assessmentGroups);

        // call to optimizer here
        MergedAssessment[] optimizedAssessments = AssessmentOptimizer.optimizeAssessmentGroups(assessmentGroups, false, false);

        // print optimizedAssessments
        //System.out.println(Arrays.toString(optimizedAssessments));



        //SaveManager.saveCollision(PATH_OUTPUT_COLLISIONS, assessments);
    }
}