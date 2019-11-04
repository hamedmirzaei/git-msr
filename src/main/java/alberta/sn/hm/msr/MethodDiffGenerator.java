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


    public HashSet<String> makeAndWriteDiffToFile(String oldFileNameWithPath, String newFileNameWithPath, CsvWriter csvWriter) throws FileException.NotExistInNewCommit, FileException.NotExistInOldCommit, FileException.CompilationError {
        String commit = newFileNameWithPath.split("/")[1];
        String path = newFileNameWithPath.substring(newFileNameWithPath.lastIndexOf(commit) + commit.length() + 1);// to the start of new folder
        path = path.substring(newFileNameWithPath.indexOf("/") + 1);//skipping new folder name

        JavaFileDetails newFileDetails = null;
        try {
            newFileDetails = new JavaFileDetails(newFileNameWithPath);
        } catch (FileNotFoundException e) {
            throw new FileException.NotExistInNewCommit(path, commit);
        } catch (ParseProblemException e) {
            throw new FileException.CompilationError(path, commit);
        }
        JavaFileDetails oldFileDetails = null;
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
                if (methodsAreEqualInNameNotParams(newNotExistMethod, oldNotExistMethod)) {
                    exist = true;
                    if (newNotExistMethod instanceof MethodDeclaration && oldNotExistMethod instanceof MethodDeclaration) {
                        String fromSignature = ((MethodDeclaration) oldNotExistMethod).getType().asString()
                                + " " + oldNotExistMethod.getSignature().asString();
                        String toSignature = ((MethodDeclaration) newNotExistMethod).getType().asString()
                                + " " + newNotExistMethod.getSignature().asString();
                        csvWriter.write(commit, path, fromSignature, toSignature);
                    } else {
                        csvWriter.write(commit, path, oldNotExistMethod.getSignature().asString(), newNotExistMethod.getSignature().asString());
                    }
                }
            }
            //get added methods
            if (!exist) {
                if (newNotExistMethod instanceof MethodDeclaration) {
                    String toSignature = ((MethodDeclaration) newNotExistMethod).getType().asString()
                            + " " + newNotExistMethod.getSignature().asString();
                    csvWriter.write(commit, path, "null", toSignature);
                } else {
                    csvWriter.write(commit, path, "null", newNotExistMethod.getSignature().asString());
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
            //get deleted methods
            if (!exist) {
                if (oldNotExistMethod instanceof MethodDeclaration) {
                    String fromSignature = ((MethodDeclaration) oldNotExistMethod).getType().asString()
                            + " " + oldNotExistMethod.getSignature().asString();
                    csvWriter.write(commit, path, fromSignature, "null");
                } else {
                    csvWriter.write(commit, path, oldNotExistMethod.getSignature().asString(), "null");
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