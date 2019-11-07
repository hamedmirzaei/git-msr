package alberta.sn.hm.msr.exception;

public class FileException {

    // old not exist
    public static class NotExistInOldCommitException extends BaseException {
        private String filePath;
        private String commitId;

        public NotExistInOldCommitException(String filePath, String commitId) {
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

    // new not exist
    public static class NotExistInNewCommitException extends BaseException {
        private String filePath;
        private String commitId;

        public NotExistInNewCommitException(String filePath, String commitId) {
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

    // compile error
    public static class CompileFailedException extends BaseException {
        private String filePath;
        private String commitId;

        public CompileFailedException(String filePath, String commitId) {
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

    // exception during generating diff file
    public static class DiffGenerationException extends BaseException {
        private String fileName;

        public DiffGenerationException(String fileName) {
            super("The .diff file " + fileName + " could not be written");
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    // exception during generating old/new file
    public static class OldNewGenerationException extends BaseException {
        private String filePath;

        public OldNewGenerationException(String filePath) {
            super("The old/new file " + filePath + " could not be written");
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }
    }

}
