# EtherHUD

[English](README.md) · [日本語](README_ja.md) · [简体中文](README_zh_cn.md) · [繁體中文](README_zh_tw.md) · [한국어](README_ko.md)

## 安装

EtherHUD 是适用于 **Minecraft 1.21.4 / Fabric** 的客户端 HUD 模组。需要 **Java 21**、**Fabric Loader 0.16.14 或更新版本**，以及适用于 1.21.4 的 **Fabric API 0.119.2 或更新版本**。将 EtherHUD 和 Fabric API 放入实例的 `mods` 文件夹。在游戏中按**右 Shift** 打开设置，也可通过 Mod Menu 打开。Mod Menu 和 Cloth Config 均为可选项。

## 设置与提示

每个模块可单独开关，总开关可临时隐藏整个 HUD。点击模块旁的设置，可用取色器与实时预览调整文字、背景、边框、圆角、缩放和位置。首次打开设置会显示六条提示，并用边框标出对应控件。可通过 **更多 → 重新查看提示** 再次打开。设置在关闭界面时保存。

## HUD 模块

| 模块 | 显示内容 |
| --- | --- |
| FPS | 当前帧率 |
| Coordinates | 玩家坐标 |
| CPS | 左键和右键的每秒点击次数 |
| Armor Status | 主手、副手、盔甲和耐久 |
| Ping | 多人游戏延迟 |
| TPS | 根据服务器时间更新估算的刻速率 |
| Player Model | 随视角方向转动的玩家 3D 模型 |
| Item Total | 物品栏内与主手物品同类型的总数 |
| Compass | 可滚动的方向与角度标尺 |
| Image Overlay | 本地图片或动态 GIF |

Armor Status 支持分隔、连体和原版快捷栏三种样式，可横排或竖排，并可设置耐久提示。原版样式可跟随快捷栏或容器界面的侧边，也可手动放置，大小随 Minecraft 的 GUI 缩放变化。Image Overlay 支持 PNG、JPEG、GIF、BMP、WBMP 和 TIFF。

## HUD 编辑器

在设置中打开 **HUD 编辑器**。拖动模块可移动，拖动角点或使用 **Ctrl＋滚轮** 可缩放。**Ctrl＋右键** 添加或取消选择，从空白处拖动可框选。方向键微调位置，按住 **Shift** 可加大步幅。**Ctrl＋Z / Ctrl＋Y** 撤销或重做，**H** 显示或隐藏工具栏。工具栏可切换吸附、网格和停用模块的预览。**Esc** 先关闭右键菜单；菜单已关闭时，保存并退出。使用**取消**可放弃本次编辑。

## Overlay

在 **更多 → Overlay** 中调整不死图腾动画的大小、高度和速度（25–300%，100% 为原版速度），也可调整第一人称火焰高度、世界中火与灵魂火的大小、主手与副手物品的大小和高度，以及盾牌不透明度。世界火焰设置在关闭页面后应用于区块渲染。

设置保存在实例的 `config/etherhud.json`。界面支持五种语言，模块名称统一使用英文。许可为 [MIT](LICENSE)。
