package manager.optimizer;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import java.time.LocalDateTime;
import java.util.List;

@PlanningSolution
public class AssessmentSchedulingSolution {

    @PlanningEntityCollectionProperty
    private List<AssessmentScheduleItem> assessmentList;

    @ValueRangeProvider(id = "timeSlotRange")
    private List<LocalDateTime> timeSlotList;

    @PlanningScore
    private HardMediumSoftLongScore score;

    public AssessmentSchedulingSolution() {}

    public AssessmentSchedulingSolution(List<AssessmentScheduleItem> assessments, List<LocalDateTime> timeSlots) {
        this.assessmentList = assessments;
        this.timeSlotList = timeSlots;
    }

    public List<LocalDateTime> getTimeSlotList() {
        return timeSlotList;
    }

    public void setTimeSlotList(List<LocalDateTime> timeSlotList) {
        this.timeSlotList = timeSlotList;
    }

    public List<AssessmentScheduleItem> getAssessmentList() {
        return assessmentList;
    }

    public void setAssessmentList(List<AssessmentScheduleItem> assessmentList) {
        this.assessmentList = assessmentList;
    }

    public HardMediumSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftLongScore score) {
        this.score = score;
    }
}
