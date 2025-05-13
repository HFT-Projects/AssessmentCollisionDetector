import java.text.SimpleDateFormat;
import java.util.*;

public class Pruefung {
    private Long nr;
    private final String name;
    private final String stg;
    private final String pversion;
    private final Calendar begin;
    private final Integer duration_min;
    private Set<String> anmeldungen = null; //TODO: set<Long> wg. MatrNr -> Hamann
    private Integer collisions_all = null;
    private Map<Pruefung, Integer> collisions = null;

    public Pruefung(Long nr, String name, String stg, String pversion, Calendar begin, Integer duration_min) {
        this.nr = nr;
        this.name = name;
        this.stg = stg;
        this.pversion = pversion;
        this.begin = begin;
        this.duration_min = duration_min;
    }

    public Long getNr() {
        return nr;
    }

    public void _overrideNr(Long nr) {
        this.nr = nr;
    }

    public String getName() {
        return name;
    }

    public String getQualified_name() {
        return stg + "-" + pversion + "-" + nr + "-" + name;
    }

    public Integer getDuration_min() {
        return duration_min;
    }

    public Calendar getBegin() {
        if (begin == null)
            return null;
        return (Calendar) begin.clone();
    }

    public Set<String> getAnmeldungen() {
        if (anmeldungen == null)
            return null;
        return new HashSet<>(anmeldungen);
    }

    public void setAnmeldungen(Set<String> anmeldungen) {
        this.anmeldungen = anmeldungen;
    }

    public Integer getCollisions_all() {
        return collisions_all;
    }

    public void setCollisions_all(Integer collisions_all) {
        this.collisions_all = collisions_all;
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
        Map<Long, Integer> collision_with_nr = new TreeMap<>();

        for (Pruefung p : collisions.keySet()) {
            collision_with_nr.put(p.getNr(), collisions.get(p));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return "Pruefung{" +
                "nr=" + nr +
                ", name='" + name + '\'' +
                ", qualified_name='" + getQualified_name() + '\'' +
                ", begin=" + sdf.format(begin.getTime()) +
                ", duration_min=" + duration_min +
                ", collisions_all=" + collisions_all +
                ", collisions=" + collision_with_nr +
                ", anmeldungen=" + anmeldungen +
                '}';
    }
}
