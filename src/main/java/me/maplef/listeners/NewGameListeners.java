package me.maplef.listeners;

import net.md_5.bungee.api.connection.ConnectedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class NewGameListeners implements Listener {
    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();
    private final Long botAcc = config.getLong("bot-account");
    private final Long groupID = config.getLong("player-group");

    @EventHandler
    public void Test(ChatEvent e, LoginEvent event){
        ConnectedPlayer player = (ConnectedPlayer) e.getSender();
        Bukkit.getLogger().info(player.getName());
        Bukkit.getLogger().info(player.getServer().getInfo().getName());
        Bukkit.getLogger().info(e.getMessage());
        Bukkit.getLogger().info(e.getSender().getSocketAddress().toString());

        Bukkit.getLogger().info(event.getConnection().getName());
        Bukkit.getLogger().info(event.getConnection().getUniqueId().toString());
    }
}
