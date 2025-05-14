import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

class Main {
    final static String PATH_INPUT_ASSESSMENTS = "resources/pruefungen.csv";
    final static String PATH_INPUT_REGISTRATIONS = "resources/anmeldungen.csv";
    final static String PATH_OUTPUT_COLLISIONS = "target/collisions.csv";

    public static void main(String[] args) throws IOException { //TODO: error handling
        Assessment[] assessments1 = LoadManager.loadExams(PATH_INPUT_ASSESSMENTS);
        Assessment[] assessments2 = LoadManager.loadMissingAssessments(PATH_INPUT_REGISTRATIONS, assessments1);
        Assessment[] assessments = Stream.of(assessments1, assessments2).flatMap(Arrays::stream).toArray(Assessment[]::new);

        LoadManager.loadRegistrations(PATH_INPUT_REGISTRATIONS, assessments);

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

        SaveManager.saveCollision(PATH_OUTPUT_COLLISIONS, assessments);

        System.out.println("Finished");
    }
}