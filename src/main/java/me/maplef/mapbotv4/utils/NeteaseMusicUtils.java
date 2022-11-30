package me.maplef.mapbotv4.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import me.maplef.mapbotv4.Main;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class NeteaseMusicUtils {
    private static final String API_URL = "https://neteasemusic.api.buguwu.net";
    private static final String UserAgent = Main.getInstance().getDescription().getName() + "/" + Main.getInstance().getDescription().getVersion();

    private static String cookie;

    public static JSONObject get(String path,Map<String,Object> params,String cookie) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("cookie", URLEncoder.encode(cookie, StandardCharsets.UTF_8));
        return get(path,params);
    }

    public static JSONObject get(String path, Map<String,Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("timestamp",System.currentTimeMillis());

        String url = API_URL + path + "?" + Joiner.on("&").useForNull("").withKeyValueSeparator("=").join(params);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpGet.addHeader("User-Agent",UserAgent);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return JSON.parseObject(EntityUtils.toString(response.getEntity()));
            }
        }catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpEntity post(String path, Map<String,Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("timestamp",System.currentTimeMillis());

        String url = API_URL + path + "?" + Joiner.on("&").useForNull("").withKeyValueSeparator("=").join(params);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.addHeader("User-Agent",UserAgent);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return response.getEntity();
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkAPIStatus() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(API_URL);
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpGet.addHeader("User-Agent",UserAgent);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return response.getCode() == 200;
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadCookie() {
        try {
            File cookieFile = new File(Main.getInstance().getDataFolder() + "/cache/neteaseMusic.cookie");
            String cookieData;
            if (cookieFile.exists()){
                cookieData = Files.readString(cookieFile.toPath());
            }else {
                setCookie(null);
                return;
            }
            setCookie(cookieData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveCookie(String cookie) throws IOException {
        setCookie(cookie);
        File cookieFile = new File(Main.getInstance().getDataFolder() + "/cache/neteaseMusic.cookie");
        if (cookieFile.exists()) {
            cookieFile.delete();
        }
        FileWriter fileWriter = new FileWriter(cookieFile);
        fileWriter.write(cookie);
        fileWriter.flush();
        fileWriter.close();
    }

    public static void removeCookie() {
        setCookie(null);
        File cookieFile = new File(Main.getInstance().getDataFolder() + "/cache/neteaseMusic.cookie");
        cookieFile.delete();
        loadCookie();
    }

    public static String getAPIUrl() {
        return API_URL;
    }

    public static String getUserAgent() {
        return UserAgent;
    }

    public static String getCookie() {
        return cookie;
    }

    public static void setCookie(String cookie) {
        NeteaseMusicUtils.cookie = cookie;
    }
}
