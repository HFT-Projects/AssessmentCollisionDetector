package data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SuppressWarnings("unused")
public abstract class Assessment extends AssessmentBase {
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

    @Override
    public Long getNumber() {
        return number;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCourseOfStudy() {
        return courseOfStudy;
    }

    @Override
    public String getAssessmentVersion() {
        return assessmentVersion;
    }

    @Override
    public String getQualifiedName() {
        return calculateQualifiedName(courseOfStudy, assessmentVersion, number, name);
    }

    public LocalDateTime getBegin() {
        return begin;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Duration getDistance(Assessment b) {
        return calculateDistance(this.begin, this.end, b.begin, b.end);
    }

    @Override
    public LocalDateTime getPrevailingBegin() {
        return begin;
    }

    @Override
    public LocalDateTime getPrevailingEnd() {
        return end;
    }

    @Override
    public Set<AssessmentEntry> getAssessmentEntries() {
        if (assessmentEntries == null)
            return null;
        return new HashSet<>(assessmentEntries);
    }

    @Override
    public Set<String> getRegisteredStudents() {
        if (registeredStudents == null)
            return null;
        return new HashSet<>(registeredStudents);
    }

    @Override
    public Integer getCollisionSum() {
        return collisionSum;
    }

    @Override
    public Map<Assessment, Integer> getCollisionCountByAssessment() {
        if (collisionCountByAssessment == null)
            return null;
        return new HashMap<>(collisionCountByAssessment);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Assessment that = (Assessment) o;
        return Objects.equals(number, that.number) && Objects.equals(name, that.name) && Objects.equals(courseOfStudy, that.courseOfStudy) && Objects.equals(assessmentVersion, that.assessmentVersion) && Objects.equals(begin, that.begin) && Objects.equals(end, that.end) && Objects.equals(assessmentEntries, that.assessmentEntries) && Objects.equals(registeredStudents, that.registeredStudents) && Objects.equals(collisionSum, that.collisionSum);
    }

    @Override
    public int hashCode() {
        // ATTENTION: only consider final/immutable attributes
        return Objects.hash(number, name, courseOfStudy, assessmentVersion, begin, end);
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
                "number='" + number + '\'' +
                ", name='" + name + '\'' +
                ", courseOfStudy='" + courseOfStudy + '\'' +
                ", assessmentVersion='" + assessmentVersion + '\'' +
                ", qualifiedName='" + getQualifiedName() + '\'' +
                ", begin=" + (begin != null ? begin.format(formatter) : null) +
                ", end=" + (end != null ? end.format(formatter) : null) +
                ", assessmentEntries='" + assessmentEntries + '\'' +
                ", registeredStudents='" + registeredStudents + '\'' +
                ", collisionSum='" + collisionSum + '\'' +
                ", collisionCountByAssessment='" + collisions + '\'' +
                '}';
    }
}
