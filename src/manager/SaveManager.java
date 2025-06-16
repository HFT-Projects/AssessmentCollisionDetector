package manager;

import data.Assessment;
import data.MergedAssessment;

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

    public static void saveCollisions(String path, Assessment[] assessments) throws UncheckedIOException {
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

    public static void saveAssessments(String path, MergedAssessment[] mergedAssessments) throws UncheckedIOException {
        List<String> lines = new LinkedList<>();

        // get date range for assessments (first & last)
        LocalDateTime first = null;
        LocalDateTime last = null;
        for (MergedAssessment mergedAssessment : mergedAssessments) {
            if (mergedAssessment.getBegin() != null && (first == null || mergedAssessment.getBegin().isBefore(first)))
                first = mergedAssessment.getBegin();
            if (mergedAssessment.getEnd() != null && (last == null || mergedAssessment.getBegin().isAfter(last)))
                last = mergedAssessment.getBegin();
        }

        // create date columns
        List<LocalDateTime> dayToColumnIndex = new LinkedList<>();

        if (first != null && last != null) {
            for (int i = 0; ; i++) {
                LocalDateTime k = first.withHour(0).withMinute(0).plusDays(i);
                LocalDateTime end = last.withHour(0).withMinute(0);
                if (k.isAfter(end))
                    break;

                if (k.getDayOfWeek() == DayOfWeek.SUNDAY)
                    continue;

                dayToColumnIndex.add(k);
            }
        }

        // convert dates to german format
        Map<DayOfWeek, String> dayAbbrev = Map.of(
                DayOfWeek.MONDAY, "Mo",
                DayOfWeek.TUESDAY, "Di",
                DayOfWeek.WEDNESDAY, "Mi",
                DayOfWeek.THURSDAY, "Do",
                DayOfWeek.FRIDAY, "Fr",
                DayOfWeek.SATURDAY, "Sa"
        );

        // build the date headers
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.");
        StringBuilder dynamicDates = new StringBuilder();
        for (LocalDateTime date : dayToColumnIndex) {
            String abbrev = dayAbbrev.get(date.getDayOfWeek());
            dynamicDates.append(abbrev).append(" ").append(date.format(dateFormatter)).append(";");
        }

        // add the header line with the date columns
        lines.add("Fak;stg;pversion;vert;pnr;pltxt1;prüfer1;prüfer2;Anzahl;pdauer;Beginn;Ende;" +
                dynamicDates + "Gruppe;Raum;Aufsicht;;Studiengang;Prüfung;ID;WiSe");

        // formatter for begin & end columns
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");

        // create the row for each Assessment
        for (MergedAssessment ma : mergedAssessments) {
            for (Assessment a : ma.getAssessments()) {
                String duration = ma.getOptimizedBegin() == null ? "" : Long.toString(Duration.between(ma.getOptimizedBegin(), ma.getOptimizedEnd()).toMinutes());

                String day = ma.getOptimizedBegin() == null ? "" : Integer.toString(ma.getOptimizedBegin().getDayOfMonth());
                int dayIndex = ma.getOptimizedBegin() == null ? 0 : dayToColumnIndex.indexOf(ma.getOptimizedBegin().withMinute(0).withHour(0));

                String beginFormatted = ma.getOptimizedBegin() == null ? "" : ma.getOptimizedBegin().format(formatter);
                String endFormatted = ma.getOptimizedEnd() == null ? "" : ma.getOptimizedEnd().format(formatter);

                String registeredStudents = a.getRegisteredStudents() != null ? Integer.toString(a.getRegisteredStudents().size()) : "";

                // check if Assessment has AssessmentEntries. if not -> write only basic information, if yes -> write a line for each entry.
                if (a.getAssessmentEntries() == null) {
                    String assessment = ";" + a.getCourseOfStudy() + ";" + a.getAssessmentVersion() + ";;" + (a.getNumber() != null ? a.getNumber() : "") + ";" + a.getName() + ";;;"
                            + registeredStudents + ";" + duration + ";" + beginFormatted + ";" + endFormatted + ";" +

                            //Leave the date columns empty if the assessment isn't on that day
                            ";".repeat(Math.max(0, dayIndex)) +
                            //Add day to the column
                            day +
                            //Leave the rest of the Date columns empty
                            ";".repeat(Math.max(0, dayToColumnIndex.size() - dayIndex)) +

                            ";;;;;;;";
                    lines.add(assessment);
                } else {
                    for (Assessment.AssessmentEntry ae : a.getAssessmentEntries()) {
                        String assessment = ae.faculty() + ";" + a.getCourseOfStudy() + ";" + a.getAssessmentVersion() + ";" + ae.vert() + ";" + (a.getNumber() != null ? a.getNumber() : "") + ";" + a.getName() + ";" + ae.examiner1() + ";" + ae.examiner2() + ";"
                                + ae.externalRegistrationCount() + ";" + ae.externalDuration() + ";" + beginFormatted + ";" + endFormatted + ";" +

                                //Leave the date columns empty if the assessment isn't on that day
                                ";".repeat(Math.max(0, dayIndex)) +
                                //Add day to the column
                                day +
                                //Leave the rest of the Date columns empty
                                ";".repeat(Math.max(0, dayToColumnIndex.size() - dayIndex)) +

                                ae.group() + ";" + ae.room() + ";" + ae.supervisor() + ";;" + ae.externalCourseOfStudy() + ";" + ae.externalExamName() + ";" + ae.externalExamId() + ";" + ae.wiSe();
                        lines.add(assessment);
                    }
                }
            }
        }

        // write to file
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path))) {
            writer.write(lines.stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse(""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
