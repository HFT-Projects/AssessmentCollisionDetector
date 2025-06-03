import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MergedAssessment extends Assessment {
    public MergedAssessment(String name, LocalDateTime begin, LocalDateTime end) {

        super(null, name, null, null, begin, end);
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
