package manager;

import data.Assessment;
import data.AssessmentBase;
import data.AssessmentEditable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadManager {
    private record DateInfo(String weekday, int dayOfMonth, int month) {
    }

    private static final Map<String, Integer> WEEKDAY_MAP = Map.of("Mo", 0, "Di", 1, "Mi", 2, "Do", 3, "Fr", 4, "Sa", 5, "So", 6);

    private static Integer checkForLeapYears(DateInfo[] dateInfos) {
        Integer leap_year = null;

        // loop through all dates to check if they are leap years
        int current_year = 0;
        for (int i = 1; i < dateInfos.length; i++) {
            int startMonth = dateInfos[i - 1].month;
            int endMonth = dateInfos[i].month;

            // check if there is a year change between the two dates and increment current year
            if (startMonth > endMonth)
                current_year++;

            // check if there was the change from feb. to march in between the last two dates
            if (startMonth <= 2 && endMonth >= 3
                    || (startMonth > endMonth && (startMonth <= 2 || endMonth >= 3))) {

                // check if there is a year change between the two dates
                int year_change_correction = (startMonth > endMonth) ? 1 : 0;

                // calculate the weekday of the second date if it were a leap year
                // by calculating the days in between the two dates if it were a leap year
                // and then adding it to the first date
                LocalDateTime start_if_leap = LocalDateTime.of(2000 - year_change_correction, startMonth, dateInfos[i - 1].dayOfMonth, 0, 0, 0);
                LocalDateTime end_if_leap = LocalDateTime.of(2000, endMonth, dateInfos[i].dayOfMonth, 0, 0, 0);
                long days_if_leap = Duration.between(start_if_leap, end_if_leap).toDays();
                int weekday_if_leap = (int) ((WEEKDAY_MAP.get(dateInfos[i - 1].weekday) + days_if_leap) % 7);

                // calculate the weekday of the second date if it were NOT a leap year
                // details see above
                LocalDateTime start_if_not_leap = LocalDateTime.of(1900 - year_change_correction, startMonth, dateInfos[i - 1].dayOfMonth, 0, 0, 0);
                LocalDateTime end_if_not_leap = LocalDateTime.of(1900, endMonth, dateInfos[i].dayOfMonth, 0, 0, 0);
                long days_if_not_leap = Duration.between(start_if_not_leap, end_if_not_leap).toDays();
                int weekday_if_not_leap = (int) ((WEEKDAY_MAP.get(dateInfos[i - 1].weekday) + days_if_not_leap) % 7);

                // get the actual weekday
                int actual_weekday = WEEKDAY_MAP.get(dateInfos[i].weekday);

                // check if it is a leap year or not (or invalid)
                if (actual_weekday == weekday_if_leap) {
                    System.out.println("LEAP");
                    leap_year = current_year;
                    break;
                } else if (actual_weekday != weekday_if_not_leap) {
                    throw new AssertionError("weekday is invalid");
                }
            }
        }

        return leap_year;
    }

    private static LocalDateTime[] completeDates(DateInfo[] dateInfos, Integer year_input) {
        // check what year is a leap year (year of first date in dateInfos is 0)
        Integer leap_year_index = checkForLeapYears(dateInfos);

        int year;

        // use given year, approximate year if no year is given.
        if (year_input != null) {
            year = year_input;
        } else {
            // use linear alternating probing (with the current year as base) to approximate the year
            // by finding a year with matching weekdays and leap year constellation.
            year = LocalDateTime.now().getYear();
            for (int i = 0; ; i++) {
                // linear alternating probing
                int cur_year = year + (int) Math.pow(-1, i) * (int) Math.ceil((float) i / 2);

                // check if the weekdays match with the first dateInfos entry
                // getDayOfWeek().getValue() -> Range 1-7; WEEKDAY_MAP -> Range 0-6
                if (LocalDateTime.of(cur_year, dateInfos[0].month, dateInfos[0].dayOfMonth, 0, 0).getDayOfWeek().getValue() - 1 == WEEKDAY_MAP.get(dateInfos[0].weekday)) {
                    // check if the leap year constellation is correct.
                    Integer leap_year = leap_year_index == null ? null : cur_year + leap_year_index;
                    if (leap_year == null || leap_year % 4 == 0 && (leap_year % 100 != 0 || leap_year % 400 == 0)) {
                        year = cur_year;
                        break;
                    }
                }
            }
        }

        // convert DateInfo objects into LocalDateTime object with the calculated year.
        LocalDateTime[] dates = new LocalDateTime[dateInfos.length];
        for (int i = 0; i < dateInfos.length; i++) {
            DateInfo di = dateInfos[i];
            dates[i] = LocalDateTime.of(year, di.month, di.dayOfMonth, 0, 0, 0);

            // make sure that the calculated year matches all entries.
            if (dates[i].getDayOfWeek().getValue() - 1 != WEEKDAY_MAP.get(di.weekday))
                throw new AssertionError("date has unexpected weekday!");
        }

        return dates;
    }

    @SuppressWarnings({"ExtractMethodRecommender", "DuplicatedCode"})
    public static Assessment[] loadExams(String path, Integer year) throws UncheckedIOException {
        List<Assessment> exams = new LinkedList<>();
        Map<String, AssessmentEditable> existingExams = new HashMap<>(); // qualifiedName -> existingExams

        List<String> rows;
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
        List<String> headerColumns = Arrays.asList(rows.getFirst().split(";"));
        indexDaysBegin = headerColumns.indexOf("Ende") + 1;
        //noinspection SpellCheckingInspection
        indexDaysEnd = headerColumns.indexOf("Gruppe") - 1;
        DateInfo[] dates = new DateInfo[indexDaysEnd - indexDaysBegin + 1];
        // create array to map the exam days to the actual date
        for (int i = indexDaysBegin; i <= indexDaysEnd; i++) {
            // create a matcher to extract the date from the following string: "Mo 01.01."
            Pattern pattern = Pattern.compile("([A-Z][a-z]) (\\d{2})\\.(\\d{2})\\.");
            Matcher matcher = pattern.matcher(headerColumns.get(i));

            if (!matcher.find())
                throw new AssertionError("date has unexpected format");

            int day = Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));

            dates[i - indexDaysBegin] = new DateInfo(matcher.group(1), day, month);
        }

        // complete Date with year
        days = completeDates(dates, year);

        // loop through body
        List<String> rowsWithoutHeader = rows.subList(1, rows.size());
        for (int i = 0; i < rowsWithoutHeader.size(); i++) {
            String row = rowsWithoutHeader.get(i);
            String[] columns = row.split(";");

            // check that data is complete
            if (columns[1].isBlank())
                throw new AssertionError("missing data in exams file in column stg (2) in line " + (i + 1));
            else if (columns[5].isBlank())
                //noinspection SpellCheckingInspection
                throw new AssertionError("missing data in exams file in column pltext1 (6) in line " + (i + 1));
            else if (columns[10].isBlank() || !columns[10].strip().matches("[0-9]{1,2}:[0-9]{2}"))
                throw new AssertionError("missing or invalid data in exams file in column Begin (11) in line " + (i + 1));
            else if (columns[11].isBlank() || !columns[11].strip().matches("[0-9]{1,2}:[0-9]{2}"))
                throw new AssertionError("missing or invalid data in exams file in column Ende (12) in line " + (i + 1));


            Long no = columns[4].isBlank() ? null : Long.parseLong(columns[4]);
            String name = columns[5];
            String courseOfStudy = columns[1];
            String assessmentVersion = columns[2].isBlank() ? null : columns[2];
            String qualifiedName = Assessment.calculateQualifiedName(courseOfStudy, assessmentVersion, no, name);

            // file contains duplicates because sometimes exams are in multiple rooms (-> ignore)
            if (existingExams.containsKey(qualifiedName)) {
                AssessmentEditable a = existingExams.get(qualifiedName);
                Set<AssessmentBase.AssessmentEntry> assessmentEntries = a.getAssessmentEntries();
                int last = columns.length - 1;
                assessmentEntries.add(new AssessmentBase.AssessmentEntry(columns[0], columns[3], columns[6], columns[7], columns[8], columns[9], columns[last - 7], columns[last - 6], columns[last - 5], columns[last - 3], columns[last - 2], columns[last - 1], columns[last]));
                a.setAssessmentEntries(assessmentEntries);
                continue;
            }

            String beginTime = columns[10];
            String endTime = columns[11];

            LocalDateTime day = null;
            for (int k = indexDaysBegin; k <= indexDaysEnd; k++) {
                if (!columns[k].isBlank()) {
                    day = days[k - indexDaysBegin];
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

            int last = columns.length - 1;
            AssessmentEditable p = new AssessmentEditable(no, name, columns[1], columns[2], begin, end);

            Set<AssessmentBase.AssessmentEntry> assessmentEntries = new HashSet<>();
            assessmentEntries.add(new AssessmentBase.AssessmentEntry(columns[0], columns[3], columns[6], columns[7], columns[8], columns[9], columns[last - 7], columns[last - 6], columns[last - 5], columns[last - 3], columns[last - 2], columns[last - 1], columns[last]));
            p.setAssessmentEntries(assessmentEntries);

            exams.add(p);
            existingExams.put(qualifiedName, p);
        }
        return exams.toArray(new Assessment[0]);
    }

    @SuppressWarnings({"DuplicatedCode", "ExtractMethodRecommender"})
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

        List<String> rows;
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
        for (int i = 0; i < rowsWithoutHeader.size(); i++) {
            String row = rowsWithoutHeader.get(i);
            String[] columns = row.split(";");

            // check that data is complete
            if (columns[2].isBlank())
                throw new AssertionError("missing data in registration file in column stg (3) in line " + (i + 1));
            else if (columns[3].isBlank())
                //noinspection SpellCheckingInspection
                throw new AssertionError("missing data in registration file in column pversion (4) in line " + (i + 1));
            else if (columns[5].isBlank() || !columns[5].strip().matches("[0-9]*"))
                throw new AssertionError("missing or invalid data in registration file in column pnr (6) in line " + (i + 1));
            else if (columns[6].isBlank())
                //noinspection SpellCheckingInspection
                throw new AssertionError("missing data in registration file in column pltxt1 (7) in line " + (i + 1));

            long assessmentNo = Long.parseLong(columns[5]);
            String name = columns[6];
            String courseOfStudy = columns[2];
            String assessmentVersion = columns[3];
            String qualifiedName = Assessment.calculateQualifiedName(courseOfStudy, assessmentVersion, assessmentNo, name);

            // ass Assessment if it doesn't already exist
            if (!existingAssessments.contains(qualifiedName)) {
                Assessment p = new AssessmentEditable(assessmentNo, name, courseOfStudy, assessmentVersion, null, null);
                additionalAssessments.add(p);
                existingAssessments.add(qualifiedName);
            }
        }

        return additionalAssessments.toArray(new Assessment[0]);
    }

    @SuppressWarnings({"DuplicatedCode", "ExtractMethodRecommender"})
    public static Map<String, Set<String>> loadRegistrations(String path, Assessment[] assessments) throws UncheckedIOException {
        Map<String, Set<String>> registrationsByAssessmentsQualifiedName = new HashMap<>(); // Assessment.qualifiedName -> StudentNo

        // add all Assessments to registrationsByAssessmentsQualifiedName
        for (Assessment p : assessments) {
            String qualifiedName = p.getQualifiedName();
            // the following exception should never occur (-> internal logic error -> bug)
            if (registrationsByAssessmentsQualifiedName.containsKey(qualifiedName))
                throw new AssertionError("there are two assessments with the same name");
            registrationsByAssessmentsQualifiedName.put(qualifiedName, new HashSet<>());
        }

        List<String> rows;
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
        for (int i = 0; i < rowsWithoutHeader.size(); i++) {
            String row = rowsWithoutHeader.get(i);
            String[] columns = row.split(";");

            // check that data is complete
            if (columns[0].isBlank())
                //noinspection SpellCheckingInspection
                throw new AssertionError("missing data in exams file in column mtknr (1) in line " + (i + 1));
            else if (columns[2].isBlank())
                throw new AssertionError("missing data in registration file in column stg (3) in line " + (i + 1));
            else if (columns[3].isBlank())
                //noinspection SpellCheckingInspection
                throw new AssertionError("missing data in registration file in column pversion (4) in line " + (i + 1));
            else if (columns[5].isBlank() || !columns[5].strip().matches("[0-9]*"))
                throw new AssertionError("missing or invalid data in registration file in column pnr (6) in line " + (i + 1));
            else if (columns[6].isBlank())
                //noinspection SpellCheckingInspection
                throw new AssertionError("missing data in registration file in column pltxt1 (7) in line " + (i + 1));

            String studentNo = columns[0];
            long assessmentNo = Long.parseLong(columns[5]);
            String assessmentName = columns[6];
            String courseOfStudy = columns[2];
            String assessmentVersion = columns[3];
            String qualifiedName = Assessment.calculateQualifiedName(courseOfStudy, assessmentVersion, assessmentNo, assessmentName);

            // the following exception should never occur (-> internal logic error -> bug)
            if (!registrationsByAssessmentsQualifiedName.containsKey(qualifiedName))
                throw new AssertionError("a user registration references a unknown assessment: " + qualifiedName);

            registrationsByAssessmentsQualifiedName.get(qualifiedName).add(studentNo);
        }

        return registrationsByAssessmentsQualifiedName;
    }
}
