package alberta.sn.hm.msr.exception;


public abstract class BaseException extends Exception {

    private String message;

    public BaseException(String msg) {
        this.message = msg;
    }

    public String getMessage() {
        return message;
    }
}