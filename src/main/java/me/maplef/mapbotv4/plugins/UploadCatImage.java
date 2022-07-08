package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.*;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class UploadCatImage implements MapbotPlugin {
    static FileConfiguration config = Main.getInstance().getConfig();

    public static MessageChain uploadImage(Long groupID, Long senderID, Message[] args) throws Exception{
        if(args.length == 0) return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" 请上传一张图片"));
        if(!(args[0] instanceof Image)) return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" 只允许上传图片哦"));

        Image image = (Image) args[0];
        String imageUrl = Image.queryUrl(image);

        String host = "https://aip.baidubce.com/rest/2.0/image-classify/v1/animal?";
        String access_token = config.getString("cat-images.upload-image.cat-detect.access-token");

        JSONObject jsonRes = new JSONObject();

        try{
            URL detectURL = new URL(host
                    + "access_token=" + access_token
                    + "&url=" + imageUrl);
            HttpURLConnection connection = (HttpURLConnection) detectURL.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null)
                result.append(line);

            jsonRes = JSON.parseObject(result.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if(jsonRes.containsKey("error_msg"))
            return MessageUtils.newChain(new At(senderID))
                    .plus(new PlainText(" 检测猫猫出错，请稍后再试：" + jsonRes.getString("error_msg")));

        JSONArray result = jsonRes.getJSONArray("result");
        JSONObject best = result.getJSONObject(0);
        double score = best.getDouble("score"); String catName = best.getString("name");

        if(catName.contains("猫") && score >= config.getDouble("cat-images.upload-image.cat-detect.require-score", 0.3)){
            String uploader = (String) DatabaseOperator.queryPlayer(senderID).get("NAME");

            byte[] imageBase64;
            try {
                URLConnection con = new URL(imageUrl).openConnection();
                InputStream is = con.getInputStream();

                ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                byte[] buff = new byte[100];
                int rc;
                while ((rc = is.read(buff, 0, 100)) > 0) {
                    swapStream.write(buff, 0, rc);
                }
                byte[] data = swapStream.toByteArray();
                imageBase64 = Base64.getEncoder().encode(data);
            } catch (Exception e) {
                e.printStackTrace();
                return MessageUtils.newChain(new At(senderID))
                        .plus(new PlainText(" 猫片编码失败：" + e.getMessage()));
            }

            try{
                Connection c = DatabaseOperator.c;
                PreparedStatement ps = c.prepareStatement("INSERT INTO cat_images (uploader, base64, url, cat_name) VALUES (?, ?, ?, ?);");
                ps.setString(1, uploader);
                ps.setString(2, new String(imageBase64));
                ps.setString(3, imageUrl);
                ps.setString(4, catName);
                ps.execute(); ps.close();
            } catch (Exception e){
                e.printStackTrace();
                return MessageUtils.newChain(new At(senderID))
                        .plus(new PlainText(" 上传猫片到数据库出错：" + e.getMessage()));
            }

            String msg = String.format(" 哇，是一只可爱的%s捏，猫片上传成功！", catName);
            return MessageUtils.newChain(new At(senderID))
                    .plus(new PlainText(msg));
        } else return MessageUtils.newChain(new At(senderID))
                    .plus(new PlainText(" 这张图片不是猫片哦，换一张吧"));
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, Message[] args) throws Exception {
        if(!config.getBoolean("cat-images.upload-image.enable"))
            return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" 上传猫片功能未开启"));

        return uploadImage(groupID, senderID, args);
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("uploadcat", UploadCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class));
        commands.put("上传猫片", UploadCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class));

        usages.put("uploadcat", "#uploadcat - 上传一张猫片");
        usages.put("上传猫片", "#上传猫片 - 上传一张猫片");

        info.put("name", "UploadCatImage");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "上传猫片");
        info.put("version", "1.0");

        return info;
    }
}
