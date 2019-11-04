package alberta.sn.hm.msr;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class JavaFileDetails {

    private String fileName;
    private List<ClassPair> classes = new ArrayList<>();
    private List<ConstructorDeclaration> constructors = new ArrayList<>();
    private List<MethodDeclaration> methods = new ArrayList<>();
    private List<CallableDeclaration> callables = new ArrayList<>();

    public JavaFileDetails(String fileName) throws FileNotFoundException {
        this.fileName = fileName;
        this.classes = getClasses(fileName);
        for (ClassPair c : this.classes) {
            this.constructors.addAll(getChildNodesNotInClass(c.clazz, ConstructorDeclaration.class));
            this.methods.addAll(getChildNodesNotInClass(c.clazz, MethodDeclaration.class));
        }
        this.callables.addAll(this.methods);
        this.callables.addAll(this.constructors);
    }

    class ClassPair {
        final ClassOrInterfaceDeclaration clazz;
        final String name;

        ClassPair(ClassOrInterfaceDeclaration c, String n) {
            clazz = c;
            name = n;
        }
    }

    private <N extends Node> List<N> getChildNodesNotInClass(Node n, Class<N> clazz) {
        List<N> nodes = new ArrayList<>();
        for (Node child : n.getChildNodes()) {
            if (child instanceof ClassOrInterfaceDeclaration) {
                // Don't go into a nested class
                continue;
            }
            if (clazz.isInstance(child)) {
                nodes.add(clazz.cast(child));
            }
            nodes.addAll(getChildNodesNotInClass(child, clazz));
        }
        return nodes;
    }

    private List<ClassPair> getClasses(Node n, String parents, boolean inMethod) {
        List<ClassPair> pairList = new ArrayList<>();
        for (Node child : n.getChildNodes()) {
            if (child instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) child;
                String cName = parents + c.getNameAsString();
                if (inMethod) {
                    System.out.println(
                            "WARNING: Class " + cName + " is located inside a method. We cannot predict its name at"
                                    + " compile time so it will not be diffed."
                    );
                } else {
                    pairList.add(new ClassPair(c, cName));
                    pairList.addAll(getClasses(c, cName + "$", inMethod));
                }
            } else if (child instanceof MethodDeclaration || child instanceof ConstructorDeclaration) {
                pairList.addAll(getClasses(child, parents, true));
            } else {
                pairList.addAll(getClasses(child, parents, inMethod));
            }
        }
        return pairList;
    }

    private List<ClassPair> getClasses(String file) throws FileNotFoundException, ParseProblemException {
        CompilationUnit cu = JavaParser.parse(new File(file));
        return getClasses(cu, "", false);
    }

    public List<CallableDeclaration> getCallables() {
        return callables;
    }
}