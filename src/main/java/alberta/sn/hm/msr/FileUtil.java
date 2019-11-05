package alberta.sn.hm.msr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class FileUtil {

    public static void recreateFolders() {
        try {
            deleteFolder(Constants.TEMP_FOLDER);
            new File(Constants.TEMP_FOLDER).mkdir();
            //deleteFolder(Constants.DATA_FOLDER);
            //new File(Constants.DATA_FOLDER).mkdir();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFolder(String path) throws IOException {
        System.out.println("DELETING " + path);
        if (new File(path).exists())
            Files.walk(Paths.get(path)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
}
