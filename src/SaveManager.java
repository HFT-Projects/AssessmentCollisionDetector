import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class SaveManager {
    private static String get_duration_string(Pruefung p) {
        if (p.getBegin() == null)
            return "Kein Datum gef";
        SimpleDateFormat sdf = new SimpleDateFormat("dd H:mm");
        SimpleDateFormat sdf2 = new SimpleDateFormat("H:mm");
        Calendar end = p.getBegin();
        end.add(Calendar.MINUTE, p.getDuration_min());
        return sdf.format(p.getBegin().getTime()) + "-" + sdf2.format(end.getTime());
    }

    public static void save_collision(String path, Pruefung[] pruefungen) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add("Fach 1;Lfd. Nr.;Fach 1;Fach 2;Datum / Uhrzeit;Kollisionen;Abstand");
        Pruefung[] pruefungen_sorted = Arrays.stream(pruefungen).sorted(Comparator.comparing(Pruefung::getQualified_name)).toArray(Pruefung[]::new);
        for (Pruefung p : pruefungen_sorted) {
            String s = p.getQualified_name() + ";;;;" + get_duration_string(p) + ";;;";
            lines.add(s);

            int i = 1;
            Map<Pruefung, Integer> collisions = p.getCollisions();
            Collection<Pruefung> pruefungen_sorted2 = collisions.keySet();
            pruefungen_sorted2 = pruefungen_sorted2.stream().sorted(Comparator.comparing(Pruefung::getQualified_name)).toList();
            for (Pruefung k : pruefungen_sorted2) {
                String distance_str;
                if (p.getBegin() == null || k.getBegin() == null) {
                    distance_str = "";
                } else {
                    Pruefung first;
                    Pruefung last;
                    if (p.getBegin().getTime().getTime() < k.getBegin().getTime().getTime()) {
                        first = p;
                        last = k;
                    } else {
                        first = k;
                        last = p;
                    }
                    long distance = (last.getBegin().getTime().getTime() - (first.getBegin().getTime().getTime() + first.getDuration_min() * 60 * 1000)) / (1000 * 60 * 60);
                    distance_str = String.format("%03d", distance);
                }
                String s2 = ";" + i + ";" + p.getQualified_name() + ";" + k.getQualified_name() + ";" + get_duration_string(k) + ";" + collisions.get(k) + ";" + distance_str;
                i++;
                lines.add(s2);
            }
        }

        Files.writeString(Paths.get(path), lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
    }
}
