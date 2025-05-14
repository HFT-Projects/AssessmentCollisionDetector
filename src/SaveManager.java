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

        lines.add("Fach 1;Lfd. Nr.;Fach 1;Fach 2;Datum / Uhrzeit;Kollisionen;Abstand");
        Assessment[] assessmentsSorted = Arrays.stream(assessments).sorted(Comparator.comparing(Assessment::getQualifiedName)).toArray(Assessment[]::new);
        for (Assessment p : assessmentsSorted) {
            String durationString = getDurationString(p);
            String s = p.getQualifiedName() + ";;;;" + durationString + ";;;";
            if (durationString.equals("Kein Datum gef")) //TODO: remove this workaround (fixes missing ; in template file)
                s = s.substring(0, s.length() - 1);
            lines.add(s);

            int i = 1;
            Map<Assessment, Integer> collisions = p.getCollisions();
            Collection<Assessment> assessmentsSorted2 = collisions.keySet();
            assessmentsSorted2 = assessmentsSorted2.stream().sorted(Comparator.comparing(Assessment::getQualifiedName)).toList();
            for (Assessment k : assessmentsSorted2) {
                String distanceStr;
                if (p.getBegin() == null || k.getBegin() == null) {
                    distanceStr = "";
                } else {
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
                    if (distance < 0)
                        distanceStr = "Überschneidung";
                }
                String s2 = ";" + i + ";" + p.getQualifiedName() + ";" + k.getQualifiedName() + ";" + getDurationString(k) + ";" + collisions.get(k) + ";" + distanceStr;
                i++;
                lines.add(s2);
            }
        }

        lines.add("");  // add blank line at the end to match with template file

        Files.writeString(Paths.get(path), lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
    }
}
