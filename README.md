# EtherHUD

[English](README.md) · [日本語](README_ja.md) · [简体中文](README_zh_cn.md) · [繁體中文](README_zh_tw.md) · [한국어](README_ko.md)

EtherHUD is a client-side HUD mod for **Minecraft 1.21.4 on Fabric**. Choose which modules to show, then arrange, resize, and style them in-game.

## Installation

1. Use **Java 21** and **Fabric Loader 0.16.14 or newer** with Minecraft 1.21.4.
2. Put EtherHUD and **Fabric API 0.119.2 or newer for Minecraft 1.21.4** in the instance's `mods` folder.
3. Press **Right Shift** in-game to open EtherHUD Settings. You can also open it from Mod Menu.

Mod Menu and Cloth Config are optional. EtherHUD is distributed under the [MIT License](LICENSE).

## Settings and tips

- Toggle each module individually or use the master switch to show or hide the entire HUD. Your individual choices are kept.
- Open a module's **Settings** to adjust its position, scale, anchor, text, background, outline, and corner radius. Use the color picker and live preview to dial in its appearance.
- The first time you open settings, six short tips highlight the controls. You can use the controls while reading. Choose **More → Replay tips** to see the guide again.
- **More** also contains bulk toggles, Overlay, reset, and the shader rendering switch. Settings are saved when you close the screen.
- The interface supports English, Japanese, Simplified Chinese, Traditional Chinese, and Korean. Module names appear in English in every language.

## HUD modules

| Module | Shows |
| --- | --- |
| FPS | Current frame rate |
| Coordinates | Player coordinates |
| CPS | Left and right clicks per second, shown as `8 \| 6 CPS` |
| Armor Status | Main hand, offhand, armor, and durability |
| Ping | Multiplayer latency |
| TPS | Estimated server tick rate from time updates |
| Player Model | A 3D view of your player that follows your viewing direction |
| Item Total | Inventory count for the item in your main hand |
| Compass | A scrolling heading and degree scale |
| Image Overlay | A local image or animated GIF |

**Armor Status** offers spaced slots, a joined strip, and a vanilla hotbar style. All three support horizontal and vertical layouts, six configurable slots, item counts, durability display, and low durability alerts. The vanilla style can follow the side of the hotbar or an inventory screen, or be placed manually. Its size follows Minecraft's GUI scale.

**Image Overlay** accepts PNG, JPEG, GIF, BMP, WBMP, and TIFF. Animated GIFs loop. If you move or rename the file, select it again in the module settings.

## HUD Editor

Choose **HUD Editor** in EtherHUD Settings. You can assign a shortcut in Minecraft's key bindings.

| Input | Action |
| --- | --- |
| Click / drag a module | Select / move |
| Ctrl + right-click | Add or remove a module from the selection |
| Drag from empty space | Select an area |
| Corner handles / Ctrl + scroll | Resize the selection |
| Right-click a module | Open its actions and detailed settings |
| Arrow keys / Shift + arrow keys | Move by 1 / 10 GUI pixels |
| Ctrl + Tab / Ctrl + Shift + Tab | Select the next / previous module |
| Ctrl + Z / Ctrl + Y | Undo / redo, up to 20 actions |
| H | Show or hide the toolbar |
| Esc | Close the context menu, or save and close the editor |
| Cancel | Discard the editor session's changes and close |

Use the toolbar to control snapping, the grid, and previews of disabled modules. Click a dashed preview to enable that module.

## Overlay

Open **More → Overlay** for the following controls.

| Setting | Adjustment |
| --- | --- |
| Totem of Undying animation | Size (10–200%), height, and speed (25–300%). 100% speed is the Minecraft default |
| First-person fire | Height of the fire overlay while burning |
| World fire | Size of fire and soul fire (25–200%). Chunk meshes are rebuilt after you close the screen |
| Held items | Size and height for the main hand and offhand separately |
| Shield | Opacity |

## Configuration and development

Settings are stored in `config/etherhud.json` in your instance, or `run/config/etherhud.json` when using the development client. Missing fields use defaults, and invalid JSON files are backed up with a `.bak` suffix.

Build with `./gradlew.bat build` on Windows or `./gradlew build` on macOS or Linux. Use the `runClient` task to launch the development client.

Rounded shapes use Minecraft 1.21.4's `ShaderManager#getProgramForLoading`, `DefaultVertexFormat.NEW_ENTITY`, and `GuiGraphics` buffers. Shaders are under `assets/etherhud/shaders/core/`. The renderer falls back to rectangle-based drawing when shader rendering is disabled or the shader fails to load. World fire uses Fabric's `modifyBlockModelAfterBake` hook, and totem speed adjusts the animation duration and progress calculation in `GameRenderer`.
