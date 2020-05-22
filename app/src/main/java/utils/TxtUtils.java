package utils;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * txt文件内容管理器
 */
public class TxtUtils {

    public static String rootXMLPath = Environment.getExternalStorageDirectory().getPath() ;

    /**
     * 保存内容到TXT文件中
     *
     * @param fileName
     * @param content
     * @return
     */
    public static boolean writeToXML(String fileName, String content) {
        FileOutputStream fileOutputStream;
        BufferedWriter bufferedWriter;
        createDirectory(rootXMLPath);
        File file = new File(rootXMLPath + "/" + fileName + ".txt");
        try {
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            bufferedWriter.write(content);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 读取XML内容
     *
     * @param filePath
     * @return
     */
    public static String readFromXML(String filePath) {
        FileInputStream fileInputStream;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder = new StringBuilder();
        File file = new File(filePath);
        if (file.exists()) {
            try {
                fileInputStream = new FileInputStream(file);
                bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 创建文件夹
     *
     * @param fileDirectory
     */
    public static void createDirectory(String fileDirectory) {
        File file = new File(fileDirectory);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
