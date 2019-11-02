package alberta.sn.hm.msr;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MethodDiff2 {


    public HashSet<String> methodDiffInClass(String oldFileNameWithPath, String newFileNameWithPath) {
        JavaFileDetails oldFileDetails = new JavaFileDetails(oldFileNameWithPath);
        JavaFileDetails newFileDetails = new JavaFileDetails(newFileNameWithPath);

        List<MethodDeclaration> newNotExistMethods = minus(newFileDetails.methods, oldFileDetails.methods);
        List<MethodDeclaration> oldNotExistMethods = minus(oldFileDetails.methods, newFileDetails.methods);


        for (MethodDeclaration newNotExistMethod : newNotExistMethods) {
            for (MethodDeclaration oldNotExistMethod : oldNotExistMethods) {
                // if name is equal
                if (methodsAreEqualInNameNotParams(newNotExistMethod, oldNotExistMethod)) {
                    System.out.println("From " + oldNotExistMethod.getSignature().asString() +
                            " To " + newNotExistMethod.getSignature().asString());
                }
            }
        }


        return null;
    }

    private List<MethodDeclaration> minus(List<MethodDeclaration> a, List<MethodDeclaration> b) {
        List<MethodDeclaration> result = new ArrayList<>();
        for (MethodDeclaration method1 : a) {
            boolean exist = false;
            for (MethodDeclaration method2 : b) {
                // if all is equal
                if (methodsAreEqualInSignature(method1, method2)) {
                    exist = true;
                    continue;
                }
            }
            if (!exist)
                result.add(method1);
        }
        return result;
    }

    private Boolean methodsAreEqualInSignature(MethodDeclaration method1, MethodDeclaration method2) {
        // is return type is equal
        if (!method1.getType().asString().equals(method2.getType().asString())) {
            return false;
        }
        // if name is equal
        if (!method1.getNameAsString().equals(method2.getNameAsString())) {
            return false;
        }
        // if parameters are equal
        if (method1.getParameters().size() == method2.getParameters().size()) {
            for (int i = 0; i < method1.getParameters().size(); i++) {
                Parameter parameter1 = method1.getParameter(i);
                Parameter parameter2 = method2.getParameter(i);
                if (!parameter1.getNameAsString().equals(parameter2.getNameAsString()) ||
                        !parameter1.getType().asString().equals(parameter2.getType().asString())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Boolean methodsAreEqualInNameNotParams(MethodDeclaration method1, MethodDeclaration method2) {
        // if name is equal
        if (!method1.getNameAsString().equals(method2.getNameAsString())) {
            return false;
        }
        // if parameters are equal
        boolean allEqual = true;
        if (method1.getParameters().size() == method2.getParameters().size()) {
            for (int i = 0; i < method1.getParameters().size(); i++) {
                Parameter parameter1 = method1.getParameter(i);
                Parameter parameter2 = method2.getParameter(i);
                if (!parameter1.getNameAsString().equals(parameter2.getNameAsString()) ||
                        !parameter1.getType().asString().equals(parameter2.getType().asString())) {
                    allEqual = false;
                }
            }
            if (allEqual)
                return false;
        }
        return true;
    }
}