package me.maplef.mapbotv4.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import me.maplef.mapbotv4.utils.TextComparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class GameListeners implements Listener {
    final FileConfiguration config = Main.getInstance().getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();
    final FileConfiguration autoReply = Main.getInstance().getAutoReplyConfig();

    private final Long groupID = config.getLong("player-group");
    private final String msgPrefix = messages.getString("message-prefix");

    @EventHandler
    public void onMessageForward(AsyncChatEvent e) {
        if(!config.getBoolean("message-forward.server-to-group")) return;
        if(e.isCancelled()) return;

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
    public void onAutoReply(AsyncChatEvent e){
        if(!autoReply.getBoolean("enable-in-group")) return;

        String message = ((TextComponent) e.message()).content();
        Set<String> rules = autoReply.getKeys(false);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            for(String ruleKey : rules){
                List<String> triggers = autoReply.getStringList(ruleKey + ".trigger");
                for(String trigger : triggers){
                    double similarity = TextComparator.getLSTSimilarity(message, trigger) * 60
                            + TextComparator.getCosSimilarity(message, trigger) * 40;
                    if(similarity >= autoReply.getInt(ruleKey + ".similarity", 100)){
                        Bukkit.getServer().broadcast(Component.text(CU.t(msgPrefix + autoReply.getString(ruleKey + ".reply", "null"))));
                        return;
                    }
                }
            }
        });
    }
}
