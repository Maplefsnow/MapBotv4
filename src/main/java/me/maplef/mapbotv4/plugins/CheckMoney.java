package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CheckMoney implements MapbotPlugin {
    static final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    private static final Long opGroup = config.getLong("op-group");

    public static double check(Object[] args) throws InvalidSyntaxException, PlayerNotFoundException, SQLException {
        if(args[0].toString().isEmpty()) throw new InvalidSyntaxException();

        String url = "jdbc:sqlite:.\\plugins\\XConomy\\playerdata\\data.db";

        String fixedName = (String) DatabaseOperator.query(args[0]).get("NAME");

        double money;
        try (Connection c = DriverManager.getConnection(url);
             Statement stmt = c.createStatement();
             ResultSet res = stmt.executeQuery(String.format("SELECT * FROM xconomy WHERE player = '%s';", fixedName))){
            money = res.getDouble("balance");
        }
        if(money == 0)
            throw new PlayerNotFoundException();
        else return money;
    }

    public static double check(Object arg) throws PlayerNotFoundException, SQLException {
        String url = "jdbc:sqlite:.\\plugins\\XConomy\\playerdata\\data.db";

        String fixedName = (String) DatabaseOperator.query(arg).get("NAME");

        double money;
        try (Connection c = DriverManager.getConnection(url);
             Statement stmt = c.createStatement();
             ResultSet res = stmt.executeQuery(String.format("SELECT * FROM xconomy WHERE player = '%s';", fixedName))){
            if(res == null) throw new PlayerNotFoundException();
            money = res.getDouble("balance");
        }

        return money;
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception{
        MessageChainBuilder msg = new MessageChainBuilder();
        String playerName;

        if(Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID) && args.length > 0){
            playerName = (String) DatabaseOperator.query(args[0]).get("NAME");
            msg.append(String.format("%s 的猫猫积分为 %.1f", playerName, check(args)));
        } else {
            playerName = (String) DatabaseOperator.query(senderID).get("NAME");
            msg.append(String.format("%s 的猫猫积分为 %.1f", playerName, check(senderID)));
        }

        return msg.build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("money", CheckMoney.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("积分", CheckMoney.class.getMethod("onEnable", Long.class, Long.class, String[].class));

        usages.put("money", "#money - 查询自己的猫猫积分");
        usages.put("积分", "#积分 - 查询自己的猫猫积分");

        info.put("name", "CheckMoney");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "查询猫猫积分");
        info.put("version", "1.3");

        return info;
    }

    public CheckMoney(){}
}
