import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import java.io.IOException;
import java.util.ArrayList;

public class MyFtpFile {
    private String path;
    private String name;
    private long size;
    private boolean isDirectory;

    public MyFtpFile(String path, boolean isDirectory, String name, long size) {
        this.path = path;
        this.isDirectory = isDirectory;
        this.name = name;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getSize() {
        return size;
    }

    public static void getFilesFtp(String path, FTPClient ftpClient, ArrayList<MyFtpFile> myFtpFiles) throws IOException {
        ArrayList<MyFtpFile> temp = new ArrayList<>();
        FTPFile[] ftpFile = ftpClient.listFiles(path);

        for (FTPFile tmp : ftpFile) {
            myFtpFiles.add(new MyFtpFile(path + "/" + tmp.getName(), tmp.isDirectory(), tmp.getName(), tmp.getSize()));
            temp.add(new MyFtpFile(path + "/" + tmp.getName(), tmp.isDirectory(), tmp.getName(), tmp.getSize()));
        }

        //Рекурсивно проходим по всем каталогам на ftp сервере и заполняем список myFtpFiles файлами и каталогами
        temp.forEach(myFtpFile -> {
            if (myFtpFile.isDirectory) {
                try {
                    getFilesFtp(myFtpFile.path, ftpClient, myFtpFiles);
                } catch (IOException e) {
                    e.printStackTrace();
                    Main.logger.severe(e.getMessage());
                }
            }
        });
        Main.logger.fine("Получен список файлов с ftp с каталога " + path);
    }
}
