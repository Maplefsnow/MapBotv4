package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.NeteaseMusicUtils;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NeteaseMusicTest implements MapbotPlugin {
    FileConfiguration config = Main.getInstance().getConfig();

    private static final Bot bot = BotOperator.getBot();

    private static String qrcodeKey;

    private MessageChain login(long groupId){
        String mode = config.getString("netease-cloud-music.login-mode","anonymous");
        return switch (Objects.requireNonNull(mode)) {
            case "qrcode" -> generateQrcode(groupId);
            case "anonymous" -> anonymousLogin();
            default -> MessageUtils.newChain(new PlainText("error"));
        };
    }

    private MessageChain anonymousLogin() {
        JSONObject response = NeteaseMusicUtils.get("/register/anonimous",null);
        if (response.getInteger("code") != 200) {
            return MessageUtils.newChain(new PlainText("游客登录失败：" + response.toJSONString()));
        }
        try {
            NeteaseMusicUtils.saveCookie(response.getString("cookie"));
            return MessageUtils.newChain(new PlainText("登陆成功！"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageChain generateQrcode(long groupId) {
        JSONObject response;
        JSONObject responseData;
        HashMap<String,Object> params;
        response = NeteaseMusicUtils.get("/login/qr/key",null);
        if (response.getInteger("code") != 200) {
            return MessageUtils.newChain(new PlainText("获取qrcode key失败：" + response.toJSONString()));
        }
        responseData = response.getJSONObject("data");
        qrcodeKey = responseData.getString("unikey");

        params = new HashMap<>();
        params.put("key",qrcodeKey);
        response = NeteaseMusicUtils.get("/login/qr/create",params);
        if (response.getInteger("code") != 200) {
            return MessageUtils.newChain(new PlainText("获取二维码内容失败：" + response.toJSONString()));
        }
        responseData = response.getJSONObject("data");
        String qrurl = responseData.getString("qrurl");

        params = new HashMap<>();
        params.put("qrimg",qrurl);
        params.put("key",qrcodeKey);
        response = NeteaseMusicUtils.get("/login/qr/create",params);
        if (response.getInteger("code") != 200) {
            return MessageUtils.newChain(new PlainText("生成二维码失败：" + response.toJSONString()));
        }
        responseData = response.getJSONObject("data");
        String qrcodeBase64 = responseData.getString("qrimg");
        String str1 = qrcodeBase64.substring(0, qrcodeBase64.indexOf(","));
        String str2 = qrcodeBase64.substring(str1.length() + 1);
        byte[] imageDecode = Base64.getDecoder().decode(str2);
        InputStream is = new ByteArrayInputStream(imageDecode);
        Image image = ExternalResource.uploadAsImage(is, Objects.requireNonNull(bot.getGroup(groupId)));
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            HashMap<String,Object> param = new HashMap<>();
            param.put("key",qrcodeKey);
            while (true) {
                JSONObject responseJson = NeteaseMusicUtils.get("/login/qr/check",param);
                if (responseJson.getInteger("code") == 803) {
                    Objects.requireNonNull(bot.getGroup(groupId)).sendMessage(new PlainText(responseJson.getString("message")));
                    try {
                        NeteaseMusicUtils.saveCookie(responseJson.getString("cookie"));
                        break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return MessageUtils.newChain(new PlainText("请使用网易云音乐手机客户端扫码登陆")).plus(image);
    }

    private MessageChain logout() {
        if (NeteaseMusicUtils.getCookie() == null) {
            return MessageUtils.newChain(new PlainText("当前未登录账号"));
        }
        JSONObject logoutResponse = NeteaseMusicUtils.get("/logout",null,NeteaseMusicUtils.getCookie());
        if (logoutResponse.getInteger("code") == 200) {
            NeteaseMusicUtils.removeCookie();
            return MessageUtils.newChain(new PlainText("已退出登录"));
        }else return MessageUtils.newChain(new PlainText("退出登录失败：" + logoutResponse.toJSONString()));
    }

    private JSONObject loginStatus() {
        if (NeteaseMusicUtils.getCookie() == null) {
            return null;
        }
        JSONObject responseData = NeteaseMusicUtils.get("/login/status",null,NeteaseMusicUtils.getCookie()).getJSONObject("data");
        return responseData.getJSONObject("profile");
    }

    private boolean isAnonymousLogin() {
        return Objects.equals(config.getString("netease-cloud-music.login-mode"), "anonymous");
    }

    private MessageChain showMenu() {
        String msg = """
                ---网易云音乐帮助---
                #netease login - 登录网易云音乐账号
                #netease logout - 退出登录
                #netease status - 查看登录、API状态
                #netease search <歌曲名称> - 搜索歌曲
                """;
        return MessageUtils.newChain(new PlainText(msg));
    }

    private MessageChain showStatus() {
        boolean apiStatus = NeteaseMusicUtils.checkAPIStatus();
        String str1;
        if (isAnonymousLogin()) {
            str1 = "游客登录";
        }else {
            JSONObject profile = loginStatus();
            boolean haveProfile = profile != null;
            if (haveProfile) {
                str1 = String.format("%s (%s)",profile.getString("nickname"),profile.getInteger("userId"));
            }else str1 = "未登录";
        }
        return MessageUtils.newChain(new PlainText(String.format("登录状态：%s\nAPI状态：%s",str1,apiStatus)));
    }

    private MessageChain search(String songName){
        String path = "/cloudsearch";

        HashMap<String, Object> params = new HashMap<>();
        params.put("keywords", URLEncoder.encode(songName, StandardCharsets.UTF_8));
        params.put("limit", "10");

        JSONObject searchRes = NeteaseMusicUtils.get(path, params);

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

    private int getMusicID(String songName){
        String path = "/cloudsearch";

        HashMap<String, Object> params = new HashMap<>();
        params.put("keywords", URLEncoder.encode(songName, StandardCharsets.UTF_8));
        params.put("limit", "10");


        JSONObject searchRes = NeteaseMusicUtils.get(path,params);

        return searchRes.getJSONObject("result").getJSONArray("songs").getJSONObject(0).getInteger("id");
    }

    private JSONObject checkMusic(int id) {
        HashMap<String,Object> param = new HashMap<>();
        param.put("id",id);
        return NeteaseMusicUtils.get("/check/music",param);
    }

    private MessageChain sendMusic(int id,long groupId) {
        NeteaseMusicUtils.SendMode sendMode = NeteaseMusicUtils.SendMode.valueOf(config.getString("netease-cloud-music.music.send-mode"));
        String soundQualityLevel = config.getString("netease-cloud-music.music.sound-quality-level", "standard");

        HashMap<String, Object> params = new HashMap<>();
        JSONObject response = checkMusic(id);
        JSONObject responseData;
        if (!response.getBoolean("success")){
            return MessageUtils.newChain(new PlainText(response.getString("message")));
        }
        params.put("id",id);
        params.put("level",soundQualityLevel);
        response = NeteaseMusicUtils.get("/song/url/v1",params,NeteaseMusicUtils.getCookie());
        if (response.getInteger("code") != 200) {
            return MessageUtils.newChain(new PlainText("获取音乐错误: " + response.toJSONString()));
        }
        responseData = response.getJSONArray("data").getJSONObject(0);

        if (responseData.getInteger("code") == 404) {
            return MessageUtils.newChain(new PlainText("此音乐不存在"));
        }else if (responseData.getInteger("code") != 200) {
            return MessageUtils.newChain(new PlainText("获取音乐错误：" + responseData.toJSONString()));
        }

        String musicUrl = responseData.getString("url");

        if (sendMode == NeteaseMusicUtils.SendMode.voice) {
            return sendVoice(musicUrl,groupId);
        }

        params = new HashMap<>();
        params.put("ids",id);
        response = NeteaseMusicUtils.get("/song/detail",params);
        JSONArray songsArray = response.getJSONArray("songs");
        JSONObject musicInfo = songsArray.getJSONObject(0);

        String musicName = musicInfo.getString("name");
        ArrayList<String> musicAuthorList = new ArrayList<>();
        JSONArray musicAuthors = musicInfo.getJSONArray("ar");

        for (int i = 0; i < musicAuthors.size(); i++) {
           musicAuthorList.add(musicAuthors.getJSONObject(i).getString("name"));
        }

        JSONObject musicAl = musicInfo.getJSONObject("al");
        String musicImageUrl = musicAl.getString("picUrl");
        if (sendMode == NeteaseMusicUtils.SendMode.both) {
            Objects.requireNonNull(bot.getGroup(groupId)).sendMessage(sendCard(id,musicUrl,musicName,musicAuthorList,musicImageUrl));
            return sendVoice(musicUrl,groupId);
        } else return sendCard(id,musicUrl,musicName,musicAuthorList,musicImageUrl);
    }

    private MessageChain sendCard(int id, String musicUrl,String musicName,ArrayList<String> musicAuthors,String musicImageUrl) {
        MusicShare share= new MusicShare(
                MusicKind.NeteaseCloudMusic,
                musicName,
                String.join("/",musicAuthors),
                "https://music.163.com/#/song?id=" + id,
                musicImageUrl,musicUrl);
        return MessageUtils.newChain(share);
    }

    private MessageChain sendVoice(String musicUrl, long groupId) {




        //Audio audio = Objects.requireNonNull(bot.getGroup(groupId)).uploadAudio(ExternalResource.create())
        return null;
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        if (args.length == 0) {
            return showMenu();
        }

        switch (args[0].contentToString()) {
            case "login" -> {
                return login(groupID);
            }
            case "search" -> {
                if (args.length < 2) throw new InvalidSyntaxException();
                return search(args[1].contentToString());
            }

            case "logout" -> {
                return logout();
            }

            case "status" -> {
                return showStatus();
            }

            case "play" -> {
                return sendMusic(Integer.parseInt(args[1].contentToString()),groupID);
            }

            default -> throw new InvalidSyntaxException();
        }
    }



    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("netease", NeteaseMusicTest.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

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
