import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class LoadManager {
    public static Assessment[] loadExams(String path) throws UncheckedIOException {
        List<Assessment> exams = new LinkedList<>();
        Set<String> existingExams = new HashSet<>(); // check existingExams by qualifiedName

        List<String> rows = null;
        try {
            rows = Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // make sure file isn't empty
        if (rows.isEmpty())
            throw new AssertionError("input file must not be empty");

        int indexDaysBegin;
        int indexDaysEnd;
        LocalDateTime[] days;

        // parse header line to extract the possible dates of the exams
        List<String> header_columns = Arrays.asList(rows.get(0).split(";"));
        indexDaysBegin = header_columns.indexOf("Ende") + 1;
        indexDaysEnd = header_columns.indexOf("Gruppe") - 1;
        // create array to map the exam days to the actual date
        days = new LocalDateTime[indexDaysEnd - indexDaysBegin + 1];
        for (int i = indexDaysBegin; i <= indexDaysEnd; i++) {
            // create a matcher to extract the date from the following string: "Mo 01.01."
            Pattern pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.");
            Matcher matcher = pattern.matcher(header_columns.get(i));

            if (!matcher.find())
                throw new AssertionError("date has unexpected format");

            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            //TODO: using year 1970 (fix with actual data)
            LocalDateTime d = LocalDateTime.of(1970, month, day, 0, 0, 0);

            days[i - indexDaysBegin] = d;
        }

        // loop through body
        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");

            // TODO: hamann: no & pversion sometimes blank
            // check that data is complete
            if (columns[1].isBlank() || /*columns[2].isBlank() || columns[4].isBlank() || !columns[4].strip().matches("[0-9]*") ||*/ columns[5].isBlank() || columns[10].isBlank() || !columns[10].strip().matches("[0-9]{1,2}:[0-9]{2}") || columns[11].isBlank() || !columns[11].strip().matches("[0-9]{1,2}:[0-9]{2}"))
                throw new AssertionError("missing data in exams file"); //TODO: specify whats missing & line

            Long no = columns[4].isBlank() ? null : Long.parseLong(columns[4]);
            String name = columns[5];
            String stg = columns[1];
            String pversion = columns[2].isBlank() ? null : columns[2];
            String qualifiedName = Assessment.calculateQualifiedName(stg, pversion, no, name);

            // file contains duplicates because sometimes exams are in multiple rooms (-> ignore)
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

    public static Assessment[] loadMissingAssessments(String path, Assessment[] assessments) throws UncheckedIOException {
        List<Assessment> additionalAssessments = new LinkedList<>();
        Set<String> existingAssessments = new HashSet<>(); // check existingAssessments by qualifiedName

        // add all exams to existingAssessments
        for (Assessment p : assessments) {
            // the following exception should never occur (-> internal logic error -> bug)
            if (existingAssessments.contains(p.getQualifiedName()))
                throw new AssertionError("there are two assessments with the same name");
            existingAssessments.add(p.getQualifiedName());
        }

        List<String> rows = null;
        try {
            rows = Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // make sure file isn't empty
        if (rows.isEmpty())
            throw new AssertionError("input file must not be empty");

        // loop through body
        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");

            // check that data is complete
            if (columns[2].isBlank() || columns[3].isBlank() || columns[5].isBlank() || !columns[5].strip().matches("[0-9]*") || columns[6].isBlank())
                throw new AssertionError("missing data in registrations file"); //TODO: specify whats missing & line

            long assessmentNo = Long.parseLong(columns[5]);
            String name = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualifiedName = Assessment.calculateQualifiedName(stg, pversion, assessmentNo, name);

            // ass Assessment if it doesn't already exist
            if (!existingAssessments.contains(qualifiedName)) {
                Assessment p = new Assessment(assessmentNo, name, stg, pversion, null, null);
                additionalAssessments.add(p);
                existingAssessments.add(qualifiedName);
            }
        }

        return additionalAssessments.toArray(new Assessment[0]);
    }

    public static Map<String, Set<String>> loadRegistrations(String path, Assessment[] assessments) throws UncheckedIOException {
        Map<String, Set<String>> registrationsByAssessmentsQualifiedName = new HashMap<>(); // Assessment.qualifiedName -> MatrNo

        // add all Assessments to registrationsByAssessmentsQualifiedName
        for (Assessment p : assessments) {
            String qualifiedName = p.getQualifiedName();
            // the following exception should never occur (-> internal logic error -> bug)
            if (registrationsByAssessmentsQualifiedName.containsKey(qualifiedName))
                throw new AssertionError("there are two assessments with the same name");
            registrationsByAssessmentsQualifiedName.put(qualifiedName, new HashSet<>());
        }

        List<String> rows = null;
        try {
            rows = Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // make sure file isn't empty
        if (rows.isEmpty())
            throw new AssertionError("input file must not be empty");

        // loop through body
        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (String row : rowsWithoutHeader) {
            String[] columns = row.split(";");

            // check that data is complete
            if (columns[0].isBlank() || columns[2].isBlank() || columns[3].isBlank() || columns[5].isBlank() || !columns[5].strip().matches("[0-9]*") || columns[6].isBlank())
                throw new AssertionError("missing data in exams file"); //TODO: specify whats missing & line

            String matrNo = columns[0];
            long assessmentNo = Long.parseLong(columns[5]);
            String assessmentName = columns[6];
            String stg = columns[2];
            String pversion = columns[3];
            String qualifiedName = Assessment.calculateQualifiedName(stg, pversion, assessmentNo, assessmentName);

            // the following exception should never occur (-> internal logic error -> bug)
            if (!registrationsByAssessmentsQualifiedName.containsKey(qualifiedName))
                throw new AssertionError("a user registration references a unknown assessment: " + qualifiedName);

            registrationsByAssessmentsQualifiedName.get(qualifiedName).add(matrNo);
        }

        return registrationsByAssessmentsQualifiedName;
    }
}
