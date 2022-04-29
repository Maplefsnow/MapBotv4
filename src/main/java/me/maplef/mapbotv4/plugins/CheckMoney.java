package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CheckMoney implements MapbotPlugin {
    static final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    static final FileConfiguration messages = Main.getInstance().getMessageConfig();
    private static final Long opGroup = config.getLong("op-group");

    private static final Economy econ = Main.getEconomy();

    public static double check(Object[] args) throws InvalidSyntaxException, PlayerNotFoundException, SQLException {
        if(args[0].toString().isEmpty()) throw new InvalidSyntaxException();

        String UUIDString = (String) DatabaseOperator.query(args[0]).get("UUID");

        UUID uuid = UUID.fromString(UUIDString);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        return econ.getBalance(player);
    }

    public static double check(Object arg) throws PlayerNotFoundException, SQLException {
        String UUIDString = (String) DatabaseOperator.query(arg).get("UUID");

        UUID uuid = UUID.fromString(UUIDString);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        return econ.getBalance(player);
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception{
        MessageChainBuilder msg = new MessageChainBuilder();
        String playerName;

        if(Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID) && args.length > 0){
            playerName = (String) DatabaseOperator.query(args[0]).get("NAME");
            msg.append(String.format("%s 的%s为 %.1f", playerName, messages.getString("currency-name"), check(args)));
        } else {
            playerName = (String) DatabaseOperator.query(senderID).get("NAME");
            msg.append(String.format("%s 的%s为 %.1f", playerName, messages.getString("currency-name"), check(senderID)));
        }

        return msg.build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("money", CheckMoney.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("钱钱", CheckMoney.class.getMethod("onEnable", Long.class, Long.class, String[].class));

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
