package data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public abstract class AssessmentBase {
    public record AssessmentEntry(String faculty, String vert, String examiner1, String examiner2,
                                  String externalRegistrationCount, String externalDuration, String group, String room,
                                  String supervisor, String externalCourseOfStudy, String externalExamName,
                                  String externalExamId,
                                  String wiSe) {
    } //TODO: wiSe?!

    public abstract Long getNumber();

    public abstract String getName();

    public abstract String getCourseOfStudy();

    public abstract String getAssessmentVersion();

    public static String calculateQualifiedName(String stg, String courseOfStudy, Long no, String name) {
        return name + ".--." + (no == null ? "" : no) + ".--." + (courseOfStudy == null ? "" : courseOfStudy) + ".--." + (stg == null ? "" : stg);
    }

    public abstract String getQualifiedName();

    public abstract LocalDateTime getPrevailingBegin();

    public abstract LocalDateTime getPrevailingEnd();

    // calculate distance between the end of the first assessment (in time, not necessarily a) and the beginning of the second (negative -> overlap)
    protected static Duration calculateDistance(LocalDateTime begin1, LocalDateTime end1, LocalDateTime begin2, LocalDateTime end2) {
        if (begin1 == null || end1 == null || begin2 == null || end2 == null) {
            return null;
        } else {
            // calculate time distance between colliding assessments (end to begin)
            if (begin1.isBefore(begin2)) {
                return Duration.between(end1, begin2);
            } else {
                return Duration.between(end2, begin1);
            }
        }
    }

    public Duration getPrevailingDistance(AssessmentBase b) {
        return calculateDistance(this.getPrevailingBegin(), this.getPrevailingEnd(), b.getPrevailingBegin(), b.getPrevailingEnd());
    }

    public abstract Set<AssessmentBase.AssessmentEntry> getAssessmentEntries();

    public abstract Set<String> getRegisteredStudents();

    public abstract Integer getCollisionSum();

    public abstract Map<? extends AssessmentBase, Integer> getCollisionCountByAssessment();
}
