package me.maplef.mapbotv4.commands;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.plugins.CheckMoney;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Mapbot implements CommandExecutor, TabExecutor {
    private final FileConfiguration config = Main.getInstance().getConfig();
    private final FileConfiguration messages = Main.getInstance().getMessageConfig();
    private final String msgHeader = "&b&l============ &d小枫4号 &b&l============&f\n";
    private final String msgFooter = "\n&b&l==============================";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String msgStart = messages.getString("message-prefix");
        if(sender instanceof Player){
            Player player = (Player) sender;
            if(args.length == 0 || args[0].equals("help")){
                player.sendMessage(getHelpMessage());
                return true;
            }
            switch (args[0]){
                case "hitokoto": {
                    String msg = msgHeader + Hitokoto.HitokotoMessage() + msgFooter;
                    player.sendMessage(CU.t(msg));
                    break;
                }

                case "keepinv": {
                    try {
                        int keepinvFlag = (Integer) DatabaseOperator.query(player.getName()).get("KEEPINV");
                        if(keepinvFlag == 1){
                            DatabaseOperator.executeCommand(String.format("UPDATE PLAYER SET KEEPINV = 0 WHERE NAME = '%s';", player.getName()));
                            player.sendMessage(CU.t(msgStart + "积分换取死亡物品保留功能已 &4&l关闭"));
                        } else {
                            DatabaseOperator.executeCommand(String.format("UPDATE PLAYER SET KEEPINV = 1 WHERE NAME = '%s';", player.getName()));
                            player.sendMessage(CU.t(msgStart + "积分换取死亡物品保留功能已 &a&l开启"));
                        }
                        return true;
                    } catch (SQLException | PlayerNotFoundException e) {
                        e.printStackTrace();
                        player.sendMessage(msgStart + "发生了亿点点错误...");
                        return true;
                    }
                }

                case "receive": {
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
                    if(args.length != 2){
                        player.sendMessage(getHelpMessage());
                        return true;
                    }

                    double playerMoney;
                    int time;
                    try{
                        playerMoney = CheckMoney.check(player.getName());
                        time = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e){
                        player.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                        return true;
                    } catch (PlayerNotFoundException e){
                        e.printStackTrace();
                        return false;
                    } catch (SQLException e){
                        player.sendMessage(CU.t(msgStart + "&c数据库异常，请稍后再试"));
                        return true;
                    }

                    double cost = time * config.getDouble("haste-per-minute-cost");
                    if(playerMoney < cost){
                        player.sendMessage(CU.t(msgStart + String.format("兑换 &e%d &b分钟的急迫V共需要 &e%.1f &b猫猫积分，&4你没有足够的积分", time, cost)));
                        return true;
                    }

                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            String.format("effect give %s minecraft:haste %d 4", player.getName(), time * 60));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            String.format("money take %s %d", player.getName(), time * (int) config.getDouble("haste-per-minute-cost")));
                    player.sendMessage(CU.t(msgStart + String.format("&a成功&b兑换 &e%d &b分钟的急迫V效果", time)));

                    return true;
                }

                case "stopserver":{
                    if(!player.hasPermission("mapbot.stopserver")){
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }

                    if(args.length == 1){
                        StopServer.stopLater(60);
                        player.sendMessage(CU.t(msgStart + "停服定时任务已&a启动"));
                        return true;
                    }

                    int time;
                    try{
                        time = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e){
                        player.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                        return true;
                    }

                    if(time < 30){
                        player.sendMessage(CU.t(msgStart + "&c请设定一个不小于30秒的倒计时"));
                        return true;
                    }

                    StopServer.stopLater(time);
                    player.sendMessage(CU.t(msgStart + "停服定时任务已&a启动"));
                    return true;
                }

                case "cancelstopserver":{
                    if(!player.hasPermission("mapbot.stopserver")){
                        player.sendMessage(CU.t(msgStart + "&c你没有使用该命令的权限"));
                        return true;
                    }

                    if(StopServer.stopCancel()){
                        player.sendMessage(CU.t(msgStart + "停服定时任务已&c取消"));
                    } else {
                        player.sendMessage(CU.t(msgStart + "&c没有正在进行的停服计划"));
                    }
                    return true;
                }

                default: player.sendMessage(CU.t(msgStart + "未知的指令")); break;
            }
        } else {
            Bukkit.getLogger().info("该指令只能由玩家执行");
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
            String[] allCommands = {"help", "hitokoto", "keepinv", "receive", "haste", "stopserver", "cancelstopserver"};

            List<String> commandList = new ArrayList<>();
            for(String commandName : allCommands)
                if(sender.hasPermission("mapbot." + commandName))
                    commandList.add(commandName);

            return commandList;
        }
        return null;
    }
}
