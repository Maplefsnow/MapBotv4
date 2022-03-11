package me.maplef.mapbotv4.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.kyori.adventure.text.TextComponent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class GameListeners implements Listener {
    final FileConfiguration config = Main.getInstance().getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

    private final Long groupID = config.getLong("player-group");
    private final String msgPrefix = messages.getString("message-prefix");

    @EventHandler
    public void MessageReceive(AsyncChatEvent e) {
        MessageChainBuilder msg = new MessageChainBuilder();
        Player player = e.getPlayer();

        String pattern = "^[@][\\w]*\\s\\w+$"; String receivedMessage = ((TextComponent) e.message()).content();
        if (Pattern.matches(pattern, receivedMessage)) {
            String atMsg = receivedMessage.substring(1);
            String atName = atMsg.split(" ")[0];

            try {
                long atQQ = Long.parseLong(DatabaseOperator.query(atName).get("QQ").toString());
                msg.append(player.getName()).append(": ").append(new At(atQQ)).append(atMsg.substring(atName.length())).build();
            } catch (SQLException | PlayerNotFoundException ex) {
                Bukkit.getServer().getLogger().warning(ex.getClass().getName() + ": " + ex.getMessage());
            }
        } else {
            msg.append(player.getName()).append(": ").append(receivedMessage);
        }

        BotOperator.sendGroupMessage(groupID, msg.build());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        Player player = e.getEntity();

        int keep = 0;
        try {
            Object tmp = DatabaseOperator.query(player.getName()).get("KEEPINV");
            if(tmp != null)
                keep = (Integer) DatabaseOperator.query(player.getName()).get("KEEPINV");
        } catch (SQLException | PlayerNotFoundException ex) {
            ex.printStackTrace();
        }

        if(keep == 1){
            if(config.getBoolean("keep-inventory.enable")){
                Economy econ = Main.getEconomy();

                double money = econ.getBalance(player);
                int cost = config.getInt("keep-inventory.cost");

                if(money < cost){
                    player.sendMessage(CU.t(msgPrefix + "你没有足够的猫猫积分以免疫死亡不掉落"));
                    return;
                }

                EconomyResponse r = econ.withdrawPlayer(player, cost);
                if(r.transactionSuccess()){
                    player.sendMessage(CU.t(msgPrefix + String.format("扣除了你 %d 猫猫积分，本次免疫死亡掉落", cost)));
                    e.setKeepInventory(true); e.setKeepLevel(true);
                    e.getDrops().clear(); e.setDroppedExp(0);
                } else {
                    player.sendMessage(CU.t(msgPrefix + "&4发生错误：" + r.errorMessage));
                }
            } else {
                player.sendMessage(CU.t(msgPrefix + "管理员未开启积分换取死亡不掉落功能"));
            }
        }

        if(config.getBoolean("keep-inventory.death-log")){
            Location deathLocation = player.getLocation();
            String msg = String.format("玩家 %s 在世界 %s 的 (%d, %d, %d) 位置死亡，背包物品已%s",
                    player.getName(), deathLocation.getWorld(),
                    deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ(),
                    e.getKeepInventory() ? "保留" : "掉落");
            Bukkit.getServer().getLogger().info(msg);
        }
    }
}
