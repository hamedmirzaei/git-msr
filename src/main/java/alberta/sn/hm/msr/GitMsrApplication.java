package alberta.sn.hm.msr;

import alberta.sn.hm.msr.exception.FileException;
import alberta.sn.hm.msr.exception.GitException;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class GitMsrApplication {

    private static CsvWriter csvWriter = new CsvWriter();
    private static String DATA_FOLDER = "data";
    private static String TEMP_FOLDER = "temp";
    private static String NEW_FOLDER = "new";
    private static String OLD_FOLDER = "old";
    private static Boolean KEEP_TEMP_FILES = false;

    private static MethodDiffGenerator methodDiffGenerator = new MethodDiffGenerator();

    public static void main(String[] args) {
        System.out.println("Application started");
        recreateFolders();
        try {
            cloneGitRepository("https://github.com/hamedmirzaei/camunda-bpm-examples.git");
        } catch (GitAPIException e) {
            System.out.println("Exception raised during cloning repository");
        }
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
                    try {
                        processCommit(repository, commit);
                    } catch (GitException.ParentNotExistForCommitException | GitException.DiffOperationException ex) {
                        System.out.println(ex.getMessage());
                    }
                    if (!KEEP_TEMP_FILES) {
                        deleteFolder(TEMP_FOLDER + "/" + commit.getId().getName());
                        new File(TEMP_FOLDER + "/" + commit.getId().getName() + ".diff").delete();
                    }
                }
            } catch (GitAPIException e) {
                System.out.println("Exception raised during getting commits from repository");
            }
        } catch (IOException e) {
            System.out.println("Exception raised during opening repository");
        }
        csvWriter.close();
    }

    private static void processCommit(Repository repository, RevCommit commit) throws GitException.ParentNotExistForCommitException, GitException.DiffOperationException {
        String commitId = commit.getId().getName();
        System.out.println("\nProcessing commit " + commitId);
        List<DiffEntry> diffEntries;
        diffEntries = executeGitDiffCommand(repository, commit);
        if (diffEntries != null && !diffEntries.isEmpty()) {
            System.out.println("There are #" + diffEntries.size() + " diff entries");
            String diffFileName = "";
            diffFileName = TEMP_FOLDER + "/" + commitId + ".diff";
            try {
                new File(diffFileName).createNewFile();
            } catch (IOException e) {
                System.out.println("Exception raised during .diff file creation");
            }
            for (DiffEntry diffEntry : diffEntries) {
                System.out.println("Processing diff entry " + diffEntry.getNewPath());
                try {
                    processDiffEntry(repository, commit, diffEntry.getNewPath());
                } catch (FileException.NotExistInOldCommit | FileException.NotExistInNewCommit | FileException.CompilationError | FileException.OldNewGenerationException ex) {
                    System.out.println(ex.getMessage());
                }
                if (!diffFileName.equals("")) {
                    try {
                        appendDiffEntryToDiffFile(repository, diffEntry, diffFileName);
                    } catch (FileException.DiffGenerationException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        } else {
            System.out.println("There is no diff entry for commit " + commitId);
        }
    }

    private static void appendDiffEntryToDiffFile(Repository repository, DiffEntry diffEntry, String diffFileName) throws FileException.DiffGenerationException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(diffFileName, true);
            DiffFormatter formatter = new DiffFormatter(out);
            formatter.setRepository(repository);
            formatter.format(diffEntry);
            out.close();
        } catch (IOException e) {
            try {
                out.close();
            } catch (IOException e1) {
            }
            throw new FileException.DiffGenerationException(diffFileName);
        }
    }

    private static List<DiffEntry> executeGitDiffCommand(Repository repository, RevCommit commit)
            throws GitException.ParentNotExistForCommitException, GitException.DiffOperationException {
        System.out.println("Making diff of commit with its parent");
        ObjectId head = commit.getTree().toObjectId();
        if (commit.getParentCount() == 0) {
            throw new GitException.ParentNotExistForCommitException(commit.getId().getName());
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
            } catch (GitAPIException | IOException e) {
                throw new GitException.DiffOperationException();
            }
        }
    }

    private static void processDiffEntry(Repository repository, RevCommit commit, String path)
            throws FileException.NotExistInOldCommit, FileException.NotExistInNewCommit, FileException.CompilationError, FileException.OldNewGenerationException {
        String commitId = commit.getId().getName();
        System.out.println("Move content of diff entry for " + path + " to " + NEW_FOLDER + " folder");
        generateOldNewFile(repository, commit.getTree(), path, commitId, true);
        System.out.println("Move content of diff entry for " + path + " to " + OLD_FOLDER + " folder");
        generateOldNewFile(repository, commit.getParent(0).getTree(), path, commitId, false);
        System.out.println("Extracting result...");
        extractResults(commitId, path);
        System.out.println("Results Extracted");
    }

    private static void extractResults(String commitId, String path) throws FileException.NotExistInOldCommit, FileException.NotExistInNewCommit, FileException.CompilationError {
        methodDiffGenerator.execute(
                TEMP_FOLDER + "/" + commitId + "/" + OLD_FOLDER + "/" + path,
                TEMP_FOLDER + "/" + commitId + "/" + NEW_FOLDER + "/" + path,
                csvWriter);
    }

    private static void generateOldNewFile(Repository repository, RevTree tree, String fileFullPath, String commitId, Boolean newFile) throws FileException.OldNewGenerationException {
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
                String tempPlusCommit = TEMP_FOLDER + "/" + commitId;
                if (newFile) {
                    new File(tempPlusCommit + "/" + NEW_FOLDER + "/" + parents).mkdirs();
                    FileOutputStream out = new FileOutputStream(tempPlusCommit + "/" + NEW_FOLDER + "/" + parents + fileName);
                    loader.copyTo(out);
                    out.close();
                } else {
                    new File(tempPlusCommit + "/" + OLD_FOLDER + "/" + parents).mkdirs();
                    FileOutputStream out = new FileOutputStream(tempPlusCommit + "/" + OLD_FOLDER + "/" + parents + fileName);
                    loader.copyTo(out);
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new FileException.OldNewGenerationException(fileFullPath);
        }
    }

    public static Repository openGitRepository() throws IOException {
        System.out.println("Repository opened");
        return Git.open(new File(DATA_FOLDER)).getRepository();
    }

    private static void cloneGitRepository(String repositoryURL) throws GitAPIException {
        System.out.println("Repository cloning...");
        Git.cloneRepository()
                .setURI(repositoryURL)
                .setDirectory(new File(DATA_FOLDER))
                .setCloneAllBranches(false)
                .call();
        System.out.println("Repository cloned");
    }

    private static void recreateFolders() {
        try {
            deleteFolder(TEMP_FOLDER);
            new File(TEMP_FOLDER).mkdir();
            deleteFolder(DATA_FOLDER);
            new File(DATA_FOLDER).mkdir();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFolder(String path) throws IOException {
        System.out.println("DELETING " + path);
        if (new File(path).exists())
            Files.walk(Paths.get(path)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
}