package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.GroupNotAllowedException;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CheckTPS implements MapbotPlugin {
    static final FileConfiguration config = Main.getInstance().getConfig();
    static final FileConfiguration messages = Main.getInstance().getMessageConfig();
    private static final Long opGroup = config.getLong("op-group");
    private static final Long playerGroup = config.getLong("player-group");

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, Message[] args) throws Exception{
        if(!Objects.equals(groupID, opGroup) && !Objects.equals(groupID, playerGroup))
            throw new GroupNotAllowedException();

        String msg = String.format("当前%s服务器tps为：%.1f", messages.getString("server-name"), Bukkit.getServer().getTPS()[0]);
        return new MessageChainBuilder().append(msg).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("tps", CheckTPS.class.getMethod("onEnable", Long.class, Long.class, Message[].class));

        usages.put("tps", "#tps - 查询服务器当前tps");

        info.put("name", "CheckTPS");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "查询tps");
        info.put("version", "1.1");

        return info;
    }

}
