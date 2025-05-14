import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class LoadManager {
    public static Assessment[] loadExams(String path) throws IOException {
        List<Assessment> exams = new LinkedList<>();
        Set<String> existingExams = new HashSet<>(); // check existingExams by qualifiedName
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
                throw new AssertionError("missing data in exams file"); //TODO: specify whats missing & line

            Long no = columns[4].isBlank() ? null : Long.parseLong(columns[4]);
            String name = columns[5];
            String stg = columns[1];
            String pversion = columns[2];

            String qualifiedName = Assessment.calculateQualifiedName(stg, pversion, no, name);

            if (existingExams.contains(qualifiedName))
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
                throw new AssertionError("the exam " + no + " " + name + "doesn't have a date.");

            String[] beginTimeSplit = beginTime.split(":");
            int hours = Integer.parseInt(beginTimeSplit[0]);
            int minutes = Integer.parseInt(beginTimeSplit[1]);
            LocalDateTime begin = day.plusMinutes(hours * 60L + minutes);


            String[] endTimeSplit = endTime.split(":");
            int hours2 = Integer.parseInt(endTimeSplit[0]);
            int minutes2 = Integer.parseInt(endTimeSplit[1]);
            LocalDateTime end = day.plusMinutes(hours2 * 60L + minutes2);

            Assessment p = new Assessment(no, name, columns[1], columns[2], begin, end);
            exams.add(p);
            existingExams.add(qualifiedName);
        }
        return exams.toArray(new Assessment[0]);
    }

    public static Assessment[] loadMissingAssessments(String path, Assessment[] assessments) throws IOException {
        List<Assessment> additionalAssessments = new LinkedList<>();
        Map<String, Assessment> assessmentsByQualifiedName = new HashMap<>();

        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (assessmentsByQualifiedName.containsKey(p.getQualifiedName()))
                throw new AssertionError("there are two assessments with the same name");
            assessmentsByQualifiedName.put(p.getQualifiedName(), p);
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");
            long assessmentNo = Long.parseLong(columns[5]);
            String name = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualifiedName = Assessment.calculateQualifiedName(stg, pversion, assessmentNo, name);
            if (!assessmentsByQualifiedName.containsKey(qualifiedName)) {
                Assessment p = new Assessment(assessmentNo, name, columns[2], columns[3], null, null);
                additionalAssessments.add(p);
                assessmentsByQualifiedName.put(qualifiedName, p);
            }
        }

        return additionalAssessments.toArray(new Assessment[0]);
    }

    public static void loadRegistrations(String path, Assessment[] assessments) throws IOException {
        Map<String, Set<String>> registrations = new HashMap<>(); // Assessment.qualifiedName -> MatrNo

        for (Assessment p : assessments) {
            String qualifiedName = p.getQualifiedName();
            // the following exception should never occur (-> internal logic error -> bug)
            if (registrations.containsKey(qualifiedName))
                throw new AssertionError("there are two assessments with the same name");
            registrations.put(qualifiedName, new HashSet<>());
        }

        List<String> rows = Files.readAllLines(Paths.get(path));

        if (rows.isEmpty())
            throw new AssertionError();

        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");

            //TODO: assert
            String matrNo = columns[0];
            long assessmentNo = Long.parseLong(columns[5]);
            String assessmentName = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualifiedName = Assessment.calculateQualifiedName(stg, pversion, assessmentNo, assessmentName);
            // the following exception should never occur (-> internal logic error -> bug)
            if (!registrations.containsKey(qualifiedName))
                throw new AssertionError("a user registration references a unknown assessment: " + qualifiedName);
            registrations.get(qualifiedName).add(matrNo);
        }

        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (p.getRegisteredStudents() != null)
                throw new AssertionError("The registration of the assessment " + p + " was already loaded.");
            p.setRegisteredStudents(registrations.get(p.getQualifiedName()));
        }
    }
}
