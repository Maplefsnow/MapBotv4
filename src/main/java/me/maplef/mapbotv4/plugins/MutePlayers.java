package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.PermissionDeniedException;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MutePlayers implements MapbotPlugin {
    Bot bot = BotOperator.getBot();

    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    private final Long opGroup = config.getLong("op-group");
    private final Long playerGroup = config.getLong("player-group");

    private void mute(String playerName, Long playerQQ, int muteTime) throws Exception{
        Member playerMember = Objects.requireNonNull(bot.getGroup(playerGroup)).get(playerQQ);
        if(playerMember == null) throw new Exception("QQ群内未找到该玩家");
        playerMember.mute(muteTime * 60);

        String commandStr = String.format("mute %s %dm", playerName, muteTime);
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr));
    }

    private void unmute(String playerName, Long playerQQ) throws Exception{
        String commandStr = String.format("mute %s", playerName);

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr));

        Member playerMember = Objects.requireNonNull(bot.getGroup(playerGroup)).get(playerQQ);
        if(playerMember == null) throw new Exception("QQ群内未找到该玩家");
        Objects.requireNonNull(Objects.requireNonNull(bot.getGroup(playerGroup)).get(playerQQ)).unmute();

    }

    public MessageChain onMute(Long groupID, Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(senderID))
            throw new NoPermissionException();
        if(args.length < 2)
            throw new InvalidSyntaxException();

        int muteTime;
        try {
            muteTime = Integer.parseInt(args[1].contentToString());
        } catch (NumberFormatException e){
            throw new Exception("请输入一个整数");
        }

        String playerName = DatabaseOperator.queryPlayer(args[0].contentToString()).get("NAME").toString();
        long playerQQ = Long.parseLong(DatabaseOperator.queryPlayer(args[0].contentToString()).get("QQ").toString());

        try {
            mute(playerName, playerQQ, muteTime);
        } catch (PermissionDeniedException ex) {
            return MessageUtils.newChain(new At(senderID), new PlainText(String.format(" 玩家 %s 在Q群中拥有更高权限，无法禁言", playerName)));
        }

        return MessageUtils.newChain(new At(senderID), new PlainText(String.format(" 禁言了 %s %d 分钟", playerName, muteTime)));
    }

    public MessageChain onUnMute(Long groupID, Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(senderID))
            throw new NoPermissionException();
        if(args.length < 1)
            throw new InvalidSyntaxException();

        String playerName = DatabaseOperator.queryPlayer(args[0].contentToString()).get("NAME").toString();
        long playerQQ = Long.parseLong(DatabaseOperator.queryPlayer(args[0].contentToString()).get("QQ").toString());

        unmute(playerName, playerQQ);

        return MessageUtils.newChain(new At(senderID), new PlainText(String.format(" 解除了 %s 的禁言", playerName)));
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        return null;
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("mute", MutePlayers.class.getMethod("onMute", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("禁言", MutePlayers.class.getMethod("onMute", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("unmute", MutePlayers.class.getMethod("onUnMute", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("解除禁言", MutePlayers.class.getMethod("onUnMute", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("mute", "#mute <player> <minutes> - 在群和游戏内禁言某个玩家");
        usages.put("禁言", "#禁言 <玩家> <分钟数> - 在群和游戏内禁言某个玩家");
        usages.put("unmute", "#unmute <玩家> <分钟数> - 解除某个玩家的禁言");
        usages.put("解除禁言", "#解除禁言 <玩家> <分钟数> - 解除某个玩家的禁言");

        info.put("name", "MutePlayers");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "禁言玩家");
        info.put("version", "1.0");

        return info;
    }
}
