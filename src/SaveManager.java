import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
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

    public static void saveCollision(String path, Assessment[] assessments) throws IOException {
        List<String> lines = new ArrayList<>();

        // add header line
        lines.add("Fach 1;Lfd. Nr.;Fach 1;Fach 2;Datum / Uhrzeit;Kollisionen;Abstand");

        Assessment[] assessmentsSorted = Arrays.stream(assessments).sorted(Comparator.comparing(Assessment::getQualifiedName)).toArray(Assessment[]::new);
        for (Assessment p : assessmentsSorted) {
            // generate title line
            String durationString = getDurationString(p);
            String header_line = p.getQualifiedName() + ";;;;" + durationString + ";;;";
            if (durationString.equals("Kein Datum gef")) //TODO: remove this workaround (fixes missing ; in template file)
                header_line = header_line.substring(0, header_line.length() - 1);
            lines.add(header_line);

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
                String entry_line = ";" + i + ";" + p.getQualifiedName() + ";" + k.getQualifiedName() + ";" + getDurationString(k) + ";" + collisionCountByAssessment.get(k) + ";" + distanceStr;

                i++;
                lines.add(entry_line);
            }
        }

        // add blank line at the end to match with template file
        lines.add("");

        Files.writeString(Paths.get(path), lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
    }
}
