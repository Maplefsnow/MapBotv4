package me.maplef.mapbotv4.listeners;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.utils.BotOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberJoinRequestEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

public class CheckInGroupListeners extends SimpleListenerHost {
    final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

    private final Long checkInGroup = config.getLong("check-in-group");
    private final Long opGroup = config.getLong("op-group");

    @EventHandler
    public void onJoinGroupRequest(MemberJoinRequestEvent e){
        if(!config.getBoolean("check-in-group-auto-manage.enable")) return;
        if(e.getGroupId() != checkInGroup) return;

        Bot bot = BotOperator.getBot();
        Stranger stranger = bot.getStranger(e.getFromId());
        if(stranger == null){
            BotOperator.sendGroupMessage(opGroup, String.format("%s 申请进入审核群，无法检测该账号 QQ 等级，请手动操作", e.getFromNick()));
            return;
        }

        if(stranger.queryProfile().getQLevel() < config.getInt("check-in-group-auto-manage.minimum-QQ-level"))
            e.reject(false, "因QQ等级过低判定为小号，请使用大号入群");
        else
            e.accept();
    }

    @EventHandler
    public void onNewCome(MemberJoinEvent e){
        if(!config.getBoolean("check-in-group-auto-manage.enable")) return;
        if(e.getGroupId() != checkInGroup) return;

        MessageChainBuilder newComeMsg = new MessageChainBuilder();
        newComeMsg.append(new At(e.getMember().getId())).append(" ").append(messages.getString("welcome-new-message.check-in-group.group"));

        BotOperator.sendGroupMessage(checkInGroup, newComeMsg.build());
    }
}
