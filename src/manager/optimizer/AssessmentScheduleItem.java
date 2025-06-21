package manager.optimizer;

import data.MergedAssessment;
import data.MergedAssessmentEditable;
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

    public AssessmentScheduleItem() {
        this.id = nextId;
        nextId++;
    }

    public AssessmentScheduleItem(MergedAssessment assessment) {
        this();
        this.assessment = assessment;
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

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    public LocalDateTime getScheduledTime() {
        if (assessment.getOptimizedBegin() == null) {
            if (assessment.getBegin() == null)
                throw new AssertionError();
            return assessment.getBegin();
        }
        return assessment.getOptimizedBegin();
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        LocalDateTime originalBegin = assessment.getBegin();
        LocalDateTime originalEnd = assessment.getEnd();

        Duration length = Duration.between(originalBegin, originalEnd);

        ((MergedAssessmentEditable) assessment).setOptimizedBegin(scheduledTime);
        ((MergedAssessmentEditable) assessment).setOptimizedEnd(scheduledTime.plus(length));
    }

    public LocalDateTime getScheduledEndTime() {
        if (assessment.getOptimizedEnd() == null) {
            if (assessment.getEnd() == null)
                throw new AssertionError();
            return assessment.getEnd();
        }
        return assessment.getOptimizedEnd();
    }

    @Override
    public String toString() {
        return "AssessmentScheduleItem{" +
                "id='" + id + '\'' +
                ", assessment=" + assessment +
                '}';
    }
}