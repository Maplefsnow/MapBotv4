package me.maplef;

import me.maplef.commands.Mapbot;
import me.maplef.listeners.GameListeners;
import me.maplef.listeners.GroupListeners;
import me.maplef.managers.LoopJobManager;
import me.maplef.plugins.Recipes;
import me.maplef.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;

public class Main extends JavaPlugin implements Listener {
    private FileConfiguration messageConfig;
    private FileConfiguration onlineTimeConfig;
    private static Main instance;

    public final Long botAcc = getConfig().getLong("bot-account");
    public final Long opGroup = getConfig().getLong("op-group");

    private final String botPassword = getConfig().getString("bot-password");

    @Override
    public void onEnable() {
        getLogger().info("MapBot已启用！");

        getConfig().options().copyDefaults(); registerConfig();
        getMessageConfig().options().copyDefaults();
        this.saveDefaultConfig();
        this.saveResource("messages.yml", false);
        this.saveResource("cat_images/catImageSample.jpg", false);
        if(botAcc == 123){
            getLogger().warning("请在生成的配置文件中修改相关配置再启动本插件");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;

        getServer().broadcastMessage(CU.t(messageConfig.getString("message-prefix") + messageConfig.getString("enable-message")));

        new BukkitRunnable(){
            @Override
            public void run() {
                getServer().getLogger().info("Mapbot正在登陆，请耐心等待...");
                BotOperator.login(botAcc, botPassword);
                BotOperator.bot.getEventChannel().registerListenerHost(new GroupListeners());
                BotOperator.send(opGroup, "Mapbot ON");
                getServer().getLogger().info("Mapbot登陆成功");
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GameListeners(), this);

        try {
            DatabaseOperator.init();
        } catch (SQLException ignored){}


        Objects.requireNonNull(getCommand("mapbot")).setExecutor(new Mapbot());
        Objects.requireNonNull(getCommand("mapbot")).setTabCompleter(new Mapbot());

        getServer().removeRecipe(NamespacedKey.minecraft("newelytra"));
        getServer().addRecipe(Recipes.elytra);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getPluginManager().registerEvents(this, this);
        } else {
            getLogger().warning("找不到前置插件PlaceHolderAPI，请安装该插件！");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        LoopJobManager.register();
    }

    @Override
    public void onDisable() {
        try {
            DatabaseOperator.c.close();
            Scheduler.scheduler.shutdown();
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
        }

        if(botAcc != 123){
            BotOperator.send(opGroup, "Mapbot OFF");
            BotOperator.close();
        }

        getServer().broadcastMessage(CU.t(messageConfig.getString("message-prefix") + messageConfig.getString("disable-message")));
        getServer().removeRecipe(NamespacedKey.minecraft("newelytra"));
        getLogger().info(ChatColor.RED + "MapBot已停止运行，感谢使用。");
    }

    public static Main getInstance() {
        return instance;
    }

    public void registerConfig() {
        messageConfig = YamlConfiguration.loadConfiguration(new File(".\\plugins\\MapBot\\messages.yml"));
        onlineTimeConfig = YamlConfiguration.loadConfiguration(new File(".\\plugins\\PlayTimeTracker\\database.yml"));
    }

    public FileConfiguration getMessageConfig() {
        return messageConfig;
    }

    public FileConfiguration getOnlineTimeConfig() {
        return onlineTimeConfig;
    }
}
