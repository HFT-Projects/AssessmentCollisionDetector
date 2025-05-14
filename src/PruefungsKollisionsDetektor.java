import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

class PruefungsKollisionsDetektor {
    final static String PATH_PRUEFUNGEN = "resources/pruefungen.csv";
    final static String PATH_ANMELDUNGEN = "resources/anmeldungen.csv";
    final static String PATH_OUTPUT = "target/collisions.csv";

    public static void main(String[] args) throws IOException { //TODO: error handling
        Pruefung[] pruefungen1 = LoadManager.loadPruefungen(PATH_PRUEFUNGEN);
        Pruefung[] pruefungen2 = LoadManager.loadMissingPruefungenFromAnmeldungen(PATH_ANMELDUNGEN, pruefungen1);
        Pruefung[] pruefungen = Stream.of(pruefungen1, pruefungen2).flatMap(Arrays::stream).toArray(Pruefung[]::new);

        LoadManager.loadAnmeldungen(PATH_ANMELDUNGEN, pruefungen);

        for (Pruefung p : pruefungen) {
            Set<String> collisionsAll = new HashSet<>();
            Map<Pruefung, Integer> collisions = new LinkedHashMap<>();

            for (Pruefung k : pruefungen) {
                if (p == k)
                    continue;
                int collisionsLocal = 0;

                Set<String> anmeldungenLoop;
                Set<String> anmeldungenCompare;
                if (k.getAnmeldungen().size() > p.getAnmeldungen().size()) {
                    anmeldungenLoop = p.getAnmeldungen();
                    anmeldungenCompare = k.getAnmeldungen();
                } else {
                    anmeldungenLoop = k.getAnmeldungen();
                    anmeldungenCompare = p.getAnmeldungen();
                }

                for (String s : anmeldungenLoop) {
                    if (anmeldungenCompare.contains(s)) {
                        collisionsLocal++;
                        collisionsAll.add(s);
                    }
                }

                if (collisionsLocal > 0)
                    collisions.put(k, collisionsLocal);
            }

            p.setCollisionsAll(collisionsAll.size());
            p.setCollisions(collisions);
        }


        SaveManager.saveCollision(PATH_OUTPUT, pruefungen);

        System.out.println("Finished");
    }
}