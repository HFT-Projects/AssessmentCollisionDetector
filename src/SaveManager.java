import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SaveManager {
    private static String getDurationString(Assessment p) {
        if (p.getBegin() == null)
            return "Kein Datum gef";
        DateTimeFormatter formatterDay = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("H:mm");
        return p.getBegin().format(formatterDay) + " " + p.getBegin().format(formatterTime) + "-" + p.getEnd().format(formatterTime);
    }

    public static void saveCollision(String path, Assessment[] assessments) throws UncheckedIOException {
        List<String> lines = new LinkedList<>();

        // add header line
        lines.add("Fach 1;Lfd. Nr.;Fach 1;Fach 2;Datum / Uhrzeit;Kollisionen;Abstand");

        Assessment[] assessmentsSorted = Arrays.stream(assessments).sorted(Comparator.comparing(Assessment::getQualifiedName)).toArray(Assessment[]::new);
        for (Assessment p : assessmentsSorted) {
            // generate title line
            String headerLine = p.getQualifiedName() + ";;;;" + getDurationString(p) + ";;;";
            lines.add(headerLine);

            // get all assessments colling with p and sort them by qualifiedName
            Map<Assessment, Integer> collisionCountByAssessment = p.getCollisionCountByAssessment();
            Collection<Assessment> collidingAssessmentsSorted = collisionCountByAssessment.keySet();
            collidingAssessmentsSorted = collidingAssessmentsSorted.stream().sorted(Comparator.comparing(Assessment::getQualifiedName)).toList();

            int i = 1;
            for (Assessment k : collidingAssessmentsSorted) {
                String distanceStr;
                // calculate time distance; replace with empty string if one assessment has no date
                if (p.getBegin() == null || k.getBegin() == null) {
                    distanceStr = "";
                } else {
                    // calculate time distance between colliding assessments (end to begin)
                    Assessment first;
                    Assessment last;
                    if (p.getBegin().isBefore(k.getBegin())) {
                        first = p;
                        last = k;
                    } else {
                        first = k;
                        last = p;
                    }
                    long distance = Duration.between(first.getEnd(), last.getBegin()).toHours(); //TODO: remove workaround (currently we truncate -> round (to 0,25? or at least 1) instead)
                    distanceStr = String.format("%03d", distance);

                    // replace distance with string "Überschneidung" if assessments overlap
                    if (distance < 0)
                        distanceStr = "Überschneidung";
                }

                // generate entry line
                String entryLine = ";" + i + ";" + p.getQualifiedName() + ";" + k.getQualifiedName() + ";" + getDurationString(k) + ";" + collisionCountByAssessment.get(k) + ";" + distanceStr;

                i++;
                lines.add(entryLine);
            }
        }

        // add blank line at the end to match with template file
        lines.add("");

        try {
            Files.writeString(Paths.get(path), lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    // debug mehthod, can get deleted after verifying the algo is running good
    public static void saveOptimizedCollision(String path, MergedAssessment[] assessments) throws UncheckedIOException {
        List<String> lines = new LinkedList<>();

        // add header line
        lines.add("Fach 1;Lfd. Nr.;Fach 1;Fach 2;Datum / Uhrzeit;Kollisionen;Abstand");

        MergedAssessment[] assessmentsSorted = Arrays.stream(assessments)
                .sorted(Comparator.comparing(MergedAssessment::getName))
                .toArray(MergedAssessment[]::new);

        for (MergedAssessment p : assessmentsSorted) {
            // generate title line
            String headerLine = p.getName() + ";;;;" + getOptimizedDurationString(p) + ";;;";
            lines.add(headerLine);

            // get all assessments colling with p and sort them by name
            Map<Assessment, Integer> collisionCountByAssessment = p.getCollisionCountByAssessment();
            Collection<Assessment> collidingAssessmentsSorted = collisionCountByAssessment.keySet();
            collidingAssessmentsSorted = collidingAssessmentsSorted.stream()
                    .sorted(Comparator.comparing(Assessment::getQualifiedName))
                    .toList();

            int i = 1;
            for (Assessment k : collidingAssessmentsSorted) {
                String distanceStr;

                // Get optimized times for current assessment
                LocalDateTime pBegin = p.getOptimizedBegin() != null ? p.getOptimizedBegin() : p.getBegin();
                LocalDateTime pEnd = p.getOptimizedEnd() != null ? p.getOptimizedEnd() : p.getEnd();

                // calculate time distance, replace with empty string if one assessment has no date
                if (pBegin == null || k.getBegin() == null) {
                    distanceStr = "";
                } else {
                    // calculate time distance between colliding assessments (end to begin)
                    Assessment first;
                    Assessment last;
                    LocalDateTime firstBegin, firstEnd, lastBegin;

                    if (pBegin.isBefore(k.getBegin())) {
                        firstBegin = pBegin;
                        firstEnd = pEnd;
                        lastBegin = k.getBegin();
                    } else {
                        firstBegin = k.getBegin();
                        firstEnd = k.getEnd();
                        lastBegin = pBegin;
                    }

                    long distance = Duration.between(firstEnd, lastBegin).toHours();
                    distanceStr = String.format("%03d", distance);

                    // replace distance with string "Überschneidung" if assessments overlap
                    if (distance < 0)
                        distanceStr = "Überschneidung";
                }

                // generate entry line
                String entryLine = ";" + i + ";" + p.getName() + ";" + k.getQualifiedName() + ";" + getDurationString(k) + ";" + collisionCountByAssessment.get(k) + ";" + distanceStr;

                i++;
                lines.add(entryLine);
            }
        }

        // add blank line at the end to match with template file
        lines.add("");

        try {
            Files.writeString(Paths.get(path), lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getOptimizedDurationString(MergedAssessment p) {
        LocalDateTime begin = p.getOptimizedBegin() != null ? p.getOptimizedBegin() : p.getBegin();
        LocalDateTime end = p.getOptimizedEnd() != null ? p.getOptimizedEnd() : p.getEnd();

        if (begin == null)
            return "Kein Datum gef";
        DateTimeFormatter formatterDay = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("H:mm");
        return begin.format(formatterDay) + " " + begin.format(formatterTime) + "-" + end.format(formatterTime);
    }
}
