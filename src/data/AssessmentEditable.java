package data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class AssessmentEditable extends Assessment {
    public AssessmentEditable(Long number, String name, String courseOfStudy, String assessmentVersion, LocalDateTime begin, LocalDateTime end) {
        super(number, name, courseOfStudy, assessmentVersion, begin, end);
    }

    public void setRegisteredStudents(Set<String> registeredStudents) {
        this.registeredStudents = registeredStudents;
    }

    public void setCollisionSum(Integer collisionSum) {
        this.collisionSum = collisionSum;
    }

    public void setCollisionCountByAssessment(Map<Assessment, Integer> collisionCountByAssessment) {
        this.collisionCountByAssessment = collisionCountByAssessment;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("Assessment", "AssessmentEditable");
    }
}
