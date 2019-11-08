package alberta.sn.hm.msr;

import alberta.sn.hm.msr.domain.CommitProcessor;
import alberta.sn.hm.msr.domain.GitExecutor;
import alberta.sn.hm.msr.exception.CommonException;
import alberta.sn.hm.msr.util.Constants;
import alberta.sn.hm.msr.util.FileUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class GitMsrApplication {

    private static Boolean cloneFromGit = false;

    public static void main(String[] args) {
        System.out.println("Application started");
        if (args.length == 0) {
            System.out.println("Arguments are not defined correctly. For more info try java -jar Git-Msr.jar -help");
            return;
        }
        /*String[] argsTest = new String[9];
        argsTest[0] = "https://github.com/hamedmirzaei/test-msr2.git";
        argsTest[1] = "-basePath";
        argsTest[2] = "C:/Git-Msr/";
        argsTest[3] = "-threadPoolSize";
        argsTest[4] = "50";
        argsTest[5] = "-keepTempFiles";
        argsTest[6] = "false";
        argsTest[7] = "-output";
        argsTest[8] = "1111111";*/
        if (isHelpArgument(args[0])) {
            return;
        }
        try {
            processInputArguments(args);
        } catch (CommonException.InputArgsValidationException e) {
            System.out.println(e.getMessage());
            return;
        }
        GitExecutor gitExecutor = new GitExecutor();
        if (!cloneFromGit) {
            Constants.properties.setRepositoryFolderPath(args[0]);
        }
        try {
            prepareTheWorld();
        } catch (IOException e) {
            System.out.println("Exception raised during preparing directories");
        }
        if (cloneFromGit) {
            try {
                gitExecutor.cloneRemoteGitRepository(args[0], Constants.properties.getRepositoryFolderPath());
            } catch (GitAPIException e) {
                System.out.println("Exception raised during cloning repository");
                return;
            }
        }

        try (Repository repository = gitExecutor.openLocalGitRepository(Constants.properties.getRepositoryFolderPath())) {
            try {
                System.out.println("Get all commits");
                Iterable<RevCommit> commitsIterable = gitExecutor.getCommitsOfRepository(repository);
                System.out.println("Iterate over commits");
                Iterator<RevCommit> iterator = commitsIterable.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    Constants.executor.execute(new CommitProcessor(repository, commit));
                }
            } catch (IOException | GitAPIException e) {
                System.out.println("Exception raised during getting commits from repository");
            }
        } catch (IOException e) {
            System.out.println("Exception raised during opening repository");
        }

        // shut down the executor pool
        Constants.executor.shutdown();
        try {
            // wait for existing threads to be finished
            Constants.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Exception raised during waiting for threads to be finished");
        }
        System.out.println("Writing results to file...");
        Constants.csvWriter.close();
        System.out.println("Completed");
    }

    private static Boolean isHelpArgument(String argument) {
        if ("-help".equals(argument) || "help".equals(argument) || "--help".equals(argument)) {
            System.out.println("Usage:");
            System.out.println("\tjava -jar Git-Msr.jar git-path [optional]");
            System.out.println();
            System.out.println("\t\tgit-path: it is either a remote git url like \"https://github.com/hamedmirzaei/test-msr2.git\"\n" +
                    "\t\t\tor a local folder containing a git repository which is cloned before like \"C:/path-to-repository/\"");
            System.out.println();
            System.out.println("\t\t[optional]: arguments that you can use to customize the application's execution");
            System.out.println();
            System.out.println("\t\t\t-basePath: the path to folder you want to keep results and temporary files of the application. \n" +
                    "\t\t\t\tThe default value is \"C:/Git-Msr/\"");
            System.out.println();
            System.out.println("\t\t\t-threadPoolSize: the number of threads that the application uses to process the git \n" +
                    "\t\t\t\trepository. The default value is 50.");
            System.out.println();
            System.out.println("\t\t\t-keepTempFiles: there are some temporary files that application creates in the middle of\n" +
                    "\t\t\t\texecution like commits and changed files in each commit. The default value is false.");
            System.out.println();
            System.out.println("\t\t\t-output: this option is used to tell the application about which results are of interest?\n" +
                    "\t\t\t\tthis is a string of length seven which each char is representing a specific result and is either 1 or 0.\n" +
                    "\t\t\t\t1 means the corresponding result is of interest and 0 otherwise. the first char is for detecting added methods.\n" +
                    "\t\t\t\tthe second char is for detecting removed methods. the third char is for detecting methods which their return\n" +
                    "\t\t\t\ttype changed. the fourth char is for detecting methods which their modifiers changed. the fifth char is for\n" +
                    "\t\t\t\tdetecting methods which parameters added to them. the sixth char is for detecting methods which parameters\n" +
                    "\t\t\t\tremoved from them. the seventh char is for detecting methods which their parameters either changed in name or type.\n" +
                    "\t\t\t\tSo for example a value of \"1000000\" means that only search for added methods, a value of \"0000100\" means\n" +
                    "\t\t\t\tthat only search for methods which some parameters added to them and a value of \"1111111\" means that all\n" +
                    "\t\t\t\tchanges are of interests.");
            System.out.println();
            System.out.println("\texample 1: java -jar Git-Msr.jar https://github.com/hamedmirzaei/test-msr2.git -basePath C:/Git-Msr/ -threadPoolSize 40");
            System.out.println("\texample 2: java -jar Git-Msr.jar C:/Git-Msr/repository/ -basePath C:/Git-Msr/ -output 1010100 -keepTempFiles true");
            return true;
        }
        return false;
    }

    private static void processInputArguments(String[] args) throws CommonException.InputArgsValidationException {
        if (args.length > 0 && args.length % 2 == 1) {
            if (args[0].startsWith("http") && args[0].endsWith(".git")) {
                cloneFromGit = true;
            } else if (!new File(args[0]).isDirectory()) {
                throw new CommonException.InputArgsValidationException("The first argument should be either a remote git url or a local directory containing your repository");
            }
            for (int i = 1; i < args.length; i += 2) {
                switch (args[i]) {
                    case "-basePath":
                        if (!new File(args[i + 1]).isDirectory()) {
                            throw new CommonException.InputArgsValidationException("BasePath should be an existing directory: " + args[i + 1]);
                        }
                        Constants.properties.setBasePath(args[i + 1]);
                        break;
                    case "-threadPoolSize":
                        try {
                            int threadPoolSize = Integer.parseInt(args[i + 1]);
                            if (threadPoolSize <= 0) {
                                throw new CommonException.InputArgsValidationException("ThreadPoolSize should be an integer greater than 0: " + args[i + 1]);
                            }
                            Constants.properties.setThreadPoolSize(threadPoolSize);
                        } catch (NumberFormatException e) {
                            throw new CommonException.InputArgsValidationException("ThreadPoolSize should be a number: " + args[i + 1]);
                        }
                        break;
                    case "-keepTempFiles":
                        if ("true".equals(args[i + 1].toLowerCase()) || "false".equals(args[i + 1].toLowerCase()))
                            Constants.properties.setKeepTemporaryFiles(Boolean.parseBoolean(args[i + 1]));
                        else {
                            throw new CommonException.InputArgsValidationException("KeepTempFiles should be either true or false: " + args[i + 1]);
                        }
                        break;
                    case "-output":
                        if (args[i + 1].length() != 7) {
                            throw new CommonException.InputArgsValidationException("Output should be 7 length string: " + args[i + 1]);
                        }
                        for (int j = 0; j < 7; j++) {
                            if (!"0".equals(args[i + 1].charAt(j) + "") && !"1".equals(args[i + 1].charAt(j) + "")) {
                                throw new CommonException.InputArgsValidationException("Output should be 7 length string of 0 and 1: " + args[i + 1]);
                            }
                        }
                        if (!args[i + 1].contains("1")) {
                            throw new CommonException.InputArgsValidationException("Output should have at least a 1: " + args[i + 1]);
                        }
                        Constants.properties.setDetectMethodAdd("1".equals(args[i + 1].charAt(0) + "") ? true : false);
                        Constants.properties.setDetectMethodRemove("1".equals(args[i + 1].charAt(1) + "") ? true : false);
                        Constants.properties.setDetectMethodChangeReturn("1".equals(args[i + 1].charAt(2) + "") ? true : false);
                        Constants.properties.setDetectMethodChangeModifier("1".equals(args[i + 1].charAt(3) + "") ? true : false);
                        Constants.properties.setDetectParameterAdd("1".equals(args[i + 1].charAt(4) + "") ? true : false);
                        Constants.properties.setDetectParameterRemove("1".equals(args[i + 1].charAt(5) + "") ? true : false);
                        Constants.properties.setDetectParameterChange("1".equals(args[i + 1].charAt(6) + "") ? true : false);
                        break;
                    default:
                        throw new CommonException.InputArgsValidationException("Argument is not valid: " + args[i]);
                }
            }
        } else {
            System.out.println("Arguments are not defined correctly. For more info try java -jar Git-Msr.jar -help");
        }
    }

    private static void prepareTheWorld() throws IOException {
        FileUtil.createPath(Constants.properties.getRepositoryFolderPath());
        FileUtil.createPath(Constants.properties.getOutputFolderPath());
    }

}