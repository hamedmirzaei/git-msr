package alberta.sn.hm.msr.exception;

public class FileException {

    // old not found
    public static class NotExistInOldCommit extends BaseException {
        private String filePath;
        private String commitId;

        public NotExistInOldCommit(String filePath, String commitId) {
            super("The file " + filePath + " is added and does not have an old version in commit " + commitId);
            this.filePath = filePath;
            this.commitId = commitId;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getCommitId() {
            return commitId;
        }
    }

    public static class NotExistInNewCommit extends BaseException {
        private String filePath;
        private String commitId;

        public NotExistInNewCommit(String filePath, String commitId) {
            super("The file " + filePath + " is deleted and does not have a new version in commit " + commitId);
            this.filePath = filePath;
            this.commitId = commitId;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getCommitId() {
            return commitId;
        }
    }

    public static class CompilationError extends BaseException {
        private String filePath;
        private String commitId;

        public CompilationError(String filePath, String commitId) {
            super("The file " + filePath + " of commit " + commitId + " has some compilation error");
            this.filePath = filePath;
            this.commitId = commitId;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getCommitId() {
            return commitId;
        }
    }

}
