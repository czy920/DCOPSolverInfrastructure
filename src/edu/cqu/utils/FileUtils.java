package edu.cqu.utils;

import java.io.File;
import java.io.FileOutputStream;

public class FileUtils {
    public static void writeStringToFile(String content, String filePath){
        File file = new File(filePath);
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
         try {
             FileOutputStream fileOutputStream = new FileOutputStream(filePath,true);
             byte[] bits = content.getBytes();
             fileOutputStream.write(bits);
             fileOutputStream.close();
         } catch (Exception e){
            e.printStackTrace();
         }
    }

    public static String joinPaths(String ...paths){
        StringBuilder stringBuilder = new StringBuilder();
        for (String path : paths){
            stringBuilder.append(path);
            stringBuilder.append("/");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}
