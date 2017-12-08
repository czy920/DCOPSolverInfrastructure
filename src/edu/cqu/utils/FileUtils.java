package edu.cqu.utils;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class FileUtils {
    public static void writeStringToFile(String content, String filePath){
         try {
             FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             byte[] bits = content.getBytes();
             fileOutputStream.write(bits);
             fileOutputStream.close();
         } catch (Exception e){

         }
    }
}
