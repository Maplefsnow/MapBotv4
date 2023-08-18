package me.maplef.mapbotv4.utils;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import me.maplef.mapbotv4.managers.ConfigManager;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.utils.DeviceVerificationRequests;
import net.mamoe.mirai.utils.DeviceVerificationResult;
import net.mamoe.mirai.utils.LoginSolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class MapbotLoginSolver extends LoginSolver {
    private static final FileConfiguration config = new ConfigManager().getConfig();
    private static final long botAccount = config.getLong("bot-account");

    private Thread threads;

    private static final List<Bot> deviceVerifyWait = new ArrayList<>();

    private static final HashMap<Bot,String> deviceVerifyCode = new HashMap<>();

    @Nullable
    @Override
    public Object onSolvePicCaptcha(@NotNull Bot bot, @NotNull byte[] bytes, @NotNull Continuation<? super String> continuation) {
        return null;
    }

    @Nullable
    @Override
    public Object onSolveSliderCaptcha(@NotNull Bot bot, @NotNull String s, @NotNull Continuation<? super String> continuation) {
        try {
            threads = new Thread(() -> {
                deviceVerifyWait.add(bot);
                bot.getLogger().warning("当前登录的QQ（"+bot.getId()+"）需要滑动验证码验证");
                bot.getLogger().warning("请打开以下链接进行验证");
                bot.getLogger().warning(s);
                bot.getLogger().warning("验证完成后，请输入指令 /mapbot captcha <ticket>");
                while(deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
            });
            threads.start();
            threads.join();
        } catch (InterruptedException | IllegalThreadStateException e) {
            bot.getLogger().warning("启动验证线程时出现异常，原因: " + e);
            return null;
        }

        if (!deviceVerifyCode.isEmpty()) {
            String result = deviceVerifyCode.get(bot);
            deviceVerifyWait.remove(bot);
            deviceVerifyCode.remove(bot);
            return result;
        } else {
            deviceVerifyWait.remove(bot);
            deviceVerifyCode.remove(bot);
            return null;
        }
    }

    @Nullable
    @Override
    public Object onSolveDeviceVerification(@NotNull Bot bot, @NotNull DeviceVerificationRequests requests, @NotNull Continuation<? super DeviceVerificationResult> $completion) {
        try {
            threads = new Thread(() -> {
                deviceVerifyWait.add(bot);
                bot.getLogger().warning("当前登录的QQ（"+bot.getId()+"）需要完成设备验证");
                bot.getLogger().warning("短信验证方式" + (requests.getSms() != null ? "可用" : "不可用，请勿使用此方式"));
                bot.getLogger().warning("其他验证方式" + (requests.getSms() != null ? "可用" : "不可用，请勿使用此方式"));
                if(requests.getPreferSms()) bot.getLogger().warning("服务器要求使用短信验证码验证，但仍可以尝试其他验证方式");
                bot.getLogger().warning("如需使用短信验证方式，请输入指令 /mapbot deviceverify sms");
                bot.getLogger().warning("如需使用其他验证方式，请输入指令 /mapbot deviceverify fallback");
                bot.getLogger().warning("如需取消登录，请输入指令 /mapbot cancel");
                bot.getLogger().warning("如需帮助，请参阅: https://docs.miraimc.dreamvoid.me/troubleshoot/verify-guide#device-verify");
                while(deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
            });
            threads.start();
            threads.join();
        } catch (InterruptedException | IllegalThreadStateException e) {
            bot.getLogger().warning("启动验证线程时出现异常，原因: " + e);
            return null;
        }

        String VerifyType = ""; // 验证方式，不加这个就屎山了

        if (deviceVerifyCode.containsKey(bot)) {
            VerifyType = deviceVerifyCode.get(bot);
            deviceVerifyCode.remove(bot);
            try {
                switch (VerifyType) {
                    case "sms" -> {
                        if (requests.getSms() != null) {
                            threads = new Thread(() -> {
                                deviceVerifyWait.add(bot);
                                bot.getLogger().warning("当前登录的QQ（" + bot.getId() + "）将使用短信验证码验证");
                                bot.getLogger().warning("一条包含验证码的短信将会发送到地区代码为" + requests.getSms().getCountryCode() + "、号码为" + requests.getSms().getPhoneNumber() + "的手机上");
                                bot.getLogger().warning("收到验证码后，请输入指令 /mapbot deviceverify <验证码>");
                                bot.getLogger().warning("如需取消登录，请输入指令 /mapbot cancel ，取消登录后需要等待至少1分钟才能重新登录");
                                requests.getSms().requestSms(new Continuation<Unit>() {
                                    @NotNull
                                    @Override
                                    public CoroutineContext getContext() {
                                        return (CoroutineContext) Dispatchers.getIO();
                                    }

                                    @Override
                                    public void resumeWith(@NotNull Object o) {
                                    }
                                });
                                while (deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
                            });
                            threads.start();
                            threads.join();
                        } else {
                            bot.getLogger().warning("当前登录的QQ（" + bot.getId() + "）不支持使用短信验证方式");
                            bot.getLogger().warning("登录可能会失败，请尝试重新登录");
                            throw new UnsupportedOperationException();
                        }
                    }
                    case "fallback" -> {
                        if (requests.getFallback() != null) {
                            threads = new Thread(() -> {
                                deviceVerifyWait.add(bot);
                                bot.getLogger().warning("当前登录的QQ（" + bot.getId() + "）将使用其他验证方式");
                                bot.getLogger().warning("请打开以下链接进行验证");
                                bot.getLogger().warning(requests.getFallback().getUrl());
                                bot.getLogger().warning("验证完成后，请输入指令 /miraiverify deviceverify");
                                bot.getLogger().warning("如需取消登录，请输入指令 /miraiverify cancel");
                                while (deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
                            });
                            threads.start();
                            threads.join();
                        } else {
                            bot.getLogger().warning("当前登录的QQ（" + bot.getId() + "）不支持使用其他验证方式");
                            bot.getLogger().warning("登录可能会失败，请尝试重新登录");
                            throw new UnsupportedOperationException();
                        }
                    }
                }
            } catch (InterruptedException | IllegalThreadStateException e) {
                bot.getLogger().warning("启动验证线程时出现异常，原因: " + e);
                return null;
            }
        }

        DeviceVerificationResult result = null;
        if (deviceVerifyCode.containsKey(bot)) {
            switch (VerifyType) {
                case "sms" -> {
                    result = requests.getSms().solved(deviceVerifyCode.get(bot));
                }
                case "fallback" -> {
                    result = requests.getFallback().solved();
                }
            }
        }

        if (deviceVerifyCode.containsKey(bot)) {
            deviceVerifyWait.remove(bot);
            deviceVerifyCode.remove(bot);
            return result;
        } else {
            deviceVerifyWait.remove(bot);
            deviceVerifyCode.remove(bot);
            return null;
        }
    }

    public static void solve() throws NoSuchElementException {
        deviceVerifyCode.put(Bot.getInstance(botAccount), "continue");
        deviceVerifyWait.remove(Bot.getInstance(botAccount));
    }

    public static void solve(String code) {
        deviceVerifyCode.put(Bot.getInstance(botAccount), code);
        deviceVerifyWait.remove(Bot.getInstance(botAccount));
    }
}
