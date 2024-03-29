package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CheckOnlineTime implements MapbotPlugin {
    private static final FileConfiguration onlineTimes = Main.getInstance().getOnlineTimeConfig();

    public static int check(String playerUUID, int mode) throws SQLException, PlayerNotFoundException {
        int dailyTime = onlineTimes.getInt(playerUUID.concat(".daily_play_time")) / 60000;
        int weeklyTime = onlineTimes.getInt(playerUUID.concat(".weekly_play_time")) / 60000;
        int monthlyTime = onlineTimes.getInt(playerUUID.concat(".monthly_play_time")) / 60000;
        int totalTime = onlineTimes.getInt(playerUUID.concat(".total_play_time")) / 60000;

        return switch (mode) {
            case 0 -> dailyTime;
            case 1 -> weeklyTime;
            case 2 -> monthlyTime;
            case 3 -> totalTime;
            default -> -1;
        };
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        if(args.length < 1) throw new InvalidSyntaxException();

        String fixedName = (String) DatabaseOperator.queryPlayer(args[0].contentToString()).get("NAME"); int onlineTime;
        String playerUUID = (String) DatabaseOperator.queryPlayer(args[0].contentToString()).get("UUID");
        String[] timeText = {"今天", "本周", "本月", "总计"};

        int mode = 0;
        if(args.length > 1){
            try{
                mode = Integer.parseInt(args[1].contentToString());
            } catch (NumberFormatException e){
                throw new Exception("请输入一个整数");
            }
        }

        onlineTime = check(playerUUID, mode);

        return new MessageChainBuilder().append(String.format("%s %s的在线时长为 %d 分钟", fixedName, timeText[mode], onlineTime)).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("onlinetime", CheckOnlineTime.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("在线时长", CheckOnlineTime.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("onlinetime", "#onlinetime <玩家ID> [时段] - 查询玩家在线时长\n" +
                "其中时段一项为可选参数，0：当天，1：当周，2：当月，3：总计");
        usages.put("在线时长", "#在线时长 <玩家ID> [时段] - 查询玩家在线时长\n" +
                "其中时段一项为可选参数，0：当天，1：当周，2：当月，3：总计");

        info.put("name", "CheckOnlineTime");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "查询在线时长");
        info.put("version", "1.2");

        return info;
    }

    public CheckOnlineTime(){}
}
