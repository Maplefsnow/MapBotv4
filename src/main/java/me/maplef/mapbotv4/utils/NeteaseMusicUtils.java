package me.maplef.mapbotv4.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import me.maplef.mapbotv4.Main;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

public class NeteaseMusicUtils {
    private static final FileConfiguration config = Main.getInstance().getConfig();
    private static final String API_URL = "https://neteasemusic.api.buguwu.net";
    private static final String UserAgent = Main.getInstance().getDescription().getName() + "/" + Main.getInstance().getDescription().getVersion();

    private static String cookie;

    public static JSONObject get(String path,HashMap<String,Object> params,String cookie) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("cookie", URLEncoder.encode(cookie, StandardCharsets.UTF_8));
        return get(path,params);
    }

    public static JSONObject get(String path, HashMap<String,Object> params) {
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

    public static HttpEntity post(String path, HashMap<String,Object> params) {
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
        NeteaseMusicUtils.cookie = cookie;
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

    public static boolean mp3ToAmr(File file) {
        String ffmpegPath = config.getString("netease-cloud-music.ffmpeg-path","ffmpeg");
        List<String> command = Lists.newArrayList();

        command.add(ffmpegPath);
        command.add("-y");
        command.add("-i");
        command.add(file.getPath());
        command.add("-ac");
        command.add("1");
        command.add("-ar");
        command.add("8000");
        command.add("-f");
        command.add("amr");
        command.add(Main.getInstance().getDataFolder() + File.separator + "tempMusic.amr");

        ProcessBuilder builder = new ProcessBuilder();

        builder.command(command);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = br.readLine()) != null) {
                Main.getInstance().getLogger().info("[NeteaseMusic.FFmpeg] " + line);
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean downloadMusic(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        long length = entity.getContentLength();
                        if (length > 0) {
                            try (InputStream inputStream = entity.getContent()) {
                                Files.copy(inputStream, Paths.get(Main.getInstance().getDataFolder().getPath() + "/tempMusic.mp3"), StandardCopyOption.REPLACE_EXISTING);
                                return true;
                            }
                        }
                        return false;
                    }
                    return false;
                }
                return false;
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    public enum SendMode {
        card,
        voice,
        both
    }

    public static String getCookie() {
        return cookie;
    }

    public static void setCookie(String cookie) {
        NeteaseMusicUtils.cookie = cookie;
        if (cookie == null) {
            return;
        }

        try {
            saveCookie(cookie);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}