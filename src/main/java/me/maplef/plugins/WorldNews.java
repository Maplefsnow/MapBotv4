package me.maplef.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.MapbotPlugin;
import me.maplef.utils.BotOperator;
import me.maplef.utils.DownloadImage;
import me.maplef.utils.HttpClient4;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.utils.ExternalResource;
import org.bukkit.Bukkit;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldNews implements MapbotPlugin {
    private final Bot bot = BotOperator.bot;

    public MessageChain SendNews(Long groupID){
        File tmp = new File(".\\plugins\\MapBot\\cat_images\\temp.jpg");
        if(tmp.exists() && tmp.delete()) {Bukkit.getLogger();}

        String apiUrl = "https://api.03c3.cn/zb/api.php";
        JSONObject imageUrlRes = JSON.parseObject(HttpClient4.doGet(apiUrl));
        String imageUrl = imageUrlRes.getString("imageUrl");

        File newsImg; ExternalResource imageResource;
        try {
            newsImg = new File(DownloadImage.download(imageUrl, "temp", ".\\plugins\\Mapbot"));
            imageResource = ExternalResource.create(newsImg);
            Image newsImage = Objects.requireNonNull(bot.getGroup(groupID)).uploadImage(imageResource);
            return new MessageChainBuilder().append(Image.fromId(newsImage.getImageId())).build();
        } catch (Exception e) {
            e.printStackTrace();
            return new MessageChainBuilder().append("获取新闻失败QAQ").build();
        }
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args){
        return SendNews(groupID);
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("news", WorldNews.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("新闻", WorldNews.class.getMethod("onEnable", Long.class, Long.class, String[].class));

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
