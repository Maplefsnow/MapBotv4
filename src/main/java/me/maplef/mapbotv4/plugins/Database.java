package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class Database implements MapbotPlugin {
    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        ConfigManager configManager = new ConfigManager();
        FileConfiguration config = configManager.getConfig();
        if (!config.getLongList("super-admin-account").contains(senderID)) throw new NoPermissionException();

        if (args[0].contentToString().equals("查询")) {
            Map<String, Object> queryRes = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 4; i < args.length; i++) {
                sb.append(args[i].contentToString()).append(" ");
            }
            sb.deleteCharAt(sb.length() - 1);
            String textString = sb.toString();
            try {
                Statement stmt = new DatabaseOperator().getConnect().createStatement();
                ResultSet rs = stmt.executeQuery(textString);
                while (rs.next()) {
                    if(rs.getString(args[1].contentToString()).equals(args[2].contentToString())){
                        try {
                            if (rs.getString("CODE").equals("null")) continue;
                            if (rs.getString("CODE").equals("已退群")) continue;
                        } catch (SQLException ignored) {}
                        ResultSetMetaData data = rs.getMetaData();
                        for(int i = 1; i <= data.getColumnCount(); ++i)
                            queryRes.put(data.getColumnName(i), rs.getObject(data.getColumnName(i)));
                        break;
                    }
                }
                return MessageUtils.newChain(new At(senderID)).plus(" " + queryRes.get(args[3].contentToString()));
            } catch (SQLException e) {
                e.printStackTrace();
                return MessageUtils.newChain(new At(senderID)).plus(" 出现异常，请查看控制台报错信息");
            }
        } else if (args[0].contentToString().equals("执行")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i].contentToString()).append(" ");
            }
            sb.deleteCharAt(sb.length() - 1);
            String textString = sb.toString();
            Statement stmt = new DatabaseOperator().getConnect().createStatement();
            stmt.execute(textString);
            int count = stmt.getUpdateCount();
            return MessageUtils.newChain(new At(senderID)).plus(" 更新计数：" + count);
        } else throw new InvalidSyntaxException();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("database", Database.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("database", "#database <查询|执行> [已知列名] [值] [需要查询的列名] <数据库指令> - 执行或查询数据库");

        info.put("name", "database");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "LQ_Snow");
        info.put("description", "执行或查询数据库");
        info.put("version", "1.0");

        return info;
    }
}
