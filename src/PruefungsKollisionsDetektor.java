import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

class PruefungsKollisionsDetektor {
    final static String PATH_PRUEFUNGEN = "resources/pruefungen.csv";
    final static String PATH_ANMELDUNGEN = "resources/anmeldungen.csv";
    final static String PATH_OUTPUT = "target/collisions.csv";

    public static void main(String[] args) throws IOException { //TODO: error handling
        Pruefung[] pruefungen1 = LoadManager.load_pruefungen(PATH_PRUEFUNGEN);
        Pruefung[] pruefungen2 = LoadManager.load_missing_pruefungen_from_anmeldungen(PATH_ANMELDUNGEN, pruefungen1);
        Pruefung[] pruefungen = Stream.of(pruefungen1, pruefungen2).flatMap(Arrays::stream).toArray(Pruefung[]::new);

        LoadManager.load_anmeldungen(PATH_ANMELDUNGEN, pruefungen);

        for (Pruefung p : pruefungen) {
            Set<String> collisions_all = new HashSet<>();
            Map<Pruefung, Integer> collisions = new LinkedHashMap<>();

            for (Pruefung k : pruefungen) {
                if (p == k)
                    continue;
                int collisions_local = 0;

                Set<String> anmeldungen_loop;
                Set<String> anmeldungen_compare;
                if (k.getAnmeldungen().size() > p.getAnmeldungen().size()) {
                    anmeldungen_loop = p.getAnmeldungen();
                    anmeldungen_compare = k.getAnmeldungen();
                } else {
                    anmeldungen_loop = k.getAnmeldungen();
                    anmeldungen_compare = p.getAnmeldungen();
                }

                for (String s : anmeldungen_loop) {
                    if (anmeldungen_compare.contains(s)) {
                        collisions_local++;
                        collisions_all.add(s);
                    }
                }

                if (collisions_local > 0)
                    collisions.put(k, collisions_local);
            }

            p.setCollisions_all(collisions_all.size());
            p.setCollisions(collisions);
        }


        SaveManager.save_collision(PATH_OUTPUT, pruefungen);

        System.out.println("Finished");
    }
}