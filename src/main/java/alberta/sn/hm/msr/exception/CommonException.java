package alberta.sn.hm.msr.exception;

public class CommonException {

    // input args validation exception
    public static class InputArgsValidationException extends BaseException {
        public InputArgsValidationException(String message) {
            super(message);
        }
    }

}
