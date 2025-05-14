import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Assessment {
    private final Long number;
    private final String name;
    private final String stg;
    private final String pversion;
    private final LocalDateTime begin;
    private final LocalDateTime end;
    private Set<String> registeredStudents = null; //TODO: set<Long> wg. MatrNo -> Hamann
    private Integer collisionsAll = null;
    private Map<Assessment, Integer> collisions = null;

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
        return stg + "-" + pversion + "-" + no + "-" + name;
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

    public Integer getCollisionsAll() {
        return collisionsAll;
    }

    public void setCollisionsAll(Integer collisionsAll) {
        this.collisionsAll = collisionsAll;
    }

    public Map<Assessment, Integer> getCollisions() {
        if (collisions == null)
            return null;
        return new LinkedHashMap<>(collisions);
    }

    public void setCollisions(Map<Assessment, Integer> collisions) {
        this.collisions = collisions;
    }

    @Override
    public String toString() {
        Map<String, Integer> collisions = new TreeMap<>();

        if (this.collisions != null) {
            for (Assessment p : this.collisions.keySet()) {
                collisions.put(p.getQualifiedName(), this.collisions.get(p));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        return "Assessment{" +
                "number=" + number +
                ", name='" + name + '\'' +
                ", qualifiedName='" + getQualifiedName() + '\'' +
                ", begin=" + begin.format(formatter) +
                ", end=" + end.format(formatter) +
                ", collisionsAll=" + collisionsAll +
                ", collisions=" + collisions +
                ", registeredStudents=" + registeredStudents +
                '}';
    }
}
