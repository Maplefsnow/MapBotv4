package me.maplef.mapbotv4.commands;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.plugins.BotQQOperator;
import me.maplef.mapbotv4.plugins.Hitokoto;
import me.maplef.mapbotv4.plugins.StopServer;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Mapbot implements CommandExecutor, TabExecutor {
    private final FileConfiguration config = Main.getInstance().getConfig();
    private final FileConfiguration messages = Main.getInstance().getMessageConfig();
    private final String msgHeader = "&b&l============ &d小枫4号 &b&l============&f\n";
    private final String msgFooter = "\n&b&l==============================";

    private final Economy econ = Main.getEconomy();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String msgStart = messages.getString("message-prefix");

        if(args.length == 0 || args[0].equals("help")){
            if(sender instanceof Player){
                Player player = (Player) sender;
                player.sendMessage(getHelpMessage());
            } else {
                Bukkit.getServer().getLogger().info(getHelpMessage());
            }
            return true;
        }

        switch (args[0]){
            case "hitokoto": {
                String msg = msgHeader + Hitokoto.HitokotoMessage() + msgFooter;
                if(sender instanceof Player){
                    Player player = (Player) sender;
                    player.sendMessage(CU.t(msg));
                } else {
                    Bukkit.getServer().getLogger().info(CU.t(msg));
                }
                break;
            }

            case "keepinv": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                try {
                    int keepinvFlag = (Integer) DatabaseOperator.query(player.getName()).get("KEEPINV");
                    if(keepinvFlag == 1){
                        DatabaseOperator.executeCommand(String.format("UPDATE PLAYER SET KEEPINV = 0 WHERE NAME = '%s';", player.getName()));
                        player.sendMessage(CU.t(msgStart + "积分兑换死亡不掉落功能已 &4&l关闭"));
                    } else {
                        DatabaseOperator.executeCommand(String.format("UPDATE PLAYER SET KEEPINV = 1 WHERE NAME = '%s';", player.getName()));
                        player.sendMessage(CU.t(msgStart + "积分兑换死亡不掉落功能已 &a&l开启"));
                    }
                    return true;
                } catch (SQLException | PlayerNotFoundException e) {
                    e.printStackTrace();
                    player.sendMessage(msgStart + "发生了亿点点错误...");
                    return true;
                }
            }

            case "receive": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                Connection c = DatabaseOperator.c;
                try (Statement stmt = c.createStatement();
                    ResultSet res = stmt.executeQuery(String.format("SELECT * FROM PLAYER WHERE NAME = '%s';", player.getName()))){
                        if(res.getBoolean("MSGREC")){
                            stmt.executeUpdate(String.format("UPDATE PLAYER SET MSGREC = 0 WHERE NAME = '%s';", player.getName()));
                            player.sendMessage(CU.t(msgStart + "你 &4&l关闭 &b了群消息接收"));
                        } else {
                            stmt.executeUpdate(String.format("UPDATE PLAYER SET MSGREC = 1 WHERE NAME = '%s';", player.getName()));
                            player.sendMessage(CU.t(msgStart + "你 &a&l开启 &b了群消息接收"));
                        }
                    return true;
                } catch (Exception e){
                    Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
                    return false;
                }
            }

            case "haste": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                if(args.length != 2){
                    player.sendMessage(getHelpMessage());
                    return true;
                }

                double playerMoney = econ.getBalance(player); int time;

                try{
                    time = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    player.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                    return true;
                }

                double cost = time * config.getDouble("haste-per-minute-cost");
                if(playerMoney < cost){
                    player.sendMessage(CU.t(msgStart + String.format("兑换 &e%d &b分钟的急迫V共需要 &e%.1f &b猫猫积分，&4你没有足够的积分", time, cost)));
                    return true;
                }

                EconomyResponse r = econ.withdrawPlayer(player, cost);
                if(r.transactionSuccess()){
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            String.format("effect give %s minecraft:haste %d 4", player.getName(), time * 60));
                    player.sendMessage(CU.t(msgStart + String.format("&a成功&b兑换 &e%d &b分钟的急迫V效果", time)));
                } else {
                    player.sendMessage(CU.t(msgStart + "&4发生错误：" + r.errorMessage));
                }

                return true;
            }

            case "stopserver":{
                if(sender instanceof Player){
                    Player player = (Player) sender;
                    if(!player.hasPermission("mapbot.stopserver")){
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }
                }

                if(args.length == 1){
                    StopServer.stopLater(60);
                    if(sender instanceof Player){
                        Player player = (Player) sender;
                        player.sendMessage(CU.t(msgStart + "停服定时任务已&a启动"));
                    } else {
                        Bukkit.getServer().getLogger().info("停服定时任务已启动");
                    }
                    return true;
                }

                int time;
                try{
                    time = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    if(sender instanceof Player){
                        Player player = (Player) sender;
                        player.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                    } else {
                        Bukkit.getServer().getLogger().info("请输入一个整数");
                    }
                    return true;
                }

                if(time < 30){
                    if(sender instanceof Player){
                        Player player = (Player) sender;
                        player.sendMessage(CU.t(msgStart + "&c请设定一个不小于30秒的倒计时"));
                    } else {
                        Bukkit.getServer().getLogger().info("请设定一个不小于30秒的倒计时");
                    }
                    return true;
                }

                StopServer.stopLater(time);
                if(sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(CU.t(msgStart + "停服定时任务已&a启动"));
                } else {
                    Bukkit.getServer().getLogger().info(CU.t("停服定时任务已&a启动"));
                }
                return true;
            }

            case "cancelstopserver":{
                if(sender instanceof Player){
                    Player player = (Player) sender;
                    if(!player.hasPermission("mapbot.stopserver")){
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }
                }

                if(StopServer.stopCancel()){
                    if(sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage(CU.t(msgStart + "停服定时任务已&c取消"));
                    } else {
                        Bukkit.getServer().getLogger().info(CU.t( "停服定时任务已&c取消"));
                    }
                } else {
                    if(sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage(CU.t(msgStart + "&c没有正在进行的停服计划"));
                    } else {
                        Bukkit.getServer().getLogger().info(CU.t( "&c没有正在进行的停服计划"));
                    }
                }
                return true;
            }

            case "login":{
                if(sender instanceof Player){
                    Player player = (Player) sender;
                    if(!player.hasPermission("mapbot.login")){
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }
                }

                BotQQOperator.login();

                return true;
            }

            default: {
                if(sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(CU.t(msgStart + "未知的指令"));
                } else {
                    Bukkit.getServer().getLogger().info(CU.t("未知的指令"));
                }
            } break;
        }

        return true;
    }

    private String getHelpMessage(){
        String msg = msgHeader +
                "&a[帮助菜单]\n" +
                "&e/mapbot help &f- 显示此菜单\n" +
                "&e/mapbot hitokoto &f- 获取一言\n" +
                "&e/mapbot keepinv &f- 切换是否自动使用猫猫积分免疫死亡掉落\n" +
                "&e/mapbot receive &f- 切换是否接收群消息\n" +
                "&e/mapbot haste &f- 积分购买急迫V\n" +
                msgFooter;
        return CU.t(msg);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if(args.length == 1){
            String[] allCommands = {"help", "hitokoto", "keepinv", "receive", "haste", "stopserver", "cancelstopserver", "login"};

            List<String> commandList = new ArrayList<>();
            for(String commandName : allCommands)
                if(sender.hasPermission("mapbot." + commandName))
                    commandList.add(commandName);

            return commandList;
        }
        return null;
    }
}
