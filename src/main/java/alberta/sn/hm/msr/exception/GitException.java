package alberta.sn.hm.msr.exception;

public class GitException {

    // parent not exist
    public static class ParentNotExistForCommitException extends BaseException {
        private String commitId;

        public ParentNotExistForCommitException(String commitId) {
            super("The commit " + commitId + " does not have a parent");
            this.commitId = commitId;
        }

        public String getCommitId() {
            return commitId;
        }
    }

    // parent not exist
    public static class DiffOperationException extends BaseException {
        public DiffOperationException() {
            super("Diff operation failed to be executed");
        }
    }

}
