package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class UploadCatImage implements MapbotPlugin {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();

    private JSONObject detectCat(Image image) throws Exception{
        String imageUrl = Image.queryUrl(image);

        String host = "https://aip.baidubce.com/rest/2.0/image-classify/v1/animal?";
        String access_token = config.getString("cat-images.upload-image.cat-detect.access-token");

        URL detectURL = new URL(host
                + "access_token=" + access_token
                + "&url=" + imageUrl);
        HttpURLConnection connection = (HttpURLConnection) detectURL.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) result.append(line);

        JSONObject jsonRes = JSON.parseObject(result.toString());

        if(jsonRes.containsKey("error_msg")) return jsonRes;

        return jsonRes.getJSONArray("result").getJSONObject(0);
    }

    private String uploadLimitCheck(String uploader, Image image) throws Exception{
        if(image.getSize() == 0)
            return "????????????????????????";
        if(image.getSize() > 1024 * config.getLong("cat-images.upload-image.limit.max-image-size"))
            return "????????????????????????";

        Connection c = new DatabaseOperator().getConnect();

        PreparedStatement queryLastCmd = c.prepareStatement("SELECT * from cat_images WHERE uploader = ? ORDER BY uploaded_time DESC LIMIT 1");
        queryLastCmd.setString(1, uploader);

        PreparedStatement queryTotalCmd = c.prepareStatement("select count(*) total from cat_images WHERE uploader = ? AND uploaded_time >= ?");
        String todayZero = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(new Date());
        queryTotalCmd.setString(1, uploader);
        queryTotalCmd.setString(2, todayZero);

        try(Statement stmt = c.createStatement();
            ResultSet resLast = queryLastCmd.executeQuery();
            ResultSet resTotal = queryTotalCmd.executeQuery()){

            if(!resLast.next() || !resTotal.next()) return "OK";

            long lastTime = resLast.getTimestamp("uploaded_time").getTime(); long nowTime = new Date().getTime();
            long intervalSec = (nowTime - lastTime)/1000; long cooldownSec = 60 * config.getLong("cat-images.upload-image.limit.cooldown");

            if(intervalSec < cooldownSec)
                return String.format("??????????????????CD?????? %d ???", cooldownSec - intervalSec);

            int totalNum = resTotal.getInt("total");
            if(totalNum >= config.getInt("cat-images.upload-image.limit.max-images-per-day"))
                return "????????????????????????????????????????????????????????????";
        } finally {
            c.close();
        }

        return "OK";
    }

    private static void uploadImage(String uploader, Image image, String catName) throws Exception{
        String imageUrl = Image.queryUrl(image);

        Connection c = new DatabaseOperator().getConnect();
        try(Statement stmt = c.createStatement();
            ResultSet res = stmt.executeQuery(String.format("SELECT * FROM cat_images WHERE url = '%s';", imageUrl))){
            if(res.next()) throw new FileAlreadyExistsException("??????????????????????????????ID: " + res.getInt("id"));
        }

        byte[] imageBase64;
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

        PreparedStatement ps = c.prepareStatement("INSERT INTO cat_images (uploader, base64, url, cat_name) VALUES (?, ?, ?, ?);");
        ps.setString(1, uploader);
        ps.setString(2, new String(imageBase64));
        ps.setString(3, imageUrl);
        ps.setString(4, catName);
        ps.execute(); ps.close(); c.close();
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        if(!config.getBoolean("cat-images.upload-image.enable"))
            return MessageUtils.newChain(new At(senderID)).plus(" ???????????????????????????");
        if(args.length == 0) return MessageUtils.newChain(new At(senderID)).plus(" ?????????????????????");
        if(!(args[0] instanceof Image) && quoteReply == null) return MessageUtils.newChain(new At(senderID)).plus(" ????????????????????????");

        Image image;
        if(quoteReply != null){
            MessageSource source = quoteReply.getSource();
            int id = source.getIds()[0];

            MessageChain quoteMessage = MessageUtils.newChain(source);
            image = quoteMessage.get(Image.Key);
            for(Message message : quoteMessage){

                Bukkit.getServer().getLogger().info(message.getClass().getName());
                Bukkit.getServer().getLogger().info(message.getClass().toString());
                if(message instanceof Image){
                    image = (Image) message;
                    break;
                }
            }
            if(image == null) return MessageUtils.newChain(new At(senderID)).plus(" ????????????????????????????????????");
        } else {
            image = (Image) args[0];
        }
        String uploader = (String) DatabaseOperator.queryPlayer(senderID).get("NAME");

        String uploadLimitMsg = uploadLimitCheck(uploader, image);
        if(!uploadLimitMsg.equals("OK"))
            return MessageUtils.newChain(new At(senderID)).plus(" ").plus(uploadLimitMsg);

        String catName = "??????"; double score = -1.0;
        if(config.getBoolean("cat-images.upload-image.cat-detect.enable")){
            JSONObject result = detectCat(image);
            if(result.containsKey("error_msg"))
                return MessageUtils.newChain(new At(senderID)).plus(" ?????????????????????").plus(result.getString("error_msg"));
            else{
                catName = result.getString("name");
                score = result.getDouble("score");
            }
        }

        if(config.getBoolean("cat-images.upload-image.cat-detect.enable")){
            if(catName.contains("???") && score >= config.getDouble("cat-images.upload-image.cat-detect.require-score")){
                try{
                    uploadImage(uploader, image, catName);
                    String msg = String.format(" ????????????????????????%s???????????????????????????", catName);
                    return MessageUtils.newChain(new At(senderID))
                            .plus(new PlainText(msg));
                } catch (Exception e){
                    return MessageUtils.newChain(new At(senderID)).plus(" ?????????????????????").plus(e.getMessage());
                }
            } else
                return MessageUtils.newChain(new At(senderID)).plus(" ??????????????????????????????????????????");
        } else {
            try{
                uploadImage(uploader, image, catName);
                return MessageUtils.newChain(new At(senderID)).plus(" ?????????????????????");
            } catch (Exception e){
                return MessageUtils.newChain(new At(senderID)).plus(" ?????????????????????").plus(e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("uploadcat", UploadCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("????????????", UploadCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("uploadcat", "#uploadcat - ??????????????????");
        usages.put("????????????", "#???????????? - ??????????????????");

        info.put("name", "UploadCatImage");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "????????????");
        info.put("version", "1.0");

        return info;
    }
}
