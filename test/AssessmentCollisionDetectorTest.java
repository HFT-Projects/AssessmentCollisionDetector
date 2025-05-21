import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentCollisionDetectorTest {
    private final static String PATH_INPUT_COLLISIONS_SAMPLE = "resources/kollisionen_v2.csv";

    private boolean compareFiles(String path1, String path2) throws IOException {
        String f1 = Files.readString(Paths.get(path1));
        String f2 = Files.readString(Paths.get(path2));
        return f1.equals(f2);

    }

    @Test
    void testMainOutputMatchesSample() throws Exception {
        Main.main(new String[]{});
        assertTrue(compareFiles(PATH_INPUT_COLLISIONS_SAMPLE, Main.PATH_OUTPUT_COLLISIONS),
                "The output file doesn't match the sample file!");
    }
}

