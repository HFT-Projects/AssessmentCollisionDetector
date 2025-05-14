import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

class AssessmentCollisionDetector {
    final static String PATH_INPUT_ASSESSMENTS = "resources/pruefungen.csv";
    final static String PATH_INPUT_REGISTRATIONS = "resources/anmeldungen.csv";
    final static String PATH_OUTPUT_COLLISIONS = "target/collisions.csv";

    public static void main(String[] args) throws IOException { //TODO: error handling
        Assessment[] assessments1 = LoadManager.loadExams(PATH_INPUT_ASSESSMENTS);
        Assessment[] assessments2 = LoadManager.loadMissingAssessments(PATH_INPUT_REGISTRATIONS, assessments1);
        Assessment[] assessments = Stream.of(assessments1, assessments2).flatMap(Arrays::stream).toArray(Assessment[]::new);

        LoadManager.loadRegistrations(PATH_INPUT_REGISTRATIONS, assessments);

        for (Assessment p : assessments) {
            Set<String> collisionsAll = new HashSet<>();
            Map<Assessment, Integer> collisions = new LinkedHashMap<>();

            for (Assessment k : assessments) {
                if (p == k)
                    continue;
                int collisionsLocal = 0;

                Set<String> registrationsLoop;
                Set<String> registrationsCompare;
                if (k.getRegisteredStudents().size() > p.getRegisteredStudents().size()) {
                    registrationsLoop = p.getRegisteredStudents();
                    registrationsCompare = k.getRegisteredStudents();
                } else {
                    registrationsLoop = k.getRegisteredStudents();
                    registrationsCompare = p.getRegisteredStudents();
                }

                for (String s : registrationsLoop) {
                    if (registrationsCompare.contains(s)) {
                        collisionsLocal++;
                        collisionsAll.add(s);
                    }
                }

                if (collisionsLocal > 0)
                    collisions.put(k, collisionsLocal);
            }

            p.setCollisionsAll(collisionsAll.size());
            p.setCollisions(collisions);
        }


        SaveManager.saveCollision(PATH_OUTPUT_COLLISIONS, assessments);

        System.out.println("Finished");
    }
}