package alberta.sn.hm.msr;

import alberta.sn.hm.msr.domain.CommitProcessor;
import alberta.sn.hm.msr.domain.GitExecutor;
import alberta.sn.hm.msr.exception.CommonException;
import alberta.sn.hm.msr.util.Constants;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class GitMsrApplication {

    public static void main(String[] args) {
        System.out.println("Application started");
        String[] argsTest = new String[8];
        argsTest[0] = "-basePath";
        argsTest[1] = "C:/Git-Msr/";
        argsTest[2] = "-threadPoolSize";
        argsTest[3] = "10";
        argsTest[4] = "-keepTempFiles";
        argsTest[5] = "false";
        argsTest[6] = "-options";
        argsTest[7] = "1111111";
        try {
            processInputArguments(argsTest);
        } catch (CommonException.InputArgsValidationException e) {
            System.out.println(e.getMessage());
            return;
        }
        GitExecutor gitExecutor = new GitExecutor();
        try {
            //gitExecutor.cloneRemoteGitRepository("https://github.com/spring-projects/spring-integration.git", Constants.properties.getRepositoryFolderPath());
            gitExecutor.cloneRemoteGitRepository("https://github.com/hamedmirzaei/test-msr2.git",
                    Constants.properties.getRepositoryFolderPath());
        } catch (GitAPIException e) {
            System.out.println("Exception raised during cloning repository");
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
            e.printStackTrace();
        }
        System.out.println("Writing results to file...");
        Constants.csvWriter.close();
        System.out.println("Completed");
    }

    private static void processInputArguments(String[] args) throws CommonException.InputArgsValidationException {
        if (args.length > 0 && args.length % 2 == 0) {
            for (int i = 0; i < args.length; i += 2) {
                switch (args[i]) {
                    case "-basePath":
                        if (!new File(args[i + 1]).isDirectory()) {
                            throw new CommonException.InputArgsValidationException("BasePath should be a directory: " + args[i + 1]);
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
                    case "-options":
                        if (args[i + 1].length() != 7) {
                            throw new CommonException.InputArgsValidationException("Options should be 7 length string: " + args[i + 1]);
                        }
                        for (int j = 0; j < 7; j++) {
                            if (!"0".equals(args[i + 1].charAt(j) + "") && !"1".equals(args[i + 1].charAt(j) + "")) {
                                throw new CommonException.InputArgsValidationException("Options should be 7 length string of 0 and 1: " + args[i + 1]);
                            }
                        }
                        if (!args[i + 1].contains("1")) {
                            throw new CommonException.InputArgsValidationException("Options should have at least a 1: " + args[i + 1]);
                        }
                        Constants.properties.setDetectMethodAdd("1".equals(args[i + 1].charAt(0)) ? true : false);
                        Constants.properties.setDetectMethodRemove("1".equals(args[i + 1].charAt(1)) ? true : false);
                        Constants.properties.setDetectMethodChangeReturn("1".equals(args[i + 1].charAt(2)) ? true : false);
                        Constants.properties.setDetectMethodChangeModifier("1".equals(args[i + 1].charAt(3)) ? true : false);
                        Constants.properties.setDetectParameterAdd("1".equals(args[i + 1].charAt(4)) ? true : false);
                        Constants.properties.setDetectParameterRemove("1".equals(args[i + 1].charAt(5)) ? true : false);
                        Constants.properties.setDetectParameterChange("1".equals(args[i + 1].charAt(6)) ? true : false);
                        break;
                    default:
                        throw new CommonException.InputArgsValidationException("arg is not valid: " + args[i]);
                }
            }
        } else {
            //TODO exception
        }
    }

}