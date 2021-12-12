package me.maplef.loops;

import me.maplef.plugins.CheckOnlineTime;
import me.maplef.utils.BotOperator;
import me.maplef.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class InnerGroupInvite implements Job {
    public static String inviteCode = "m@plef_233";

    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();
    private final Long opGroup = config.getLong("op-group");
    private final Long playerGroupID = config.getLong("player-group");
    private final Long innerGroupID = config.getLong("inner-player-group");
    final Bot bot = BotOperator.bot;

    @Override
    public void execute(JobExecutionContext context){
        inviteCode = RandomStringUtils.randomAlphanumeric(6);
        Connection c = DatabaseOperator.c;

        List<String> kickList = new ArrayList<>();
        List<String> inviteList = new ArrayList<>();
        List<String> whiteList = config.getStringList("inner-player-group-whitelist");

        try (Statement stmt = c.createStatement();
             ResultSet res = stmt.executeQuery("SELECT * FROM PLAYER;")){
            while(res.next()){
                String playerName = res.getString("NAME");
                long playerQQ = Long.parseLong(res.getString("QQ"));
                if(whiteList.contains(playerName)) continue;

                int weeklyTime = CheckOnlineTime.check(playerName, 1);
                int totalTime = CheckOnlineTime.check(playerName, 3);

                if(weeklyTime / 7 < config.getInt("isActive-time-per-day") && totalTime < 10000 && Objects.requireNonNull(bot.getGroup(innerGroupID)).contains(playerQQ)){
                    Objects.requireNonNull(Objects.requireNonNull(bot.getGroup(innerGroupID)).get(playerQQ)).kick("你因为当周活跃未达标，被移出本群qwq");
                    kickList.add(playerName);
                }

                if(weeklyTime / 7 >= config.getInt("isActive-time-per-day")){
                    if(!Objects.requireNonNull(bot.getGroup(innerGroupID)).contains(playerQQ)){
                        String inviteGreeting = String.format("你好, %s, 这里是小枫4号\n" +
                                "在上一周的巡视中，我很高兴看到猫猫大陆又多出了一位像你一样的活跃玩家，感谢你为猫猫大陆作出的卓越贡献\n" +
                                "鉴于你在服务器内的活跃行为，特此邀请你加入猫猫大陆内群，和更多的活跃玩家一起交流\n" +
                                "同时，猫猫大陆管理组也会更重视内群玩家的意见和反馈并及时作出回应\n" +
                                "请拿好此唯一凭证，勿将此凭证告知他人\n" +
                                "这是猫猫大陆内群群号: %d" +
                                "猫猫大陆内群欢迎你的到来", playerName, innerGroupID);

                        Bukkit.getLogger().info(playerQQ + "");

                        try{
                            bot.getStrangerOrFail(playerGroupID).sendMessage(inviteGreeting);
                            bot.getStrangerOrFail(playerGroupID).sendMessage("这是你的凭证");
                            bot.getStrangerOrFail(playerGroupID).sendMessage(inviteCode);
                            inviteList.add(playerName);
                        } catch (NoSuchElementException e){
                            Bukkit.getLogger().warning(e.getClass() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception exception){
            exception.printStackTrace();
        }

        String kickMsg = String.format("已从内群中移出了 %d 位不活跃玩家: \n", kickList.size());
        for(String name : kickList)
            kickMsg = kickMsg.concat(name).concat(", ");
        kickMsg = kickMsg.substring(0, kickMsg.length() - 2);
        BotOperator.send(opGroup, kickMsg);

        String inviteMsg = String.format("已邀请 %d 位活跃玩家进入内群: \n", inviteList.size());
        for(String name : inviteList)
            inviteMsg = inviteMsg.concat(name).concat(", ");
        inviteMsg = inviteMsg.substring(0, inviteMsg.length() - 2);
        inviteMsg = inviteMsg.concat("\n本批次入群凭证为: ").concat(inviteCode);
        BotOperator.send(opGroup, inviteMsg);
    }
}
