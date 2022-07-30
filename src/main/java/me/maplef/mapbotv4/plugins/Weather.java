package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.HttpUtils;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Weather implements MapbotPlugin {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();

    public String WeatherMessage(String location) throws Exception{
        StringBuilder msg = new StringBuilder();

        // Weather API Key
        String KEY = Objects.requireNonNull(config.getString("QWeather-APIKey"));

        // Request url concat
        String getIdURL = "https://geoapi.qweather.com/v2/city/lookup?key=".concat(KEY).concat("&location=").concat(location);
        String getNowURL = "https://devapi.qweather.com/v7/weather/now?key=".concat(KEY).concat("&location=");
        String getNowAirURL = "https://devapi.qweather.com/v7/air/now?key=".concat(KEY).concat("&location=");
        String getFutureURL = "https://devapi.qweather.com/v7/weather/3d?key=".concat(KEY).concat("&location=");

        // Not found exception
        String idString = HttpUtils.doGet(getIdURL);
        if(idString.isEmpty()) idString = HttpUtils.doGet(getIdURL);
        JSONObject cityRes = JSON.parseObject(idString);
        if(cityRes.getString("code").equals("404"))
            throw new Exception("未找到该城市");

        boolean inChina = cityRes.getJSONArray("location").getJSONObject(0).getString("country").equals("中国");

        // Concat Urls
        JSONObject id = cityRes.getJSONArray("location").getJSONObject(0);
        getNowURL = getNowURL.concat(id.get("id").toString());
        getNowAirURL = getNowAirURL.concat(id.get("id").toString());
        getFutureURL = getFutureURL.concat(id.get("id").toString());

        try{
            String nowRes = HttpUtils.doGet(getNowURL);
            if(nowRes.isEmpty()) nowRes = HttpUtils.doGet(getNowURL);
            String futureRes = HttpUtils.doGet(getFutureURL);
            if(futureRes.isEmpty()) futureRes = HttpUtils.doGet(getFutureURL);

            JSONObject now = JSON.parseObject(nowRes).getJSONObject("now");
            JSONArray future = JSON.parseObject(futureRes).getJSONArray("daily");
            JSONObject tomorrow = future.getJSONObject(1);
            JSONObject afterTomorrow = future.getJSONObject(2);

            if(inChina){
                String nowAirRes = HttpUtils.doGet(getNowAirURL);
                if(nowAirRes.isEmpty()) nowAirRes = HttpUtils.doGet(getNowAirURL);
                JSONObject nowAir = JSON.parseObject(nowAirRes).getJSONObject("now");
                msg.append(String.format("%s现在的天气为%s，气温%s摄氏度，体感温度%s摄氏度，%s%s级，空气质量%s，空气质量指数%s，气压%s百帕\n",
                        id.get("name"), now.get("text"), now.get("temp"), now.get("feelsLike"), now.get("windDir"), now.get("windScale"), nowAir.get("category"), nowAir.get("aqi"), now.get("pressure")));
            } else {
                msg.append(String.format("%s现在的天气为%s，气温%s摄氏度，体感温度%s摄氏度，%s%s级，气压%s百帕\n",
                        id.get("name"), now.get("text"), now.get("temp"), now.get("feelsLike"), now.get("windDir"), now.get("windScale"), now.get("pressure")));
            }

            msg.append(String.format("%s明天白天的天气为%s，夜间天气为%s，最高温度%s摄氏度，最低温度%s摄氏度，%s%s级\n",
                    id.get("name"), tomorrow.get("textDay"), tomorrow.get("textNight"), tomorrow.get("tempMax"), tomorrow.get("tempMin"), tomorrow.get("windDirDay"), tomorrow.get("windScaleDay")));
            msg.append(String.format("%s后天白天的天气为%s，夜间天气为%s，最高温度%s摄氏度，最低温度%s摄氏度，%s%s级",
                    id.get("name"), afterTomorrow.get("textDay"), afterTomorrow.get("textNight"), afterTomorrow.get("tempMax"), afterTomorrow.get("tempMin"), afterTomorrow.get("windDirDay"), afterTomorrow.get("windScaleDay")));

            return msg.toString();
        } catch (Exception e){
            throw new Exception("查询天气信息失败，请稍后重试");
        }
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        if(args.length < 1) throw new InvalidSyntaxException();
        return new MessageChainBuilder().append(WeatherMessage(args[0].contentToString())).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("weather", Weather.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("天气", Weather.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("weather", "#weather <城市名> - 查询指定城市的天气");
        usages.put("天气", "#天气 <城市名> - 查询指定城市的天气");

        info.put("name", "weather");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "查天气");
        info.put("version", "1.3");

        return info;
    }

    public Weather(){}
}
