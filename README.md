# Mining Git Software Repository

This application has been implemented as a programming task for working under supervision of `Prof. Nadi` 
at `University of Alberta`. The purpose of the application is to detect methods which some parameters has 
been added to them over time through commits.
To make the application more useful, we provided the option to detect all the following changes:

* Detect added methods
* Detect removed methods
* Detect changes in return type of methods
* Detect changes in modifiers of methods
* Detect added parameters to methods
* Detect removed parameters to methods
* Detect changes in parameters (name or type)


## Requirements
* maven
* java 1.8+

## Assumptions (important)

* all methods, including added, removed and changed are detected
    - add means exist in new commit but not old one
    - remove means exist in old commit and removed in the new one
    - change means exist in both new and old commits but changed in return type, modifiers, or parameters

* because of method overloading, we may have multiple changes for a single method.
```
old file:
    void method1(int x)
new file:
    void method1(int x, int y)
    void method1(int x, string s)
result contains two records:
    void method1(int x)     changed    to    void method1(int x, int y)
    void method1(int x)     changed    to    void method1(int x, string s)
```

* in java each class can have multiple constructors. we treat them just like methods


* since columns in csv files are separated by comma "," and because in java, method parameters are separated by comma
    too, we replace java parameter separator with semi colon ";"


* since in git all files are treated as plain text and because in java, methods can be broken into multiple lines,
    in order to have complete signature of methods, we build changed java files for each commit (notice that just the
    CHANGED files with `JAVA` extension will be built - for example txt files will be excluded)


* since we build each java file and parse it, the files with compilation error like forgetting to put "class" identifier
    will be excluded from results


* Some files may be deleted or added in a commit, since the file does not exist anymore in either old or new commit,
    they will be excluded from the results


* the first commit will not be included in result since there is nothing to compare it with


* in the process of developing a project, packages can be renamed. so, you may find a record in result which its file
    does not exist in the current working packages


* Since java has primitive data types like "int" in addition to corresponding class types like "Integer", we consider
    these as two different data types and a method change will be detected because of changing parameter from int to Integer


* one of java features is to create classes inside other classes (inner classes). in this version, we excluded changes
	to the methods of inner classes


* each git repository can have multiple branches with different chains of commits. in this version we just inspect changes
    in the current main branch (HEAD or master)

## How to compile and run

### Compile
After cloning the project from GitHub, simply go to the project root and run this:
```
mvn clean package
```
This will package all the project files and dependencies in a single executable jar file names `Git-Msr.jar` in
the `target` folder of project root directory. For the sake of simplicity, a copy of the jar is placed at `results` 
folder of projects root (in case you don't have `maven`, you can use it).

### Usage
Open a cmd prompt (or terminal) in the place of `Git-Msr.jar` file. Run the jar file using provided options explained below:
```
java -jar Git-Msr.jar git-path [optional]

git-path: it is either a remote git url like https://github.com/hamedmirzaei/test-msr2.git or a local folder 
containing a git repository which is cloned before like C:/path-to-repository/

[optional]: arguments that you can use to customize the application's execution:
-basePath: the path to folder you want to keep results and temporary files of the application. The default 
           value is C:/Git-Msr/
           
-threadPoolSize: the number of threads that the application uses to process the git repository. The default 
           value is 50.
                 
-keepTempFiles: there are some temporary files that application creates in the middle of execution like 
            commits and changed files in each commit. The default value is false.

-output: this option is used to tell the application about which results are of interest? this is a string of 
            length seven which each char is representing a specific result and is either 1 or 0. 
            1 means the corresponding result is of interest and 0 otherwise. 
            The first char is for detecting added methods.
            The second char is for detecting removed methods. 
            The third char is for detecting methods which their return type changed. 
            The fourth char is for detecting methods which their modifiers changed. 
            The fifth char is for detecting methods which parameters added to them. 
            The sixth char is for detecting methods which parameters removed from them. 
            The seventh char is for detecting methods which their parameters either changed in name or type. 
            So for example a value of 1000000 means that only search for added methods, a value of 0000100 
            means that only search for methods which some parameters added to them and a value of 1111111 
            means that all changes are of interests.
```
### Example 1: The default execution
The following will clone a git repository at `https://github.com/hamedmirzaei/test-msr2.git`\
It will be processed with 50 concurrent thread since it is the default value for `-threadPoolSize`. \
The results will be stored at path `C:/Git-Msr/` since it is the default value for `-basePath`.\
All types of changes will be detected since the default value for `-output` is `1111111`.\
Temporary files will be deleted since the default value for `-keepTempFiles` is `false`.
``` 
java -jar Git-Msr.jar https://github.com/hamedmirzaei/test-msr2.git
```
### Example 2
The following will clone a git repository at `https://github.com/hamedmirzaei/test-msr2.git`\
It will be processed with 40 concurrent thread. \
The results will be stored at path `C:/Git-Msr/` since it is the default value for `-basePath`.\
All types of changes will be detected since the default value for `-output` is `1111111`.\
Temporary files will be deleted since the default value for `-keepTempFiles` is `false`.
``` 
java -jar Git-Msr.jar https://github.com/hamedmirzaei/test-msr2.git -threadPoolSize 40
```
### Example 3
The following will work with directory `C:/Git-Msr/repository/` which contains your cloned git repository\
It will be processed with 50 concurrent thread since it is the default value for `-threadPoolSize`. \
The results will be stored at path `C:/Git-Msr2/`.\
It will detect added methods, return type changes and added parameters since the value for `-output` is `1010100`.\
Temporary files will be stored since the value for `-keepTempFiles` is `true`.
```
java -jar Git-Msr.jar C:/Git-Msr/repository/ -basePath C:/Git-Msr2/ -output 1010100 -keepTempFiles true
```
## Result
The result of the application will be `CSV` file containing following columns:
* Type: which may be one or multiple of these separated by semicolon
    - METHOD_ADD
    - METHOD_REMOVE
    - METHOD_CHANGE_RETURN
    - METHOD_CHANGE_MODIFIER
    - PARAMETER_ADD
    - PARAMETER_REMOVE
    - PARAMETER_CHANGE
* Commit SHA
* Java File
* Old Function Signature
* New Function Signature

## Case studies
The application runs on following three popular java projects and the results are at `results` folder of
project's root directory.
* [`Spring Batch`](https://github.com/spring-projects/spring-batch)
* [`Spring Integration`](https://github.com/spring-projects/spring-integration)
* [`Spring State Machinen`](https://github.com/spring-projects/spring-statemachine)