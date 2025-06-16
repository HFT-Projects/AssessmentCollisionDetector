package manager.optimizer;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import data.MergedAssessment;
import data.MergedAssessmentEditable;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

@PlanningEntity
public class AssessmentScheduleItem {

    @PlanningId
    private String id;

    private MergedAssessment assessment;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private LocalDateTime scheduledTime;

    public AssessmentScheduleItem() {
        this.id = UUID.randomUUID().toString();
    }

    public AssessmentScheduleItem(MergedAssessment assessment) {
        this.id = UUID.randomUUID().toString();
        this.assessment = assessment;
        this.scheduledTime = assessment.getBegin();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MergedAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(MergedAssessment assessment) {
        this.assessment = assessment;
    }

    // Getter & setter for OptaPlanner
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getAssessmentName() {
        return assessment != null ? assessment.getName() : null;
    }

    public LocalDateTime getOriginalBegin() {
        return assessment != null ? assessment.getBegin() : null;
    }

    public LocalDateTime getOriginalEnd() {
        return assessment != null ? assessment.getEnd() : null;
    }

    public LocalDateTime getScheduledEndTime() {
        if (scheduledTime == null || assessment == null) return null;

        LocalDateTime originalBegin = assessment.getBegin();
        LocalDateTime originalEnd = assessment.getEnd();

        if (originalBegin == null || originalEnd == null) return null;

        Duration originalDuration = Duration.between(originalBegin, originalEnd);
        return scheduledTime.plus(originalDuration);
    }

    public void applyScheduleToAssessment() {
        if (assessment instanceof MergedAssessmentEditable && scheduledTime != null) {
            MergedAssessmentEditable editable = (MergedAssessmentEditable) assessment;
            editable.setOptimizedBegin(scheduledTime);
            editable.setOptimizedEnd(getScheduledEndTime());
        }
    }

    @Override
    public String toString() {
        return "AssessmentScheduleItem{" +
                "id='" + id + '\'' +
                ", assessment=" + (assessment != null ? assessment.getName() : "null") +
                ", scheduledTime=" + scheduledTime +
                '}';
    }
}