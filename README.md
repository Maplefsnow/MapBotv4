# MapBotv4

## 简介
MapBotv4 是枫号机器人Mapbot的第四代，作为 Minecraft bukkit/spigot/paper 服务端插件服役于[猫猫大陆](https://www.catland.top)，主要负责服务器和QQ群内消息互通，以及其他趣味性和工具性作用。

作者希望 Mapbot 能够为更多有相关需求的服主提供力所能及的帮助，也希望有想法的开发者能利用 Mapbot 的框架继续添加自己需要的功能，故将此项目在 Github 开源。目前此项目仍在不断完善中，部分定制化色彩较重的功能正在逐渐大众化，请积极提交 issue 以提出建议或反馈您遇到的任何问题，在此不胜感激。

**未经作者允许，本项目禁止任何类型的转载，禁止商用和售卖**。Github 和 [MCBBSv3](https://www.mcbbs.net/forum.php?mod=viewthread&tid=1313995&page=1&extra=#pid24967494) 、[MCBBSv4](https://beta.mcbbs.net/resource/servermod/cbo4opvt) 目前为获取本项目的唯三渠道，如在其他平台获取此插件请及时告知作者。

*本项目遵循 CC-BY-NC-SA 4.0 协议*

## 使用方法
### 准备材料
- [必需] 至少两个 QQ 群，其一为玩家群（下称`player-group`），另一为管理人员群（下称`op-group`）
- [必需] 一个能正常通过手机端登陆的 QQ 小号
- [必需] 前置插件 [Vault](https://github.com/MilkBowl/Vault) 和至少一个经济插件（如Essentials自带的经济管理）
- [必需] 自行搭建的签名服务 [QSign](https://github.com/fuqiuluo/unidbg-fetch-qsign)
- [可选] 玩家审核群（下称`check-in-group`）
- [可选] [和风天气](https://dev.qweather.com) API-KEY
- [可选] [百度识图](https://ai.baidu.com/ai-doc/IMAGERECOGNITION/Zk3bcxdfr) API-KEY
- [可选] [FFmpeg](https://github.com/BtbN/FFmpeg-Builds/releases)

### 启动
1. 按照 [QSign](https://github.com/fuqiuluo/unidbg-fetch-qsign) 的 [部署方法](https://github.com/fuqiuluo/unidbg-fetch-qsign/wiki) 自行部署并配置签名服务器，记住签名服务器的端口。
2. 将此插件的 .jar 文件放入服务端 plugins 文件夹，并加载插件。
3. 初次启动插件会自动生成配置文件和数据库文件。此时插件会自动卸载，请在相应文件夹下修改 config.yml 中的相关设置。
4. 至服务器根目录编辑生成的 `KFCFactory.json` 文件，参照 [fix-protocol-version的配置文件示例](https://github.com/cssxsh/fix-protocol-version) ，按照 `config.yml` 中配置的 `qsign-qq-version` 和签名服务器端口进行修改。
5. 重新加载插件，按照提示完成滑动验证码和设备锁验证。
6. QQ 登录成功后服务器控制台将显示 “QQ账号 xxx 登陆成功！”，同时 op-group 显示成功消息，正常使用即可。

### 常见异常及解决方法
- 登陆时提示需要滑块验证：按照控制台提示操作。
- 登陆时提示需要设备锁验证：按照控制台提示操作。
- 控制台提示登陆成功但 op-group 未见消息：
    - 控制台**无风控提示**：消息可能被腾讯拦截，尝试同时在电脑端登录此 QQ 号并在电脑端随意发送信息以解除拦截。
    - 控制台**有风控提示**：此账号已被腾讯风控，请用手机登录该账号并通过验证码验证以解除风控。
- 其他异常或您认为不必要的报错信息：请 [提交 issue](https://github.com/Maplefsnow/MapBotv4/issues) 。

### 使用
和 Minecraft 游戏内输入指令一样，在 player-group 或 op-group 中发送带 `#` 前缀的命令以使用各项功能，详情可发送 `#help` 查看命令使用方法。如只需查看单个命令使用方法，只需发送命令头再查看返回的报错消息即可。

命令前缀默认为 `#`，如需更改可在 `config.yml` 中配置。

### 权限
- 在 player-group 中的 QQ 账户拥有基础权限。
- 部分命令需要管理员权限，在 op-group 中的 QQ 账户将自动拥有管理员权限。
- 部分特殊操作需要超级管理员权限，需在 `config.yml` 中配置超级管理员账户。

## 功能

*建议将机器人账号设为所有群的管理员以使用全部功能。*

***以下功能均可进行配置，详见配置文件。***

### 定时任务

- [需和风天气API] 早晚定时在群内发送消息问好，问早安时可同时播报某地（可在配置文件中设置）的天气预报
- 定时检测服务器 TPS，如发现低于设定的阈值则会在 op-group 告警所有管理

### 群管理

- 有玩家进入 player-group 时在服内和大群中同时发送欢迎消息
- 有玩家退出 player-group 时在 op-group 发送提示消息
- [需 check-in-group] 自动通过审核群的加群验证，并发送欢迎消息引导玩家进行审核
- [需 check-in-group] 有玩家进入 player-group 时，若此玩家同时也在 check-in-group 中，则在 check-in-group 播报审核通过信息
- 自动撤回触发关键字检测的群员消息，并给予警告
- 使用 `#mute` 同时在群内和游戏内禁言某玩家

### 玩家管理

- 随时在群内查询在线玩家数和玩家列表
- 完善的玩家数据库，玩家须在群中绑定游戏id才可正常使用全部功能
- [需开启 online-mode] 如玩家修改游戏id（不换号）只需在群内使用 `#updateid` 即可根据 UUID 自动获取新id并更新，如玩家换号则只能通过管理权限解绑再重新绑定
- 玩家入群绑定id后自动修改群名片，自动添加服务器白名单

### 群内控制台

可在任意群内发送以 `/` 为前缀的服务端指令，bot 将在服务器控制台执行此条指令并返回指令执行结果（部分无返回信息的指令则提示无返回信息）。

命令前缀默认为 `/`，如需更改可在 `config.yml` 中配置。

### 趣味功能

- 玩家自主上传图片至数据库，随时调取数据库中图片
    - [需百度识图] 玩家上传图片时检测是否含有猫，不含有猫则不允许上传
- 停服倒计时，更可配合 Stp 插件把玩家在关服前传送至其他子服
- 随机在玩家群内发送提示性消息
- 群复读机
- 一言
- 每日新闻播报
- [需 PlayTimeTracker 在线时间检测插件] 随时在群内查询玩家在线时长
- 随时在群内查询玩家货币数
- 关键字触发发送特定消息

*如有其他您期望看到的功能，请发送 issue 来提出宝贵建议*

## 致谢
感谢你看到这里，本项目仍有诸多不足之处，请提出宝贵意见，我将积极改进。
