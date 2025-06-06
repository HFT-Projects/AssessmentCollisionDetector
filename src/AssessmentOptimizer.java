import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AssessmentOptimizer {

    private static class TimeSlot {
        LocalDateTime start;
        Duration duration;

        TimeSlot(LocalDateTime start, Duration duration) {
            this.start = start;
            this.duration = duration;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TimeSlot timeSlot = (TimeSlot) obj;
            return start.equals(timeSlot.start) && duration.equals(timeSlot.duration);
        }

        @Override
        public int hashCode() {
            return start.hashCode() + duration.hashCode();
        }
    }

    // false/true je nachdem was man angezeigt bekommen will
    private static final boolean SHOW_INDIVIDUAL_MOVES = false;
    private static final boolean SHOW_GROUP_SUMMARY = true;
    private static final boolean SHOW_DETAILED_STATS = false;

    private static void debugMove(String message) {
        if (SHOW_INDIVIDUAL_MOVES) {
            System.out.println(message);
        }
    }

    private static void debugGroupInfo(String message) {
        if (SHOW_GROUP_SUMMARY) {
            System.out.println(message);
        }
    }

    private static void debugStats(String message) {
        if (SHOW_DETAILED_STATS) {
            System.out.println(message);
        }
    }

    // IMPORTANT! ORGANIZED OUTPUT IS CONTROLLED BY THE DECLARATIONS: SHOW_INDIVIDUAL_MOVES etc.
    // IN TEST.JAVA PRINT_ORGANIZED_ASSESSMENTS WAS COMMENTED OUT FOR CLEAN OUTPUT - SEE CHANGES AFTER PUSH
    public static Map<Assessment, MergedAssessment> mergeAssessments(Assessment[] assessments) {
        Map<String, List<MergedAssessment>> nameToMergedAssessment = new HashMap<>();
        Map<Assessment, MergedAssessment> assessmentToMergedAssessment = new HashMap<>();

        for (Assessment a : assessments) {
            if (!nameToMergedAssessment.containsKey(a.getName())) {
                List<MergedAssessment> ass = new LinkedList<>();
                nameToMergedAssessment.put(a.getName(), ass);
            }

            boolean found = false;

            for (MergedAssessment ma : nameToMergedAssessment.get(a.getName())) {
                if (ma.getBegin() == null || a.getBegin() == null || !ma.getBegin().equals(a.getBegin()))
                    if (!(ma.getBegin() == null && a.getBegin() == null))
                        continue;
                if (ma.getEnd() == null || a.getEnd() == null || !ma.getEnd().equals(a.getEnd()))
                    if (!(ma.getEnd() == null && a.getEnd() == null))
                        continue;

                Set<String> registeredStudents = ma.getRegisteredStudents();
                registeredStudents.addAll(a.getRegisteredStudents());
                ma.setRegisteredStudents(registeredStudents);

                Map<Assessment, Integer> ccba = ma.getCollisionCountByAssessment();
                ccba.putAll(a.getCollisionCountByAssessment());
                ma.setCollisionCountByAssessment(ccba);

                ma.setCollisionSum(ma.getCollisionSum() + a.getCollisionSum());

                assessmentToMergedAssessment.put(a, ma);

                found = true;
                break;
            }

            if (!found) {
                MergedAssessment ma = new MergedAssessment(a.getName(), a.getBegin(), a.getEnd());
                ma.setRegisteredStudents(a.getRegisteredStudents());
                ma.setCollisionCountByAssessment(a.getCollisionCountByAssessment());
                ma.setCollisionSum(a.getCollisionSum());
                nameToMergedAssessment.get(a.getName()).add(ma);
                assessmentToMergedAssessment.put(a, ma);
            }

        }

        return assessmentToMergedAssessment;
    }

    private static void getAssessmentGroupsRecursive(MergedAssessment assessment, List<MergedAssessment> assessmentGroup, Map<Assessment, MergedAssessment> assessmentToMergedAssessment) {
        assessmentGroup.add(assessment);
        for (Assessment a : assessment.getCollisionCountByAssessment().keySet()) {
            MergedAssessment ma = assessmentToMergedAssessment.get(a);
            if (assessmentGroup.contains(ma))
                continue;
            getAssessmentGroupsRecursive(ma, assessmentGroup, assessmentToMergedAssessment);
        }
    }

    public static MergedAssessment[][] getAssessmentGroups(Map<Assessment, MergedAssessment> assessmentToMergedAssessment) {
        Set<MergedAssessment> alreadyProcessed = new HashSet<>();
        List<List<MergedAssessment>> assessmentGroups = new LinkedList<>();

        for (MergedAssessment ma : new HashSet<>(assessmentToMergedAssessment.values())) {
            if (alreadyProcessed.contains(ma))
                continue;
            List<MergedAssessment> assessmentGroup = new LinkedList<>();

            getAssessmentGroupsRecursive(ma, assessmentGroup, assessmentToMergedAssessment);

            alreadyProcessed.addAll(assessmentGroup);
            assessmentGroups.add(assessmentGroup);
        }

        return assessmentGroups.stream().map((mal) -> mal.toArray(new MergedAssessment[0])).toArray(MergedAssessment[][]::new);
    }

    private static double getSatisfactionValueOfAssessment(Assessment assessment) {
        double sum = 0;
        int collisions_with_date = 0;

        if (assessment.getBegin() == null || assessment.getEnd() == null) {
            return -1;
        }

        for (Assessment b : assessment.getCollisionCountByAssessment().keySet()) {
            if (b.getBegin() == null || b.getEnd() == null) {
                continue;
            }

            collisions_with_date++;

            // calculate time distance between colliding assessments (end to begin)
            Assessment first;
            Assessment last;
            if (assessment.getBegin().isBefore(b.getBegin())) {
                first = assessment;
                last = b;
            } else {
                first = b;
                last = assessment;
            }
            long distance_hours = Duration.between(first.getEnd(), last.getBegin()).toHours();
            long distance_days = ChronoUnit.DAYS.between(first.getEnd().toLocalDate(), last.getBegin().toLocalDate());

            double satis;
            if (distance_days >= 2) {
                satis = 1;
            } else if (distance_days == 1) {
                satis = 0.9;
            } else {
                if (distance_hours <= 1)
                    satis = 0;
                else if (distance_hours <= 3)
                    satis = 0.1;
                else
                    satis = 0.2 + (double)distance_hours / 12 * 0.6;
            }
            sum += satis * Math.sqrt(assessment.getCollisionCountByAssessment().get(b));
        }
        return sum / collisions_with_date;
    }

    public static MergedAssessment[][] optimizeAssessments(MergedAssessment[][] assessmentGroups) {
        for (MergedAssessment[] assessmentGroup : assessmentGroups) {
            optimizeGroup(assessmentGroup);
        }
        return assessmentGroups;
    }

    private static void optimizeGroup(MergedAssessment[] group) {
        if (group.length <= 1) {
            return; // no optimization needed for 0 or 1 prüfungen
        }

        debugGroupInfo("\n=== Optimiere Gruppe mit " + group.length + " Prüfungen ===");

        // only show the first three prüfungen
        int maxShow = Math.min(3, group.length);
        for (int i = 0; i < maxShow; i++) {
            MergedAssessment a = group[i];
            System.out.println("Prüfung " + i + ": " + a.getName() +
                    " (Kollisionen: " + a.getCollisionSum() + ")");
        }

        if (group.length > 3) {
            System.out.println("... und " + (group.length - 3) + " weitere");
        }

        // sorted form most prüfungen in the group
        Arrays.sort(group, (a, b) -> Integer.compare(b.getCollisionSum(), a.getCollisionSum()));

        //selfexplenetory -> schreibt mir wenn nciht
        List<TimeSlot> availableSlots = getAvailableTimeSlots(group);
        debugStats("Verfügbare Zeitslots: " + availableSlots.size() + " Stück");
        for (MergedAssessment assessment : group) {
            optimizeAssessment(assessment, group, availableSlots);
        }

        //output
        int totalCollisionsBefore = Arrays.stream(group).mapToInt(Assessment::getCollisionSum).sum();
        int totalCollisionsAfter = calculateNewCollisions(group);
        double improvement = totalCollisionsBefore > 0 ?
                ((double)(totalCollisionsBefore - totalCollisionsAfter) / totalCollisionsBefore) * 100 : 0;

        debugGroupInfo("--- Optimierung abgeschlossen für Gruppe ---");
        System.out.println("Kollisionen: " + totalCollisionsBefore + " → " + totalCollisionsAfter +
                " (Verbesserung: " + (totalCollisionsBefore - totalCollisionsAfter) +
                " = " + String.format("%.1f%%", improvement) + ")");

        debugGroupInfo("FINALE TERMINE:");
        for (MergedAssessment assessment : group) {
            System.out.println("  " + assessment.getName() +
                    ": " + assessment.getOptimizedBegin());
        }

        // debug studenten
        debugGroupInfo("STUDENT-BEISPIELE:");
            // Nimm die ersten 2 Prüfungen und zeige gemeinsame Studenten
            MergedAssessment p1 = group[0];
            MergedAssessment p2 = group[1];

            Set<String> gemeinsam = new HashSet<>(p1.getRegisteredStudents());
            gemeinsam.retainAll(p2.getRegisteredStudents());

            if (!gemeinsam.isEmpty()) {
                String student = gemeinsam.iterator().next(); // Ersten nehmen
                System.out.println("Student " + student.substring(0,8) + "...");
                System.out.println("  " + p1.getName() + ": " + p1.getOptimizedBegin());
                System.out.println("  " + p2.getName() + ": " + p2.getOptimizedBegin());

                if (p1.getOptimizedBegin() != null && p2.getOptimizedBegin() != null) {
                    long tageAbstand = ChronoUnit.DAYS.between(
                            p1.getOptimizedBegin().toLocalDate(),
                            p2.getOptimizedBegin().toLocalDate()
                    );
                    System.out.println("  → Abstand: " + Math.abs(tageAbstand) + " Tage");
                }
            }
    }

    private static void optimizeAssessment(MergedAssessment assessment, MergedAssessment[] group, List<TimeSlot> availableSlots) {
        if (availableSlots.isEmpty()) {
            return; //Guard Clause if somethign fails
        }

        TimeSlot bestSlot = null;
        double bestSatisfaction = Double.NEGATIVE_INFINITY;

        // core Algorithm testing for the wenigsten collisions
        for (TimeSlot slot : availableSlots) {
            //temp
            LocalDateTime oldBegin = assessment.getOptimizedBegin();
            LocalDateTime oldEnd = assessment.getOptimizedEnd();

            assessment.setOptimizedBegin(slot.start);
            assessment.setOptimizedEnd(slot.start.plus(slot.duration));

            double satisfaction = getSatisfactionValueOfAssessment(assessment);

            if (satisfaction > bestSatisfaction) {  // HÖHER ist besser
                bestSatisfaction = satisfaction;
                bestSlot = slot;

                if (satisfaction >= 3.0) {
                    break;
                }
            }

            // Restore old times
            assessment.setOptimizedBegin(oldBegin);
            assessment.setOptimizedEnd(oldEnd);
            debugStats("Teste " + assessment.getName() + " auf " + slot.start +
                    " -> Satisfaction: " + satisfaction);
        }

        // best slot found and prüfung set on it
        if (bestSlot != null) {
            debugStats("Gewählter Slot: " + bestSlot.start + " Duration: " + bestSlot.duration);

            if (bestSlot.start != assessment.getBegin()) {
                debugMove("Verschiebe " + assessment.getName() + " von " + assessment.getBegin() + " nach " + bestSlot.start);
                assessment.setOptimizedBegin(bestSlot.start);
                assessment.setOptimizedEnd(bestSlot.start.plus(bestSlot.duration));
            }

            boolean removed = availableSlots.remove(bestSlot);
            debugStats("Slot entfernt: " + removed + ". Verbleibende Slots: " + availableSlots.size());
        }
    }

    private static List<TimeSlot> getAvailableTimeSlots(MergedAssessment[] group) {
        List<TimeSlot> timeSlots = new ArrayList<>();

        // gather all timslots
        for (MergedAssessment assessment : group) {
            if (assessment.getBegin() != null && assessment.getEnd() != null) {
                Duration duration = Duration.between(assessment.getBegin(), assessment.getEnd());
                TimeSlot slot = new TimeSlot(assessment.getBegin(), duration);

                if (!containsTimeSlot(timeSlots, slot)) {
                    timeSlots.add(slot);
                }
            }
        }

        // generate new timslots if none available
        generateUniversalTimeSlots(timeSlots, group);

        return timeSlots;
    }

    // its applicable on all times and not hardcoded (Hat mich meine letzten nerven gekostet)
    private static void generateUniversalTimeSlots(List<TimeSlot> timeSlots, MergedAssessment[] group) {
        // extract zeitraum der prüfungen
        LocalDateTime earliestDate = null;
        LocalDateTime latestDate = null;

        for (MergedAssessment assessment : group) {
            if (assessment.getBegin() != null) {
                if (earliestDate == null || assessment.getBegin().isBefore(earliestDate)) {
                    earliestDate = assessment.getBegin();
                }
                if (latestDate == null || assessment.getBegin().isAfter(latestDate)) {
                    latestDate = assessment.getBegin();
                }
            }
        }

        // verschiebe if no termine could be found
        if (earliestDate != null && latestDate != null) {
            LocalDateTime startRange = earliestDate.minusDays(6);
            LocalDateTime endRange = latestDate.plusDays(6);

            // gather all durations
            Set<Duration> existingDurations = new HashSet<>();
            for (MergedAssessment assessment : group) {
                if (assessment.getBegin() != null && assessment.getEnd() != null) {
                    existingDurations.add(Duration.between(assessment.getBegin(), assessment.getEnd()));
                }
            }

            // if no duration could be found add a standrd duration of 2 hours
            // can be changed without exploding the whole algorithm
            if (existingDurations.isEmpty()) {
                existingDurations.add(Duration.ofHours(2));
            }

            // gatehr all start times
            Set<LocalTime> existingTimes = new HashSet<>();
            for (MergedAssessment assessment : group) {
                if (assessment.getBegin() != null) {
                    existingTimes.add(assessment.getBegin().toLocalTime());
                }
            }

            // if no starting time has been found they get one between 8:00 and 16:00
            if (existingTimes.isEmpty()) {
                existingTimes.addAll(Arrays.asList(
                        LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
                        LocalTime.of(11, 0), LocalTime.of(12, 0), LocalTime.of(13, 0),
                        LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0),
                        LocalTime.of(17, 0)
                ));
            } else {
                existingTimes.addAll(Arrays.asList(
                        LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
                        LocalTime.of(13, 0), LocalTime.of(14, 30), LocalTime.of(16, 0)
                ));
            }

            // generate slots for each day in duration
            LocalDateTime current = startRange.toLocalDate().atStartOfDay();
            while (current.isBefore(endRange)) {
                // monaday - friday
                if (current.getDayOfWeek().getValue() <= 5) {
                    for (LocalTime time : existingTimes) {
                        LocalDateTime slotStart = current.toLocalDate().atTime(time);

                        for (Duration duration : existingDurations) {
                            timeSlots.add(new TimeSlot(slotStart, duration));
                        }
                    }
                }
                current = current.plusDays(1);
            }
        }
    }

    // self explenatory
    private static boolean containsTimeSlot(List<TimeSlot> slots, TimeSlot newSlot) {
        for (TimeSlot slot : slots) {
            if (slot.start.equals(newSlot.start) && slot.duration.equals(newSlot.duration)) {
                return true;
            }
        }
        return false;
    }

    private static int calculateNewCollisions(MergedAssessment[] group) {
        int totalCollisions = 0;

        for (int i = 0; i < group.length; i++) {
            for (int j = i + 1; j < group.length; j++) {
                MergedAssessment a = group[i];
                MergedAssessment b = group[j];

                LocalDateTime aStart = a.getOptimizedBegin();
                LocalDateTime aEnd = a.getOptimizedEnd();
                LocalDateTime bStart = b.getOptimizedBegin();
                LocalDateTime bEnd = b.getOptimizedEnd();

                if (aStart != null && aEnd != null && bStart != null && bEnd != null) {
                    // check überschneidung
                    totalCollisions = getTotalCollisions(totalCollisions, a, b, aStart, aEnd, bStart, bEnd);
                }
            }
        }

        return totalCollisions;
    }

    private static int getTotalCollisions(int totalCollisions, MergedAssessment a, MergedAssessment b, LocalDateTime aStart,
                                          LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        // checking if they are überschneiden
        if (aStart.isBefore(bEnd) && aEnd.isAfter(bStart)) {

            //safety mechanism if one of them is null -> NullPointerException
            Set<String> aStudents = a.getRegisteredStudents();
            Set<String> bStudents = b.getRegisteredStudents();

            if (aStudents != null && bStudents != null) {
                Set<String> commonStudents = new HashSet<>(aStudents);
                commonStudents.retainAll(bStudents);
                totalCollisions += commonStudents.size();
            }
        }
        return totalCollisions;
    }
}

