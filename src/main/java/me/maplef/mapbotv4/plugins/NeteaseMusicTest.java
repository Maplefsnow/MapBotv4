package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.utils.HttpClient4;
import me.maplef.mapbotv4.utils.UrlUtils;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class NeteaseMusicTest implements MapbotPlugin {
    FileConfiguration config = Main.getInstance().getConfig();

    private final String DOMAIN = "https://netease-cloud-music-api-one-azure-11.vercel.app";

    private MessageChain login(){
        String address = DOMAIN + "/login/cellphone";

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("phone", config.getString("netease-cloud-music.profile.phone"));
        params.put("password", config.getString("netease-cloud-music.profile.password"));

        String loginUrl = UrlUtils.addParams(address, params);

        JSONObject loginRes = JSON.parseObject(HttpClient4.doGet(loginUrl));

        int code = loginRes.getInteger("code");
        if(code == 200) {
            JSONObject profile = loginRes.getJSONObject("profile");
            String nickname = profile.getString("nickname");
            return MessageUtils.newChain(new PlainText(String.format("网易云音乐账户 %s 登陆成功", nickname)));
        } else {
            return MessageUtils.newChain(new PlainText("网易云音乐账户登陆失败，请检查控制台输出"));
        }
    }

    private MessageChain search(String songName){
        String address = DOMAIN + "/cloudsearch";

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("keywords", songName);
        params.put("limit", "10");

        String searchUrl = UrlUtils.addParams(address, params);

        JSONObject searchRes = JSON.parseObject(HttpClient4.doGet(searchUrl));

        JSONArray songs = searchRes.getJSONObject("result").getJSONArray("songs");
        StringBuilder resStringBuilder = new StringBuilder();
        for(int i=0; i<=4; i++){
            JSONObject song = songs.getJSONObject(i);

            JSONArray authors = song.getJSONArray("ar");
            StringBuilder authorListBuilder = new StringBuilder();
            for(Object author : authors) authorListBuilder.append(((JSONObject) author).getString("name")).append(", ");
            String authorList = authorListBuilder.toString();
            authorList = authorList.substring(0, authorList.length()-2);

            String singleStr = String.format("%d. %s - %s", i+1, song.getString("name"), authorList);
            resStringBuilder.append(singleStr).append("\n");
        }

        String resString = resStringBuilder.toString();
        resString = resString.substring(0, resString.length()-1);

        return MessageUtils.newChain(new PlainText(String.format("\"%s\" 的搜索结果:\n%s", songName, resString)));
    }

    private String getMusicID(String songName){
        String address = DOMAIN + "/cloudsearch";

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("keywords", songName);
        params.put("limit", "10");

        String searchUrl = UrlUtils.addParams(address, params);

        JSONObject searchRes = JSON.parseObject(HttpClient4.doGet(searchUrl));

        return searchRes.getJSONObject("result").getJSONArray("songs").getJSONObject(0).getString("id");
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, Message[] args) throws Exception {
        switch (args[0].contentToString()) {
            case "login": {
                return login();
            }
            case "search": {
                if (args.length < 2) throw new InvalidSyntaxException();
                return search(args[1].contentToString());
            }
            default:
                throw new IllegalStateException("未知的参数: " + args[0]);
        }
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("netease", NeteaseMusicTest.class.getMethod("onEnable", Long.class, Long.class, Message[].class));

        usages.put("netease", "#netease - 网易云音乐（测试版）");

        info.put("name", "netease");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "网易云音乐");
        info.put("version", "0.1");

        return info;
    }
}
