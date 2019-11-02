package alberta.sn.hm.msr;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@SpringBootApplication
public class GitMsrApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(GitMsrApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Git git = Git.cloneRepository()
                .setURI("https://github.com/hamedmirzaei/test-msr2.git")
                .setDirectory(new File("data"))
                .setCloneAllBranches(true)
                .call();

        /*try (Repository repository = openJGitCookbookRepository()) {
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().all().call();

                Iterator<RevCommit> iterator = commits.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    System.out.println("LogCommit: " + commit);
                }
            }
        }*/
    }

    public static Repository openJGitCookbookRepository() throws IOException {
        return Git.open(new File("data")).getRepository();
    }
}
