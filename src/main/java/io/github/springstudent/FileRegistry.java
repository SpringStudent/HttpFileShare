package io.github.springstudent;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ZhouNing
 * @date 2025/6/9 14:39
 **/
public class FileRegistry {

    private static final Map<String, FileInfo> fileMap = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void put(String id, File file) {
        fileMap.put(id, new FileInfo(file));
    }

    public static Collection<String> list() {
        return fileMap.keySet();
    }

    public static FileInfo get(String id) {
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
