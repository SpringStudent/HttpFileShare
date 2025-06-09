package io.github.springstudent;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ZhouNing
 * @date 2025/6/9 14:39
 **/
public class FileRegistry {
    private static final ConcurrentHashMap<String, File> fileMap = new ConcurrentHashMap<>();

    public static void put(String id, File file) {
        fileMap.put(id, file);
    }

    public static File get(String id) {
        return fileMap.get(id);
    }

    public static void del(String id) {
        fileMap.remove(id);
    }
}
