package alberta.sn.hm.msr.domain;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;

public class GitExecutor {

    public Repository openLocalGitRepository(String gitRepositoryLocalPath) throws IOException {
        System.out.println("Repository opened");
        return Git.open(new File(gitRepositoryLocalPath)).getRepository();
    }

    public void cloneRemoteGitRepository(String gitRepositoryRemoteURL, String gitRepositoryLocalPath) throws GitAPIException {
        System.out.println("Repository cloning...");
        Git.cloneRepository()
                .setURI(gitRepositoryRemoteURL)
                .setDirectory(new File(gitRepositoryLocalPath))
                .setCloneAllBranches(false)
                .call();
        System.out.println("Repository cloned");
    }

    public Iterable<RevCommit> getCommitsOfRepository(Repository repository) throws IOException, GitAPIException {
        Git git = new Git(repository);
        Iterable<RevCommit> commitsIterable = git.log().all().call();
        return commitsIterable;
    }

}
