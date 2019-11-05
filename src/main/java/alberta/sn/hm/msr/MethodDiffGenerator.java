package alberta.sn.hm.msr;

import alberta.sn.hm.msr.exception.FileException;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MethodDiffGenerator {


    public HashSet<String> execute(String oldFileNameWithPath, String newFileNameWithPath, CsvWriter csvWriter)
            throws FileException.NotExistInNewCommit, FileException.NotExistInOldCommit, FileException.CompilationError {
        String commit = newFileNameWithPath.split("/")[1];
        String path = newFileNameWithPath.substring(newFileNameWithPath.lastIndexOf(commit) + commit.length() + 1);// to the start of new folder
        path = path.substring(newFileNameWithPath.indexOf("/"));//skipping new folder name

        JavaFileDetails newFileDetails;
        try {
            newFileDetails = new JavaFileDetails(newFileNameWithPath);
        } catch (FileNotFoundException e) {
            throw new FileException.NotExistInNewCommit(path, commit);
        } catch (ParseProblemException e) {
            throw new FileException.CompilationError(path, commit);
        }
        JavaFileDetails oldFileDetails;
        try {
            oldFileDetails = new JavaFileDetails(oldFileNameWithPath);
        } catch (FileNotFoundException e) {
            throw new FileException.NotExistInOldCommit(path, commit);
        } catch (ParseProblemException e) {
            throw new FileException.CompilationError(path, commit);
        }

        // get diff of methods
        List<CallableDeclaration> newNotExistMethods = minus(newFileDetails.getCallables(), oldFileDetails.getCallables());
        List<CallableDeclaration> oldNotExistMethods = minus(oldFileDetails.getCallables(), newFileDetails.getCallables());

        boolean exist;
        for (CallableDeclaration newNotExistMethod : newNotExistMethods) {
            //get changed methods
            exist = false;
            for (CallableDeclaration oldNotExistMethod : oldNotExistMethods) {
                if (methodsAreEqualInName(newNotExistMethod, oldNotExistMethod)) {
                    exist = true;
                    csvWriter.write("CHANGE", commit, path, getMethodSignature(oldNotExistMethod), getMethodSignature(newNotExistMethod));
                }
            }
            //get added methods
            if (!exist) {
                if (newNotExistMethod instanceof MethodDeclaration)
                    csvWriter.write("ADD", commit, path, "null", getMethodSignature(newNotExistMethod));
            }
        }

        for (CallableDeclaration oldNotExistMethod : oldNotExistMethods) {
            exist = false;
            for (CallableDeclaration newNotExistMethod : newNotExistMethods) {
                if (methodsAreEqualInName(oldNotExistMethod, newNotExistMethod)) {
                    exist = true;
                }
            }
            //get deleted methods
            if (!exist) {
                csvWriter.write("REMOVE", commit, path, getMethodSignature(oldNotExistMethod), "null");
            }
        }
        return null;
    }

    private List<CallableDeclaration> minus(List<CallableDeclaration> a, List<CallableDeclaration> b) {
        List<CallableDeclaration> result = new ArrayList<>();
        boolean exist;
        String method1Signature;
        String method2Signature;
        for (CallableDeclaration method1 : a) {
            exist = false;
            method1Signature = getMethodSignature(method1);
            for (CallableDeclaration method2 : b) {
                // if all is equal
                method2Signature = getMethodSignature(method2);
                if (method1Signature.equals(method2Signature)) {
                    //if (methodsAreEqualInSignature(method1, method2)) {
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
        // if return type is equal
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

    private Boolean methodsAreEqualInName(CallableDeclaration method1, CallableDeclaration method2) {
        // check method names
        if (!method1.getNameAsString().equals(method2.getNameAsString())) {
            return false;
        }
        return true;
    }

    private String getMethodSignature(CallableDeclaration method) {
        if (method.toString().contains("void main"))
            System.out.println();
        String signature = "";
        if (!method.getModifiers().toString().equals(""))
            signature = signature + getModifiers(method) + " ";

        if (method instanceof MethodDeclaration)
            if (!((MethodDeclaration) method).getType().toString().equals(""))
                signature = signature + ((MethodDeclaration) method).getType().toString() + " ";

        signature = signature + method.getNameAsString() + "(";

        if (method.getParameters().size() > 0)
            signature = signature +getParameters(method);

        signature = signature + ")";
        return signature;
    }

    private String getModifiers(CallableDeclaration method) {
        return method.getModifiers().toString()
                .replace("[", "")
                .replace("]", "")
                .replace(",", "")
                .toLowerCase();
    }

    private String getParameters(CallableDeclaration method) {
        return method.getParameters().toString().trim().substring(1, method.getParameters().toString().trim().length() - 1);
    }
}