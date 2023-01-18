package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.utils.*;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class CatSaying implements MapbotPlugin {
    private static final Bot bot = BotOperator.getBot();

    private static final String GET_URL = "https://sslapi.cdn.v2.copa-api.fun/?s=App.CatSaying.GetRandomCatsaying";
    private static final String SUBMIT_URL = "https://sslapi.cdn.v2.copa-api.fun/?s=App.CatSaying.SubmitCatSaying";

    public static MessageChain getCatSayingMessage(Long groupID) throws Exception{
        String resString = HttpUtils.doGet(GET_URL);
        if(resString.equals("")) resString = HttpUtils.doGet(GET_URL);
        if(resString.equals("")) throw new Exception("获取猫言猫语失败");

        JSONObject jsonRes = JSON.parseObject(resString);

        if(jsonRes.getInteger("ret") != 200) throw new Exception("获取猫言猫语失败");

        JSONObject data = jsonRes.getJSONObject("data");
        int id = data.getInteger("id");
        String gameName = data.getString("gamename");
        int type = data.getInteger("type");
        String saying = data.getString("saying");

        if(type == 0){
            return MessageUtils.newChain(new PlainText(String.format("%s\n\n—— %s\n(Powered by COPAv2, ID: %d)", saying, gameName, id)));
        } else if(type == 1){
            File tmp = new File(Main.getInstance().getDataFolder(), "temp.jpg");
            if(tmp.exists()) //noinspection StatementWithEmptyBody
                if(tmp.delete());

            File sayingImg; ExternalResource imageResource;
            try {
                sayingImg = new File(DownloadImage.download(saying, "temp", String.valueOf(Main.getInstance().getDataFolder())));
                imageResource = ExternalResource.create(sayingImg);
                Image newsImage = Objects.requireNonNull(bot.getGroup(groupID)).uploadImage(imageResource);
                return MessageUtils.newChain(Image.fromId(newsImage.getImageId()),
                        new PlainText(String.format("\n\n—— %s\n(Powered by COPAv2, ID: %d)", gameName, id)));
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("获取猫言失败");
            }
        } else throw new Exception("其他类型猫言功能正在开发中，敬请期待...");
    }

    public static String submitCatSaying(Long senderID, String[] args) throws Exception {
        if(args.length == 0) throw new InvalidSyntaxException();

        String urlPattern = "(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]";

        String playerName = DatabaseOperator.queryPlayer(senderID).get("NAME").toString();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        params.put("gamename", playerName);
        params.put("qq", String.valueOf(senderID));

        if(Pattern.matches(urlPattern, args[0])) params.put("type", "1");
        else params.put("type", "0");
        params.put("saying", args[0]);

        String url = UrlUtils.addParams(SUBMIT_URL, params);

        String resString = HttpUtils.doGet(url);
        if(resString.isEmpty()) throw new Exception("提交猫言失败");
        JSONObject jsonRes = JSON.parseObject(resString);
        if(jsonRes.getInteger("ret") != 200) throw new Exception("提交猫言失败");
        JSONObject data = jsonRes.getJSONObject("data");
        if(Objects.equals(data.getString("status"), "failed")) throw new Exception("提交猫言失败");
        String id = data.getString("id");

        return String.format("提交猫言成功，此猫言的待审id为 %s，请等待审核", id);
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        return null;
    }

    public MessageChain onGetCatSaying(Long groupID, Long senderID, String[] args) throws Exception {
        return getCatSayingMessage(groupID);
    }

    public MessageChain onSubmitCatSaying(Long groupID, Long senderID, String[] args) throws Exception{
        return MessageUtils.newChain(new At(senderID), new PlainText(" " + submitCatSaying(senderID, args)));
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("catsaying", CatSaying.class.getMethod("onGetCatSaying", Long.class, Long.class, String[].class));
        commands.put("猫言", CatSaying.class.getMethod("onGetCatSaying", Long.class, Long.class, String[].class));
        commands.put("猫言猫语", CatSaying.class.getMethod("onGetCatSaying", Long.class, Long.class, String[].class));
        commands.put("submitcatsaying", CatSaying.class.getMethod("onSubmitCatSaying", Long.class, Long.class, String[].class));
        commands.put("提交猫言", CatSaying.class.getMethod("onSubmitCatSaying", Long.class, Long.class, String[].class));
        commands.put("提交猫言猫语", CatSaying.class.getMethod("onSubmitCatSaying", Long.class, Long.class, String[].class));


        usages.put("catsaying", "#catsaying - 听听猫言猫语");
        usages.put("猫言", "#猫言 - 听听猫言猫语");
        usages.put("猫言猫语", "#猫言猫语 - 听听猫言猫语");
        usages.put("submitcatsaying", "#submitcatsaying <saying> - submit a catsaying");
        usages.put("提交猫言", "#提交猫言 <内容> - 提交一条猫言猫语");
        usages.put("提交猫言猫语", "#提交猫言猫语 <内容> - 提交一条猫言猫语");

        info.put("name", "CatSaying");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "进行猫言猫语相关操作");
        info.put("version", "1.1");

        return null;
    }
}
