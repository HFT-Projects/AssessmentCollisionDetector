import data.*;

import java.time.LocalDateTime;
import java.util.*;

public class AssessmentOptimizer {
    private record AssessmentIdentifier(String name, LocalDateTime begin ,LocalDateTime end) {}

    // ATTENTION: calling this method invalidates all previous created MergesAssessments. Using them afterward can result in unexpected behavior.
    public static MergedAssessment[] mergeAssessments(Assessment[] assessments) {
        MergedAssessmentEditable._resetAssessmentToMergedAssessmentMap();
        Map<AssessmentIdentifier, MergedAssessment> mergedAssessments = new HashMap<>();

        for (Assessment a : assessments) {
            AssessmentIdentifier ai = new AssessmentIdentifier(a.getName(), a.getBegin(), a.getEnd());

            if (mergedAssessments.containsKey(ai)) {
                List<Assessment> currentAssessments = new LinkedList<>(Arrays.asList(mergedAssessments.get(ai).getAssessments()));
                currentAssessments.add(a);
                ((MergedAssessmentEditable)mergedAssessments.get(ai)).setAssessments(currentAssessments.toArray(new Assessment[0]));
            } else {
                MergedAssessmentEditable ma = new MergedAssessmentEditable();
                ma.setAssessments(new Assessment[]{a});
                mergedAssessments.put(ai, ma);
            }
        }

        return mergedAssessments.values().toArray(new MergedAssessment[0]);
    }

    private static void getAssessmentGroupsRecursive(MergedAssessment assessment, List<MergedAssessment> assessmentGroup) {
        assessmentGroup.add(assessment);
        for (Assessment a : assessment.getCollisionCountByAssessment().keySet()) {
            MergedAssessment ma = MergedAssessment.getAssessmentToMergedAssessmentMap().get(a);
            if (assessmentGroup.contains(ma))
                continue;
            getAssessmentGroupsRecursive(ma, assessmentGroup);
        }
    }

    public static MergedAssessment[][] getAssessmentGroups(MergedAssessment[] mergedAssessments) {
        Set<MergedAssessment> alreadyProcessed = new HashSet<>();
        List<List<MergedAssessment>> assessmentGroups = new LinkedList<>();

        for (MergedAssessment ma : mergedAssessments) {
            if (alreadyProcessed.contains(ma))
                continue;
            List<MergedAssessment> assessmentGroup = new LinkedList<>();

            getAssessmentGroupsRecursive(ma, assessmentGroup);

            alreadyProcessed.addAll(assessmentGroup);
            assessmentGroups.add(assessmentGroup);
        }

        return assessmentGroups.stream().map((mal) -> mal.toArray(new MergedAssessment[0])).toArray(MergedAssessment[][]::new);
    }
}

