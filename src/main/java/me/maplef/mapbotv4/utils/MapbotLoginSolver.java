package me.maplef.mapbotv4.utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import me.maplef.mapbotv4.Main;
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
import java.util.logging.Logger;

public class MapbotLoginSolver extends LoginSolver {
    private static final FileConfiguration config = new ConfigManager().getConfig();
    private static final long botAccount = config.getLong("bot-account");

    public static final Logger logger = Main.getInstance().getLogger();

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
                logger.warning("当前登录的QQ（"+bot.getId()+"）需要滑动验证码验证");
                logger.warning("请使用 mirai-login-solver-sakura 打开以下链接进行验证");
                logger.warning(s);
                logger.warning("mirai-login-solver-sakura 下载地址：https://github.com/KasukuSakura/mirai-login-solver-sakura");
                logger.warning("验证完成后，请输入指令 /mapbot captcha <ticket>");
                while(deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
            });
            threads.start();
            threads.join();
        } catch (InterruptedException | IllegalThreadStateException e) {
            logger.warning("启动验证线程时出现异常，原因: " + e);
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
                logger.warning("当前登录的QQ（"+bot.getId()+"）需要完成设备验证");
                logger.warning("短信验证方式" + (requests.getSms() != null ? "可用" : "不可用，请勿使用此方式"));
                logger.warning("其他验证方式" + (requests.getSms() != null ? "可用" : "不可用，请勿使用此方式"));
                if(requests.getPreferSms()) logger.warning("服务器要求使用短信验证码验证，但仍可以尝试其他验证方式");
                logger.warning("如需使用短信验证方式，请输入指令 /mapbot deviceverify sms");
                logger.warning("如需使用其他验证方式，请输入指令 /mapbot deviceverify fallback");
                logger.warning("如需取消登录，请输入指令 /mapbot cancel");
                while(deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
            });
            threads.start();
            threads.join();
        } catch (InterruptedException | IllegalThreadStateException e) {
            logger.severe("启动验证线程时出现异常，原因: " + e);
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
                                logger.warning("当前登录的QQ（" + bot.getId() + "）将使用短信验证码验证");
                                logger.warning("一条包含验证码的短信将会发送到地区代码为" + requests.getSms().getCountryCode() + "、号码为" + requests.getSms().getPhoneNumber() + "的手机上");
                                logger.warning("收到验证码后，请输入指令 /mapbot deviceverify <验证码>");
                                logger.warning("如需取消登录，请输入指令 /mapbot cancel ，取消登录后需要等待至少1分钟才能重新登录");
                                requests.getSms().requestSms(new Continuation<>() {
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
                            logger.warning("当前登录的QQ（" + bot.getId() + "）不支持使用短信验证方式");
                            logger.warning("登录可能会失败，请尝试重新登录");
                            throw new UnsupportedOperationException();
                        }
                    }
                    case "fallback" -> {
                        if (requests.getFallback() != null) {
                            threads = new Thread(() -> {
                                deviceVerifyWait.add(bot);
                                logger.warning("当前登录的QQ（" + bot.getId() + "）将使用其他验证方式");
                                logger.warning("请打开以下链接进行验证");
                                logger.warning(requests.getFallback().getUrl());
                                logger.warning("验证完成后，请输入指令 /miraiverify deviceverify");
                                logger.warning("如需取消登录，请输入指令 /miraiverify cancel");
                                while (deviceVerifyWait.contains(bot)) if (deviceVerifyCode.containsKey(bot)) break;
                            });
                            threads.start();
                            threads.join();
                        } else {
                            logger.warning("当前登录的QQ（" + bot.getId() + "）不支持使用其他验证方式");
                            logger.warning("登录可能会失败，请尝试重新登录");
                            throw new UnsupportedOperationException();
                        }
                    }
                }
            } catch (InterruptedException | IllegalThreadStateException e) {
                logger.warning("启动验证线程时出现异常，原因: " + e);
                return null;
            }
        }

        DeviceVerificationResult result = null;
        if (deviceVerifyCode.containsKey(bot)) {
            switch (VerifyType) {
                case "sms" -> result = requests.getSms().solved(deviceVerifyCode.get(bot));

                case "fallback" -> result = requests.getFallback().solved();
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
