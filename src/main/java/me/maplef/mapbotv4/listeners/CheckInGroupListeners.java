package me.maplef.mapbotv4.listeners;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.utils.BotOperator;
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

    @EventHandler
    public void onJoinGroupRequest(MemberJoinRequestEvent e){
        if(!config.getBoolean("check-in-group-auto-manage.enable")) return;
        if(e.getGroupId() != checkInGroup) return;

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
