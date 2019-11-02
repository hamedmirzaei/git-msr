package alberta.sn.hm.msr;

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
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
                            showFileContent(repository, commit, diffEntry.getNewPath());
                            try (DiffFormatter formatter = new DiffFormatter(new FileOutputStream(diffFileName, true))) {
                                formatter.setRepository(repository);
                                formatter.format(diffEntry);
                            }
                        }
                        /*GitHubDiffParser parser = new GitHubDiffParser();
                        InputStream in = new FileInputStream(diffFileName);
                        List<Diff> diff = parser.parse(in);
                        System.out.println();*/
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
        ObjectId oldHead = commit.getParent(0).getTree().toObjectId();

        System.out.println();
        System.out.println("Printing diff between old: " + commit.getParent(0).getId().getName() + " and new: " + commit.getId().getName());

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

    private static void showFileContent(Repository repository, RevTree tree, String fileFullPath, Boolean newFile) throws IOException {
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(fileFullPath));
            while (treeWalk.next()) {
                System.out.println("found: " + treeWalk.getNameString());
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                // and then one can the loader to read the file
                String fileName = fileFullPath.substring(fileFullPath.lastIndexOf('/') + 1);
                if (newFile)
                    loader.copyTo(new FileOutputStream("temp/new_" + fileName));
                else
                    loader.copyTo(new FileOutputStream("temp/old_" + fileName));
            }
        }
    }

    private static void showFileContent(Repository repository, RevCommit commit, String path) throws IOException {
        System.out.println("############################" + commit.getId().getName() + "############################");
        showFileContent(repository, commit.getTree(), path, true);
        System.out.println("#####" + commit.getId().getName() + "#####");
        showFileContent(repository, commit.getParent(0).getTree(), path, false);
        System.out.println("############################" + commit.getId().getName() + "############################");
    }

    public static Repository openRepository() throws IOException {
        return Git.open(new File("data")).getRepository();
    }
}