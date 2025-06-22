package manager.optimizer;

import data.MergedAssessment;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.time.Duration;
import java.time.LocalDateTime;

@SuppressWarnings("unused")
@PlanningEntity
public class AssessmentScheduleItem {

    private static int nextId = 0;

    @PlanningId
    private final int id;

    private MergedAssessment assessment;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    LocalDateTime scheduledTime;

    public AssessmentScheduleItem() {
        this.id = nextId;
        nextId++;
    }

    public AssessmentScheduleItem(MergedAssessment assessment) {
        this();
        this.assessment = assessment;
        scheduledTime = assessment.getBegin();
    }

    public int getId() {
        return id;
    }

    public MergedAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(MergedAssessment assessment) {
        this.assessment = assessment;
    }

    public LocalDateTime getScheduledTime() {
        return this.scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getScheduledEndTime() {
        LocalDateTime originalBegin = assessment.getBegin();
        LocalDateTime originalEnd = assessment.getEnd();

        Duration length = Duration.between(originalBegin, originalEnd);

        return scheduledTime.plus(length);
    }

    @Override
    public String toString() {
        return "AssessmentScheduleItem{" +
                "id='" + id + '\'' +
                ", assessment=" + assessment +
                '}';
    }
}