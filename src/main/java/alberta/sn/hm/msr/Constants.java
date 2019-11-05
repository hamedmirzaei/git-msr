package alberta.sn.hm.msr;

public class Constants {

    public static String DATA_FOLDER = "C:/Users/h.mirzaei/IdeaProjects/My Git Projects/data";
    public static String TEMP_FOLDER = "C:/Users/h.mirzaei/IdeaProjects/My Git Projects/temp";
    public static String RESULT_FILE = TEMP_FOLDER + "/result.csv";
    public static String NEW_FOLDER = "new";
    public static String OLD_FOLDER = "old";
    public static Boolean KEEP_TEMP_FILES = false;

    public static CsvWriter csvWriter = new CsvWriter();
    public static MethodDiffGenerator methodDiffGenerator = new MethodDiffGenerator();

}
