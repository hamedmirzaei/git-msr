package alberta.sn.hm.msr;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class DiffRenamedFile {
    public static void main(String[] args)
            throws IOException, GitAPIException {
        try (Repository repo = openJGitCookbookRepository()) {
            try (Git git = new Git(repo)) {
                Iterable<RevCommit> commits = git.log().all().call();
                // the first is the latest
                Iterator<RevCommit> iterator = commits.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    if (iterator.hasNext()) {
                        System.out.println("LogCommit: " + commit);
                        runDiff(repo,
                                commit.getId().getName() + "^",
                                commit.getId().getName(),
                                "src/main/java/GitMsrApplication.java");
                    }
                }
            }
        }
    }

    private static void runDiff(Repository repo, String oldCommit, String newCommit, String path) throws IOException, GitAPIException {
        // Diff README.md between two commits. The file is named README.md in
        // the new commit (5a10bd6e), but was named "jgit-cookbook README.md" in
        // the old commit (2e1d65e4).
        DiffEntry diff = diffFile(repo,
                oldCommit,
                newCommit,
                path);

        if (diff != null) {
            // Display the diff
            System.out.println("Showing diff of " + path + " between " + oldCommit + " and " + newCommit);
            try (DiffFormatter formatter = new DiffFormatter(System.out)) {
                formatter.setRepository(repo);
                //noinspection ConstantConditions
                formatter.format(diff);
            }
        }
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    private static @NonNull
    DiffEntry diffFile(Repository repo, String oldCommit,
                       String newCommit, String path) throws IOException, GitAPIException {
        Config config = new Config();
        config.setBoolean("diff", null, "renames", true);
        DiffConfig diffConfig = config.get(DiffConfig.KEY);
        try (Git git = new Git(repo)) {
            List<DiffEntry> diffList = git.diff().
                    setOldTree(prepareTreeParser(repo, oldCommit)).
                    setNewTree(prepareTreeParser(repo, newCommit)).
                    setPathFilter(FollowFilter.create(path, diffConfig)).
                    call();
            if (diffList.size() == 0)
                return null;
            if (diffList.size() > 1)
                throw new RuntimeException("invalid diff");
            return diffList.get(0);
        }
    }

    public static Repository openJGitCookbookRepository() throws IOException {
        return Git.open(new File("data")).getRepository();
    }
}