package me.maplef.plugins;

import me.clip.placeholderapi.PlaceholderAPI;
import me.maplef.MapbotPlugin;
import me.maplef.exceptions.GroupNotAllowedException;
import me.maplef.Main;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CheckTPS implements MapbotPlugin {
    static final FileConfiguration config = Main.getInstance().getConfig();
    static final FileConfiguration messages = Main.getInstance().getMessageConfig();
    private static final Long opGroup = config.getLong("op-group");
    private static final Long playerGroup = config.getLong("player-group");

    public static double check(){
        if(Bukkit.getServer().getOnlinePlayers().toArray().length == 0) return 20.0;

        String tpsStr = "%server_tps_1%";
        Player player = (Player) Bukkit.getServer().getOnlinePlayers().toArray()[0];
        tpsStr = PlaceholderAPI.setPlaceholders(player, tpsStr);

        return Double.parseDouble(tpsStr);
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception{
        if(!Objects.equals(groupID, opGroup) && !Objects.equals(groupID, playerGroup))
            throw new GroupNotAllowedException();

        String msg = String.format("当前%s服务器tps为：%.1f", messages.getString("server-name"), check());
        return new MessageChainBuilder().append(msg).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("tps", CheckTPS.class.getMethod("onEnable", Long.class, Long.class, String[].class));

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
