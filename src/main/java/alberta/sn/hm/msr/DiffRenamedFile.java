package alberta.sn.hm.msr;

import com.github.stkent.githubdiffparser.GitHubDiffParser;
import com.github.stkent.githubdiffparser.models.Diff;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DiffRenamedFile {
    public static void main(String[] args) throws IOException, GitAPIException {
        recreateTempFolder();
        try (Repository repository = openRepository()) {
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().all().call();
                // the first is the latest
                // for each commit except the first one, run diff of current commit with its parent
                Iterator<RevCommit> iterator = commits.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    List<DiffEntry> diffEntries = runDiff(repository, commit);
                    if (!diffEntries.isEmpty()) {
                        String diffFileName = "temp/" + commit.getId().getName() + ".diff";
                        new File(diffFileName).createNewFile();
                        for (DiffEntry diffEntry : diffEntries) {
                            System.out.println("Output record: " + commit.getId().getName() + ", " + diffEntry.getNewPath() + ", ");
                            try (DiffFormatter formatter = new DiffFormatter(new FileOutputStream(diffFileName, true))) {
                                formatter.setRepository(repository);
                                formatter.format(diffEntry);
                            }
                        }
                        GitHubDiffParser parser = new GitHubDiffParser();
                        InputStream in = new FileInputStream(diffFileName);
                        List<Diff> diff = parser.parse(in);
                        System.out.println();
                    }
                }
            }
        }
    }

    private static void recreateTempFolder() throws IOException {
        Files.walk(Paths.get("temp")).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        new File("temp").mkdir();
    }

    private static List<DiffEntry> runDiff(Repository repository, RevCommit commit) throws IOException, GitAPIException {
        ObjectId head = commit.getTree().toObjectId();
        if (commit.getParentCount() == 0)
            return Collections.EMPTY_LIST;
        //TODO supposed that there is only one parent
        ObjectId oldHead = commit.getParent(0).getTree().toObjectId();

        showFileContent(repository, commit);

        System.out.println("Printing diff between " + oldHead + " and " + head);

        try (Git git = new Git(repository)) {
            try (ObjectReader reader = repository.newObjectReader()) {
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, head);
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, oldHead);
                // finally get the list of changed files
                return git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
            }
        }
    }

    private static void showFileContent(Repository repository, RevTree tree) throws IOException {
        System.out.println("******************************" + tree.getName() + "******************************");
        // now try to find a specific file
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            //treeWalk.setFilter(PathFilter.create("README.md"));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file 'README.md'");
            }
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            // and then one can the loader to read the file
            loader.copyTo(System.out);
        }
        System.out.println();
    }

    private static void showFileContent(Repository repository, RevCommit commit) throws IOException {
        System.out.println("############################" + commit.getId().getName() + "############################");
        if (commit.getParentCount() != 0) {
            showFileContent(repository, commit.getTree());
            showFileContent(repository, commit.getParent(0).getTree());
        }
        System.out.println("############################" + commit.getId().getName() + "############################");
    }

    public static Repository openRepository() throws IOException {
        return Git.open(new File("data")).getRepository();
    }
}