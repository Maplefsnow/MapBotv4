package me.maplef.mapbotv4.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtils {
    public static String doGet(String URL){
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder result = new StringBuilder();
        try{
            URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Accept", "application/json");

            conn.connect();
            is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null){
                result.append(line);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                if(br != null){
                    br.close();
                }
                if(is != null){
                    is.close();
                }
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
            assert conn != null;
            conn.disconnect();
        }
        return result.toString();
    }
}