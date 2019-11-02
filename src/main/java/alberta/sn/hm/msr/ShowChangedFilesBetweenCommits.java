package alberta.sn.hm.msr;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * Snippet which shows how to show diffs between two commits.
 *
 * @author dominik.stadler at gmx.at
 */
public class ShowChangedFilesBetweenCommits {

    public static void main(String[] args) throws IOException, GitAPIException {

        try (Repository repository = openRepository()) {
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().all().call();
                // the first is the latest
                Iterator<RevCommit> iterator = commits.iterator();
                while (iterator.hasNext()) {
                    RevCommit commit = iterator.next();
                    if (iterator.hasNext()) {
                        ObjectId oldHead = commit.getTree().toObjectId();
                        ObjectId head = commit.getParent(0).getTree().toObjectId();
                        //ObjectId oldHead = repository.resolve(commit.getId().getName() + "^{tree}");
                        //ObjectId head = repository.resolve(commit.getId().getName() + "{tree}");
                        System.out.println("Printing diff between tree: " + oldHead + " and " + head);

                        // prepare the two iterators to compute the diff between
                        try (ObjectReader reader = repository.newObjectReader()) {
                            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                            oldTreeIter.reset(reader, oldHead);
                            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                            newTreeIter.reset(reader, head);

                            // finally get the list of changed files
                            List<DiffEntry> diffs = git.diff()
                                    .setNewTree(newTreeIter)
                                    .setOldTree(oldTreeIter)
                                    .call();
                            for (DiffEntry entry : diffs) {
                                System.out.println("Entry: " + entry);
                            }
                        }

                    }
                }
            }


        }

        System.out.println("Done");
    }

    public static Repository openRepository() throws IOException {
        return Git.open(new File("data")).getRepository();
    }
}