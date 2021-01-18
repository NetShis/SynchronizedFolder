import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class MyFile {
    private String name;
    private String pathString;
    private boolean isDirectory;
    private Path path;

    public MyFile(Path path) {
        this.path = path;
        name = path.getName(path.getNameCount() - 1).toString();
        isDirectory = Files.isDirectory(path);
        pathString = path.toString().replace('\\', '/');
        pathString = pathString.substring(1, pathString.length());
    }

    public String getPathString() {
        return pathString;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    static public ArrayList<MyFtpFile> choseFtpFileToDownload(ArrayList<MyFtpFile> myFtpFiles, ArrayList<MyFile> myFiles) {
        ArrayList<MyFtpFile> myFtpFilesToDownloads = new ArrayList<>();

        //Проходим по списку с файлами на ftp и если такого каталога нет на компьютере, то создаем его
        //Если это файл, то добавляем его в список для загрузки
        myFtpFiles.forEach(myFtpFile -> {
            if (myFtpFile.isDirectory()) {
                if (myFiles.stream().noneMatch(myFile -> myFile.getPathString().equals(myFtpFile.getPath()))) {
                    try {
                        Files.createDirectory(Paths.get("." + myFtpFile.getPath()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Main.logger.severe(e.getMessage());
                    }
                }
            } else {
                if (myFiles.stream().noneMatch(myFile -> myFile.getPathString().equals(myFtpFile.getPath())))
                    myFtpFilesToDownloads.add(myFtpFile);
            }
        });

        //Удаляем файлы и каталоги на компьютере если их нет на ftp сервере
        myFiles.forEach(myFile -> {
            String tmpPath;
            if (myFile.getPathString().endsWith("__Loading!__"))
                tmpPath = myFile.getPathString().replaceFirst("__Loading!__", "");
            else tmpPath = myFile.getPathString();

            if (myFtpFiles.stream().
                    noneMatch(myFtpFile -> myFtpFile.getPath().equals(tmpPath))) {
                try {
                    Files.delete(myFile.path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Main.logger.fine("Сформирован список файлов для загрузки с ftp");
        return myFtpFilesToDownloads;
    }


    static public String downloader(ArrayList<MyFtpFile> ftpFilesToDownloads, FTPClient ftpClient, String pathToSynchronizedFolder) throws IOException {
        String filesDownloads = "";
        InputStream inputStream = null;
        OutputStream outputStream = null;
        FileStore fileStore = Files.getFileStore(Paths.get("./" + pathToSynchronizedFolder));

        for (MyFtpFile myFtpFile : ftpFilesToDownloads) {
            try {
                if (fileStore.getUsableSpace() < myFtpFile.getSize()) {
                    Main.logger.severe("Недостаточно свободного места на диске!");
                    filesDownloads = "Недостаточно свободного места на диске!";
                    break;
                }
                outputStream = Files.newOutputStream(Paths.get("." + myFtpFile.getPath() + "__Loading!__"), CREATE, APPEND);
                inputStream = ftpClient.retrieveFileStream(myFtpFile.getPath());
                inputStream.skip(Files.size(Paths.get("." + myFtpFile.getPath() + "__Loading!__")));
                byte[] buffer = new byte[1024 * 1024 * 10];
                int c = inputStream.read(buffer);
                while (c != -1) {
                    outputStream.write(buffer, 0, c);
                    c = inputStream.read(buffer);
                }
                ftpClient.completePendingCommand();
                inputStream.close();
                outputStream.close();
                Files.move(Paths.get("." + myFtpFile.getPath() + "__Loading!__"), Paths.get("." + myFtpFile.getPath()));
                Main.logger.fine(myFtpFile.getPath() + " Загружен");
                filesDownloads = filesDownloads + myFtpFile.getPath() + "\n";

            } catch (IOException e) {
                e.printStackTrace();
                Main.logger.severe(e.getMessage());
            }
        }
        return filesDownloads;
    }
}
