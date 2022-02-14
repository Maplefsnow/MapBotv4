# MapBotv4

## 简介
MapBotv4 是枫号机器人Mapbot的第四代，作为 Minecraft bukkit/spigot/paper 服务端插件服役于[猫猫大陆](https://www.catland.top)，主要负责服务器和QQ群内消息互通，以及其他趣味性和工具性作用。

作者希望 Mapbot 能够为更多有相关需求的服主提供力所能及的帮助，也希望有想法的开发者能利用 Mapbot 的框架继续添加自己需要的功能，故将此项目在 Github 开源。目前此项目仍在不断完善中，部分定制化色彩较重的功能正在逐渐大众化，请积极提交 issue 以提出建议或反馈您遇到的任何问题，在此不胜感激。

**未经作者允许，本项目禁止任何类型的转载，禁止商用和售卖**。Github 目前为获取本项目的唯一渠道，如在其他平台获取此插件请及时告知作者。

## 使用方法
### 准备材料
- [必需] 至少两个 QQ 群，其一为玩家群（下称`player-group`），另一为管理人员群（下称`op-group`）
- [必需] 一个开启设备锁的 QQ 小号
- [必需] 前置插件 [PlaceHolderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)
- [可选] 玩家审核群（下称`check-in-group`）
- [可选] [和风天气](https://dev.qweather.com)API-KEY

### 启动
1. 将此插件的 .jar 文件放入 plugins 文件夹，重启服务端以使插件加载。
2. 初次启动插件会自动生成配置文件和数据库文件，插件会自动卸载，请在相应文件夹下修改 config.yml 中的相关设置再次启动。
3. QQ 登录成功后 op-group 会自动发送成功消息，正常使用即可。

### 常见异常及解决方法
- 登陆时提示需要滑块验证：[解决方法](https://docs.mirai.mamoe.net/mirai-login-solver-selenium/#%E6%89%8B%E5%8A%A8%E5%AE%8C%E6%88%90%E6%BB%91%E5%8A%A8%E9%AA%8C%E8%AF%81)
- 控制台提示登陆成功，op-group 未见消息：消息可能被腾讯拦截，尝试同时在电脑端登录此 QQ 号并在电脑端随意发送信息以解除拦截。
- 其他错误：请提交 issue

### 使用
和 Minecraft 游戏内输入指令一样，在 player-group 或 op-group 中发送带 `#` 前缀的命令以使用各项功能，详情可发送 `#help` 查看命令使用方法。如只需查看单个命令使用方法，只需发送命令头再查看返回的报错消息即可。

### 权限
- 部分命令需要高级权限，在 op-group 中的 QQ 账户将自动拥有高级权限。
- 部分特殊操作需要超级管理员权限，可在 `config.yml` 中配置超级管理员账户。

## 致谢
感谢你看到这里，本项目仍有诸多不足之处，请提出宝贵意见，我将积极改进。
