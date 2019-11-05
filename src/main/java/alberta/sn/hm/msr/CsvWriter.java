package alberta.sn.hm.msr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvWriter {

    List<String> records = Collections.synchronizedList(new ArrayList());
    private FileWriter csvWriter;

    public CsvWriter() {
        try {
            this.csvWriter = new FileWriter(Constants.RESULT_FILE);
            this.csvWriter.write("Type,Commit SSH,File Name,From Signature, To Signature\n");
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
