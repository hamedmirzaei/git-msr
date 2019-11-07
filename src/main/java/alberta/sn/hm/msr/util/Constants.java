package alberta.sn.hm.msr.util;

import alberta.sn.hm.msr.domain.MethodDiffGenerator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Constants {

    public static String NEW_FOLDER_NAME = "new";
    public static String OLD_FOLDER_NAME = "old";

    public static MyProperties properties = new MyProperties("config.properties");
    public static CsvWriter csvWriter = new CsvWriter();
    public static MethodDiffGenerator methodDiffGenerator = new MethodDiffGenerator();

    public static ExecutorService executor = new ThreadPoolExecutor(Constants.properties.getThreadPoolSize(),
            Constants.properties.getThreadPoolSize(),
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

}
