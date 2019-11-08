package alberta.sn.hm.msr.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class FileUtil {

    public static void deletePath(String path) throws IOException {
        System.out.println("DELETING " + path);
        if (new File(path).exists())
            Files.walk(Paths.get(path)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    public static void createPath(String path) throws IOException {
        if (new File(path).exists())
            deletePath(path);
        System.out.println("CREATING " + path);
        new File(path).mkdirs();

    }
}
