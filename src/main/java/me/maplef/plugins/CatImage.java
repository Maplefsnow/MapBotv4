package me.maplef.plugins;

import me.maplef.MapbotPlugin;
import me.maplef.utils.BotOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.utils.ExternalResource;
import org.bukkit.Bukkit;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class CatImage implements MapbotPlugin {
    public static MessageChain getImage(Long groupID) throws Exception{
        String imagePath = ".\\plugins\\MapBot\\cat_images";
        File[] imageList = Objects.requireNonNull(new File(imagePath).listFiles());

        Random random = new Random();
        int pos = random.nextInt(imageList.length);
        File image = imageList[pos];
        ExternalResource imageResource = ExternalResource.create(image);

        Bot bot = BotOperator.bot;
        try{
            Image catImage = Objects.requireNonNull(bot.getGroup(groupID)).uploadImage(imageResource);
            return new MessageChainBuilder().append(Image.fromId(catImage.getImageId())).build();
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            throw new Exception("发送猫片失败QAQ，请再试一次吧");
        }
    }

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception{
        return new MessageChainBuilder().append(getImage(groupID)).build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("cat", CatImage.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("猫片", CatImage.class.getMethod("onEnable", Long.class, Long.class, String[].class));

        usages.put("cat", "#cat - 让我们看点猫片");
        usages.put("猫片", "#猫片 - 让我们看点猫片");

        info.put("name", "CatImage");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取一张猫片");
        info.put("version", "1.3");

        return info;
    }

    public CatImage(){}
}
