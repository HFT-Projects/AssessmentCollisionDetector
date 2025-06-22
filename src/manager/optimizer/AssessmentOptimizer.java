package manager.optimizer;

import data.Assessment;
import data.MergedAssessment;
import data.MergedAssessmentEditable;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class AssessmentOptimizer {
    // Optimization timeouts
    private static final int LARGE_GROUP_THRESHOLD = 36;
    private static final String MOVE_THREAD_COUNT = "1";
    private static final int DEFAULT_START_HOUR = 8;
    private static final int DEFAULT_END_HOUR = 18;
    private static final int MAX_WEEKDAY = 5;

    // WARNING: calling this method invalidates all previous created MergesAssessments. Using them afterward can result in unexpected behavior.
    // merges all Assessments with the same name, begin & end into one together into one MergedAssessment
    public static MergedAssessment[] mergeAssessments(Assessment[] assessments) {
        MergedAssessmentEditable._resetAssessmentToMergedAssessmentMap();
        Map<AssessmentIdentifier, MergedAssessment> mergedAssessments = new HashMap<>();

        for (Assessment a : assessments) {
            // create an AssessmentIdentifier and merge all Assessments which match the same Identifier into one MergedAssessment
            AssessmentIdentifier ai = new AssessmentIdentifier(a.getName(), a.getBegin(), a.getEnd());

            if (mergedAssessments.containsKey(ai)) {
                List<Assessment> currentAssessments = new LinkedList<>(Arrays.asList(mergedAssessments.get(ai).getAssessments()));
                currentAssessments.add(a);
                ((MergedAssessmentEditable) mergedAssessments.get(ai)).setAssessments(currentAssessments.toArray(new Assessment[0]));
            } else {
                MergedAssessmentEditable ma = new MergedAssessmentEditable();
                ma.setAssessments(new Assessment[]{a});
                mergedAssessments.put(ai, ma);
            }
        }

        return mergedAssessments.values().toArray(new MergedAssessment[0]);
    }

    // puts all MergedAssessments which are related together.
    // related doesn't necessarily mean that they collide directly because they could collide indirectly (e.g. a - b - c)
    public static MergedAssessment[][] getAssessmentGroups(MergedAssessment[] mergedAssessments) {
        Set<MergedAssessment> alreadyProcessed = new HashSet<>();
        List<List<MergedAssessment>> assessmentGroups = new LinkedList<>();

        for (MergedAssessment ma : mergedAssessments) {
            if (alreadyProcessed.contains(ma))
                continue;
            List<MergedAssessment> assessmentGroup = new LinkedList<>();

            getAssessmentGroupsRecursive(ma, assessmentGroup);

            alreadyProcessed.addAll(assessmentGroup);
            assessmentGroups.add(assessmentGroup);
        }

        return assessmentGroups.stream().map((mal) -> mal.toArray(new MergedAssessment[0])).toArray(MergedAssessment[][]::new);
    }

    private static void getAssessmentGroupsRecursive(MergedAssessment assessment, List<MergedAssessment> assessmentGroup) {
        assessmentGroup.add(assessment);
        for (MergedAssessment a : assessment.getCollisionCountByAssessment().keySet()) {
            if (assessmentGroup.contains(a))
                continue;
            getAssessmentGroupsRecursive(a, assessmentGroup);
        }
    }

    // ATTENTION: when optimizing via groups, it's impossible to respect rooms / supervisors because they need to be respected
    // over all groups and not only side one group.
    public static MergedAssessment[] optimizeAssessmentGroups(MergedAssessment[][] assessmentGroups, double timeout, Runnable hardConstraintViolatedCallback) {
        AssessmentSchedulingConstraintProvider.respectRooms = false;
        AssessmentSchedulingConstraintProvider.respectSupervisors = false;

        List<MergedAssessment[]> groups = new ArrayList<>();

        for (MergedAssessment[] group : assessmentGroups) {
            //noinspection DuplicatedCode
            Arrays.stream(group).filter(ma -> ma.getBegin() != null && ma.getEnd() != null).map(ma -> (MergedAssessmentEditable) ma).forEach(ma -> {
                ma.setOptimizedBegin(ma.getBegin());
                ma.setOptimizedEnd(ma.getEnd());
            });

            // only assessments with times can be optimized.
            MergedAssessment[] validAssessments = Arrays.stream(group)
                    .filter(a -> a.getBegin() != null && a.getEnd() != null)
                    .toArray(MergedAssessment[]::new);

            if (validAssessments.length <= 1) {
                continue;
            }

            groups.add(validAssessments);
        }

        // optimize larger groups
        groups.stream().filter(a -> a.length > LARGE_GROUP_THRESHOLD).sorted(Comparator.comparing(a -> a.length, Comparator.reverseOrder())).forEach(a -> optimizeAssessments(a, true, Math.round(timeout * ((double) 2 / 3) * 60), hardConstraintViolatedCallback));

        // optimize smaller groups in parallel & with smaller time limit
        groups.stream().filter(a -> a.length <= LARGE_GROUP_THRESHOLD).sorted(Comparator.comparing(a -> a.length, Comparator.reverseOrder())).parallel().forEach(a -> optimizeAssessments(a, false, Math.round(timeout * ((double) 1 / 3) * 60), hardConstraintViolatedCallback));

        return Arrays.stream(assessmentGroups).flatMap(Arrays::stream).toArray(MergedAssessment[]::new);
    }

    public static MergedAssessment[] optimizeAssessments(MergedAssessment[] assessments, boolean respectRooms, boolean respectSupervisors, double timeout, Runnable hardConstraintViolatedCallback) {
        AssessmentSchedulingConstraintProvider.respectRooms = respectRooms;
        AssessmentSchedulingConstraintProvider.respectSupervisors = respectSupervisors;

        //noinspection DuplicatedCode
        Arrays.stream(assessments).filter(ma -> ma.getBegin() != null && ma.getEnd() != null).map(ma -> (MergedAssessmentEditable) ma).forEach(ma -> {
            ma.setOptimizedBegin(ma.getBegin());
            ma.setOptimizedEnd(ma.getEnd());
        });

        // only assessments with times can be optimized.
        MergedAssessment[] validAssessments = Arrays.stream(assessments)
                .filter(a -> a.getBegin() != null && a.getEnd() != null)
                .toArray(MergedAssessment[]::new);

        if (validAssessments.length <= 1) {
            return assessments;
        }

        optimizeAssessments(validAssessments, true, Math.round(timeout * 60), hardConstraintViolatedCallback);

        return assessments;
    }

    private static void optimizeAssessments(MergedAssessment[] assessments, boolean isLargeGroup, long timeoutSeconds, Runnable hardConstraintViolatedCallback) {
        // get all possible time slots
        List<LocalDateTime> timeSlots = generateTimeSlots(assessments);

        // create AssessmentScheduleItems for all assessments
        List<AssessmentScheduleItem> wrappers = Arrays.stream(assessments)
                .map(AssessmentScheduleItem::new)
                .toList();

        AssessmentSchedulingSolution problem = new AssessmentSchedulingSolution(wrappers, timeSlots);

        SolverConfig config = new SolverConfig()
                .withSolutionClass(AssessmentSchedulingSolution.class)
                .withEntityClasses(AssessmentScheduleItem.class)
                .withConstraintProviderClass(AssessmentSchedulingConstraintProvider.class);
        //.withEnvironmentMode(EnvironmentMode.FAST_ASSERT);

        if (isLargeGroup) {
            config.withTerminationSpentLimit(Duration.ofSeconds(timeoutSeconds))
                    // use multiple threads
                    .withMoveThreadCount(MOVE_THREAD_COUNT);
        } else {
            config.withTerminationSpentLimit(Duration.ofSeconds(timeoutSeconds));
        }

        SolverFactory<AssessmentSchedulingSolution> solverFactory = SolverFactory.create(config);
        Solver<AssessmentSchedulingSolution> solver = solverFactory.buildSolver();
        @SuppressWarnings("unused")
        AssessmentSchedulingSolution solution = solver.solve(problem);

        solution.getAssessmentList().forEach(a -> {
            ((MergedAssessmentEditable) a.getAssessment()).setOptimizedBegin(a.getScheduledTime());
            ((MergedAssessmentEditable) a.getAssessment()).setOptimizedEnd(a.getScheduledEndTime());
        });

        if (solution.getScore().hardScore() > 0)
            hardConstraintViolatedCallback.run();

        // System.out.println(assessments.length + "; " + solution.getScore().toString());
    }

    private static List<LocalDateTime> generateTimeSlots(MergedAssessment[] assessments) {
        TimeRange timeRange = findTimeRange(assessments);
        if (timeRange.earliest == null || timeRange.latest == null) {
            return new ArrayList<>();
        }

        Set<LocalTime> actualAssessmentTimes = collectActualAssessmentTimes(assessments);
        return createTimeSlotsFromActualTimes(timeRange, actualAssessmentTimes);
    }

    private static TimeRange findTimeRange(MergedAssessment[] assessments) {
        LocalDateTime earliest = null;
        LocalDateTime latest = null;

        for (MergedAssessment assessment : assessments) {
            LocalDateTime begin = assessment.getBegin();
            LocalDateTime end = assessment.getEnd();

            if (begin != null && (earliest == null || begin.isBefore(earliest))) {
                earliest = begin;
            }
            if (end != null && (latest == null || end.isAfter(latest))) {
                latest = end;
            }
        }

        return new TimeRange(earliest, latest);
    }

    private static Set<LocalTime> collectActualAssessmentTimes(MergedAssessment[] assessments) {
        Set<LocalTime> AssessmentTimes = new HashSet<>();
        for (MergedAssessment assessment : assessments) {
            if (assessment.getBegin() != null) {
                AssessmentTimes.add(assessment.getBegin().toLocalTime());
            }
        }

        int minTimesNeeded = Math.max(assessments.length, 5);
        if (AssessmentTimes.size() < minTimesNeeded) {
            for (int hour = DEFAULT_START_HOUR; hour <= DEFAULT_END_HOUR; hour++) {
                AssessmentTimes.add(LocalTime.of(hour, 0));
                AssessmentTimes.add(LocalTime.of(hour, 30));
                if (AssessmentTimes.size() >= minTimesNeeded) break;
            }
        }

        return AssessmentTimes;
    }

    private static List<LocalDateTime> createTimeSlotsFromActualTimes(TimeRange timeRange, Set<LocalTime> assessmentTimes) {
        List<LocalDateTime> timeSlots = new ArrayList<>();

        LocalDate startDate = timeRange.earliest.toLocalDate();
        LocalDate endDate = timeRange.latest.toLocalDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() <= MAX_WEEKDAY) {
                for (LocalTime time : assessmentTimes) {
                    timeSlots.add(LocalDateTime.of(date, time));
                }
            }
        }

        return timeSlots;
    }

    // Helper Records
    private record TimeRange(LocalDateTime earliest, LocalDateTime latest) {
    }

    private record AssessmentIdentifier(String name, LocalDateTime begin, LocalDateTime end) {
    }
}