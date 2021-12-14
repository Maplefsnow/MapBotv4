package me.maplef.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.MapbotPlugin;
import me.maplef.utils.HttpClient4;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Hitokoto implements MapbotPlugin {
    public static String HitokotoMessage() {
        String jsonString = HttpClient4.doGet("https://v1.hitokoto.cn/");
        if (jsonString.isEmpty())  jsonString = HttpClient4.doGet("https://v1.hitokoto.cn/");
        JSONObject res = JSON.parseObject(jsonString);

        return String.format("%s\n\n—— %s\n(来自hitokoto.cn, ID: %s)",
                res.get("hitokoto"), res.get("from").toString().replace("\"",""), res.get("id"));
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args){
        return new MessageChainBuilder().append(HitokotoMessage()).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("hitokoto", Hitokoto.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("一言", Hitokoto.class.getMethod("onEnable", Long.class, Long.class, String[].class));

        usages.put("hitokoto", "#hitokoto - 获取一言");
        usages.put("一言", "#一言 - 获取一言");

        info.put("name", "hitokoto");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取一言");
        info.put("version", "1.3");

        return info;
    }

    public Hitokoto(){}
}
