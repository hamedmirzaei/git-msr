package alberta.sn.hm.msr.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyProperties {

    private Properties properties;
    private String basePath;
    private Integer threadPoolSize;
    private Boolean keepTemporaryFiles;
    private Boolean detectMethodAdd;
    private Boolean detectMethodRemove;
    private Boolean detectMethodChangeReturn;
    private Boolean detectMethodChangeModifier;
    private Boolean detectParameterAdd;
    private Boolean detectParameterRemove;
    private Boolean detectParameterChange;

    public MyProperties(String classPathFileName) {
        initializeAllFields();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(classPathFileName)) {
            properties = new Properties();
            if (input == null) {
                System.out.println("There is no file names " + classPathFileName + " in the class path");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            System.out.println("Some problem occurred during opening " + classPathFileName + " file in the class path");
        }
        try {
            prepareTheWorld();
        } catch (IOException e) {
            System.out.println("Some problem occurred during creating required application's paths");
        }
    }

    private void initializeAllFields() {
        this.basePath = null;
        this.threadPoolSize = null;
        this.keepTemporaryFiles = null;
        this.detectMethodAdd = null;
        this.detectMethodRemove = null;
        this.detectMethodChangeReturn = null;
        this.detectMethodChangeModifier = null;
        this.detectParameterAdd = null;
        this.detectParameterRemove = null;
        this.detectParameterChange = null;
    }

    private void prepareTheWorld() throws IOException {
        FileUtil.createPath(getBasePath());
        FileUtil.createPath(getRepositoryFolderPath());
        FileUtil.createPath(getOutputFolderPath());
    }

    private String getBasePath() {
        if (this.basePath != null)
            return this.basePath;
        String basePath = properties.getProperty("base.path");
        return basePath.endsWith("/") ? basePath : basePath + "/";
    }

    private String getRepositoryFolderName() {
        return properties.getProperty("repository.folder.name");
    }

    public String getRepositoryFolderPath() {
        return getBasePath() + getRepositoryFolderName();
    }

    public String getOutputFolderName() {
        return properties.getProperty("output.folder.name");
    }

    public String getOutputFolderPath() {
        return getBasePath() + getOutputFolderName();
    }

    private String getCSVFileName() {
        return properties.getProperty("csv.file.name");
    }

    public String getCSVFilePath() {
        return getOutputFolderPath() + "/" + getCSVFileName();
    }

    public String getCSVFileHeader() {
        return properties.getProperty("csv.file.header");
    }

    public Integer getThreadPoolSize() {
        if (this.threadPoolSize != null)
            return this.threadPoolSize;
        return Integer.parseInt(properties.getProperty("thread.pool.size"));
    }

    public Boolean getKeepTemporaryFiles() {
        if (this.keepTemporaryFiles != null)
            return this.keepTemporaryFiles;
        return Boolean.parseBoolean(properties.getProperty("keep.temporary.files"));
    }

    public Boolean getDetectMethodAdd() {
        if (this.detectMethodAdd != null)
            return this.detectMethodAdd;
        return Boolean.parseBoolean(properties.getProperty("detect.method.add"));
    }

    public Boolean getDetectMethodRemove() {
        if (this.detectMethodRemove != null)
            return this.detectMethodRemove;
        return Boolean.parseBoolean(properties.getProperty("detect.method.remove"));
    }

    public Boolean getDetectMethodChangeReturn() {
        if (this.detectMethodChangeReturn != null)
            return this.detectMethodChangeReturn;
        return Boolean.parseBoolean(properties.getProperty("detect.method.change.return"));
    }

    public Boolean getDetectMethodChangeModifier() {
        if (this.detectMethodChangeModifier != null)
            return this.detectMethodChangeModifier;
        return Boolean.parseBoolean(properties.getProperty("detect.method.change.modifier"));
    }

    public Boolean getDetectParameterAdd() {
        if (this.detectParameterAdd != null)
            return this.detectParameterAdd;
        return Boolean.parseBoolean(properties.getProperty("detect.parameter.add"));
    }

    public Boolean getDetectParameterRemove() {
        if (this.detectParameterRemove != null)
            return this.detectParameterRemove;
        return Boolean.parseBoolean(properties.getProperty("detect.parameter.remove"));
    }

    public Boolean getDetectParameterChange() {
        if (this.detectParameterChange != null)
            return this.detectParameterChange;
        return Boolean.parseBoolean(properties.getProperty("detect.parameter.change"));
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void setKeepTemporaryFiles(Boolean keepTemporaryFiles) {
        this.keepTemporaryFiles = keepTemporaryFiles;
    }

    public void setDetectMethodAdd(Boolean detectMethodAdd) {
        this.detectMethodAdd = detectMethodAdd;
    }

    public void setDetectMethodRemove(Boolean detectMethodRemove) {
        this.detectMethodRemove = detectMethodRemove;
    }

    public void setDetectMethodChangeReturn(Boolean detectMethodChangeReturn) {
        this.detectMethodChangeReturn = detectMethodChangeReturn;
    }

    public void setDetectMethodChangeModifier(Boolean detectMethodChangeModifier) {
        this.detectMethodChangeModifier = detectMethodChangeModifier;
    }

    public void setDetectParameterAdd(Boolean detectParameterAdd) {
        this.detectParameterAdd = detectParameterAdd;
    }

    public void setDetectParameterRemove(Boolean detectParameterRemove) {
        this.detectParameterRemove = detectParameterRemove;
    }

    public void setDetectParameterChange(Boolean detectParameterChange) {
        this.detectParameterChange = detectParameterChange;
    }
}
