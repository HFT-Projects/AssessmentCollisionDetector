import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class LoadManager {
    public static Pruefung[] load_pruefungen(String path) throws IOException {
        List<Pruefung> pruefungen = new LinkedList<>();
        Set<String> existingPruefungen = new HashSet<>(); // check existingPruefungen by qualifiedName
        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        int index_days_begin;
        int index_days_end;
        LocalDateTime[] days;

        {
            List<String> columns = Arrays.asList(rows.get(0).split(";"));
            index_days_begin = columns.indexOf("Ende") + 1;
            index_days_end = columns.indexOf("Gruppe") - 1;

            days = new LocalDateTime[index_days_end - index_days_begin + 1];

            for (int i = index_days_begin; i <= index_days_end; i++) {
                Pattern pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.");
                Matcher matcher = pattern.matcher(columns.get(i));
                if (!matcher.find())
                    throw new AssertionError("date has unexpected format");
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                //TODO: using year 1900 (fix with actual data)
                LocalDateTime d = LocalDateTime.of(1900, month, day, 0, 0, 0);
                days[i - index_days_begin] = d;
            }
        }

        List<String> rows_without_title = rows.subList(1, rows.size());
        for (String row : rows_without_title) {
            String[] columns = row.split(";");

            if (columns[1].isBlank() || /*columns[2].isBlank() || columns[4].isBlank() ||*/ columns[5].isBlank() || columns[10].isBlank() || columns[11].isBlank())
                throw new AssertionError("missing data in pruefungen file"); //TODO: specify whats missing & line

            Long nr = columns[4].isBlank() ? null : Long.parseLong(columns[4]);
            String name = columns[5];
            String stg = columns[1];
            String pversion = columns[2];

            String qualified_name = Pruefung.calculateQualifiedName(stg, pversion, nr, name);

            if (existingPruefungen.contains(qualified_name))
                continue;


            String begin_time = columns[10];
            String end_time = columns[11];

            LocalDateTime day = null;
            for (int i = index_days_begin; i <= index_days_end; i++) {
                if (!columns[i].isBlank()) {
                    day = days[i - index_days_begin];
                    break;
                }
            }
            // assert that the loop actually hits eventually
            if (day == null)
                throw new AssertionError("the pruefung " + nr + " " + name + "doesn't have a date.");

            String[] begin_time_split = begin_time.split(":");
            int hours = Integer.parseInt(begin_time_split[0]);
            int minutes = Integer.parseInt(begin_time_split[1]);
            LocalDateTime begin = day.plusMinutes(hours * 60L + minutes);


            String[] end_time_split = end_time.split(":");
            int hours2 = Integer.parseInt(end_time_split[0]);
            int minutes2 = Integer.parseInt(end_time_split[1]);
            LocalDateTime end = day.plusMinutes(hours2 * 60L + minutes2);

            Pruefung p = new Pruefung(nr, name, columns[1], columns[2], begin, end);
            pruefungen.add(p);
            existingPruefungen.add(qualified_name);
        }
        return pruefungen.toArray(new Pruefung[0]);
    }

    public static Pruefung[] load_missing_pruefungen_from_anmeldungen(String path, Pruefung[] pruefungen) throws IOException {
        List<Pruefung> new_pruefungen = new LinkedList<>();
        Map<String, Pruefung> pruefungen_by_qualified_name = new HashMap<>();

        for (Pruefung p : pruefungen) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (pruefungen_by_qualified_name.containsKey(p.getQualified_name()))
                throw new AssertionError("there are two pruefungen with the same name");
            pruefungen_by_qualified_name.put(p.getQualified_name(), p);
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        List<String> rows_without_title = rows.subList(1, rows.size());
        for (String row : rows_without_title) {
            String[] columns = row.split(";");
            long pruefungs_nr = Long.parseLong(columns[5]);
            String name = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualified_name = Pruefung.calculateQualifiedName(stg, pversion, pruefungs_nr, name);
            if (!pruefungen_by_qualified_name.containsKey(qualified_name)) {
                Pruefung p = new Pruefung(pruefungs_nr, name, columns[2], columns[3], null, null);
                new_pruefungen.add(p);
                pruefungen_by_qualified_name.put(qualified_name, p);
            }
        }

        return new_pruefungen.toArray(new Pruefung[0]);
    }

    public static void load_anmeldungen(String path, Pruefung[] pruefungen) throws IOException {
        Map<String, Set<String>> anmeldungen = new HashMap<>(); // Pruefung.qualifiedName -> MatrNr

        for (Pruefung p : pruefungen) {
            String qualified_name = p.getQualified_name();
            // the following exception should never occur (-> internal logic error -> bug)
            if (anmeldungen.containsKey(qualified_name))
                throw new AssertionError("there are two pruefungen with the same name");
            anmeldungen.put(qualified_name, new HashSet<>());
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        List<String> rows_without_title = rows.subList(1, rows.size());
        for (String row : rows_without_title) {
            String[] columns = row.split(";");

            //TODO: assert
            String matr_nr = columns[0];
            long pruefungs_nr = Long.parseLong(columns[5]);
            String pruefungs_name = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualified_name = Pruefung.calculateQualifiedName(stg, pversion, pruefungs_nr, pruefungs_name);
            // the following exception should never occur (-> internal logic error -> bug)
            if (!anmeldungen.containsKey(qualified_name))
                throw new AssertionError("a user anmeldung references a unknown pruefung: " + qualified_name);
            anmeldungen.get(qualified_name).add(matr_nr);
        }

        for (Pruefung p : pruefungen) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getAnmeldungen() != null)
                throw new AssertionError("The Anmeldungen of the Prüfung " + p + " was already loaded.");
            p.setAnmeldungen(anmeldungen.get(p.getQualified_name()));
        }
    }
}
