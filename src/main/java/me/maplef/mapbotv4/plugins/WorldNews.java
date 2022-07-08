package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DownloadImage;
import me.maplef.mapbotv4.utils.HttpClient4;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldNews implements MapbotPlugin {
    private final Bot bot = BotOperator.getBot();

    public MessageChain SendNews(Long groupID){
        File tmp = new File(".\\plugins\\MapBot\\temp.jpg");
        if(tmp.exists()) //noinspection StatementWithEmptyBody
            if(tmp.delete());

        String apiUrl = "https://api.03c3.cn/zb/api.php";
        JSONObject imageUrlRes = JSON.parseObject(HttpClient4.doGet(apiUrl));
        String imageUrl = imageUrlRes.getString("imageUrl");

        File newsImg; ExternalResource imageResource;
        try {
            newsImg = new File(DownloadImage.download(imageUrl, "temp", String.valueOf(Main.getInstance().getDataFolder())));
            imageResource = ExternalResource.create(newsImg);
            Image newsImage = Objects.requireNonNull(bot.getGroup(groupID)).uploadImage(imageResource);
            return MessageUtils.newChain(Image.fromId(newsImage.getImageId()));
        } catch (Exception e) {
            e.printStackTrace();
            return MessageUtils.newChain(new PlainText("获取新闻失败QAQ"));
        }
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, Message[] args){
        return SendNews(groupID);
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("news", WorldNews.class.getMethod("onEnable", Long.class, Long.class, Message[].class));
        commands.put("新闻", WorldNews.class.getMethod("onEnable", Long.class, Long.class, Message[].class));

        usages.put("news", "#news - 获取今日新闻");
        usages.put("新闻", "#新闻 - 获取今日新闻");

        info.put("name", "WorldNews");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取今日新闻");
        info.put("version", "1.2");

        return info;
    }
}
