package me.maplef.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class DownloadImage {
    public static String download(String urlString, String fileName, String path) throws Exception {
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        InputStream is = con.getInputStream();

        byte[] bs = new byte[1024];
        int len;
        String filePath = path + "\\" + fileName + ".jpg";
        File file = new File(filePath);
        FileOutputStream os = new FileOutputStream(file, true);
        while ((len = is.read(bs)) != -1)
            os.write(bs, 0, len);

        os.close(); is.close();
        return filePath;
    }
}
