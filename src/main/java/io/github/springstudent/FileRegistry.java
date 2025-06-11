package io.github.springstudent;

import java.io.File;
import java.util.Collection;
import java.util.List;
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

    public static Collection<String> list(){
        return fileMap.keySet();
    }

    public static File get(String id) {
        return fileMap.get(id);
    }

    public static void del(String id) {
        fileMap.remove(id);
    }

    public static void clear() {
        fileMap.clear();
    }

    public static boolean contains(String id) {
        return fileMap.containsKey(id);
    }
}
