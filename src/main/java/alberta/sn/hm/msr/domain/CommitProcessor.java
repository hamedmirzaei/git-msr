package alberta.sn.hm.msr.domain;

import alberta.sn.hm.msr.exception.FileException;
import alberta.sn.hm.msr.exception.GitException;
import alberta.sn.hm.msr.util.Constants;
import alberta.sn.hm.msr.util.FileUtil;
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
import java.util.List;

public class CommitProcessor implements Runnable {

    private Repository repository;
    private RevCommit commit;

    public CommitProcessor(Repository repository, RevCommit commit) {
        this.repository = repository;
        this.commit = commit;
    }

    @Override
    public void run() {
        try {
            processCommit(repository, commit);
        } catch (GitException.ParentNotExistForCommitException | GitException.DiffOperationException ex) {
            System.out.println(ex.getMessage());
        }

        if (!Constants.properties.getKeepTemporaryFiles()) {
            try {
                FileUtil.deletePath(Constants.properties.getOutputFolderPath() + "/" + commit.getId().getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            new File(Constants.properties.getOutputFolderPath() + "/" + commit.getId().getName() + ".diff").delete();
        }
    }


    private void processCommit(Repository repository, RevCommit commit) throws GitException.ParentNotExistForCommitException, GitException.DiffOperationException {
        String commitId = commit.getId().getName();
        System.out.println("\nProcessing commit " + commitId);
        List<DiffEntry> diffEntries;
        diffEntries = executeGitDiffCommand(repository, commit);
        if (diffEntries != null && !diffEntries.isEmpty()) {
            System.out.println("There are #" + diffEntries.size() + " diff entries");
            String diffFileName = "";
            if (Constants.properties.getKeepTemporaryFiles()) {
                diffFileName = Constants.properties.getOutputFolderPath() + "/" + commitId + ".diff";
                try {
                    new File(diffFileName).createNewFile();
                } catch (IOException e) {
                    System.out.println("Exception raised during .diff file creation");
                }
            }
            for (DiffEntry diffEntry : diffEntries) {
                System.out.println("Processing diff entry " + diffEntry.getNewPath());
                try {
                    processDiffEntry(repository, commit, diffEntry.getNewPath());
                } catch (FileException.NotExistInOldCommitException | FileException.NotExistInNewCommitException | FileException.CompileFailedException | FileException.OldNewGenerationException ex) {
                    System.out.println(ex.getMessage());
                }
                if (Constants.properties.getKeepTemporaryFiles() && !diffFileName.equals("")) {
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

    private void appendDiffEntryToDiffFile(Repository repository, DiffEntry diffEntry, String diffFileName) throws FileException.DiffGenerationException {
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

    private List<DiffEntry> executeGitDiffCommand(Repository repository, RevCommit commit)
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

    private void processDiffEntry(Repository repository, RevCommit commit, String path)
            throws FileException.NotExistInOldCommitException, FileException.NotExistInNewCommitException, FileException.CompileFailedException, FileException.OldNewGenerationException {
        String commitId = commit.getId().getName();
        System.out.println("Move content of diff entry for " + path + " to " + Constants.NEW_FOLDER_NAME + " folder");
        generateOldNewFile(repository, commit.getTree(), path, commitId, true);
        System.out.println("Move content of diff entry for " + path + " to " + Constants.OLD_FOLDER_NAME + " folder");
        generateOldNewFile(repository, commit.getParent(0).getTree(), path, commitId, false);
        System.out.println("Extracting result...");
        extractResults(commitId, path);
        System.out.println("Results Extracted");
    }

    private void extractResults(String commitId, String path) throws FileException.NotExistInOldCommitException, FileException.NotExistInNewCommitException, FileException.CompileFailedException {
        Constants.methodDiffGenerator.execute(
                Constants.properties.getOutputFolderPath() + "/" + commitId + "/" + Constants.OLD_FOLDER_NAME + "/" + path,
                Constants.properties.getOutputFolderPath() + "/" + commitId + "/" + Constants.NEW_FOLDER_NAME + "/" + path,
                Constants.csvWriter);
    }

    private void generateOldNewFile(Repository repository, RevTree tree, String fileFullPath, String commitId, Boolean newFile) throws FileException.OldNewGenerationException {
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
                String outputPlusCommit = Constants.properties.getOutputFolderPath() + "/" + commitId;
                if (newFile) {
                    new File(outputPlusCommit + "/" + Constants.NEW_FOLDER_NAME + "/" + parents).mkdirs();
                    FileOutputStream out = new FileOutputStream(outputPlusCommit + "/" + Constants.NEW_FOLDER_NAME + "/" + parents + fileName);
                    loader.copyTo(out);
                    out.close();
                } else {
                    new File(outputPlusCommit + "/" + Constants.OLD_FOLDER_NAME + "/" + parents).mkdirs();
                    FileOutputStream out = new FileOutputStream(outputPlusCommit + "/" + Constants.OLD_FOLDER_NAME + "/" + parents + fileName);
                    loader.copyTo(out);
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new FileException.OldNewGenerationException(fileFullPath);
        }
    }

}
