import java.util.*;

public class AssessmentOptimizer {
    public static MergedAssessment[] mergeAssessments(Assessment[] assessments) {
        Map<String, List<MergedAssessment>> nameToMergedAssessment = new HashMap<>();

        for (Assessment a : assessments) {
            if (!nameToMergedAssessment.containsKey(a.getName())) {
                List<MergedAssessment> ass = new LinkedList<>();
                nameToMergedAssessment.put(a.getName(), ass);
            }

            boolean found = false;

            for (MergedAssessment ma : nameToMergedAssessment.get(a.getName())) {
                if (ma.getBegin() == null || a.getBegin() == null || !ma.getBegin().equals(a.getBegin()))
                    if (!(ma.getBegin() == null && a.getBegin() == null))
                        continue;
                if (ma.getEnd() == null || a.getEnd() == null || !ma.getEnd().equals(a.getEnd()))
                    if (!(ma.getEnd() == null && a.getEnd() == null))
                        continue;

                Set<String> registeredStudents = ma.getRegisteredStudents();
                registeredStudents.addAll(a.getRegisteredStudents());
                ma.setRegisteredStudents(registeredStudents);

                Map<Assessment, Integer> ccba = ma.getCollisionCountByAssessment();
                ccba.putAll(a.getCollisionCountByAssessment());
                ma.setCollisionCountByAssessment(ccba);

                ma.setCollisionSum(ma.getCollisionSum() + a.getCollisionSum());

                found = true;
                break;
            }

            if (!found) {
                MergedAssessment ma = new MergedAssessment(a.getName(), a.getBegin(), a.getEnd());
                ma.setRegisteredStudents(a.getRegisteredStudents());
                ma.setCollisionCountByAssessment(a.getCollisionCountByAssessment());
                ma.setCollisionSum(a.getCollisionSum());
                nameToMergedAssessment.get(a.getName()).add(ma);
            }

        }

        return nameToMergedAssessment.values().stream().flatMap(List::stream).toArray(MergedAssessment[]::new);
    }
}
