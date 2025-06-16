package manager.optimizer;

import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public class AssessmentSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                studentAtTwoAssessmentsConflict(factory), // hard constraint
                wayTooLittleTimeConflict(factory), // hard constraint (1 per Hour per Student)
                tooLittleTimeConflict(factory), // medium constraint
                minimizeAssessmentsPerDay(factory), // soft constraint
                maximizeTimeBetweenAssessments(factory) // soft constraint
        };
    }

    Constraint studentAtTwoAssessmentsConflict(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::checkStudentsAtTwoAssessmentsAtTheSameTime)
                .penalize(HardMediumSoftLongScore.ofHard(1000))
                .asConstraint("Student at two assessments conflict");
    }

    Constraint wayTooLittleTimeConflict(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::checkAssessmentsCollide)
                .penalizeLong(HardMediumSoftLongScore.ONE_HARD, (assessment1, assessment2) -> {
                    LocalDateTime begin1 = assessment1.getScheduledTime();
                    LocalDateTime begin2 = assessment2.getScheduledTime();

                    if (begin1 == null || begin2 == null) return 0;

                    AssessmentScheduleItem first;
                    AssessmentScheduleItem last;

                    if (begin1.isBefore(begin2)) {
                        first = assessment1;
                        last = assessment2;
                    } else {
                        first = assessment2;
                        last = assessment1;
                    }

                    double hoursBetween = Duration.between(first.getScheduledEndTime(), last.getScheduledTime()).toHours();

                    Integer collisions = assessment1.getAssessment().getCollisionCountByAssessment().get(assessment2.getAssessment());

                    return (long) Math.ceil(Math.max(0, 1 - hoursBetween) * collisions);
                })
                .asConstraint("way too little time conflict");
    }

    Constraint tooLittleTimeConflict(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::checkAssessmentsCollide)
                .penalizeLong(HardMediumSoftLongScore.ONE_MEDIUM, (assessment1, assessment2) -> {
                    LocalDateTime begin1 = assessment1.getScheduledTime();
                    LocalDateTime begin2 = assessment2.getScheduledTime();

                    if (begin1 == null || begin2 == null) return 0;

                    AssessmentScheduleItem first;
                    AssessmentScheduleItem last;

                    if (begin1.isBefore(begin2)) {
                        first = assessment1;
                        last = assessment2;
                    } else {
                        first = assessment2;
                        last = assessment1;
                    }

                    double hoursBetween = Duration.between(first.getScheduledEndTime(), last.getScheduledTime()).toHours();

                    Integer collisions = assessment1.getAssessment().getCollisionCountByAssessment().get(assessment2.getAssessment());

                    return (long) Math.ceil(Math.max(0, 3 - hoursBetween) * collisions);
                })
                .asConstraint("too little time conflict");
    }

    Constraint minimizeAssessmentsPerDay(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::checkStudentsHasMultipleAssessmentsAtOneDay)
                .penalize(HardMediumSoftLongScore.ONE_SOFT)
                .asConstraint("Multiple assessments per day");
    }

    Constraint maximizeTimeBetweenAssessments(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::checkAssessmentsCollide)
                .rewardLong(HardMediumSoftLongScore.ONE_SOFT, (assessment1, assessment2) -> {
                    LocalDateTime begin1 = assessment1.getScheduledTime();
                    LocalDateTime begin2 = assessment2.getScheduledTime();

                    if (begin1 == null || begin2 == null) return 0;

                    AssessmentScheduleItem first;
                    AssessmentScheduleItem last;

                    if (begin1.isBefore(begin2)) {
                        first = assessment1;
                        last = assessment2;
                    } else {
                        first = assessment2;
                        last = assessment1;
                    }

                    double hoursBetween = Duration.between(first.getScheduledEndTime(), last.getScheduledTime()).toHours();

                    Integer collisions = assessment1.getAssessment().getCollisionCountByAssessment().get(assessment2.getAssessment());

                    return Math.round(Math.sqrt(Math.min(hoursBetween, 36)) * Math.sqrt(collisions));
                })
                .asConstraint("maximize time between assessments");
    }

    //helper method studentAtTwoAssessmentsConflict
    private boolean checkStudentsAtTwoAssessmentsAtTheSameTime(AssessmentScheduleItem assessment1, AssessmentScheduleItem assessment2) {

        if (assessment1 == assessment2) return false;

        LocalDateTime begin1 = assessment1.getScheduledTime();
        LocalDateTime begin2 = assessment2.getScheduledTime();

        AssessmentScheduleItem first;
        AssessmentScheduleItem last;

        if (begin1.isBefore(begin2)) {
            first = assessment1;
            last = assessment2;
        } else {
            first = assessment2;
            last = assessment1;
        }

        double distance = Duration.between(first.getScheduledEndTime(), last.getScheduledTime()).toMinutes();

        return checkAssessmentsCollide(assessment1, assessment2) && distance < 0;
    }

    //helper method for minimizeAssessmentsPerDay
    private boolean checkStudentsHasMultipleAssessmentsAtOneDay(AssessmentScheduleItem assessment1, AssessmentScheduleItem assessment2) {

        LocalDateTime time1 = assessment1.getScheduledTime();
        LocalDateTime time2 = assessment2.getScheduledTime();

        if (time1 == null || time2 == null) return false;

        LocalDateTime day1 = LocalDateTime.of(time1.getYear(), time1.getMonth(), time1.getDayOfMonth(), 0, 0);
        LocalDateTime day2 = LocalDateTime.of(time2.getYear(), time2.getMonth(), time2.getDayOfMonth(), 0, 0);

        return checkAssessmentsCollide(assessment1, assessment2) && Objects.equals(day1, day2);
    }

    private boolean checkAssessmentsCollide(AssessmentScheduleItem assessment1, AssessmentScheduleItem assessment2) {
        Set<String> student1 = assessment1.getAssessment().getRegisteredStudents();
        Set<String> student2 = assessment2.getAssessment().getRegisteredStudents();

        if (student1 == null || student2 == null) return false;
        if (assessment1.equals(assessment2))
            return false;

        return student1.stream().anyMatch(student2::contains);
    }
}
