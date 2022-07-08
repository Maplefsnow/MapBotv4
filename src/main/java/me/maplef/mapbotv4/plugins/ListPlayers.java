package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListPlayers implements MapbotPlugin {
    static FileConfiguration messages = Main.getInstance().getMessageConfig();

    public static List<String> list(){
        List<String> onlineList = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers())
            onlineList.add(player.getName());

        return onlineList;
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, Message[] args){
        List<String> onlineList = list();

        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(String.format("%s当前在线 %d 人：\n", messages.getString("server-name"), onlineList.size()));
        for(String playerName : onlineList)
            msgBuilder.append(playerName).append(", ");

        String msg = msgBuilder.toString();
        msg = msg.substring(0, msg.length()-2);

        return new MessageChainBuilder().append(msg).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("list", ListPlayers.class.getMethod("onEnable", Long.class, Long.class, Message[].class));
        commands.put("在线", ListPlayers.class.getMethod("onEnable", Long.class, Long.class, Message[].class));

        usages.put("list", "#list - 获取在线玩家列表");
        usages.put("在线", "#在线 - 获取在线玩家列表");

        info.put("name", "ListPlayers");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取在线玩家列表");
        info.put("version", "1.3");

        return info;
    }
}
