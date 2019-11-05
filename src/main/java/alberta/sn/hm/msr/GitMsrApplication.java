package alberta.sn.hm.msr;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GitMsrApplication {

    private static ExecutorService executor = new ThreadPoolExecutor(50, 50,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public static void main(String[] args) {
        System.out.println("Application started");
        FileUtil.recreateFolders();
        /*try {
            cloneGitRepository("https://github.com/apache/camel.git");
        } catch (GitAPIException e) {
            System.out.println("Exception raised during cloning repository");
        }*/
        //cloneGitRepository("https://github.com/hamedmirzaei/test-msr2.git");

        try (Repository repository = openGitRepository()) {
            try (Git git = new Git(repository)) {
                System.out.println("Get all commits");
                Iterable<RevCommit> commits = git.log().all().call();
                // the first is the latest
                // for each commit except the first one, run diff of current commit with its parent
                System.out.println("Iterate over commits");
                Iterator<RevCommit> iterator = commits.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    executor.submit(new CommitProcessor(repository, commit));
                }
            } catch (GitAPIException e) {
                System.out.println("Exception raised during getting commits from repository");
            }
        } catch (IOException e) {
            System.out.println("Exception raised during opening repository");
        }
        Constants.csvWriter.close();
    }

    public static Repository openGitRepository() throws IOException {
        System.out.println("Repository opened");
        return Git.open(new File(Constants.DATA_FOLDER)).getRepository();
    }

    private static void cloneGitRepository(String repositoryURL) throws GitAPIException {
        System.out.println("Repository cloning...");
        Git.cloneRepository()
                .setURI(repositoryURL)
                .setDirectory(new File(Constants.DATA_FOLDER))
                .setCloneAllBranches(false)
                .call();
        System.out.println("Repository cloned");
    }
}