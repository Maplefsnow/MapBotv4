package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.utils.HttpClient4;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CatSaying implements MapbotPlugin {
    private final static String url = "https://v2.copa.mrzzj.top/?s=App.CatSaying.GetRandomCatsaying";

    public static String getCatSayingMessage() throws Exception{
        String resString = HttpClient4.doGet(url);
        if(resString == null) resString = HttpClient4.doGet(url);

        JSONObject jsonRes = JSON.parseObject(resString);

        if(jsonRes.getInteger("ret") != 200) throw new Exception("获取猫言猫语失败");

        JSONObject data = jsonRes.getJSONObject("data");
        int id = data.getInteger("id");
        String gameName = data.getString("gamename");
        int type = data.getInteger("type");
        String saying = data.getString("saying");

        if(type == 0){
            return String.format("%s\n\n—— %s\n(Powered by COPAv2, ID: %d)", saying, gameName, id);
        } else throw new Exception("非文本功能正在开发中，敬请期待...");
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception {
        return MessageUtils.newChain(new PlainText(getCatSayingMessage()));
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("catsaying", CatSaying.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("猫言", CatSaying.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("猫言猫语", CatSaying.class.getMethod("onEnable", Long.class, Long.class, String[].class));

        usages.put("catsaying", "#catsaying - 听听猫言猫语");
        usages.put("猫言", "#猫言 - 听听猫言猫语");
        usages.put("猫言猫语", "#猫言猫语 - 听听猫言猫语");

        info.put("name", "CatSaying");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取猫言");
        info.put("version", "1.0");

        return info;
    }
}
