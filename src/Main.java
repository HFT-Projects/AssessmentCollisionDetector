import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

class Main {

    public static Assessment[] runProcessing(String PATH_INPUT_ASSESSMENTS, String PATH_INPUT_REGISTRATIONS, String PATH_OUTPUT_COLLISIONS  ) throws IOException { //TODO: error handling
        Assessment[] assessments1 = LoadManager.loadExams(PATH_INPUT_ASSESSMENTS);
        Assessment[] assessments2 = LoadManager.loadMissingAssessments(PATH_INPUT_REGISTRATIONS, assessments1);
        Assessment[] assessments = Stream.of(assessments1, assessments2).flatMap(Arrays::stream).toArray(Assessment[]::new);

        Map<String, Set<String>> registrationsByAssessmentsQualifiedName = LoadManager.loadRegistrations(PATH_INPUT_REGISTRATIONS, assessments);
        // save registrations into Assessment objects
        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getRegisteredStudents() != null)
                throw new AssertionError("The registration of the assessment " + p + " was already loaded.");

            p.setRegisteredStudents(registrationsByAssessmentsQualifiedName.get(p.getQualifiedName()));
        }

        Map<Assessment, CollisionDetector.ReturnTuple> collisions = CollisionDetector.detectCollisions(assessments);
        // save collisions into Assessment objects
        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getCollisionSum() != null || p.getCollisionCountByAssessment() != null)
                throw new AssertionError("The collisions of the assessment " + p + " was already loaded.");

            CollisionDetector.ReturnTuple collision = collisions.get(p);
            p.setCollisionSum(collision.collisionSum());
            p.setCollisionCountByAssessment(collision.collisionCountByAssessment());
        }


        System.out.println("Finished");
        return assessments;


    }
}