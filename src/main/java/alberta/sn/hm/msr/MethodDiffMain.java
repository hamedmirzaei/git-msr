package alberta.sn.hm.msr;

import java.util.HashSet;

public class MethodDiffMain {
    public static void main(String[] args) {
        String oldFileNameWithPath = "src/main/java/alberta/sn/hm/msr/GitMsrApplication.java";
        String newFileNameWithPath = "data/src/main/java/GitMsrApplication.java";
        MethodDiff2 methodDiff2 = new MethodDiff2();
        HashSet<String> changedMethods = methodDiff2.methodDiffInClass(
                oldFileNameWithPath,
                newFileNameWithPath
        );
        System.out.println();
    }
}
