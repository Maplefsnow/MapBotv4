package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CheckLocation implements MapbotPlugin {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    private final Long opGroup = config.getLong("op-group");

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        if(args.length < 1) throw new InvalidSyntaxException();
        if(!Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID)) throw new NoPermissionException();

        String targetPlayer = args[0].contentToString();

        String playerInfo;
        String msg = "";
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(player.getName().toLowerCase(Locale.ROOT).contains(targetPlayer) || targetPlayer.equals("*") || targetPlayer.equals("all")){
                playerInfo = String.format("[%s] %s (%d, %d, %d)", player.getName(),
                        Objects.requireNonNull(player.getLocation().getWorld()).getName(),
                        player.getLocation().getBlockX(),
                        player.getLocation().getBlockY(),
                        player.getLocation().getBlockZ());

                msg = msg.concat(playerInfo).concat("\n\n");
            }
        }

        if(msg.isEmpty())
            return new MessageChainBuilder().append("找不到该玩家或该玩家不在线").build();

        msg = msg.substring(0, msg.length() - 2);
        return MessageUtils.newChain(new PlainText(msg));
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("location", CheckLocation.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("位置", CheckLocation.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("location", "#location <玩家ID> - 获取指定玩家位置");
        usages.put("位置", "#位置 <玩家ID> - 获取指定玩家位置");

        info.put("name", "CheckLocation");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取指定玩家位置");
        info.put("version", "1.0");

        return info;
    }
}
