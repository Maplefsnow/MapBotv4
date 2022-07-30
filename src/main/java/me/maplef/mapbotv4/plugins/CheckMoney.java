package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CheckMoney implements MapbotPlugin {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    FileConfiguration messages = configManager.getMessageConfig();

    private static final Economy econ = Main.getEconomy();

    public static double check(Object[] args) throws InvalidSyntaxException, PlayerNotFoundException, SQLException {
        if(args[0].toString().isEmpty()) throw new InvalidSyntaxException();

        String UUIDString = (String) DatabaseOperator.queryPlayer(args[0]).get("UUID");

        UUID uuid = UUID.fromString(UUIDString);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        return econ.getBalance(player);
    }

    public static double check(Object arg) throws PlayerNotFoundException, SQLException {
        String UUIDString = (String) DatabaseOperator.queryPlayer(arg).get("UUID");

        UUID uuid = UUID.fromString(UUIDString);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        return econ.getBalance(player);
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        long opGroup = config.getLong("op-group");

        MessageChainBuilder msg = new MessageChainBuilder();
        String playerName;

        if(Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID) && args.length > 0){
            playerName = (String) DatabaseOperator.queryPlayer(args[0].contentToString()).get("NAME");
            msg.append(String.format("%s 的%s为 %.1f", playerName, messages.getString("currency-name"), check(args[0].contentToString())));
        } else {
            playerName = (String) DatabaseOperator.queryPlayer(senderID).get("NAME");
            msg.append(String.format("%s 的%s为 %.1f", playerName, messages.getString("currency-name"), check(senderID)));
        }

        return msg.build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("money", CheckMoney.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("钱钱", CheckMoney.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("money", "#money - 查询自己的货币数");
        usages.put("钱钱", "#钱钱 - 查询自己的货币数");

        info.put("name", "CheckMoney");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "查询货币");
        info.put("version", "1.3");

        return info;
    }
}
