package alberta.sn.hm.msr.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvWriter {

    private List<String> records = Collections.synchronizedList(new ArrayList());
    private FileWriter csvWriter;

    public CsvWriter() {
        try {
            this.csvWriter = new FileWriter(Constants.properties.getCSVFilePath());
            this.csvWriter.write(Constants.properties.getCSVFileHeader() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String changeType, String commit, String fileName, String fromSignature, String toSignature) {
        this.records.add(changeType + "," + commit + "," + fileName + "," +
                fromSignature.replaceAll(",", ";") + "," +
                toSignature.replaceAll(",", ";"));
    }

    public void close() {
        try {
            for (String record : records) {
                this.csvWriter.write(record + "\n");
            }
            this.csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
