package data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Assessment {
    private final Long number;
    private final String name;
    private final String stg;
    private final String pversion;
    private final LocalDateTime begin;
    private final LocalDateTime end;
    private Set<String> registeredStudents = null; //TODO: set<Long> wg. MatrNo -> Hamann
    private Integer collisionSum = null;
    private Map<Assessment, Integer> collisionCountByAssessment = null;

    public Assessment(Long number, String name, String stg, String pversion, LocalDateTime begin, LocalDateTime end) {
        this.number = number;
        this.name = name;
        this.stg = stg;
        this.pversion = pversion;
        this.begin = begin;
        this.end = end;
    }

    public Long getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public static String calculateQualifiedName(String stg, String pversion, Long no, String name) {
        return name + ".--." + (no == null ? "" : no) + ".--." + (pversion == null ? "" : pversion) + ".--." + (stg == null ? "" : stg);
    }

    public String getQualifiedName() {
        return calculateQualifiedName(stg, pversion, number, name);
    }

    public LocalDateTime getBegin() {
        return begin;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Set<String> getRegisteredStudents() {
        if (registeredStudents == null)
            return null;
        return new HashSet<>(registeredStudents);
    }

    public void setRegisteredStudents(Set<String> registeredStudents) {
        this.registeredStudents = registeredStudents;
    }

    public Integer getCollisionSum() {
        return collisionSum;
    }

    public void setCollisionSum(Integer collisionSum) {
        this.collisionSum = collisionSum;
    }

    public Map<Assessment, Integer> getCollisionCountByAssessment() {
        if (collisionCountByAssessment == null)
            return null;
        return new HashMap<>(collisionCountByAssessment);
    }

    public void setCollisionCountByAssessment(Map<Assessment, Integer> collisionCountByAssessment) {
        this.collisionCountByAssessment = collisionCountByAssessment;
    }

    @Override
    public String toString() {
        // create map with qualifiedName as string instead of object to print this instead because printing the actual Assessment object would create an infinite loop.
        Map<String, Integer> collisions = new HashMap<>();
        if (this.collisionCountByAssessment != null) {
            for (Assessment p : this.collisionCountByAssessment.keySet()) {
                collisions.put(p.getQualifiedName(), this.collisionCountByAssessment.get(p));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        return "Assessment{" +
                "number=" + number +
                ", name='" + name + '\'' +
                ", qualifiedName='" + getQualifiedName() + '\'' +
                ", begin=" + (begin != null ? begin.format(formatter) : null) +
                ", end=" + (end != null ? end.format(formatter) : null) +
                ", collisionSum=" + collisionSum +
                ", collisionCountByAssessment=" + collisions +
                ", registeredStudents=" + registeredStudents +
                '}';
    }
}
