package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.MapbotPlugin;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerInfo implements MapbotPlugin {
    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        int totalChunkCnt = 0;
        StringBuilder worldChunkInfo = new StringBuilder();
        StringBuilder worldTimeInfo = new StringBuilder();

        double[] tps = Bukkit.getServer().getTPS();
        double averageTickTime = Bukkit.getServer().getAverageTickTime();

        List<World> worlds = Bukkit.getServer().getWorlds();
        for(World world : worlds){
            String worldName = world.getName();
            int chunkCnt = world.getLoadedChunks().length;
            long worldTime = world.getTime();

            int hour = Math.toIntExact(worldTime / 1000);
            int min = (int) ((3.6*worldTime - hour*3600) / 60);
            int sec = (int) (3.6*worldTime - hour*3600 - min*60);

            totalChunkCnt += chunkCnt;

            worldChunkInfo.append(String.format("%s: %d\n", worldName, chunkCnt));
            worldTimeInfo.append(String.format("%s: %d:%d:%d\n", worldName, hour, min, sec));
        }

        String infoMsg = String.format("""
                [区块信息]
                总加载区块：%d
                %s
                ————————
                [TPS/MSPT]
                tps: %.2f, %.2f, %.2f
                mspt: %.2f
                ————————
                [世界游戏时]
                %s
                """,
                totalChunkCnt,
                worldChunkInfo.toString().trim(),
                tps[0], tps[1], tps[2],
                averageTickTime,
                worldTimeInfo.toString().trim());

        return MessageUtils.newChain(new PlainText(infoMsg));
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("serverinfo", ServerInfo.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("服务器信息", ServerInfo.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("serverinfo", "#serverinfo - 获取服务器相关信息");
        usages.put("服务器信息", "#服务器信息 - 获取服务器相关信息");

        info.put("name", "ServerInfo");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取服务器信息");
        info.put("version", "0.1");

        return info;
    }
}
