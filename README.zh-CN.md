# CloudX

[中文主文档](README.md) | [English](README.md#english)

`CloudX` 是一个面向 Codex 的独立第三方 Android 远程控制客户端。它通过
一次性二维码与受支持的 Windows 桌面端配对，让你可以在 Android 设备上远程
查看和操作 Codex 对话。CloudX 与 OpenAI 没有隶属、授权或背书关系。

## 下载

[**下载最新版 CloudX APK**](https://github.com/DENGGL2/CloudX/releases/download/v0.1.23/CloudX-0.1.23-arm64-v8a-debug.apk)

## 界面截图

| 1. 选择配对方式 | 2. 外观与语言设置 | 3. 已连接会话列表 |
|:--:|:--:|:--:|
| [![选择 Cloudflare Tunnel 或 WebRTC Direct](docs/screenshots/01-pairing-methods.png)](docs/screenshots/01-pairing-methods.png) | [![调整外观、语言与任务选项](docs/screenshots/02-appearance-settings.png)](docs/screenshots/02-appearance-settings.png) | [![查看已连接电脑上的会话](docs/screenshots/03-connected-conversations.png)](docs/screenshots/03-connected-conversations.png) |

| 4. 权限确认与运行状态 | 5. 会话置顶与归档 |
|:--:|:--:|
| [![在对话详情中处理权限确认](docs/screenshots/04-approval-request.png)](docs/screenshots/04-approval-request.png) | [![通过侧滑操作置顶或归档会话](docs/screenshots/05-conversation-actions.png)](docs/screenshots/05-conversation-actions.png) |

## 主要功能

- 在手机上查看会话列表、对话详情、执行过程和任务结果。
- 新建对话、发送消息，并处理电脑端发起的权限确认。
- 通过一次性二维码完成设备配对，支持 Cloudflare Tunnel 和 WebRTC Direct。
- 支持附件、图片预览与任务状态通知。
- 支持跟随系统、中文和 English 三种界面语言选项。
- 支持原生与玻璃界面风格，并可调整字体和玻璃效果。

## 对话功能

### 发起新对话

- 输入要交给 Codex 的任务描述。
- 选择电脑端提供的项目和模型。
- 按所选模型支持的范围选择推理层级。
- 选择请求批准、帮我批准或完全访问权限；不可用的权限不会开放选择。
- 项目、模型、推理层级和权限选项均由已配对的电脑动态提供，创建成功后会直接进入对话详情。

### 对话详情结构

- 用户消息、Codex 回复和执行过程按时间顺序呈现。
- 执行活动集中分组，支持思考、命令、网页搜索、工具调用、文件修改、计划和图片等类型。
- 显示进行中、已完成、已停止和失败状态，并在可用时显示执行耗时。
- 运行中的执行详情自动展开，任务结束后默认收起；可随时点击查看每一步。
- 命令活动可打开命令详情，分别查看和复制命令及输出。
- 回复内容支持标题、列表、任务项、引用、代码块、表格和差异内容等 Markdown 展示。
- 图片附件支持缩略图和大图预览；文件附件支持内容预览和下载。

### 对话详情操作

- 在发送前切换模型、推理层级和访问权限。
- 添加图片、添加文件，或从电脑端可用列表中选择 Skill。
- 任务运行中可以发送新指令并按设置插队或排队，也可以停止当前任务。
- 收到权限请求时可直接允许或拒绝；离开最新位置后可一键回到最新消息。

无论选择哪种网络传输方式，移动端都使用相同的配对、设备身份、权限、会话、
对话、确认和附件协议。

## 使用流程

1. 在 Android 上安装 APK。
2. 在需要配对的 Windows 电脑上启动 `desktop-connector\pair.bat`（默认
   Cloudflare；传入 `2` 使用 WebRTC Direct）。
3. 使用对应的传输方式：
   - `Cloudflare Tunnel`：Quick Tunnel，不需要域名或令牌；重启后地址会变化，需要重新扫码。
   - `WebRTC Direct`：在网络条件允许时使用直接加密 DataChannel；启动器会提供公开的 HTTPS 信令入口。
4. 手机端选择相同的传输方式，点击 `开始`。
5. 扫描桌面端显示的二维码并确认配对。

二维码只包含短时有效的签名引导数据。手机完成配对后只保存设备身份和授权
路由，不保存一次性二维码令牌或 nonce。桌面连接器可以从状态存储中撤销已
授权的手机；过期或重复使用的配对邀请会被拒绝。

## 从源码构建

使用 Gradle 构建 ARM64 Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug -Parm64Only=true
```

APK 输出到 `app/build/outputs/apk/debug/`。

### 版本规则

- `versionName` 是用户可见的语义化版本号，功能和修复版本递增补丁号。
- `versionCode` 是 Android 使用的单调递增更新编号，每个分发版本都必须增加。

## Windows 桌面端前置条件

Windows Agent 包含在本仓库中。请安装 JDK 17、Node.js、Android/Gradle 构建
环境、Codex CLI 和 `cloudflared.exe`，然后从本仓库运行配对启动器：

```powershell
.\desktop-connector\pair.bat
```

使用 Cloudflare 模式时，`cloudflared.exe` 必须位于 `PATH` 中，或者将
`CLOUDX_CLOUDFLARED_PATH` 设置为完整路径。Quick Tunnel 免费使用，不需要
账号、域名或令牌。Cloudflare 承载远程 HTTPS 请求；本地 Connector 服务仍
只监听 `127.0.0.1`，应用层挑战-响应认证仍然启用。

使用 WebRTC 模式时，启动器会启动仓库内的信令服务，并通过公开的 HTTPS
Quick Tunnel 暴露它。服务只保存短时有效的 SDP 和 ICE 记录；正常对话和文件
数据通过加密 DataChannel 或 TURN 中继传输。STUN 本身不能跨网络中继数据。
如需在没有 VPN 的情况下提高跨网络连接成功率，请配置短时有效的 TURN 凭据，
包括手机网络阻止 UDP 时可用的 TCP/443 地址：
`CLOUDX_WEBRTC_TURN_SERVERS=url|username|credential;url|username|credential`。
凭据会被复制到短时有效的二维码路由中，不会打包进 APK。Cloudflare Quick
Tunnel 不能保证在所有网络环境下都无需 VPN。

## 自动构建

[GitHub Actions 工作流](https://github.com/DENGGL2/CloudX/actions) 会构建并
上传 ARM64 Debug APK。在发布正式签名版本前，请配置 Android SDK 和正式发布
签名密钥。

## 快速开始

克隆本仓库，在准备配对的 Windows 电脑上运行桌面端：

```powershell
.\desktop-connector\pair.bat
```

启动器不会弹出控制台窗口，默认使用 Cloudflare Tunnel；传入 `2` 使用 RTC
Direct。RTC 模式会启动仓库内的信令服务，并通过 Cloudflare Quick Tunnel 只
暴露信令服务，然后自动打开新的二维码图片。手机端必须选择相同模式后再点击
`开始`。每次运行都会生成新的二维码文件和新的 Quick Tunnel 地址，不会重复
使用旧二维码。

如需提高受限网络下的 RTC 连接成功率，请在启动器运行前配置短时有效的 TURN
凭据：

```powershell
$env:CLOUDX_WEBRTC_TURN_SERVERS = "turns:turn.example.com:443|username|credential"
```

使用 Cloudflare Realtime TURN 时，请将长期 TURN 密钥和 API Token 保存在 Windows
桌面端，让 `pair.bat` 为每次配对运行生成短时凭据：

```powershell
$env:CLOUDX_CLOUDFLARE_TURN_KEY_ID = "your-turn-key-id"
$env:CLOUDX_CLOUDFLARE_TURN_API_TOKEN = "your-turn-api-token"
.\desktop-connector\pair.bat
```

密钥和 API Token 不会写入二维码或 APK。如果没有配置 TURN，RTC 仍会尝试连接，
但在 NAT 或防火墙限制下可能失败。

WebRTC 对话数据通过加密 DataChannel 或 TURN 中继传输。Cloudflare 在此模式下
只作为 HTTPS 信令入口，不承载对话数据。

## 支持与联系方式

Bug、功能建议和项目问题请提交到 [GitHub Issues](https://github.com/DENGGL2/CloudX/issues)。
上传日志或截图前，请删除二维码、配对令牌、TURN 凭据、访问令牌和其他敏感信息。

## 许可证

CloudX 使用 [MIT 许可证](LICENSE)。第三方依赖和随项目发布的资源可能受其
各自许可证约束。
