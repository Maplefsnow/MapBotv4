package me.maplef.mapbotv4.loops;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.plugins.CheckOnlineTime;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.contact.PermissionDeniedException;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InnerGroupKick implements Job {
    final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    private final Long opGroup = config.getLong("op-group");
    private final Long innerGroup = config.getLong("inner-player-group");
    final Bot bot = BotOperator.getBot();

    @Override
    public void execute(JobExecutionContext context) {
        ContactList<NormalMember> inner_players = Objects.requireNonNull(bot.getGroup(innerGroup)).getMembers();
        List<String> kickList = new ArrayList<>();
        List<String> whitelist = config.getStringList("inner-player-group-auto-manage.kick.whitelist");

        for(NormalMember member : inner_players) {
            try {
                String player_name = DatabaseOperator.queryPlayer(member.getId()).get("NAME").toString();

                if(whitelist.contains(player_name)) continue;

                int weekly_online_time = CheckOnlineTime.check(player_name, 1);
                int total_online_time = CheckOnlineTime.check(player_name, 3);

                if(weekly_online_time < config.getInt("inner-player-group-auto-manage.kick.requirement") && total_online_time < 20000) {
                    try{
                        member.kick("你因为当周活跃未达标被移出本群，请再接再厉吧qwq");
                        kickList.add(player_name);
                    } catch (PermissionDeniedException ignored){}
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (PlayerNotFoundException ignored) {}
        }

        StringBuilder kickMsg = new StringBuilder(String.format("从内群中移出了 %d 名玩家：\n", kickList.size()));
        for(String name : kickList) kickMsg.append(name).append(", ");
        String msg = kickMsg.toString();
        BotOperator.sendGroupMessage(opGroup, msg.substring(0, msg.length()-2));
    }
}
