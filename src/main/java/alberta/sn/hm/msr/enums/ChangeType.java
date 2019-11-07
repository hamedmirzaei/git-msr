package alberta.sn.hm.msr.enums;

public enum ChangeType {

    METHOD_ADD("METHOD_ADD"),
    METHOD_REMOVE("METHOD_REMOVE"),
    METHOD_CHANGE_RETURN("METHOD_CHANGE_RETURN"),
    METHOD_CHANGE_MODIFIER("METHOD_CHANGE_MODIFIER"),
    PARAMETER_ADD("PARAMETER_ADD"),
    PARAMETER_REMOVE("PARAMETER_REMOVE"),
    PARAMETER_CHANGE("PARAMETER_CHANGE");

    private String changeName;

    ChangeType(String changeName) {
        this.changeName = changeName;
    }

}
