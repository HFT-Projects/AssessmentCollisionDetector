package optimizer;

import data.*;
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
    private static final Duration SMALL_GROUP_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration LARGE_GROUP_TIMEOUT = Duration.ofMinutes(1);
    private static final int LARGE_GROUP_THRESHOLD = 50;
    private static final String MOVE_THREAD_COUNT = "1";
    private static final int DEFAULT_START_HOUR = 8;
    private static final int DEFAULT_END_HOUR = 18;
    private static final int MAX_WEEKDAY = 5;
    private static final boolean VERBOSE_LOGGING = false;

    private static final Set<MergedAssessment> optimizedAssessments = new HashSet<>();

    // ATTENTION: calling this method invalidates all previous created MergesAssessments. Using them afterward can result in unexpected behavior.
    public static MergedAssessment[] mergeAssessments(Assessment[] assessments) {
        MergedAssessmentEditable._resetAssessmentToMergedAssessmentMap();
        Map<AssessmentIdentifier, MergedAssessment> mergedAssessments = new HashMap<>();

        for (Assessment a : assessments) {
            AssessmentIdentifier ai = new AssessmentIdentifier(a.getName(), a.getBegin(), a.getEnd());

            if (mergedAssessments.containsKey(ai)) {
                List<Assessment> currentAssessments = new LinkedList<>(Arrays.asList(mergedAssessments.get(ai).getAssessments()));
                currentAssessments.add(a);
                ((MergedAssessmentEditable)mergedAssessments.get(ai)).setAssessments(currentAssessments.toArray(new Assessment[0]));
            } else {
                MergedAssessmentEditable ma = new MergedAssessmentEditable();
                ma.setAssessments(new Assessment[]{a});
                mergedAssessments.put(ai, ma);
            }
        }

        return mergedAssessments.values().toArray(new MergedAssessment[0]);
    }

    private static void getAssessmentGroupsRecursive(MergedAssessment assessment, List<MergedAssessment> assessmentGroup) {
        assessmentGroup.add(assessment);
        for (MergedAssessment a : assessment.getCollisionCountByAssessment().keySet()) {
            if (assessmentGroup.contains(a))
                continue;
            getAssessmentGroupsRecursive(a, assessmentGroup);
        }
    }

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

    public static MergedAssessment[] optimizeAssessmentGroups(MergedAssessment[][] assessmentGroups) {
        List<MergedAssessment[]> largeGroups = new ArrayList<>();
        List<MergedAssessment[]> smallGroups = new ArrayList<>();

        for (MergedAssessment[] group : assessmentGroups) {
            if (group.length < 2) {
                if (VERBOSE_LOGGING) {
                    OptimizationDebugLogger.logSkippingGroup("single group: " + group[0].getName());
                }
                continue;
            }

            MergedAssessment[] validAssessments = Arrays.stream(group)
                    .filter(a -> a.getBegin() != null && a.getEnd() != null)
                    .toArray(MergedAssessment[]::new);

            if (validAssessments.length < 2) {
                if (VERBOSE_LOGGING) {
                    OptimizationDebugLogger.logSkippingGroup("group: not enough valid assessments");
                }
                continue;
            }

            if (validAssessments.length >= LARGE_GROUP_THRESHOLD) {
                largeGroups.add(validAssessments);
            } else {
                smallGroups.add(validAssessments);
            }
        }

        OptimizationDebugLogger.logOptimizationStart(largeGroups.size(), smallGroups.size());

        for (MergedAssessment[] largeGroup : largeGroups) {
            OptimizationDebugLogger.logLargeGroupOptimization(largeGroup.length);
            optimizeAssessments(largeGroup, true);
        }

        if (!smallGroups.isEmpty()) {
            OptimizationDebugLogger.logSmallGroupsOptimization(smallGroups.size());
            optimizeSmallGroupsParallel(smallGroups);
        }

        MergedAssessment[] allAssessments = Arrays.stream(assessmentGroups).flatMap(Arrays::stream).toArray(MergedAssessment[]::new);
        fillMissingOptimizedTimes(allAssessments);

        return allAssessments;
    }

    public static void optimizeAssessments(MergedAssessment[] assessments) {
        optimizeAssessments(assessments, false);
    }

    private static void optimizeAssessments(MergedAssessment[] assessments, boolean isLargeGroup) {
        List<LocalDateTime> timeSlots = generateTimeSlots(assessments);
        List<AssessmentScheduleItem> wrappers = Arrays.stream(assessments)
                .map(AssessmentScheduleItem::new)
                .toList();

        ExamSchedulingSolution problem = new ExamSchedulingSolution(wrappers, timeSlots);

        SolverConfig config = new SolverConfig()
                .withSolutionClass(ExamSchedulingSolution.class)
                .withEntityClasses(AssessmentScheduleItem.class)
                .withConstraintProviderClass(ExamSchedulingConstraintProvider.class);

        if (isLargeGroup) {
            config.withTerminationSpentLimit(LARGE_GROUP_TIMEOUT)
                    .withMoveThreadCount(MOVE_THREAD_COUNT);
        } else {
            config.withTerminationSpentLimit(SMALL_GROUP_TIMEOUT);
        }

        SolverFactory<ExamSchedulingSolution> solverFactory = SolverFactory.create(config);
        Solver<ExamSchedulingSolution> solver = solverFactory.buildSolver();
        ExamSchedulingSolution solution = solver.solve(problem);

        for (AssessmentScheduleItem item : solution.getAssessmentList()) {
            item.applyScheduleToAssessment();
            optimizedAssessments.add(item.getAssessment());
        }

        OptimizationDebugLogger.logFinalScore(solution.getScore().toString());
    }

    private static void optimizeSmallGroupsParallel(List<MergedAssessment[]> smallGroups) {
        smallGroups.parallelStream().forEach(group -> optimizeAssessments(group, false));
    }

    public static void logGroupStatistics(MergedAssessment[][] assessmentGroups) {
        int largeGroups = 0;
        int smallGroups = 0;

        for (MergedAssessment[] group : assessmentGroups) {
            if (group.length < 2) {
                continue;
            }

            MergedAssessment[] validAssessments = Arrays.stream(group)
                    .filter(a -> a.getBegin() != null && a.getEnd() != null)
                    .toArray(MergedAssessment[]::new);

            if (validAssessments.length < 2) {
                continue;
            }

            if (validAssessments.length >= LARGE_GROUP_THRESHOLD) {
                largeGroups++;
            } else {
                smallGroups++;
            }
        }

        long estimatedSeconds = (largeGroups * LARGE_GROUP_TIMEOUT.getSeconds()) +
                (smallGroups * SMALL_GROUP_TIMEOUT.getSeconds());

        OptimizationDebugLogger.logGroupOverview(largeGroups, smallGroups, estimatedSeconds);
    }

    private static List<LocalDateTime> generateTimeSlots(MergedAssessment[] assessments) {
        TimeRange timeRange = findTimeRange(assessments);
        if (timeRange.earliest == null || timeRange.latest == null) {
            OptimizationDebugLogger.logProblem("earliest or latest is null!");
            return new ArrayList<>();
        }

        Set<LocalTime> actualExamTimes = collectActualExamTimes(assessments);
        return createTimeSlotsFromActualTimes(timeRange, actualExamTimes);
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

    private static Set<LocalTime> collectActualExamTimes(MergedAssessment[] assessments) {
        Set<LocalTime> examTimes = new HashSet<>();
        for (MergedAssessment assessment : assessments) {
            if (assessment.getBegin() != null) {
                examTimes.add(assessment.getBegin().toLocalTime());
            }
        }

        int minTimesNeeded = Math.max(assessments.length, 5);
        if (examTimes.size() < minTimesNeeded) {
            for (int hour = DEFAULT_START_HOUR; hour <= DEFAULT_END_HOUR; hour++) {
                examTimes.add(LocalTime.of(hour, 0));
                examTimes.add(LocalTime.of(hour, 30));
                if (examTimes.size() >= minTimesNeeded) break;
            }
        }

        return examTimes;
    }

    private static List<LocalDateTime> createTimeSlotsFromActualTimes(TimeRange timeRange, Set<LocalTime> examTimes) {
        List<LocalDateTime> timeSlots = new ArrayList<>();

        LocalDate startDate = timeRange.earliest.toLocalDate();
        LocalDate endDate = timeRange.latest.toLocalDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() <= MAX_WEEKDAY) {
                for (LocalTime time : examTimes) {
                    timeSlots.add(LocalDateTime.of(date, time));
                }
            }
        }

        return timeSlots;
    }

    private static void fillMissingOptimizedTimes(MergedAssessment[] assessments) {
        for (MergedAssessment ma : assessments) {
            if (!optimizedAssessments.contains(ma) &&
                    ma.getOptimizedBegin() == null &&
                    ma.getBegin() != null) {

                ((MergedAssessmentEditable) ma).setOptimizedBegin(ma.getBegin());
                ((MergedAssessmentEditable) ma).setOptimizedEnd(ma.getEnd());
            }
        }
        optimizedAssessments.clear();
    }

    // Development/Debug methods - can be removed for production
    public static void analyzeAssessmentTimes(MergedAssessment[] assessments) {
        int withTimes = 0;
        int withoutTimes = 0;

        for (MergedAssessment ma : assessments) {
            if (ma.getBegin() != null) {
                withTimes++;
            } else {
                withoutTimes++;
            }
        }

        OptimizationDebugLogger.logAssessmentTimeAnalysis(withTimes, withoutTimes);
    }

    public static void printOptimizedTimes(MergedAssessment[] assessments) {
        OptimizationDebugLogger.logOptimizedTimes(assessments);
    }

    public static void debugAssessmentFiltering(MergedAssessment[][] assessmentGroups, String searchName) {
        OptimizationDebugLogger.logDebugAssessmentFiltering(assessmentGroups, searchName);
    }

    // Helper Records
    private record TimeRange(LocalDateTime earliest, LocalDateTime latest) {}
    private record AssessmentIdentifier(String name, LocalDateTime begin, LocalDateTime end) {}
}