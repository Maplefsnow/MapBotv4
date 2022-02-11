package me.maplef.mapbotv4.utils;

import java.io.*;
import java.util.Base64;

public class Base64Utils {
    public static String fileToBase64(File file) {
        String strBase64 = null;
        try {
            InputStream in = new FileInputStream(file);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            strBase64 = encode(bytes);
            in.close();
        } catch (IOException fe) {
            fe.printStackTrace();
        }
        return strBase64;
    }

    public static void base64ToFile(String strBase64, File toFile) throws IOException {
        ByteArrayInputStream in = null;
        FileOutputStream out = null;
        try {
            byte[] bytes = decode(strBase64.trim());
            in = new ByteArrayInputStream(bytes);
            byte[] buffer = new byte[1024];
            out = new FileOutputStream(toFile);
            int byteread;
            while ((byteread = in.read(buffer)) != -1) {
                out.write(buffer, 0, byteread);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if(out != null) out.close();
            if(in != null) in.close();
        }
    }

    public static String encode(byte[] bstr) {
        return Base64.getEncoder().encodeToString(bstr);
    }

    public static byte[] decode(String str) {
        return Base64.getDecoder().decode(str);
    }
}
