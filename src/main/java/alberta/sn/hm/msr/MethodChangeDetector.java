package alberta.sn.hm.msr;

import alberta.sn.hm.msr.exception.FileException;
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
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

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

public class MethodChangeDetector {

    private static CsvWriter csvWriter = new CsvWriter();
    private static String TEMP_FOLDER = "temp";
    private static String NEW_FOLDER = "new";
    private static String OLD_FOLDER = "old";

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
                    processCommit(repository, commit);
                }
            }
        }
        csvWriter.close();
    }

    private static void processCommit(Repository repository, RevCommit commit) throws IOException, GitAPIException {
        String commitId = commit.getId().getName();
        System.out.println("\nProcessing commit " + commitId);
        List<DiffEntry> diffEntries = executeGitDiffCommand(repository, commit);
        if (!diffEntries.isEmpty()) {
            System.out.println("There are #" + diffEntries.size() + " diff entries");
            String diffFileName = TEMP_FOLDER + "/" + commitId + ".diff";
            new File(diffFileName).createNewFile();
            for (DiffEntry diffEntry : diffEntries) {
                System.out.println("Processing diff entry " + diffEntry.getNewPath());
                generateOldNewFileForDiffEntry(repository, commit, diffEntry.getNewPath());
                appendDiffEntryToDiffFile(repository, diffEntry, diffFileName);
            }
        } else {
            System.out.println("There is no diff entry for commit " + commitId);
        }
    }

    private static void appendDiffEntryToDiffFile(Repository repository, DiffEntry diffEntry, String diffFileName) throws IOException {
        try (DiffFormatter formatter = new DiffFormatter(new FileOutputStream(diffFileName, true))) {
            formatter.setRepository(repository);
            formatter.format(diffEntry);
        }
    }

    private static List<DiffEntry> executeGitDiffCommand(Repository repository, RevCommit commit) throws IOException, GitAPIException {
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
                TreeFilter filter = PathSuffixFilter.create(".java");
                return git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).setPathFilter(filter).call();
            }
        }
    }

    private static void generateOldNewFileForDiffEntry(Repository repository, RevCommit commit, String path) throws IOException {
        System.out.println("Move content of diff entry for " + path + " to " + NEW_FOLDER + " folder");
        String commitId = commit.getId().getName();
        generateOldNewFileForDiffEntry(repository, commit.getTree(), path, commitId, true);
        System.out.println("Move content of diff entry for " + path + " to " + OLD_FOLDER + " folder");
        generateOldNewFileForDiffEntry(repository, commit.getParent(0).getTree(), path, commitId, false);

        MethodDiffGenerator methodDiffGenerator = new MethodDiffGenerator();
        try {
            methodDiffGenerator.makeAndWriteDiffToFile(
                    TEMP_FOLDER + "/" + commitId + "/" + OLD_FOLDER + "/" + path,
                    TEMP_FOLDER + "/" + commitId + "/" + NEW_FOLDER + "/" + path,
                    csvWriter);
        } catch (FileException.NotExistInOldCommit | FileException.NotExistInNewCommit | FileException.CompilationError e) {
            System.out.println(e.getMessage());
        }
    }

    private static void generateOldNewFileForDiffEntry(Repository repository, RevTree tree, String fileFullPath, String commitName, Boolean newFile) throws IOException {
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
                String tempPlusCommit = TEMP_FOLDER + "/" + commitName;
                if (newFile) {
                    new File(tempPlusCommit + "/" + NEW_FOLDER + "/" + parents).mkdirs();
                    loader.copyTo(new FileOutputStream(tempPlusCommit + "/" + NEW_FOLDER + "/" + parents + fileName));
                } else {
                    new File(tempPlusCommit + "/" + OLD_FOLDER + "/" + parents).mkdirs();
                    loader.copyTo(new FileOutputStream(tempPlusCommit + "/" + OLD_FOLDER + "/" + parents + fileName));
                }
            }
        }
    }

    public static Repository openRepository() throws IOException {
        System.out.println("Repository opened");
        return Git.open(new File("data")).getRepository();
    }

    private static void recreateTempFolder() throws IOException {
        Files.walk(Paths.get(TEMP_FOLDER)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        new File(TEMP_FOLDER).mkdir();
    }
}