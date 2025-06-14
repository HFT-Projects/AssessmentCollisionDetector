public class OptimizationDebugLogger {
    private static final boolean ENABLE_DEBUG = true; // Set to false for production

    public static void logGroupOverview(int largeGroups, int smallGroups, long estimatedSeconds) {
        if (!ENABLE_DEBUG) return;

        System.out.println("=== GROUP OVERVIEW ===");
        System.out.println("Large groups (≥50): " + largeGroups);
        System.out.println("Small groups: " + smallGroups);
        System.out.println("Estimated time: " + estimatedSeconds + " seconds");
        System.out.println("=====================");
    }

    public static void logOptimizationStart(int largeGroupCount, int smallGroupCount) {
        if (!ENABLE_DEBUG) return;

        System.out.println("=== OPTIMIZATION STARTS ===");
        System.out.println("Large groups (≥50): " + largeGroupCount);
        System.out.println("Small groups: " + smallGroupCount);
    }

    public static void logLargeGroupOptimization(int assessmentCount) {
        if (!ENABLE_DEBUG) return;

        System.out.println("Optimizing large group: " + assessmentCount + " Assessments");
    }

    public static void logSmallGroupsOptimization(int groupCount) {
        if (!ENABLE_DEBUG) return;

        System.out.println("Optimizing " + groupCount + " small groups in parallel...");
    }

    public static void logFinalScore(String score) {
        if (!ENABLE_DEBUG) return;

        System.out.println("Final score: " + score);
    }

    public static void logProblem(String message) {
        if (!ENABLE_DEBUG) return;

        System.out.println("PROBLEM: " + message);
    }

    public static void logSkippingGroup(String reason) {
        if (!ENABLE_DEBUG) return;

        System.out.println("Skipping " + reason);
    }

    public static void logAssessmentTimeAnalysis(int withTimes, int withoutTimes) {
        if (!ENABLE_DEBUG) return;

        double percentage = 100.0 * withoutTimes / (withTimes + withoutTimes);
        System.out.println("=== ASSESSMENT TIME ANALYSIS ===");
        System.out.println("With times: " + withTimes);
        System.out.println("Without times: " + withoutTimes);
        System.out.println("Percentage without times: " + percentage + "%");
        System.out.println("===============================");
    }

    public static void logOptimizedTimes(data.MergedAssessment[] assessments) {
        if (!ENABLE_DEBUG) return;

        System.out.println("=== OPTIMIZED TIMES ===");
        for (data.MergedAssessment ma : assessments) {
            String name = ma.getQualifiedName();
            java.time.LocalDateTime originalBegin = ma.getBegin();
            java.time.LocalDateTime originalEnd = ma.getEnd();
            java.time.LocalDateTime optimizedBegin = ma.getOptimizedBegin();
            java.time.LocalDateTime optimizedEnd = ma.getOptimizedEnd();

            System.out.println(name + "," +
                    (originalBegin != null ? originalBegin : "null") + "," +
                    (originalEnd != null ? originalEnd : "null") + "," +
                    (optimizedBegin != null ? optimizedBegin : "null") + "," +
                    (optimizedEnd != null ? optimizedEnd : "null"));
        }
    }

    public static void logDebugAssessmentFiltering(data.MergedAssessment[][] assessmentGroups, String searchName) {
        if (!ENABLE_DEBUG) return;

        System.out.println("=== DEBUG: Searching for '" + searchName + "' ===");

        for (int i = 0; i < assessmentGroups.length; i++) {
            data.MergedAssessment[] group = assessmentGroups[i];

            boolean containsSearched = java.util.Arrays.stream(group)
                    .anyMatch(a -> a.getQualifiedName().contains(searchName));

            if (containsSearched) {
                System.out.println("Found in Group " + i + " (size: " + group.length + "):");

                for (data.MergedAssessment ma : group) {
                    String times = (ma.getBegin() != null) ? "HAS_TIME" : "NO_TIME";
                    System.out.println("  - " + ma.getQualifiedName() + " [" + times + "]");
                }

                data.MergedAssessment[] validAssessments = java.util.Arrays.stream(group)
                        .filter(a -> a.getBegin() != null && a.getEnd() != null)
                        .toArray(data.MergedAssessment[]::new);

                System.out.println("After filtering: " + validAssessments.length + " valid assessments");
                System.out.println("Will be optimized: " + (validAssessments.length >= 2));

                break;
            }
        }
    }
}