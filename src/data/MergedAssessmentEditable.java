package data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("unused")
public class MergedAssessmentEditable extends MergedAssessment {
    public MergedAssessmentEditable() {
        super();
    }

    public static void _resetAssessmentToMergedAssessmentMap() {
        assessmentToMergedAssessmentMap = new HashMap<>();
    }

    public void setAssessments(Assessment[] assessments) {
        // check that the assessments don't have an inconsistencies.
        if (assessments != null && assessments.length > 1) {
            String name = assessments[0].getName();
            LocalDateTime begin = assessments[0].getBegin();
            LocalDateTime end = assessments[0].getEnd();

            for (int i = 1; i < assessments.length; i++) {
                Assessment a = assessments[i];
                if (!Objects.equals(name, a.getName()) ||
                        !Objects.equals(begin, a.getBegin()) ||
                        !Objects.equals(end, a.getEnd()))
                    throw new AssertionError("Assessments must have same name, begin & end"); // TODO: specify which one is wrong
            }
        }

        if (this.assessments != null) {
            for (Assessment a : this.assessments) {
                assessmentToMergedAssessmentMap.remove(a);
            }
        }

        if (assessments != null) {
            for (Assessment a : assessments) {
                if (assessmentToMergedAssessmentMap.containsKey(a))
                    throw new AssertionError("Assessment is already part of another MergedAssessment!");
                assessmentToMergedAssessmentMap.put(a, this);
            }
        }

        this.assessments = assessments;
    }

    public void setOptimizedBegin(LocalDateTime optimizedBegin) {
        this.optimizedBegin = optimizedBegin;
    }

    public void setOptimizedEnd(LocalDateTime optimizedEnd) {
        this.optimizedEnd = optimizedEnd;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("MergedAssesssment", "MergedAssessmentEditable");
    }
}
