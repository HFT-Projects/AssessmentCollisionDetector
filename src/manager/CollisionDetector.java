package manager;

import data.Assessment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CollisionDetector {
    // record to return tuple of collisionSum and collisionCountByAssessment in detectCollisions
    public record ReturnTuple(int collisionSum, Map<Assessment, Integer> collisionCountByAssessment) {
    }

    static Map<Assessment, ReturnTuple> detectCollisions(Assessment[] assessments) {
        Map<Assessment, ReturnTuple> collisions = new HashMap<>();

        for (Assessment p : assessments) {
            Map<Assessment, Integer> collisionCountByAssessment = new HashMap<>();
            int collisionSum = 0;

            for (Assessment k : assessments) {
                // Assessment can't collide with itself
                if (p == k)
                    continue;

                int collisionsLocal = 0;

                // choose assessment with smaller number of registrations for the loop
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
                    }
                }

                if (collisionsLocal > 0)
                    collisionCountByAssessment.put(k, collisionsLocal);
                collisionSum += collisionsLocal;
            }

            collisions.put(p, new ReturnTuple(collisionSum, collisionCountByAssessment));
        }

        return collisions;
    }
}
