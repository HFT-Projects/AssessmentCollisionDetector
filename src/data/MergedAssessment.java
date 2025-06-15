package data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class MergedAssessment {
    static protected Map<Assessment, MergedAssessment> assessmentToMergedAssessmentMap = new HashMap<>();
    protected Assessment[] assessments;
    protected LocalDateTime optimizedBegin;
    protected LocalDateTime optimizedEnd;

    public MergedAssessment() {
    }

    public static Map<Assessment, MergedAssessment> getAssessmentToMergedAssessmentMap() {
        return new HashMap<>(assessmentToMergedAssessmentMap);
    }

    public static MergedAssessment[] getMergedAssessmentsFromAssessments(Assessment[] assessments) {
        Set<MergedAssessment> mergedAssessments = new LinkedHashSet<>();

        for (Assessment a : assessments) {
            if (!assessmentToMergedAssessmentMap.containsKey(a))
                throw new AssertionError("Assessment is not in assessmentToMergedAssessmentMap");
            mergedAssessments.add(assessmentToMergedAssessmentMap.get(a));
        }

        return mergedAssessments.toArray(new MergedAssessment[0]);
    }

    public Assessment[] getAssessments() {
        return assessments.clone();
    }

    public String getName() {
        if (assessments == null || assessments.length == 0)
            return null;
        return assessments[0].getName();
    }

    public String getQualifiedName() {
        return Assessment.calculateQualifiedName(getCourseOfStudy(), getAssessmentVersion(), getNumber(), getName());
    }

    public LocalDateTime getBegin() {
        if (assessments == null || assessments.length == 0)
            return null;
        return assessments[0].getBegin();
    }

    public LocalDateTime getEnd() {
        if (assessments == null || assessments.length == 0)
            return null;
        return assessments[0].getEnd();
    }

    public LocalDateTime getOptimizedBegin() {
        return optimizedBegin;
    }

    public LocalDateTime getOptimizedEnd() {
        return optimizedEnd;
    }

    private <T> T quarry(Function<Assessment, T> function) {
        if (assessments == null || assessments.length == 0)
            return null;
        T base = function.apply(assessments[0]);
        for (int i = 1; i<assessments.length; i++) {
            T compare = function.apply(assessments[i]);
            if (!Objects.equals(base, compare))
                return null;
        }
        return base;
    }

    public Long getNumber() {
        return quarry(Assessment::getNumber);
    }

    public String getCourseOfStudy() {
        return quarry(Assessment::getCourseOfStudy);
    }

    public String getAssessmentVersion() {
        return quarry(Assessment::getAssessmentVersion);
    }

    public Set<String> getRegisteredStudents() {
        if (assessments == null)
            return null;
        Set<String> result = new HashSet<>();
        for (Assessment a : assessments) {
            if (a.getRegisteredStudents() == null)
                continue;
            result.addAll(a.getRegisteredStudents());
        }
        return result;
    }

    public Integer getCollisionSum() {
        if (assessments == null)
            return null;
        int result = 0;
        for (Assessment a : assessments) {
            if (a.getCollisionSum() == null) {
                continue;
            }
            result += a.getCollisionSum();
        }
        return result;
    }

    public Map<MergedAssessment, Integer> getCollisionCountByAssessment() {
        if (assessments == null)
            return null;
        Map<MergedAssessment, Integer> result = new HashMap<>();
        for (Assessment a : assessments) {
            if (a.getCollisionCountByAssessment() == null) {
                continue;
            }

            for (Assessment b : a.getCollisionCountByAssessment().keySet()) {
                MergedAssessment mb = assessmentToMergedAssessmentMap.get(b);
                if (!result.containsKey(mb)) {
                    result.put(mb, a.getCollisionCountByAssessment().get(b));
                    continue;
                }
                result.put(mb, result.get(mb) + a.getCollisionCountByAssessment().get(b));
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MergedAssessment that = (MergedAssessment) o;
        return Objects.deepEquals(assessments, that.assessments) && Objects.equals(optimizedBegin, that.optimizedBegin) && Objects.equals(optimizedEnd, that.optimizedEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(assessments), optimizedBegin, optimizedEnd);
    }

    @Override
    public String toString() {
        // create map with qualifiedName as string instead of object to print this instead because printing the actual Assessment object would create an infinite loop.
        Map<String, Integer> collisions = new HashMap<>();
        Map<MergedAssessment, Integer> collisionsRaw = getCollisionCountByAssessment();
        if (collisionsRaw != null) {
            for (MergedAssessment p : collisionsRaw.keySet()) {
                collisions.put(p.getQualifiedName(), collisionsRaw.get(p));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        LocalDateTime begin = getBegin();
        LocalDateTime end = getEnd();

        return "MergedAssessment{" +
                "assessments='" + Arrays.toString(assessments) + '\'' +
                ", name='" + getName() + '\'' +
                "', qualifiedName='" + getQualifiedName() + '\'' +
                ", begin='" + (begin != null ? formatter.format(begin) : null) + '\'' +
                ", end='" + (end != null ? formatter.format(end) : null) + '\'' +
                ", optimizedBegin='" + (optimizedBegin != null ? formatter.format(optimizedBegin) : null) + '\'' +
                ", optimizedEnd='" + (optimizedEnd != null ? formatter.format(optimizedEnd) : null) + '\'' +
                ", number='" + getNumber() + '\'' +
                ", courseOfStudy='" + getCourseOfStudy() + '\'' +
                ", assessmentVersion='" + getAssessmentVersion() + '\'' +
                ", registeredStudents='" + getRegisteredStudents() + '\'' +
                ", collisionSum='" + getCollisionSum() + '\'' +
                ", collisionCountByAssessment='" + collisions+ '\'' +
                '}';
    }
}
