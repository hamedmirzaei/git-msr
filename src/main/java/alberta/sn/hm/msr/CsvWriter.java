package alberta.sn.hm.msr;

import java.io.FileWriter;
import java.io.IOException;

public class CsvWriter {

    private FileWriter csvWriter;

    public CsvWriter() {
        try {
            this.csvWriter = new FileWriter("temp/result.csv");
            this.csvWriter.write("Type,Commit SSH,File Name,From Signature, To Signature\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String changeType, String commit, String fileName, String fromSignature, String toSignature) {
        try {
            this.csvWriter.write(changeType + "," + commit + "," + fileName + "," +
                    fromSignature.replaceAll(",", ";") + "," +
                    toSignature.replaceAll(",", ";") + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
