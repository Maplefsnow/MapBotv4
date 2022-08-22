package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.utils.Base64Utils;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.HttpUtils;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GetRandACGImage implements MapbotPlugin {
    private static final String apiUrl = "https://hy.buguwu.net/acg/random.php?return=json";
    private static final Bot bot = BotOperator.getBot();

    public String downloadImageToBase64(URL url) {
        BufferedImage image;
        File storeFile = new File("temp");
        try {
            image = ImageIO.read(url);
            ImageIO.write(image, "jpg",storeFile);
            String base64 = Base64Utils.fileToBase64(storeFile);
            storeFile.delete();
            return base64;
        } catch (IOException e) {
            e.printStackTrace();
            return "[error]";
        }
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        String jsonString = HttpUtils.doGet(apiUrl);
        JSONObject jsonObject = JSON.parseObject(jsonString);
        String id = jsonObject.getString("id");
        String base64 = downloadImageToBase64(new URL(jsonObject.getString("acgurl")));
        if (base64.equals("[error]")) {
            return MessageUtils.newChain(new At(senderID)).plus("获取图片失败").plus(jsonString);
        }
        byte[] imageDecode = Base64.getDecoder().decode(base64);
        InputStream is = new ByteArrayInputStream(imageDecode);
        Image image = ExternalResource.uploadAsImage(is, Objects.requireNonNull(bot.getGroup(groupID)));
        return MessageUtils.newChain(new At(senderID)).plus(" ID: " + id).plus(image);
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("acg", GetRandACGImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("来点二次元", GetRandACGImage.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("acg", "#acg - 让我们看点二次元");
        usages.put("来点二次元", "#来点二次元 - 让我们看点二次元");

        info.put("name", "ACGImage");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "buguwu");
        info.put("description", "获取一张二次元");
        info.put("version", "1.0");

        return info;
    }

    /*public static void main(String[] args) throws MalformedURLException {
        String jsonString = HttpUtils.doGet(apiUrl);
        JSONObject jsonObject = JSON.parseObject(jsonString);
        String id = jsonObject.getString("id");
        System.out.println(id);
        String base64 = new GetRandACGImage().downloadImageToBase64(new URL(jsonObject.getString("acgurl")));
        System.out.println(base64);
    }*/
}