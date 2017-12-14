package edu.cqu.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class FileUtils {
    public static void writeStringToFile(String content, String filePath){
        File file = new File(filePath);
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
         try {
             FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             byte[] bits = content.getBytes();
             fileOutputStream.write(bits);
             fileOutputStream.close();
         } catch (Exception e){

         }
    }
}
