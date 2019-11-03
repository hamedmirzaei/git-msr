package alberta.sn.hm.msr;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MethodDiff2 {


    public HashSet<String> methodDiffInClass(String oldFileNameWithPath, String newFileNameWithPath) {
        JavaFileDetails oldFileDetails = new JavaFileDetails(oldFileNameWithPath);
        JavaFileDetails newFileDetails = new JavaFileDetails(newFileNameWithPath);

        List<CallableDeclaration> newNotExistMethods = minus(newFileDetails.getCallables(), oldFileDetails.getCallables());
        List<CallableDeclaration> oldNotExistMethods = minus(oldFileDetails.getCallables(), newFileDetails.getCallables());


        boolean exist;
        for (CallableDeclaration newNotExistMethod : newNotExistMethods) {
            exist = false;
            for (CallableDeclaration oldNotExistMethod : oldNotExistMethods) {
                // if name is equal
                if (methodsAreEqualInNameNotParams(newNotExistMethod, oldNotExistMethod)) {
                    exist = true;
                    if (newNotExistMethod instanceof MethodDeclaration && oldNotExistMethod instanceof MethodDeclaration) {
                        System.out.println("CHANGED: From " + ((MethodDeclaration) oldNotExistMethod).getType().asString()
                                + " " + oldNotExistMethod.getSignature().asString() +
                                " To " + ((MethodDeclaration) newNotExistMethod).getType().asString()
                                + " " + newNotExistMethod.getSignature().asString());
                    } else {
                        System.out.println("CHANGED: From " + oldNotExistMethod.getSignature().asString() +
                                " To " + newNotExistMethod.getSignature().asString());
                    }
                }
            }
            if (!exist) {
                if (newNotExistMethod instanceof MethodDeclaration) {
                    System.out.println("ADDED: " + ((MethodDeclaration) newNotExistMethod).getType().asString()
                            + " " + newNotExistMethod.getSignature().asString());
                } else {
                    System.out.println("ADDED: " + newNotExistMethod.getSignature().asString());
                }
            }
        }

        for (CallableDeclaration oldNotExistMethod : oldNotExistMethods) {
            exist = false;
            for (CallableDeclaration newNotExistMethod : newNotExistMethods) {
                if (methodsAreEqualInNameNotParams(oldNotExistMethod, newNotExistMethod)) {
                    exist = true;
                }
            }
            if (!exist) {
                if (oldNotExistMethod instanceof MethodDeclaration) {
                    System.out.println("REMOVED: " + ((MethodDeclaration) oldNotExistMethod).getType().asString()
                            + " " + oldNotExistMethod.getSignature().asString());
                } else {
                    System.out.println("ADDED: " + oldNotExistMethod.getSignature().asString());
                }
            }
        }

        return null;
    }

    private List<CallableDeclaration> minus(List<CallableDeclaration> a, List<CallableDeclaration> b) {
        List<CallableDeclaration> result = new ArrayList<>();
        boolean exist;
        for (CallableDeclaration method1 : a) {
            exist = false;
            for (CallableDeclaration method2 : b) {
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

    private Boolean methodsAreEqualInSignature(CallableDeclaration method1, CallableDeclaration method2) {
        // is return type is equal
        if (method1 instanceof MethodDeclaration && method2 instanceof MethodDeclaration)
            if (!((MethodDeclaration) method1).getType().asString().equals(((MethodDeclaration) method2).getType().asString())) {
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

    private Boolean methodsAreEqualInNameNotParams(CallableDeclaration method1, CallableDeclaration method2) {
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
        } else {
            return true;
        }
        return true;
    }
}