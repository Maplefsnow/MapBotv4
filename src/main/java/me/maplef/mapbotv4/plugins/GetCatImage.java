package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.bukkit.configuration.file.FileConfiguration;

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
    private static final Bot bot = BotOperator.getBot();
    static FileConfiguration config = Main.getInstance().getConfig();

//    public static MessageChain getImage(Long groupID) throws Exception{
//        String imagePath = ".\\plugins\\MapBot\\cat_images";
//        File[] imageList = Objects.requireNonNull(new File(imagePath).listFiles());
//
//        Random random = new Random();
//        int pos = random.nextInt(imageList.length);
//        File image = imageList[pos];
//        ExternalResource imageResource = ExternalResource.create(image);
//
//        try{
//            Image catImage = Objects.requireNonNull(bot.getGroup(groupID)).uploadImage(imageResource);
//            return new MessageChainBuilder().append(Image.fromId(catImage.getImageId())).build();
//        } catch (Exception e) {
//            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
//            throw new Exception("发送猫片失败QAQ，请再试一次吧");
//        }
//    }

    public static MessageChain getImage(Long groupID) {
        int id;
        String uploader, imageBase64str, catName;
        Timestamp uploaded_time;

        Connection c = DatabaseOperator.c;
        try(Statement stmt = c.createStatement();
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
            return MessageUtils.newChain(new PlainText("数据库异常"));
        }

        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(uploaded_time);

        byte[] imageDecode = Base64.getDecoder().decode(imageBase64str);
        InputStream is = new ByteArrayInputStream(imageDecode);
        Image image = ExternalResource.uploadAsImage(is, Objects.requireNonNull(bot.getGroup(groupID)));

        String catImageInfo = String.format("猫片ID: %d\n" +
                                            "品种: %s\n" +
                                            "上传者: %s\n" +
                                            "上传时间: %s", id, catName, uploader, timeStr);
        return MessageUtils.newChain(image).plus("\n\n").plus(catImageInfo);
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, Message[] args) throws Exception{
        if(!config.getBoolean("cat-images.get-image.enable"))
            return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" 猫片功能未开启"));

        return new MessageChainBuilder().append(getImage(groupID)).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("cat", GetCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class));
        commands.put("猫片", GetCatImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class));

        usages.put("cat", "#cat - 让我们看点猫片");
        usages.put("猫片", "#猫片 - 让我们看点猫片");

        info.put("name", "CatImage");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取一张猫片");
        info.put("version", "1.3");

        return info;
    }
}
