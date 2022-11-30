package me.maplef.mapbotv4;

import me.maplef.mapbotv4.commands.Mapbot;
import me.maplef.mapbotv4.listeners.GameListeners;
import me.maplef.mapbotv4.loops.BaiduAccessTokenUpdate;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.managers.LoopJobManager;
import me.maplef.mapbotv4.plugins.BotQQOperator;
import me.maplef.mapbotv4.utils.*;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;

public class Main extends JavaPlugin {
    private final FileConfiguration onlineTimeConfig = YamlConfiguration.loadConfiguration(new File(".\\plugins\\PlayTimeTracker\\database.yml"));
    private static Main instance;
    private static Economy econ = null;

    ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        if(!getDataFolder().exists()){
            Bukkit.getServer().getLogger().severe(String.format("[%s] 未检测到配置文件，请在生成的配置文件中修改相关配置再启动本插件", getDescription().getName()));
            this.saveDefaultConfig();
            this.saveResource("messages.yml", false);
            this.saveResource("auto_reply.yml", false);
            configManager = new ConfigManager();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager();
        FileConfiguration messageConfig = configManager.getMessageConfig();

        Bukkit.getServer().getLogger().info(String.format("[%s] %s", getDescription().getName(), messageConfig.getString("enable-message.console")));
        Bukkit.getServer().getLogger().info(String.format("[%s] Mapbot交流群：835413855，欢迎加群讨论！", getDescription().getName()));

        if (!setupEconomy()) {
            Bukkit.getServer().getLogger().severe(String.format("[%s] 找不到前置插件 vault，请安装该插件！", getDescription().getName()));
            Bukkit.getServer().getLogger().severe(String.format("[%s] 如已安装 vault 请确认是否已安装任一经济管理插件如 EssentialsX 等", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BotQQOperator.login();

        this.getServer().getPluginManager().registerEvents(new GameListeners(), this);

        Objects.requireNonNull(getCommand("mapbot")).setExecutor(new Mapbot());
        Objects.requireNonNull(getCommand("mapbot")).setTabCompleter(new Mapbot());

        try {
            new DatabaseOperator().init();
        } catch (SQLException e){
            e.printStackTrace();
        }

        if(this.getConfig().getBoolean("cat-images.upload-image.cat-detect.enable")) new BaiduAccessTokenUpdate().updateAuth();

        new LoopJobManager().register();

        getServer().broadcast(Component.text(CU.t(messageConfig.getString("message-prefix") + messageConfig.getString("enable-message.server"))));

        NeteaseMusicUtils.loadCookie();
    }

    @Override
    public void onDisable() {
        FileConfiguration messageConfig = configManager.getMessageConfig();

        try {
            new DatabaseOperator().getConnect().close();
            Scheduler.scheduler.shutdown();
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
        }

        if(BotOperator.getBot() != null) BotQQOperator.logout();

        getServer().broadcast(Component.text(CU.t(messageConfig.getString("message-prefix") + messageConfig.getString("disable-message.server"))));
        Bukkit.getServer().getLogger().info(String.format("[%s] %s", getDescription().getName(), messageConfig.getString("disable-message.console")));
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

    public FileConfiguration getOnlineTimeConfig() {
        return onlineTimeConfig;
    }

    public ConfigManager getConfigManager(){
        return configManager;
    }

}
