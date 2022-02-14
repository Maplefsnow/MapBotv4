package me.maplef.mapbotv4.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class GameListeners implements Listener {
    final FileConfiguration config = Main.getInstance().getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

    private final Long groupID = config.getLong("player-group");
    private final String msgPrefix = messages.getString("message-prefix");

    @EventHandler
    public void MessageReceive(AsyncPlayerChatEvent e) {
        MessageChainBuilder msg = new MessageChainBuilder();
        Player player = e.getPlayer();

        String pattern = "^[@][\\w]*\\s\\w+$";
        if (Pattern.matches(pattern, e.getMessage())) {
            String atMsg = e.getMessage().substring(1);
            String atName = atMsg.split(" ")[0];

            try {
                long atQQ = Long.parseLong(DatabaseOperator.query(atName).get("QQ").toString());
                msg.append(player.getName()).append(": ").append(new At(atQQ)).append(atMsg.substring(atName.length())).build();
            } catch (SQLException | PlayerNotFoundException ex) {
                Bukkit.getServer().getLogger().warning(ex.getClass().getName() + ": " + ex.getMessage());
            }
        } else {
            msg.append(player.getName()).append(": ").append(e.getMessage());
        }

        BotOperator.sendGroupMessage(groupID, msg.build());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        Player player = e.getEntity();
        if(player.getName().equals("Maplef_snow")){
            e.setKeepInventory(true); e.setKeepLevel(true); e.getDrops().clear();
            return;
        }

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
                String moneyString = PlaceholderAPI.setPlaceholders(player, "%vault_eco_balance_fixed%");
                float money = Float.parseFloat(moneyString);
                int cost = config.getInt("keep-inventory.cost");

                if(money < cost){
                    player.sendMessage(CU.t(msgPrefix + "你没有足够的猫猫积分以免疫死亡不掉落"));
                    return;
                }

                String takeMoneyCommand = String.format("money take %s %d", player.getName(), cost);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), takeMoneyCommand);
                player.sendMessage(CU.t(msgPrefix + String.format("扣除了你 %d 猫猫积分，本次免疫死亡掉落", cost)));
                player.sendMessage(CU.t(msgPrefix + "如需关闭本功能请执行 &6/mb help &b以获得帮助"));
                e.setKeepInventory(true); e.setKeepLevel(true);
                e.getDrops().clear(); e.setDroppedExp(0);
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
