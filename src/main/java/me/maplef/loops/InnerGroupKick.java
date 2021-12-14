package me.maplef.loops;

import me.maplef.exceptions.PlayerNotFoundException;
import me.maplef.plugins.CheckOnlineTime;
import me.maplef.utils.BotOperator;
import me.maplef.utils.DatabaseOperator;
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
    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();
    private final Long opGroup = config.getLong("op-group");
    private final Long innerGroup = config.getLong("inner-player-group");
    final Bot bot = BotOperator.bot;

    @Override
    public void execute(JobExecutionContext context) {
        ContactList<NormalMember> inner_players = Objects.requireNonNull(bot.getGroup(innerGroup)).getMembers();
        List<String> kickList = new ArrayList<>();

        for(NormalMember member : inner_players) {
            try {
                String player_name = DatabaseOperator.query(member.getId()).get("NAME").toString();
                int weekly_online_time = CheckOnlineTime.check(player_name, 1);
                int total_online_time = CheckOnlineTime.check(player_name, 3);

                if(weekly_online_time < 400 && total_online_time < 20000) {
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
        for(String name : kickList)
            kickMsg.append(name).append(", ");
        String msg = kickMsg.toString();
        BotOperator.send(opGroup, msg.substring(0, msg.length()-2));
    }
}
