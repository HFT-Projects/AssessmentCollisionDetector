package optimizer;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.time.LocalDateTime;
import java.util.List;

@PlanningSolution
public class ExamSchedulingSolution {

    @PlanningEntityCollectionProperty
    private List<AssessmentScheduleItem> assessmentList;

    @ValueRangeProvider(id = "timeSlotRange")
    private List<LocalDateTime> timeSlotList;

    @PlanningScore
    private HardSoftScore score;

    public ExamSchedulingSolution() {}

    public ExamSchedulingSolution(List<AssessmentScheduleItem> assessments, List<LocalDateTime> timeSlots) {
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

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
