package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class GetCatImage implements MapbotPlugin {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    private static final Bot bot = BotOperator.getBot();

    public static MessageChain getRandomImage(Long groupID) {
        int id;
        String uploader, imageBase64str, catName;
        Timestamp uploaded_time;

        try(Connection c = new DatabaseOperator().getConnect();
            Statement stmt = c.createStatement();
            ResultSet res = stmt.executeQuery("SELECT * " +
                    "FROM cat_images AS t1 JOIN (SELECT ROUND(RAND() * ((SELECT MAX(id) FROM cat_images)-(SELECT MIN(id) FROM cat_images))+(SELECT MIN(id) FROM cat_images)) AS id) AS t2 " +
                    "WHERE t1.id >= t2.id " +
                    "ORDER BY t1.id LIMIT 1;")){
            if(res.next()){
                id = res.getInt("id");
                uploader = res.getString("uploader");
                uploaded_time = res.getTimestamp("uploaded_time");
                imageBase64str = res.getString("base64");
                catName = res.getString("cat_name");
            } else return MessageUtils.newChain(new PlainText("FAILED!"));
        } catch (SQLException e){
            e.printStackTrace();
            return MessageUtils.newChain(new PlainText("???????????????"));
        }

        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(uploaded_time);

        byte[] imageDecode = Base64.getDecoder().decode(imageBase64str);
        InputStream is = new ByteArrayInputStream(imageDecode);
        Image image = ExternalResource.uploadAsImage(is, Objects.requireNonNull(bot.getGroup(groupID)));

        String catImageInfo = String.format("??????ID: %d\n" +
                                            "??????: %s\n" +
                                            "?????????: %s\n" +
                                            "????????????: %s", id, catName, uploader, timeStr);
        return MessageUtils.newChain(image).plus("\n\n").plus(catImageInfo);
    }

    public static MessageChain getImageByID(Long senderID, Long groupID, int id){
        String uploader, imageBase64str, catName;
        Timestamp uploaded_time;

        try(Connection c = new DatabaseOperator().getConnect();
            Statement stmt = c.createStatement();
            ResultSet res = stmt.executeQuery(String.format("SELECT * FROM cat_images WHERE id = %d;", id))){
            if(res.next()){
                uploader = res.getString("uploader");
                uploaded_time = res.getTimestamp("uploaded_time");
                imageBase64str = res.getString("base64");
                catName = res.getString("cat_name");
            } else return MessageUtils.newChain(new At(senderID)).plus(" ??????????????????");
        } catch (SQLException e){
            e.printStackTrace();
            return MessageUtils.newChain(new PlainText("???????????????"));
        }

        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(uploaded_time);

        byte[] imageDecode = Base64.getDecoder().decode(imageBase64str);
        InputStream is = new ByteArrayInputStream(imageDecode);
        Image image = ExternalResource.uploadAsImage(is, Objects.requireNonNull(bot.getGroup(groupID)));

        String catImageInfo = String.format("??????ID: %d\n" +
                "??????: %s\n" +
                "?????????: %s\n" +
                "????????????: %s", id, catName, uploader, timeStr);
        return MessageUtils.newChain(image).plus("\n\n").plus(catImageInfo);
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        if(!config.getBoolean("cat-images.get-image.enable"))
            return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" ?????????????????????"));

        if(args.length > 0){
            int id;
            try {
                id = Integer.parseInt(args[0].contentToString());
            } catch (NumberFormatException e){
                return MessageUtils.newChain(new At(senderID)).plus(" ?????????????????????");
            }
            return getImageByID(senderID, groupID, id);
        } else
            return getRandomImage(groupID);
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("cat", GetCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("??????", GetCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("cat", "#cat - ?????????????????????");
        usages.put("??????", "#?????? - ?????????????????????");

        info.put("name", "CatImage");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "??????????????????");
        info.put("version", "1.3");

        return info;
    }
}
