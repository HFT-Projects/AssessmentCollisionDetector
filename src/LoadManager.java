import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadManager {
    public static void load_anmeldungen(String path, Pruefung[] pruefungen) throws IOException {
        Map<String, Set<String>> anmeldungen = new HashMap<>();
        List<String> rows = Files.readAllLines(Paths.get(path));

        for (Pruefung p : pruefungen) {
            String s = p.getName();
            assert !anmeldungen.containsKey(s);
            anmeldungen.put(s, new HashSet<>());
        }

        //TODO: assertions

        List<String> rows_without_title = rows.subList(1, rows.size());
        for (String row : rows_without_title) {
            String[] columns = row.split(";");

            String matr_nr = columns[0];
            String prue_name = columns[6];
            assert anmeldungen.containsKey(prue_name); //TODO
            anmeldungen.get(prue_name).add(matr_nr);
        }

        for (Pruefung p : pruefungen) {
            assert p.getAnmeldungen() == null : "The Anmeldungen of the Prüfung " + p + " was already loaded.";
            p.setAnmeldungen(anmeldungen.get(p.getName()));
        }
    }

    public static Pruefung[] load_missing_pruefungen_from_anmeldungen(String path, Pruefung[] pruefungen) throws IOException {
        List<Pruefung> new_pruefungen = new LinkedList<>();
        Map<String, Pruefung> pruefungen_by_name = new HashMap<>();

        for (Pruefung p : pruefungen) {
            assert !pruefungen_by_name.containsKey(p.getName()); //TODO
            pruefungen_by_name.put(p.getName(), p);
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        //TODO: assertions

        List<String> rows_without_title = rows.subList(1, rows.size());
        for (String row : rows_without_title) {
            String[] columns = row.split(";");
            long pruefungs_nr = Long.parseLong(columns[5]);
            String name = columns[6];
            if (!pruefungen_by_name.containsKey(name)) {
                Pruefung p = new Pruefung(pruefungs_nr, name, columns[2], columns[3], null, null);
                new_pruefungen.add(p);
                pruefungen_by_name.put(name, p);
            } else if (pruefungen_by_name.get(name).getNr() == null) {
                pruefungen_by_name.get(name)._overrideNr(pruefungs_nr);
            }
        }

        return new_pruefungen.toArray(new Pruefung[0]);
    }

    public static Pruefung[] load_pruefungen(String path) throws IOException {
        List<Pruefung> pruefungen = new LinkedList<>();
        Set<Long> existingPruefungen = new HashSet<>();
        List<String> rows = Files.readAllLines(Paths.get(path));

        //TODO: assertions

        int index_days_begin;
        int index_days_end;
        Calendar[] days;

        {
            List<String> columns = Arrays.asList(rows.get(0).split(";"));
            index_days_begin = columns.indexOf("Ende") + 1;
            index_days_end = columns.indexOf("Gruppe") - 1;

            days = new Calendar[index_days_end - index_days_begin + 1];

            for (int i = index_days_begin; i <= index_days_end; i++) {
                Pattern pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.");
                Matcher matcher = pattern.matcher(columns.get(i));
                matcher.find(); //TODO assert true
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                Calendar c = Calendar.getInstance();
                //TODO: assuming current year (fix with actual data)
                c.set(Calendar.MONTH, month - 1);
                c.set(Calendar.DAY_OF_MONTH, day);
                c.set(Calendar.SECOND, 0);
                days[i - index_days_begin] = c;
            }
        }

        List<String> rows_without_title = rows.subList(1, rows.size());
        for (String row : rows_without_title) {
            String[] columns = row.split(";");

            Long nr = columns[4].isBlank() ? null : Long.parseLong(columns[4]);

            if (existingPruefungen.contains(nr))
                continue;

            String name = columns[5];
            String begin_time = columns[10];
            String end_time = columns[11];

            Calendar begin = null;
            for (int i = index_days_begin; i <= index_days_end; i++) {
                if (!columns[i].isBlank()) {
                    begin = (Calendar) days[i - index_days_begin].clone();
                }
            }

            assert begin != null; //TODO: make sure it happens

            String[] begin_time_split = begin_time.split(":");
            begin.set(Calendar.HOUR_OF_DAY, Integer.parseInt(begin_time_split[0]));
            begin.set(Calendar.MINUTE, Integer.parseInt(begin_time_split[1]));


            String[] end_time_split = end_time.split(":");
            Calendar end = (Calendar) begin.clone();
            end.set(Calendar.HOUR, Integer.parseInt(end_time_split[0]));
            end.set(Calendar.MINUTE, Integer.parseInt(end_time_split[1]));

            long duration_min = (end.getTime().getTime() - begin.getTime().getTime()) / (1000 * 60);


            Pruefung p = new Pruefung(nr, name, columns[1], columns[2], begin, (int) duration_min);
            pruefungen.add(p);
            existingPruefungen.add(nr);
        }
        return pruefungen.toArray(new Pruefung[0]);
    }
}
