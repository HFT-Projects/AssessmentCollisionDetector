import data.Assessment;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
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
                Duration distance = p.getDistance(k);
                if (distance == null) {
                    distanceStr = "";
                } else {
                    distanceStr = String.format("%03d", distance.toHours()); //TODO: remove workaround (currently we truncate -> round (to 0,25? or at least 1) instead)

                    // replace distance with string "Überschneidung" if assessments overlap
                    if (distance.toHours() < 0)
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

    public static void saveAssessments(String path, Assessment[] assessments) throws UncheckedIOException {
        List<String> lines = new LinkedList<>();

        //Get the Dates for the Assessments
        LocalDateTime first = null;
        LocalDateTime last = null;
        for (Assessment assessment : assessments) {
            if (assessment.getBegin() != null && (first == null || assessment.getBegin().isBefore(first)))
                first = assessment.getBegin();
            if (assessment.getEnd() != null && (last == null || assessment.getBegin().isAfter(last)))
                last = assessment.getBegin();
        }

        if (first == null || last == null)
            throw new AssertionError();

        List<LocalDateTime> dayToRowIndex = new LinkedList<>();

        for (int i = 0; ; i++) {
            LocalDateTime k = first.withHour(0).withMinute(0).plusDays(i);
            LocalDateTime end = last.withHour(0).withMinute(0);
            if (k.isAfter(end))
                break;

            if (k.getDayOfWeek() == DayOfWeek.SUNDAY)
                continue;

            dayToRowIndex.add(k);
        }

        //Convert Dates to correct Format for Excel Doc
        Map<DayOfWeek, String> dayAbbrev = Map.of(
                DayOfWeek.MONDAY, "Mo",
                DayOfWeek.TUESDAY, "Di",
                DayOfWeek.WEDNESDAY, "Mi",
                DayOfWeek.THURSDAY, "Do",
                DayOfWeek.FRIDAY, "Fr",
                DayOfWeek.SATURDAY, "Sa"
        );

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.");

        // Build the dynamic date headers
        StringBuilder dynamicDates = new StringBuilder();
        for (LocalDateTime date : dayToRowIndex) {
            String abbrev = dayAbbrev.get(date.getDayOfWeek());
            dynamicDates.append(abbrev).append(" ").append(date.format(dateFormatter)).append(";");
        }

        // Add the header line with the dynamic date columns
        lines.add("Fak;stg;pversion;vert;pnr;pltxt1;prüfer1;prüfer2;Anzahl;pdauer;Beginn;Ende;" +
                dynamicDates + "Gruppe;Raum;Aufsicht;;Studiengang;Prüfung;ID;WiSe");

        //Format for Begin and End
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        //Create the String for each Assessment
        for (Assessment a : assessments) {
            long distance = a.getBegin() == null ? 0 : Duration.between(a.getBegin(), a.getEnd()).toMinutes();
            int day = a.getBegin() == null ? 0 : a.getBegin().getDayOfMonth();
            int dayIndex = a.getBegin() == null ? 0 : dayToRowIndex.indexOf(a.getBegin().withMinute(0).withHour(0));
            String beginFormatted = a.getBegin() == null ? "" : a.getBegin().format(formatter);
            String endFormatted = a.getEnd() == null ? "" : a.getEnd().format(formatter);
            String registeredStudents = a.getRegisteredStudents() != null ? Integer.toString(a.getRegisteredStudents().size()) : "";

            String pruefung = ";" + a.getCourseOfStudy() + ";" + a.getAssessmentVersion() + ";;" + a.getNumber() + ";" + a.getName() + ";;;"
                    + registeredStudents + ";" + distance + ";" + beginFormatted + ";" + endFormatted + ";" +

                    //Leave the date columns empty if the assessment isn't on that day
                    ";".repeat(Math.max(0, dayIndex)) +
                    //Add day to the column
                    day + ";" +
                    //Leave the rest of the Date columns empty
                    ";".repeat(Math.max(0, dayToRowIndex.size() - dayIndex)) +

                    ";;;;;;;;";
            lines.add(pruefung);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path))) {
            writer.write(lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
