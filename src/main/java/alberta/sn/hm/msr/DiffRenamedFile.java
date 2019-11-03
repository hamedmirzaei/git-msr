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
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DiffRenamedFile {

    public static void main(String[] args) throws IOException, GitAPIException {
        System.out.println("Application started");
        recreateTempFolder();
        try (Repository repository = openRepository()) {
            try (Git git = new Git(repository)) {
                System.out.println("Get all commits");
                Iterable<RevCommit> commits = git.log().all().call();
                // the first is the latest
                // for each commit except the first one, run diff of current commit with its parent
                System.out.println("Iterate over commits");
                Iterator<RevCommit> iterator = commits.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    System.out.println("\nProcessing commit " + commit.getId().getName());
                    List<DiffEntry> diffEntries = runDiff(repository, commit);

                    if (!diffEntries.isEmpty()) {
                        System.out.println("There are #" + diffEntries.size() + " diff entries");
                        String diffFileName = "temp/" + commit.getId().getName() + ".diff";
                        new File(diffFileName).createNewFile();
                        for (DiffEntry diffEntry : diffEntries) {
                            System.out.println("Processing diff entry " + diffEntry.getNewPath());
                            moveDiffEntryToFile(repository, commit, diffEntry.getNewPath());
                            try (DiffFormatter formatter = new DiffFormatter(new FileOutputStream(diffFileName, true))) {
                                formatter.setRepository(repository);
                                formatter.format(diffEntry);
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<DiffEntry> runDiff(Repository repository, RevCommit commit) throws IOException, GitAPIException {
        System.out.println("Making diff of commit with its parent");
        ObjectId head = commit.getTree().toObjectId();
        if (commit.getParentCount() == 0) {
            System.out.println("This diff does not have a parent, so there is no diff");
            return Collections.EMPTY_LIST;
        }
        ObjectId oldHead = commit.getParent(0).getTree().toObjectId();
        System.out.println("The parent is " + commit.getParent(0).getId().getName());

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

    private static void moveDiffEntryToFile(Repository repository, RevCommit commit, String path) throws IOException {
        System.out.println("Move content of diff entry for " + path + " to new file");
        moveDiffEntryToFile(repository, commit.getTree(), path, commit.getId().getName(), true);
        System.out.println("Move content of diff entry for " + path + " to old file");
        moveDiffEntryToFile(repository, commit.getParent(0).getTree(), path, commit.getId().getName(), false);

        MethodDiff2 methodDiff2 = new MethodDiff2();
        methodDiff2.methodDiffInClass(
                "temp/" + commit.getId().getName() + "/old/" + path,
                "temp/" + commit.getId().getName() + "/new/" + path);
        /*GitHubDiffParser parser = new GitHubDiffParser();
                        InputStream in = new FileInputStream(diffFileName);
                        List<Diff> diff = parser.parse(in);
                        System.out.println();*/
    }

    private static void moveDiffEntryToFile(Repository repository, RevTree tree, String fileFullPath, String commitName, Boolean newFile) throws IOException {
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(fileFullPath));
            while (treeWalk.next()) {
                //System.out.println("found: " + treeWalk.getNameString());
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);

                // and then one can the loader to read the file
                int lastSlashIndex = fileFullPath.lastIndexOf('/') + 1;
                String fileName = fileFullPath.substring(lastSlashIndex);
                String parents = fileFullPath.substring(0, lastSlashIndex);
                String tempPlusCommit = "temp/" + commitName + "/";
                if (newFile) {
                    new File(tempPlusCommit + "new/" + parents).mkdirs();
                    loader.copyTo(new FileOutputStream(tempPlusCommit + "new/" + parents + fileName));
                } else {
                    new File(tempPlusCommit + "old/" + parents).mkdirs();
                    loader.copyTo(new FileOutputStream(tempPlusCommit + "old/" + parents + fileName));
                }
            }
        }
    }

    public static Repository openRepository() throws IOException {
        System.out.println("Repository opened");
        return Git.open(new File("data")).getRepository();
    }

    private static void recreateTempFolder() throws IOException {
        Files.walk(Paths.get("temp")).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        new File("temp").mkdir();
    }
}