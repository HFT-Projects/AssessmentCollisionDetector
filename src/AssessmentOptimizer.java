import java.util.*;

public class AssessmentOptimizer {
    public static Map<Assessment, MergedAssessment> mergeAssessments(Assessment[] assessments) {
        Map<String, List<MergedAssessment>> nameToMergedAssessment = new HashMap<>();
        Map<Assessment, MergedAssessment> assessmentToMergedAssessment = new HashMap<>();

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

                assessmentToMergedAssessment.put(a, ma);

                found = true;
                break;
            }

            if (!found) {
                MergedAssessment ma = new MergedAssessment(a.getName(), a.getBegin(), a.getEnd());
                ma.setRegisteredStudents(a.getRegisteredStudents());
                ma.setCollisionCountByAssessment(a.getCollisionCountByAssessment());
                ma.setCollisionSum(a.getCollisionSum());
                nameToMergedAssessment.get(a.getName()).add(ma);
                assessmentToMergedAssessment.put(a, ma);
            }

        }

        return assessmentToMergedAssessment;
    }

    private static void getAssessmentGroupsRecursive(MergedAssessment assessment, List<MergedAssessment> assessmentGroup, Map<Assessment, MergedAssessment> assessmentToMergedAssessment) {
        assessmentGroup.add(assessment);
        for (Assessment a : assessment.getCollisionCountByAssessment().keySet()) {
            MergedAssessment ma = assessmentToMergedAssessment.get(a);
            if (assessmentGroup.contains(ma))
                continue;
            getAssessmentGroupsRecursive(ma, assessmentGroup, assessmentToMergedAssessment);
        }
    }

    public static MergedAssessment[][] getAssessmentGroups(Map<Assessment, MergedAssessment> assessmentToMergedAssessment) {
        Set<MergedAssessment> alreadyProcessed = new HashSet<>();
        List<List<MergedAssessment>> assessmentGroups = new LinkedList<>();

        for (MergedAssessment ma : new HashSet<>(assessmentToMergedAssessment.values())) {
            if (alreadyProcessed.contains(ma))
                continue;
            List<MergedAssessment> assessmentGroup = new LinkedList<>();

            getAssessmentGroupsRecursive(ma, assessmentGroup, assessmentToMergedAssessment);

            alreadyProcessed.addAll(assessmentGroup);
            assessmentGroups.add(assessmentGroup);
        }

        return assessmentGroups.stream().map((mal) -> mal.toArray(new MergedAssessment[0])).toArray(MergedAssessment[][]::new);
    }

    public static MergedAssessment[][] optimizeAssessments(MergedAssessment[][] assessmentGroups) {
        throw new RuntimeException("not implemented");
    }
}

