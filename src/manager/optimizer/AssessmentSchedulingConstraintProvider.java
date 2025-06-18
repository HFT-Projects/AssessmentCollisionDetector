package manager.optimizer;

import data.AssessmentBase;
import data.Assessment;

import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class AssessmentSchedulingConstraintProvider implements ConstraintProvider {
    public static boolean respect_rooms;
    public static boolean respect_supervisors;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                studentAtTwoAssessmentsConflict(factory), // hard constraint (100000)
                roomConflict(factory),  // hard constraint (500)
                supervisorConflict(factory), // hard constraint (500)
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
                .penalize(HardMediumSoftLongScore.ofHard(100000))
                .asConstraint("Student at two assessments conflict");
    }

    Constraint roomConflict(ConstraintFactory factory) {
        if (!respect_rooms)
            return factory
                    .forEach(AssessmentScheduleItem.class)
                    .filter(a -> false)
                    .penalize(HardMediumSoftLongScore.ONE_HARD, a -> 0)
                    .asConstraint("room conflict no_op");
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter((assessment1, assessment2) -> {
                            if (assessment1 == assessment2)
                                return false;

                            Duration distance = getDistance(assessment1, assessment2);
                            if (distance == null || distance.toMinutes() >= 0)
                                return false;

                            Set<String> rooms1 = new HashSet<>();
                            Set<String> rooms2 = new HashSet<>();

                            for (Assessment a : assessment1.getAssessment().getAssessments()) {
                                for (AssessmentBase.AssessmentEntry ae : a.getAssessmentEntries()) {
                                    rooms1.add(ae.room());
                                }
                            }

                            for (Assessment a : assessment2.getAssessment().getAssessments()) {
                                for (AssessmentBase.AssessmentEntry ae : a.getAssessmentEntries()) {
                                    rooms2.add(ae.room());
                                }
                            }

                            return rooms1.stream().anyMatch(rooms2::contains);
                        })
                .penalize(HardMediumSoftLongScore.ofHard(500))
                .asConstraint("room conflict");
    }

    Constraint supervisorConflict(ConstraintFactory factory) {
        if (!respect_rooms)
            return factory
                    .forEach(AssessmentScheduleItem.class)
                    .filter(a -> false)
                    .penalize(HardMediumSoftLongScore.ONE_HARD, a -> 0)
                    .asConstraint("supervisor conflict no_op");
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter((assessment1, assessment2) -> {
                    if (assessment1 == assessment2)
                        return false;

                    Duration distance = getDistance(assessment1, assessment2);
                    if (distance == null || distance.toMinutes() >= 0)
                        return false;

                    Set<String> supervisors1 = new HashSet<>();
                    Set<String> supervisors2 = new HashSet<>();

                    for (Assessment a : assessment1.getAssessment().getAssessments()) {
                        for (AssessmentBase.AssessmentEntry ae : a.getAssessmentEntries()) {
                            supervisors1.add(ae.supervisor());
                        }
                    }

                    for (Assessment a : assessment2.getAssessment().getAssessments()) {
                        for (AssessmentBase.AssessmentEntry ae : a.getAssessmentEntries()) {
                            supervisors2.add(ae.supervisor());
                        }
                    }

                    return supervisors1.stream().anyMatch(supervisors2::contains);
                })
                .penalize(HardMediumSoftLongScore.ofHard(500))
                .asConstraint("supervisor conflict");
    }

    Constraint wayTooLittleTimeConflict(ConstraintFactory factory) {
        return factory
                .forEach(AssessmentScheduleItem.class)
                .join(AssessmentScheduleItem.class)
                .filter(this::checkAssessmentsCollide)
                .penalizeLong(HardMediumSoftLongScore.ONE_HARD, (assessment1, assessment2) -> {
                    Duration distance = getDistance(assessment1, assessment2);

                    if (distance == null)
                        return 0;

                    double hoursBetween = distance.toHours();

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
                    Duration distance = getDistance(assessment1, assessment2);

                    if (distance == null)
                        return 0;

                    double hoursBetween = distance.toHours();

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
                    Duration distance = getDistance(assessment1, assessment2);

                    if (distance == null)
                        return 0;

                    double hoursBetween = distance.toHours();

                    Integer collisions = assessment1.getAssessment().getCollisionCountByAssessment().get(assessment2.getAssessment());

                    return Math.round(Math.sqrt(Math.min(hoursBetween, 36)) * Math.sqrt(collisions));
                })
                .asConstraint("maximize time between assessments");
    }

    //helper method studentAtTwoAssessmentsConflict
    private boolean checkStudentsAtTwoAssessmentsAtTheSameTime(AssessmentScheduleItem assessment1, AssessmentScheduleItem assessment2) {
        if (assessment1 == assessment2) return false;

        Duration distance = getDistance(assessment1, assessment2);

        return checkAssessmentsCollide(assessment1, assessment2) && distance != null && distance.toMinutes() < 0;
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

    private Duration getDistance(AssessmentScheduleItem assessment1, AssessmentScheduleItem assessment2) {
        LocalDateTime begin1 = assessment1.getScheduledTime();
        LocalDateTime begin2 = assessment2.getScheduledTime();

        if (begin1 == null || begin2 == null)
            return null;

        AssessmentScheduleItem first;
        AssessmentScheduleItem last;

        if (begin1.isBefore(begin2)) {
            first = assessment1;
            last = assessment2;
        } else {
            first = assessment2;
            last = assessment1;
        }

        return Duration.between(first.getScheduledEndTime(), last.getScheduledTime());
    }
}
