import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

public class Callml implements Callable<Void> {

    private final String folderOut;
    private final LinkItem linkItem;
    private final Callback callback;

    public Callml(String outputFolder, LinkItem linkItem, Callback callback) {
        this.folderOut = outputFolder;
        this.linkItem = linkItem;
        this.callback = callback;
    }

    public Void call() {

        byte[] fileData = Main.fetch(linkItem.url).blob();
        new File(folderOut).mkdir();

        long timeInSec = System.currentTimeMillis();
        try {
            FileOutputStream stream = new FileOutputStream(folderOut + "/"
                    + linkItem.nameFiles);
            stream.write(fileData);
            stream.close();
            int filesCount = Statistics.filesCount.incrementAndGet();
            int percent = 100 * filesCount / Statistics.totalFilesCount.get();
            int filesSize = Statistics.filesSize.addAndGet(fileData.length);
            int downloadTime = Statistics.downloadTime.addAndGet((int) (System.currentTimeMillis() - timeInSec));
            long speed = filesCount / (downloadTime == 0 ? 1 : downloadTime);
            callback.report(String.format("Завершено: %d%%\n"
                    + "Загружено: %d файлов, %d bytes\n"
                    + "Время: %d милисекунд\n"
                    + "Средняя скорость: %d файлов в милисекунду\n", percent, filesCount, filesSize, downloadTime, speed));

        } catch (IOException ex) {
        }
        return null;
    }
}

