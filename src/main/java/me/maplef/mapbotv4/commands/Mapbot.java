package me.maplef.mapbotv4.commands;

import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.plugins.BotQQOperator;
import me.maplef.mapbotv4.plugins.Hitokoto;
import me.maplef.mapbotv4.plugins.StopServer;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Mapbot implements CommandExecutor, TabExecutor {
    ConfigManager configManager = new ConfigManager();

    private final String msgHeader = "&b&l============ &d小枫4号 &b&l============&f\n";
    private final String msgFooter = "\n&b&l==============================";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        FileConfiguration messages = configManager.getMessageConfig();
        String msgStart = messages.getString("message-prefix");

        if(args.length == 0 || args[0].equals("help")){
            sender.sendMessage(getHelpMessage());
            return true;
        }

        switch (args[0]) {
            case "hitokoto" -> {
                String msg = msgHeader + Hitokoto.HitokotoMessage() + msgFooter;
                sender.sendMessage(CU.t(msg));
            }
            case "receive" -> {
                if (!(sender instanceof Player)) {
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                try (Connection c = new DatabaseOperator().getConnect();
                     Statement stmt = c.createStatement();
                     ResultSet res = stmt.executeQuery(String.format("SELECT * FROM PLAYER WHERE NAME = '%s';", player.getName()))) {
                    if (res.getBoolean("MSGREC")) {
                        stmt.executeUpdate(String.format("UPDATE PLAYER SET MSGREC = 0 WHERE NAME = '%s';", player.getName()));
                        player.sendMessage(CU.t(msgStart + "你 &4&l关闭 &b了群消息接收"));
                    } else {
                        stmt.executeUpdate(String.format("UPDATE PLAYER SET MSGREC = 1 WHERE NAME = '%s';", player.getName()));
                        player.sendMessage(CU.t(msgStart + "你 &a&l开启 &b了群消息接收"));
                    }
                    return true;
                } catch (Exception e) {
                    Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
                    return false;
                }
            }
            case "reload" -> {
                if(!sender.hasPermission("mapbot.reload")){
                    sender.sendMessage((CU.t(msgStart + "&c你没有使用该命令的权限")));
                    return true;
                }

                ConfigManager.reloadConfig("config.yml");
                ConfigManager.reloadConfig("messages.yml");
                ConfigManager.reloadConfig("auto_reply.yml");

                sender.sendMessage((CU.t(msgStart + "配置文件重载完毕")));
                return true;
            }
            case "stopserver" -> {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission("mapbot.stopserver")) {
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }
                }

                if (args.length == 1) {
                    new StopServer().stopLater(60);
                    sender.sendMessage((CU.t(msgStart + "停服定时任务已&a启动")));
                    return true;
                }

                int time;
                try {
                    time = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                    return true;
                }

                if (time < 30) {
                    sender.sendMessage(CU.t(msgStart + "&c请设定一个不小于30秒的倒计时"));
                    return true;
                }

                new StopServer().stopLater(time);
                sender.sendMessage(CU.t(msgStart + "停服定时任务已&a启动"));
                return true;
            }
            case "cancelstopserver" -> {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission("mapbot.stopserver")) {
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }
                }

                if (new StopServer().stopCancel())
                    sender.sendMessage(CU.t(msgStart + "停服定时任务已&c取消"));
                else
                    sender.sendMessage(CU.t(msgStart + "&c没有正在进行的停服计划"));

                return true;
            }
            case "login" -> {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission("mapbot.login")) {
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }
                }

                BotQQOperator.login();

                return true;
            }
            default -> sender.sendMessage(CU.t(msgStart + "未知的指令"));
        }

        return true;
    }

    private String getHelpMessage(){
        String msg = msgHeader +
                "&a[帮助菜单]\n" +
                "&e/mapbot help &f- 显示此菜单\n" +
                "&e/mapbot hitokoto &f- 获取一言\n" +
                "&e/mapbot receive &f- 切换是否接收群消息\n" +
                msgFooter;
        return CU.t(msg);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if(args.length == 1){
            String[] allCommands = {"help", "hitokoto", "receive", "stopserver", "cancelstopserver", "login", "reload"};

            List<String> commandList = new ArrayList<>();
            for(String commandName : allCommands)
                if(sender.hasPermission("mapbot." + commandName))
                    commandList.add(commandName);

            return commandList;
        }
        return null;
    }
}
