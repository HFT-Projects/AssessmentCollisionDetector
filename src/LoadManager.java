import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class LoadManager {
    public static Pruefung[] loadPruefungen(String path) throws IOException {
        List<Pruefung> pruefungen = new LinkedList<>();
        Set<String> existingPruefungen = new HashSet<>(); // check existingPruefungen by qualifiedName
        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        int indexDaysBegin;
        int indexDaysEnd;
        LocalDateTime[] days;

        {
            List<String> columns = Arrays.asList(rows.get(0).split(";"));
            indexDaysBegin = columns.indexOf("Ende") + 1;
            indexDaysEnd = columns.indexOf("Gruppe") - 1;

            days = new LocalDateTime[indexDaysEnd - indexDaysBegin + 1];

            for (int i = indexDaysBegin; i <= indexDaysEnd; i++) {
                Pattern pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.");
                Matcher matcher = pattern.matcher(columns.get(i));
                if (!matcher.find())
                    throw new AssertionError("date has unexpected format");
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                //TODO: using year 1900 (fix with actual data)
                LocalDateTime d = LocalDateTime.of(1900, month, day, 0, 0, 0);
                days[i - indexDaysBegin] = d;
            }
        }

        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");

            if (columns[1].isBlank() || /*columns[2].isBlank() || columns[4].isBlank() ||*/ columns[5].isBlank() || columns[10].isBlank() || columns[11].isBlank())
                throw new AssertionError("missing data in pruefungen file"); //TODO: specify whats missing & line

            Long nr = columns[4].isBlank() ? null : Long.parseLong(columns[4]);
            String name = columns[5];
            String stg = columns[1];
            String pversion = columns[2];

            String qualifiedName = Pruefung.calculateQualifiedName(stg, pversion, nr, name);

            if (existingPruefungen.contains(qualifiedName))
                continue;


            String beginTime = columns[10];
            String endTime = columns[11];

            LocalDateTime day = null;
            for (int i = indexDaysBegin; i <= indexDaysEnd; i++) {
                if (!columns[i].isBlank()) {
                    day = days[i - indexDaysBegin];
                    break;
                }
            }
            // assert that the loop actually hits eventually
            if (day == null)
                throw new AssertionError("the pruefung " + nr + " " + name + "doesn't have a date.");

            String[] beginTimeSplit = beginTime.split(":");
            int hours = Integer.parseInt(beginTimeSplit[0]);
            int minutes = Integer.parseInt(beginTimeSplit[1]);
            LocalDateTime begin = day.plusMinutes(hours * 60L + minutes);


            String[] endTimeSplit = endTime.split(":");
            int hours2 = Integer.parseInt(endTimeSplit[0]);
            int minutes2 = Integer.parseInt(endTimeSplit[1]);
            LocalDateTime end = day.plusMinutes(hours2 * 60L + minutes2);

            Pruefung p = new Pruefung(nr, name, columns[1], columns[2], begin, end);
            pruefungen.add(p);
            existingPruefungen.add(qualifiedName);
        }
        return pruefungen.toArray(new Pruefung[0]);
    }

    public static Pruefung[] loadMissingPruefungenFromAnmeldungen(String path, Pruefung[] pruefungen) throws IOException {
        List<Pruefung> additionalPruefungen = new LinkedList<>();
        Map<String, Pruefung> pruefungenByQualifiedName = new HashMap<>();

        for (Pruefung p : pruefungen) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (pruefungenByQualifiedName.containsKey(p.getQualifiedName()))
                throw new AssertionError("there are two pruefungen with the same name");
            pruefungenByQualifiedName.put(p.getQualifiedName(), p);
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");
            long pruefungsNr = Long.parseLong(columns[5]);
            String name = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualifiedName = Pruefung.calculateQualifiedName(stg, pversion, pruefungsNr, name);
            if (!pruefungenByQualifiedName.containsKey(qualifiedName)) {
                Pruefung p = new Pruefung(pruefungsNr, name, columns[2], columns[3], null, null);
                additionalPruefungen.add(p);
                pruefungenByQualifiedName.put(qualifiedName, p);
            }
        }

        return additionalPruefungen.toArray(new Pruefung[0]);
    }

    public static void loadAnmeldungen(String path, Pruefung[] pruefungen) throws IOException {
        Map<String, Set<String>> anmeldungen = new HashMap<>(); // Pruefung.qualifiedName -> MatrNr

        for (Pruefung p : pruefungen) {
            String qualifiedName = p.getQualifiedName();
            // the following exception should never occur (-> internal logic error -> bug)
            if (anmeldungen.containsKey(qualifiedName))
                throw new AssertionError("there are two pruefungen with the same name");
            anmeldungen.put(qualifiedName, new HashSet<>());
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");

            //TODO: assert
            String matrNr = columns[0];
            long pruefungsNr = Long.parseLong(columns[5]);
            String pruefungsName = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualifiedName = Pruefung.calculateQualifiedName(stg, pversion, pruefungsNr, pruefungsName);
            // the following exception should never occur (-> internal logic error -> bug)
            if (!anmeldungen.containsKey(qualifiedName))
                throw new AssertionError("a user anmeldung references a unknown pruefung: " + qualifiedName);
            anmeldungen.get(qualifiedName).add(matrNr);
        }

        for (Pruefung p : pruefungen) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getAnmeldungen() != null)
                throw new AssertionError("The Anmeldungen of the Prüfung " + p + " was already loaded.");
            p.setAnmeldungen(anmeldungen.get(p.getQualifiedName()));
        }
    }
}
