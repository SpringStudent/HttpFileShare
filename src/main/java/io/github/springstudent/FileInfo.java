package io.github.springstudent;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ZhouNing
 * @date 2025/6/11 16:13
 **/
public class FileInfo {
    private final File file;
    private final AtomicInteger downloadCount;

    public FileInfo(File file) {
        this.file = file;
        this.downloadCount = new AtomicInteger(0);
    }

    public File getFile() {
        return file;
    }

    public int addDownloadCount() {
        return downloadCount.incrementAndGet();
    }

    public int getDownloadCount() {
        return downloadCount.get();
    }
}
