package me.maplef.mapbotv4.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import me.maplef.mapbotv4.utils.TextComparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class GameListeners implements Listener {
    ConfigManager configManager = new ConfigManager();

    @EventHandler
    public void onMessageForward(AsyncChatEvent e) {
        FileConfiguration config = configManager.getConfig();

        Long groupID = config.getLong("player-group");

        if(!config.getBoolean("message-forward.server-to-group.enable", true)) return;
        if(e.isCancelled()) return;

        MessageChain msg = null;
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        Player player = e.getPlayer();
        String pattern = "^[@][\\w]*\\s\\w+$"; String receivedMessage = serializer.serialize(e.message());

        switch (config.getString("message-forward.server-to-group.mode", "all")) {
            case "all" -> {
                if (Pattern.matches(pattern, receivedMessage)) {
                    String atMsg = receivedMessage.substring(1);
                    String atName = atMsg.split(" ")[0];

                    try {
                        long atQQ = (Long) DatabaseOperator.queryPlayer(atName).get("QQ");
                        msg = MessageUtils.newChain(new PlainText(player.getName() + ": ")).plus(new At(atQQ)).plus(atMsg.substring(atName.length()));
                    } catch (PlayerNotFoundException ex) {
                        Bukkit.getServer().getLogger().warning(ex.getClass().getName() + ": " + ex.getMessage());
                        return;
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        return;
                    }
                } else {
                    msg = MessageUtils.newChain(new PlainText(player.getName() + ": " + receivedMessage));
                }
                BotOperator.sendGroupMessage(groupID, msg);
            }

            case "prefix" -> {
                String prefix = config.getString("message-forward.server-to-group.prefix", ".");
                if(!receivedMessage.startsWith(prefix)) return;
                receivedMessage = receivedMessage.substring(prefix.length());

                if (Pattern.matches(pattern, receivedMessage)) {
                    String atMsg = receivedMessage.substring(1);
                    String atName = atMsg.split(" ")[0];

                    try {
                        long atQQ = Long.parseLong(DatabaseOperator.queryPlayer(atName).get("QQ").toString());
                        msg = MessageUtils.newChain(new PlainText(player.getName() + ": ")).plus(new At(atQQ)).plus(atMsg.substring(atName.length()));
                    } catch (SQLException | PlayerNotFoundException ex) {
                        Bukkit.getServer().getLogger().warning(ex.getClass().getName() + ": " + ex.getMessage());
                    }
                } else {
                    msg = MessageUtils.newChain(new PlainText(player.getName() + ": " + receivedMessage));
                }
                BotOperator.sendGroupMessage(groupID, msg);
            }

            default -> Bukkit.getServer().getLogger().warning(String.format("[%s] config.yml: message-forward.server-to-group.mode 选择错误，请重新填写", Main.getInstance().getDescription().getName()));
        }
    }

    @EventHandler
    public void onAutoReply(AsyncChatEvent e){
        FileConfiguration messages = configManager.getMessageConfig();
        FileConfiguration autoReply = configManager.getAutoReplyConfig();

        String msgPrefix = messages.getString("message-prefix");

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

    @EventHandler
    public void onUpdateOfflineUUID(PlayerLoginEvent e){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();
        if(config.getBoolean("bind-id-and-qq.online-mode")) return;

        String msgPrefix = messages.getString("message-prefix");

        Player player = e.getPlayer();
        String playerName = player.getName();
        UUID uuid = player.getUniqueId();

        Connection c = new DatabaseOperator().getConnect();
        try(PreparedStatement ps = c.prepareStatement("UPDATE PLAYER SET UUID = ? WHERE NAME = ?;")){
            ps.setString(1, uuid.toString());
            ps.setString(2, playerName);
            ps.execute();
        } catch (SQLException ex){
            ex.printStackTrace();
            player.sendMessage(CU.t(msgPrefix + "更新离线玩家UUID失败，请联系管理员检查控制台报错"));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent loginEvent){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        long playerGroup = config.getLong("player-group");

        String loginMessage = messages.getString("player-login-message", "");

        if(loginEvent != null && !loginMessage.isEmpty()){
            BotOperator.sendGroupMessage(playerGroup, loginMessage.replace("{PLAYER}", loginEvent.getPlayer().getName()));
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent quitEvent) {
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        long playerGroup = config.getLong("player-group");

        String quitMessage = messages.getString("player-logout-message", "");

        if (quitEvent != null && !quitMessage.isEmpty()){
            BotOperator.sendGroupMessage(playerGroup, quitMessage.replace("{PLAYER}", quitEvent.getPlayer().getName()));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        FileConfiguration config = configManager.getConfig();
        long playerGroup = config.getLong("player-group");

        if(!config.getBoolean("message-forward.death-message.enable")) return;

        Component deathMessageComponent = e.deathMessage();
        if(deathMessageComponent == null) return;

        String deathMessage = PlainTextComponentSerializer.plainText().serialize(deathMessageComponent);

        BotOperator.sendGroupMessage(playerGroup, deathMessage);
    }

}
