import data.Assessment;
import data.AssessmentEditable;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class AssessmentsManager {
    public static Assessment[] loadAllAssessments(String pathInputAssessments, String pathInputRegistrations) {
        Assessment[] assessments1 = LoadManager.loadExams(pathInputAssessments);
        Assessment[] assessments2 = LoadManager.loadMissingAssessments(pathInputRegistrations, assessments1);
        return Stream.of(assessments1, assessments2).flatMap(Arrays::stream).toArray(Assessment[]::new);
    }

    public static void loadRegistrationsIntoAssessments(Assessment[] assessments, String pathInputRegistrations) {
        Map<String, Set<String>> registrationsByAssessmentsQualifiedName = LoadManager.loadRegistrations(pathInputRegistrations, assessments);
        // save registrations into Assessment objects
        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getRegisteredStudents() != null)
                throw new AssertionError("The registration of the assessment " + p + " was already loaded.");

            ((AssessmentEditable) p).setRegisteredStudents(registrationsByAssessmentsQualifiedName.get(p.getQualifiedName()));
        }
    }

    public static void loadCollisionsIntoAssessments(Assessment[] assessments) {
        Map<Assessment, CollisionDetector.ReturnTuple> collisions = CollisionDetector.detectCollisions(assessments);
        // save collisions into Assessment objects
        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getCollisionSum() != null || p.getCollisionCountByAssessment() != null)
                throw new AssertionError("The collisions of the assessment " + p + " was already loaded.");

            CollisionDetector.ReturnTuple collision = collisions.get(p);
            ((AssessmentEditable) p).setCollisionSum(collision.collisionSum());
            ((AssessmentEditable) p).setCollisionCountByAssessment(collision.collisionCountByAssessment());
        }
    }
}
