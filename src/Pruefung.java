import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Pruefung {
    private final Long nr;
    private final String name;
    private final String stg;
    private final String pversion;
    private final LocalDateTime begin;
    private final LocalDateTime end;
    private Set<String> anmeldungen = null; //TODO: set<Long> wg. MatrNr -> Hamann
    private Integer collisionsAll = null;
    private Map<Pruefung, Integer> collisions = null;

    public Pruefung(Long nr, String name, String stg, String pversion, LocalDateTime begin, LocalDateTime end) {
        this.nr = nr;
        this.name = name;
        this.stg = stg;
        this.pversion = pversion;
        this.begin = begin;
        this.end = end;
    }

    public Long getNr() {
        return nr;
    }

    public String getName() {
        return name;
    }

    public static String calculateQualifiedName(String stg, String pversion, Long nr, String name) {
        return stg + "-" + pversion + "-" + nr + "-" + name;
    }

    public String getQualifiedName() {
        return calculateQualifiedName(stg, pversion, nr, name);
    }

    public LocalDateTime getBegin() {
        return begin;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Set<String> getAnmeldungen() {
        if (anmeldungen == null)
            return null;
        return new HashSet<>(anmeldungen);
    }

    public void setAnmeldungen(Set<String> anmeldungen) {
        this.anmeldungen = anmeldungen;
    }

    public Integer getCollisionsAll() {
        return collisionsAll;
    }

    public void setCollisionsAll(Integer collisionsAll) {
        this.collisionsAll = collisionsAll;
    }

    public Map<Pruefung, Integer> getCollisions() {
        if (collisions == null)
            return null;
        return new LinkedHashMap<>(collisions);
    }

    public void setCollisions(Map<Pruefung, Integer> collisions) {
        this.collisions = collisions;
    }

    @Override
    public String toString() {
        Map<String, Integer> collisions = new TreeMap<>();

        if (this.collisions != null) {
            for (Pruefung p : this.collisions.keySet()) {
                collisions.put(p.getQualifiedName(), this.collisions.get(p));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        return "Pruefung{" +
                "nr=" + nr +
                ", name='" + name + '\'' +
                ", qualifiedName='" + getQualifiedName() + '\'' +
                ", begin=" + begin.format(formatter) +
                ", end=" + end.format(formatter) +
                ", collisionsAll=" + collisionsAll +
                ", collisions=" + collisions +
                ", anmeldungen=" + anmeldungen +
                '}';
    }
}
