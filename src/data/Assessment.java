package data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public abstract class Assessment {
    public record AssessmentEntry(String faculty, String vert, String examiner1, String examiner2,
                                  String externalRegistrationCount, String externalDuration, String group, String room,
                                  String supervisor, String externalCourseOfStudy, String externalExamName,
                                  String externalExamId,
                                  String wiSe) {
    } //TODO: wiSe?!

    protected final Long number;
    protected final String name;
    protected final String courseOfStudy;
    protected final String assessmentVersion;
    protected final LocalDateTime begin;
    protected final LocalDateTime end;
    protected Set<AssessmentEntry> assessmentEntries;
    protected Set<String> registeredStudents = null;
    protected Integer collisionSum = null;
    protected Map<Assessment, Integer> collisionCountByAssessment = null;

    public Assessment(Long number, String name, String courseOfStudy, String assessmentVersion, LocalDateTime begin, LocalDateTime end) {
        this.number = number;
        this.name = name;
        this.courseOfStudy = courseOfStudy;
        this.assessmentVersion = assessmentVersion;
        this.begin = begin;
        this.end = end;
    }

    public Long getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getCourseOfStudy() {
        return courseOfStudy;
    }

    public String getAssessmentVersion() {
        return assessmentVersion;
    }

    public static String calculateQualifiedName(String stg, String courseOfStudy, Long no, String name) {
        return name + ".--." + (no == null ? "" : no) + ".--." + (courseOfStudy == null ? "" : courseOfStudy) + ".--." + (stg == null ? "" : stg);
    }

    public String getQualifiedName() {
        return calculateQualifiedName(courseOfStudy, assessmentVersion, number, name);
    }

    public LocalDateTime getBegin() {
        return begin;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    // calculate distance between the end of the first assessment (in time, not necessarily a) and the beginning of the second (negative -> overlap)
    public static Duration getDistance(Assessment a, Assessment b) {
        if (a.begin == null || b.begin == null) {
            return null;
        } else {
            // calculate time distance between colliding assessments (end to begin)
            Assessment first;
            Assessment last;
            if (a.begin.isBefore(b.begin)) {
                first = a;
                last = b;
            } else {
                first = b;
                last = a;
            }
            return Duration.between(first.getEnd(), last.getBegin());
        }
    }

    public Duration getDistance(Assessment b) {
        return getDistance(this, b);
    }

    public Set<AssessmentEntry> getAssessmentEntries() {
        if (assessmentEntries == null)
            return null;
        return new HashSet<>(assessmentEntries);
    }

    public Set<String> getRegisteredStudents() {
        if (registeredStudents == null)
            return null;
        return new HashSet<>(registeredStudents);
    }

    public Integer getCollisionSum() {
        return collisionSum;
    }

    public Map<Assessment, Integer> getCollisionCountByAssessment() {
        if (collisionCountByAssessment == null)
            return null;
        return new HashMap<>(collisionCountByAssessment);
    }

    @Override
    public String toString() {
        // create map with qualifiedName as string instead of object to print this instead because printing the actual Assessment object would create an infinite loop.
        Map<String, Integer> collisions = new HashMap<>();
        if (this.collisionCountByAssessment != null) {
            for (Assessment p : this.collisionCountByAssessment.keySet()) {
                collisions.put(p.getQualifiedName(), this.collisionCountByAssessment.get(p));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        return "Assessment{" +
                "number=" + number +
                ", name='" + name + '\'' +
                ", courseOfStudy='" + courseOfStudy + '\'' +
                ", assessmentVersion='" + assessmentVersion + '\'' +
                ", qualifiedName='" + getQualifiedName() + '\'' +
                ", begin=" + (begin != null ? begin.format(formatter) : null) +
                ", end=" + (end != null ? end.format(formatter) : null) +
                ", assessmentEntries='" + assessmentEntries + '\'' +
                ", registeredStudents=" + registeredStudents +
                ", collisionSum=" + collisionSum +
                ", collisionCountByAssessment=" + collisions +
                '}';
    }
}
