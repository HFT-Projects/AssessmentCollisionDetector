import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MergedAssessment extends Assessment {

    private LocalDateTime optimizedBegin;
    private LocalDateTime optimizedEnd;

    public MergedAssessment(String name, LocalDateTime begin, LocalDateTime end) {
        super(null, name, null, null, begin, end);
        this.optimizedBegin = begin;
        this.optimizedEnd = end;
    }

    public LocalDateTime getOptimizedBegin() {
        return optimizedBegin != null ? optimizedBegin : getBegin();
    }

    public LocalDateTime getOptimizedEnd() {
        return optimizedEnd != null ? optimizedEnd : getEnd();
    }

    public void setOptimizedBegin(LocalDateTime begin) {
        this.optimizedBegin = begin;
    }

    public void setOptimizedEnd(LocalDateTime end) {
        this.optimizedEnd = end;
    }

    @Override
    public String toString() {
        // create map with qualifiedName as string instead of object to print this instead because printing the actual Assessment object would create an infinite loop.
        Map<String, Integer> collisions = new HashMap<>();
        if (this.getCollisionCountByAssessment() != null) {
            for (Assessment p : this.getCollisionCountByAssessment().keySet()) {
                collisions.put(p.getQualifiedName(), this.getCollisionCountByAssessment().get(p));
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        return "MergedAssessment{" +
                ", name='" + getName() + '\'' +
                ", begin=" + (getBegin() != null ? getBegin().format(formatter) : null)+
                ", end=" + (getEnd() != null ? getEnd().format(formatter) : null) +
                ", collisionSum=" + getCollisionSum() +
                ", collisionCountByAssessment=" + collisions +
                ", registeredStudents=" + getRegisteredStudents() +
                '}';
    }
}
