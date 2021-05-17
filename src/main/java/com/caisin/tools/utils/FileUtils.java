package com.caisin.tools.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class FileUtils {
    /**
     * 获取目录下sql文件
     *
     * @param path
     * @return
     */
    public static List<File> getFiles(String path) {
        File dirFile = new File(path);
        return getFiles(dirFile);
    }

    public static List<File> getFiles(File dirFile) {
        ArrayList<File> files = new ArrayList<>();
        if (!dirFile.isDirectory()) {
            if (dirFile.isFile()) {
                files.add(dirFile);
            }
            return files;
        }
        for (File file : Objects.requireNonNull(dirFile.listFiles())) {
            if (file.isDirectory()) {
                files.addAll(getFiles(file.getPath()));
            } else {
                files.add(file);
            }
        }
        return files;
    }

    public static List<File> getContainsFiles(String path, String contain) {
        List<File> files = getFiles(path);
        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i).getName();
            if (!name.contains(contain)) {
                files.remove(i);
                i--;
            }
        }
        return files;
    }

    public static List<File> getFiles(String path, Function<String,Boolean> filter) {
        File dirFile = new File(path);
        ArrayList<File> files = new ArrayList<>();
        if (!dirFile.isDirectory()) {
            String fileName = dirFile.getName().toLowerCase();
            if (dirFile.isFile() && filter.apply(fileName)) {
                files.add(dirFile);
            }
            return files;
        }
        for (File file : Objects.requireNonNull(dirFile.listFiles())) {
            if (file.isDirectory()) {
                files.addAll(getFiles(file.getPath(),filter));
            } else {
                String fileName = file.getName().toLowerCase();
                if (filter.apply(fileName)) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public static List<File> getStartWithFiles(String path, String startWith) {
        List<File> files = getFiles(path);
        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i).getName();
            if (!name.startsWith(startWith)) {
                files.remove(i);
                i--;
            }
        }
        return files;
    }

    public static List<File> getEndWithFiles(String path, String endWith) {
        List<File> files = getFiles(path);
        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i).getName();
            if (!name.endsWith(endWith)) {
                files.remove(i);
                i--;
            }
        }
        return files;
    }
    public static void deepSearchDo(String path, Function<File,Boolean> filter) {
        List<File> files = getFiles(path);
        for (File file : files) {
            filter.apply(file);
        }
    }
    public static void deepSearchDo(File path, Function<File,Boolean> filter) {
        List<File> files = getFiles(path);
        for (File file : files) {
            filter.apply(file);
        }
    }
}
