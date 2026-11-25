# EtherHUD

[English](README.md) · [日本語](README_ja.md) · [简体中文](README_zh_cn.md) · [繁體中文](README_zh_tw.md) · [한국어](README_ko.md)

## 安裝

EtherHUD 是適用於 **Minecraft 1.21.4 / Fabric** 的用戶端 HUD 模組。需要 **Java 21**、**Fabric Loader 0.16.14 或更新版本**，以及適用於 1.21.4 的 **Fabric API 0.119.2 或更新版本**。將 EtherHUD 和 Fabric API 放入遊戲實例的 `mods` 資料夾。在遊戲中按**右 Shift** 開啟設定，也可以透過 Mod Menu 開啟。Mod Menu 和 Cloth Config 均為選用項目。

## 設定與提示

可分別切換各模組，或用總開關暫時隱藏整個 HUD。點選模組旁的設定，可用選色器和即時預覽調整文字、背景、外框、圓角、縮放及位置。首次開啟設定會顯示六則提示，並用外框標出對應的控制項。可透過 **更多 → 重新查看提示** 再次開啟。關閉畫面時會儲存設定。

## HUD 模組

| 模組 | 顯示內容 |
| --- | --- |
| FPS | 目前的畫面更新率 |
| Coordinates | 玩家座標 |
| CPS | 滑鼠左鍵和右鍵的每秒點擊次數 |
| Armor Status | 主手、副手、盔甲與耐久度 |
| Ping | 多人遊戲的連線延遲 |
| TPS | 根據伺服器的時間更新估算的刻速率 |
| Player Model | 隨視角方向轉動的玩家 3D 模型 |
| Item Total | 物品欄中與主手物品同類型的總數 |
| Compass | 可捲動的方位與角度刻度 |
| Image Overlay | 本機圖片或動態 GIF |

Armor Status 提供分隔、連體與原版快捷列三種樣式，支援橫向與直向排列，也可設定耐久警告。原版樣式可跟隨快捷列或容器畫面的側邊，也可以手動放置，大小會隨 Minecraft 的 GUI 縮放變化。Image Overlay 支援 PNG、JPEG、GIF、BMP、WBMP 和 TIFF。

## HUD 編輯器

從設定開啟 **HUD 編輯器**。拖曳模組可移動，拖曳角點或使用 **Ctrl＋滾輪** 可縮放。按住 **Ctrl** 並按滑鼠右鍵可新增或取消選取，在空白處拖曳可框選。方向鍵可微調位置，按住 **Shift** 可增加移動距離。**Ctrl＋Z / Ctrl＋Y** 可復原或重做，**H** 可顯示或隱藏工具列。工具列可切換吸附、格線及停用模組的預覽。**Esc** 會先關閉右鍵選單；選單已關閉時，會儲存並離開。按下**取消**可捨棄本次編輯。

## Overlay

在 **更多 → Overlay** 可以調整不死圖騰動畫的大小、高度和速度（25–300%，100% 為原版速度）、第一人稱火焰高度、世界中火與靈魂火的大小、主手與副手物品的大小和高度，以及盾牌不透明度。世界火焰設定會在關閉畫面後套用至區塊繪製。

設定儲存在遊戲實例的 `config/etherhud.json`。介面支援五種語言，模組名稱統一使用英文。授權為 [MIT](LICENSE)。
