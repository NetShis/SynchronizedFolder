import javafx.application.Application;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Main {

    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        FileHandler fileHandler = null;

        //Параметры для подключения к ftp серверу
        String ftpsServer = "";
        int ftpsPort = 21;
        String ftpsUser = "";
        String ftpsPass = "";

        //Название папки на компьютере, которая будет синхронизироваться с папкой на ftp
        //Папка на ftp названа так же
        String pathToSynchronizedFolder = "SynchronizedFolder";
        Path synchronizedFolder = Paths.get("./" + pathToSynchronizedFolder);

        //Список файлов на ftp и на компьютере
        ArrayList<MyFile> myFiles = new ArrayList<>();
        ArrayList<MyFtpFile> myFtpFiles = new ArrayList<>();

        try {
            fileHandler = new FileHandler("log.log");
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Не удалось создать файл лога " + e);
        }
        logger.fine("Приложение запущено");

        if (!Files.exists(synchronizedFolder)) {
            try {
                Files.createDirectory(synchronizedFolder);
            } catch (IOException e) {
                e.printStackTrace();
                logger.severe(e.getMessage());
            }
        }

        //Создаем список из всех файлов и каталогов в синхронизируемой папке
        try {
            Files.walk(synchronizedFolder).sorted(Comparator.reverseOrder()).forEach(path -> {
                if (!path.toString().equals(".\\" + pathToSynchronizedFolder)) myFiles.add(new MyFile(path));

            });
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }

        try {
            FTPClient ftpClient = new FTPClient();
            ftpClient.setAutodetectUTF8(true);
            ftpClient.connect(ftpsServer, ftpsPort);
            // check FTP connection
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                System.err.println("FTP server refused connection.");
                logger.severe("FTP server refused connection.");
                System.exit(1);
            } else {
                //System.out.println("Connected to " + ftpsServer + ":" + ftpsPort + ".");
                logger.info("Connected to " + ftpsServer + ":" + ftpsPort + ".");
            }
            // check FTP login
            if (ftpClient.login(ftpsUser, ftpsPass)) {
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                //System.out.println("Logged into FTP server successfully");
                logger.info("Logged into FTP server successfully");
            } else {
                System.out.println("Failed log into FTP server");
                logger.info("Failed log into FTP server");
                ftpClient.logout();
                ftpClient.disconnect();
            }

            //Получаем список файлов на ftp сервере
            MyFtpFile.getFilesFtp("/" + pathToSynchronizedFolder, ftpClient, myFtpFiles);

            //Формируем список для загрузки с ftp
            ArrayList<MyFtpFile> ftpFilesToDownloads = MyFile.choseFtpFileToDownload(myFtpFiles, myFiles);

            //Загружаем файлы с ftp из сформированного списка загрузки
            String filesDownloads = MyFile.downloader(ftpFilesToDownloads, ftpClient, pathToSynchronizedFolder);

            ftpClient.logout();
            ftpClient.disconnect();
            logger.fine("Приложение завершено");

            //Выводим окошко со списком загруженных файлов
            if (!filesDownloads.equals("")) Application.launch(SuccessfulUploadMessage.class, filesDownloads);

        } catch (Exception e) {
            System.out.println("Exception when merging Reminderlist is: " + e);
            logger.severe(e.getMessage());
        }
    }
}
