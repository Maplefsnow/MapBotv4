package me.maplef.mapbotv4;

import me.maplef.mapbotv4.commands.Mapbot;
import me.maplef.mapbotv4.listeners.GameListeners;
import me.maplef.mapbotv4.managers.LoopJobManager;
import me.maplef.mapbotv4.plugins.BotQQOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import me.maplef.mapbotv4.utils.Scheduler;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;

public class Main extends JavaPlugin implements Listener {
    private FileConfiguration messageConfig;
    private FileConfiguration onlineTimeConfig;
    private static Main instance;
    private static Economy econ = null;

    public final Long opGroup = getConfig().getLong("op-group");

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(); this.registerConfig();
        this.getMessageConfig().options().copyDefaults();

        Bukkit.getServer().getLogger().info(messageConfig.getString("enable-message.console"));

        if(!getDataFolder().exists()){
            getLogger().warning("请在生成的配置文件中修改相关配置再启动本插件");
            this.saveDefaultConfig();
            this.saveResource("messages.yml", false);
            this.saveResource("cat_images/catImageSample.jpg", false);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy() ) {
            Bukkit.getServer().getLogger().severe(String.format("[%s] 找不到前置插件 vault，请安装该插件！", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;

        BotQQOperator.login();

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new GameListeners(), this);

        Objects.requireNonNull(getCommand("mapbot")).setExecutor(new Mapbot());
        Objects.requireNonNull(getCommand("mapbot")).setTabCompleter(new Mapbot());

        try {
            DatabaseOperator.init();
        } catch (SQLException e){
            e.printStackTrace();
        }

        LoopJobManager.register();

        getServer().broadcast(Component.text(CU.t(messageConfig.getString("message-prefix") + messageConfig.getString("enable-message.server"))));
    }

    @Override
    public void onDisable() {
        try {
            DatabaseOperator.c.close();
            Scheduler.scheduler.shutdown();
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
        }

        BotQQOperator.logout();

        getServer().broadcast(Component.text(CU.t(messageConfig.getString("message-prefix") + messageConfig.getString("disable-message.server"))));
        getLogger().info(messageConfig.getString("disable-message.console"));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        econ = rsp.getProvider();
        return true;
    }

    public static Main getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return econ;
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
