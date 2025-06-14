package optimizer;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public class ExamSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                studentConflict(factory),
                minimizeExamsPerDay(factory),
                maximizeTimeBetweenExams(factory)
        };
    }

    Constraint studentConflict(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::haveSameStudents)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student conflict");
    }

    Constraint minimizeExamsPerDay(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::sameDayAndStudent)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Multiple exams per day");
    }

    Constraint maximizeTimeBetweenExams(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::student1ContainsStudent2)
                .reward(HardSoftScore.ONE_SOFT, (exam1, exam2) -> {
                    LocalDateTime time1 = exam1.getScheduledTime();
                    LocalDateTime time2 = exam2.getScheduledTime();

                    if (time1 == null || time2 == null) return 0;

                    long hoursBetween = Math.abs(Duration.between(time1, time2).toHours());
                    return (int) Math.min(hoursBetween, 168); // Max 1 Woche
                })
                .asConstraint("Time between exams");
    }

    //helper method studentConflict
    private boolean haveSameStudents(AssessmentScheduleItem exam1, AssessmentScheduleItem exam2) {

        if (exam1 == exam2) return false;

        LocalDateTime time1 = exam1.getScheduledTime();
        LocalDateTime time2 = exam2.getScheduledTime();

        return student1ContainsStudent2(exam1, exam2) && Objects.equals(time1, time2);
    }

    //helper mehtod minimizeExamsPerDay
    private boolean sameDayAndStudent(AssessmentScheduleItem exam1, AssessmentScheduleItem exam2) {

        LocalDateTime temp1 = exam1.getScheduledTime();
        LocalDateTime temp2 = exam2.getScheduledTime();
        if (temp1 == null || temp2 == null) return false;

        LocalDate time1 = temp1.toLocalDate();
        LocalDate time2 = temp2.toLocalDate();


        return student1ContainsStudent2(exam1, exam2) && Objects.equals(time1, time2);
    }

    private boolean student1ContainsStudent2(AssessmentScheduleItem exam1, AssessmentScheduleItem exam2) {
        Set<String> student1 = exam1.getAssessment().getRegisteredStudents();
        Set<String> student2 = exam2.getAssessment().getRegisteredStudents();

        if (student1 == null || student2 == null) return false;

        return student1.stream().anyMatch(student2::contains);
    }
}
