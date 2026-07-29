# SAP Animation Exporter 使用文档 / User Guide

- [中文](#中文)
- [English](#english)

---

# 中文

## 1. 插件简介

`sap_animation_exporter.js` 是 Shadows And Petals 的 Blockbench 桌面版插件，用于制作玩家使用物品时的第一人称与第三人称动画。

插件会创建带有玩家参考模型、手持物品预览、相机预设和基础 HUD 的专用工作区，并导出以下三类资源：

- SAP 骨架（rig）
- SAP 动画控制器（controller）
- NeoForge 实体动画片段（clip）

插件只负责制作与导出资源。导出后还需要在模组客户端代码中注册 `UseAnimationProfile`，并由物品代码决定动画的触发条件、状态和播放时间。

## 2. 环境要求与安装

要求：

- Blockbench 桌面版 5.0.6 或更高版本
- 本插件不支持 Blockbench Web 版

安装：

1. 打开 Blockbench 的插件窗口。
2. 选择从文件加载插件。
3. 选择 `tools/blockbench/sap_animation_exporter.js`。
4. 安装成功后，新建项目列表中会出现“SAP 使用动画”格式。

开发期间修改插件源码后，可在插件窗口重新加载插件，或重启 Blockbench。

## 3. 快速开始

1. 选择“文件 → 新建 → 新建 SAP 使用动画项目”。
2. 勾选需要的第一人称和/或第三人称工作区。
3. 选择 Steve（宽手臂）或 Alex（细手臂）参考模型。
4. 填写命名空间及骨架/控制器路径。
5. 使用“文件 → 导入 → 设置 SAP 预览物品”加载物品模型。
6. 进入 Blockbench 的动画模式，新建动画并给可导出骨骼制作关键帧。
7. 需要时填写事件、速度、叠加模式和控制器过渡。
8. 选择“文件 → 导出 → 导出 SAP 动画资源包”，将结果导出到 `src/main/resources` 或 `src/generated/resources`。
9. 在 Java 客户端代码中注册导出的骨架、控制器和动画片段。

请保存 Blockbench 工程文件（通常为 `.bbmodel`）。导出的 JSON 不能代替工程源文件。

## 4. 新建项目

新建项目对话框包含以下选项：

| 选项 | 说明 |
| --- | --- |
| 第一人称工作区 | 创建第一人称双臂及左右物品 socket |
| 第三人称工作区 | 创建完整玩家参考模型及左右手物品预览锚点 |
| 显示第一人称基础 HUD | 在第一人称相机中显示准星、生命值、饱食度、经验条和快捷栏 |
| 原版玩家参考模型 | `Steve` 为 4 像素宽手臂，`Alex` 为 3 像素宽手臂 |
| 命名空间 | 资源命名空间，例如 `shadowsandpetals` |
| 骨架/控制器路径 | 骨架和控制器的默认资源路径，例如 `animation/hammer` |

至少选择一个人称工作区。

> “配置 SAP 使用动画项目”中的人称列表只修改项目配置，不会自动创建或删除模板骨骼。因此应在新建项目时选好所需工作区；如果后来新增人称，建议重新创建项目或手动建立完全一致的骨架。

## 5. 工作区结构

### 5.1 组角色

每个组都有一个 `SAP Role`：

| 角色 | 是否导出 | 用途 |
| --- | --- | --- |
| `animated_bone` | 是 | 可制作位置、旋转和缩放关键帧的运行时骨骼 |
| `socket` | 是 | 运行时附着点，例如第一人称手持物品位置 |
| `reference` | 否 | 玩家或物品的可视化参考 |
| `guide` | 否 | 工作区根节点、相机辅助结构和第三人称物品锚点 |

不要给 `reference` 或 `guide` 组制作动画。插件会拒绝导出带关键帧的预览物品组和第三人称物品锚点。

### 5.2 第一人称

默认可导出结构：

```text
SAP 第一人称 (guide)
├─ first_person_right_arm (animated_bone)
│  ├─ 第一人称右臂参考 (reference)
│  └─ first_person_right_item (socket)
└─ first_person_left_arm (animated_bone)
   ├─ 第一人称左臂参考 (reference)
   └─ first_person_left_item (socket)
```

- 给 `first_person_*_arm` 打关键帧可同时移动手臂和物品。
- 给 `first_person_*_item` 打关键帧可让物品相对手臂独立运动。
- 第一人称使用全局变换空间；切换到第一人称相机时插件会自动设置。
- 导出时第一人称骨骼使用局部原点作为静止平移，并以零 pivot 进入运行时。

### 5.3 第三人称

默认可导出骨骼：

```text
SAP 第三人称 (guide)
└─ root
   ├─ body
   ├─ head
   ├─ right_arm
   ├─ left_arm
   ├─ right_leg
   └─ left_leg
```

第三人称物品始终跟随对应玩家手臂：

- 右手物品跟随 `right_arm`
- 左手物品跟随 `left_arm`

`main_hand_item`、`off_hand_item` 及其父级只用于预览，不是运行时 socket，不要给它们打关键帧。

插件会自动把 Blockbench 第三人称旋转换算为 Minecraft 坐标约定，请不要手动反转 X/Z 旋转。

## 6. 预览工具

### 6.1 人称相机

“视图”菜单提供：

- `SAP 相机：第一人称`
- `SAP 相机：第三人称`
- `切换 SAP 第一人称左手显示`
- `切换 SAP 第一人称基础 HUD`

相机、左手显隐和 HUD 都只影响编辑器预览，不会写入动画资源。第三人称正面视图中的左右位置采用 Minecraft 渲染坐标，看起来会像面对真实人物时一样左右相反。

### 6.2 玩家皮肤

选择“文件 → 导入 → 加载 SAP 玩家皮肤”，可用本地 PNG 替换参考模型纹理。

限制：

- 文件必须是有效 PNG。
- 尺寸必须严格为 `64 × 64`。
- 加载皮肤只替换纹理，不会在 Steve 与 Alex 手臂几何之间切换。

### 6.3 手持物品预览

选择“文件 → 导入 → 设置 SAP 预览物品”，再选择左手或右手以及 Minecraft Java 物品模型 JSON。

如果项目同时包含第一与第三人称，插件会在两个工作区中同时创建该手的预览。再次设置同一只手会替换旧预览；可通过“工具 → 清除 SAP 预览物品”移除。

插件会：

- 解析模型父级；
- 加载引用纹理；
- 应用 `firstperson_*hand` 与 `thirdperson_*hand` display 变换；
- 对缺少左手 display 的模型使用右手 display 并进行镜像；
- 为 `builtin/generated`、`item/generated` 和 `item/handheld` 创建平面预览。

资源解析规则：

- 如果选择的模型位于某个 `assets/<namespace>/...` 下，插件会自动推断资源根目录和命名空间。
- 其他资源包或依赖模组资源可在“工具 → 配置 SAP 使用动画项目”的“本地资源根目录”中填写。
- 多个资源根目录使用英文分号 `;` 分隔。
- 每个资源根目录都应直接包含 `assets` 文件夹。

MC 26 的直接 `minecraft:model` 物品包装 JSON 可以解析；属性选择、条件、范围分派等复杂物品模型分派器不能作为预览入口。遇到此类模型时，请直接选择最终的 `assets/<namespace>/models/item/*.json`。

预览物品不会导出。请给第一人称手臂/socket 或第三人称玩家手臂制作动画，而不是给导入的物品几何制作关键帧。

## 7. 制作动画

### 7.1 基本流程

1. 进入 Blockbench 的动画模式。
2. 新建动画。
3. 使用最终希望生成的资源路径作为动画名，例如 `use/hammer`。
4. 设置动画长度和循环模式。
5. 只给 `animated_bone` 或第一人称 `socket` 制作关键帧。
6. 根据需要编辑动画的 SAP 自定义属性。

支持导出的位置、旋转和缩放通道。插值仅支持：

- Linear
- Catmull-Rom

Catmull-Rom 会导出为 `minecraft:catmullrom`，其他插值类型一律导出为 `minecraft:linear`。使用其他曲线不会自动烘焙中间帧。

第三人称预览物品没有独立运行时变换；如需物品相对手臂摆动，只能在运行时代码中增加对应 socket/binding 后，再扩展骨架与插件工程。

### 7.2 动画名称

动画名称会被规范化为小写资源路径：

- `animation.use_hammer` → `use_hammer`
- `animation.use.hammer` → `use/hammer`
- `UseHammer` → `use_hammer`

不同动画规范化后的名称必须唯一。名称同时用作：

- 控制器 state 名称；
- 动画片段资源路径；
- 动画片段 ID 的 path 部分。

控制器的 `initial` 是本次导出列表中的第一个动画。只导出当前动画时，该动画会成为初始 state。

### 7.3 动画自定义属性

选中动画后可编辑以下 SAP 属性：

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `SAP Events` / `sap_events` | `[]` | 按时间升序排列的事件数组 |
| `SAP Speed` / `sap_speed` | `1` | 播放速度，必须为有限且不小于 0 的数；`0` 会冻结动画 |
| `SAP Additive` / `sap_additive` | `false` | 运行时是否以叠加方式混合该 state |

事件示例：

```json
[
  {
    "time": 0.15,
    "id": "shadowsandpetals:hammer_swing"
  },
  {
    "time": 0.42,
    "id": "shadowsandpetals:hammer_hit"
  }
]
```

事件要求：

- `time` 单位为秒；
- 必须按时间升序排列；
- 必须位于 `0` 到动画长度之间；
- `id` 必须是完整的 `namespace:path`。

事件只会写入控制器。具体游戏行为仍需运行时代码消费事件 ID。

### 7.4 控制器过渡

在“工具 → 配置 SAP 使用动画项目”中填写“控制器过渡 JSON”：

```json
[
  {
    "from": "windup",
    "to": "strike",
    "duration": 0.08
  },
  {
    "from": "strike",
    "to": "recover",
    "duration": 0.12
  }
]
```

`from` 和 `to` 必须是规范化后的动画/state 名称，`duration` 为非负秒数。同一方向的 state 对不能重复。

过渡配置只提供两个 state 之间的混合时长，不会根据条件自动切换 state。state 选择仍由物品或其他运行时代码负责。

### 7.5 静止姿势

选中 `animated_bone` 或 `socket` 后，使用“工具 → 编辑 SAP 骨骼静止姿势”设置静止平移和缩放。静止旋转来自组本身的旋转值。

注意：

- 缩放各轴必须为非零有限值。
- 第一人称静止平移会同步移动其参考结构。
- 不要随意改动模板骨骼名称、父子关系或预览角色，否则运行时 binding 和预览修复逻辑可能失效。

## 8. 项目配置

“工具 → 配置 SAP 使用动画项目”包含：

| 配置 | 说明 |
| --- | --- |
| 人称配置 | 仅允许 `first_person`、`third_person`，多个值以逗号分隔 |
| 命名空间 | 导出资源的 namespace |
| 骨架路径 | SAP rig 的资源路径 |
| 控制器路径 | SAP controller 的资源路径 |
| 本地资源根目录 | 解析预览模型父级和纹理时使用，多个目录以 `;` 分隔 |
| 显示第一人称基础 HUD | 编辑器预览设置 |
| 控制器过渡 JSON | `from`、`to`、`duration` 数组 |

命名空间只能使用小写字母、数字、下划线、点和连字符，并必须以字母或数字开头。资源路径允许小写字母、数字、下划线、点、斜杠和连字符；不能以斜杠开头或结尾，也不能包含空路径段、`.` 或 `..` 段。

## 9. 导出

选择“文件 → 导出 → 导出 SAP 动画资源包”，设置：

- 命名空间；
- 骨架路径；
- 控制器路径；
- 导出全部动画或仅导出当前动画。

推荐选择以下任一目录作为导出根目录：

```text
<project>/src/main/resources
```

或：

```text
<project>/src/generated/resources
```

以命名空间 `shadowsandpetals`、骨架路径 `animation/hammer`、控制器路径 `animation/hammer`、动画名 `use/hammer` 为例，插件会生成：

```text
assets/
└─ shadowsandpetals/
   ├─ sap/
   │  └─ animations/
   │     ├─ rigs/
   │     │  └─ animation/hammer.json
   │     └─ controllers/
   │        └─ animation/hammer.json
   └─ neoforge/
      └─ animations/
         └─ entity/
            └─ use/hammer.json
```

对应资源 ID：

```text
rig:        shadowsandpetals:animation/hammer
controller: shadowsandpetals:animation/hammer
clip:       shadowsandpetals:use/hammer
state:      use/hammer
```

导出前插件会检查：

- 至少存在一个可导出动画和一个可导出骨骼/socket；
- 骨骼名称非空且不重复；
- 动画路径非空且不重复；
- 动画长度大于 0；
- 动画引用的骨骼存在于 rig；
- 预览物品和第三人称物品锚点没有关键帧；
- 事件和资源 ID 合法；
- 数值为有限值，静止缩放不为 0。

运行时资源加载器还要求同一通道的关键帧时间严格递增、不能重复，并且所有关键帧都位于动画长度内。

## 10. Java 运行时接入

导出资源不会自动注册。应在 `SAPAnimations.register()` 中创建并注册一个 `UseAnimationProfile`。下面是第一与第三人称同时启用的简化示例：

```java
var rig = new AnimationResourceRef.Rig(
        ShadowsAndPetals.asResource("animation/hammer"));
var controller = new AnimationResourceRef.Controller(
        ShadowsAndPetals.asResource("animation/hammer"));
var clip = new AnimationResourceRef.Clip(
        ShadowsAndPetals.asResource("use/hammer"));
var state = new AnimationResourceRef.State(controller, "use/hammer");

SAPAnimationRegistry.register(new UseAnimationProfile(
        ShadowsAndPetals.asResource("hammer_use"),
        rig,
        controller,
        Set.of(clip),
        state,
        new UseAnimationProfile.FirstPersonBinding(
                HumanoidArm.RIGHT,
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                Map.of(
                        HumanoidArm.RIGHT,
                        new AnimationResourceRef.Socket(
                                rig, "first_person_right_item"),
                        HumanoidArm.LEFT,
                        new AnimationResourceRef.Socket(
                                rig, "first_person_left_item")
                )
        ),
        new UseAnimationProfile.ThirdPersonBinding(
                HumanoidArm.RIGHT,
                UseAnimationProfile.MirrorPolicy.MIRROR_TO_USE_ARM,
                ModelPartRigBinder.RotationMode.REPLACE,
                Map.of(
                        UseAnimationProfile.HumanoidBone.RIGHT_ARM,
                        new AnimationResourceRef.Bone(rig, "right_arm"),
                        UseAnimationProfile.HumanoidBone.LEFT_ARM,
                        new AnimationResourceRef.Bone(rig, "left_arm")
                )
        )
));
```

注册时使用的 rig、controller、clip、state、bone 和 socket 名称必须与导出结果完全一致。只需要某一个人称时，可将另一个 binding 设为 `null`。

物品渲染代码负责：

- 判断动画是否应当播放；
- 选择 controller state；
- 计算从 0 开始的本地动画时间；
- 调用 `UseAnimationPlayer.applyFirstPerson(...)` 或 `UseAnimationPlayer.applyThirdPerson(...)`；
- 根据需要消费 controller 事件。

资源在客户端资源重载时加载并校验。缺失文件、未注册 clip、错误 bone/socket、非法 state 或非法过渡都会使资源重载失败，并在日志中给出原因。

## 11. 常见问题

### 导入物品时提示无法解析父模型或纹理

确认选择的文件位于标准 `assets/<namespace>/models/...` 结构中，或在项目配置中添加正确的资源根目录。资源根目录应是 `assets` 的父目录。

### 导入 MC 26 物品 JSON 时提示不支持分派器

选择分派器最终指向的具体 `models/item/*.json`，不要选择包含条件、属性选择或范围分派的入口文件。

### 导出提示预览物品存在关键帧

删除预览物品组、`main_hand_item` 或 `off_hand_item` 上的关键帧，把动画移到第一人称 arm/socket 或第三人称 `right_arm`/`left_arm`。

### 第一人称设置预览物品后手臂消失

这是预期的预览行为。物品存在时插件会隐藏内置第一人称参考臂，避免参考几何遮挡物品。实际手臂是否渲染由运行时代码决定。

### 修改人称配置后没有出现新工作区

项目配置不会生成模板。请在新建项目时选择所需人称，或重新创建工程。

### 游戏中没有动画，但 JSON 已经导出

检查相应 `UseAnimationProfile` 是否已在 `SAPAnimations.register()` 中注册，以及物品渲染代码是否调用了 `UseAnimationPlayer`。

## 12. Blockbench 脚本 API

插件加载后会暴露只读全局对象：

```js
globalThis.SAPAnimationStudio
```

可在 Blockbench 开发者控制台或其他本地插件中调用：

```js
SAPAnimationStudio.createProject({
    profiles: ["first_person", "third_person"],
    playerModel: "steve",
    namespace: "shadowsandpetals",
    rigPath: "animation/hammer",
    controllerPath: "animation/hammer",
    resourceRoots: "D:/Game/MinecraftMod/NeoForge/ShadowsAndPetals/src/main/resources",
    showLeftHand: true,
    showFirstPersonHud: true,
    transitions: []
});

SAPAnimationStudio.setPreviewItem(
    "D:/path/to/assets/example/models/item/hammer.json",
    {hand: "right"}
);

SAPAnimationStudio.clearPreviewItem({hand: "right"});
SAPAnimationStudio.loadPlayerSkin("D:/path/to/skin.png");
SAPAnimationStudio.activateProfile("first_person");
SAPAnimationStudio.setFirstPersonHudVisible(false);

SAPAnimationStudio.exportBundle(
    "D:/path/to/project/src/main/resources",
    {
        namespace: "shadowsandpetals",
        rigPath: "animation/hammer",
        controllerPath: "animation/hammer"
    }
);
```

其他可用成员：

- `version`
- `generateRig()`

---

# English

## 1. Overview

`sap_animation_exporter.js` is a desktop Blockbench plugin for authoring first-person and third-person player use animations for Shadows And Petals.

It creates a dedicated workspace with player references, held-item previews, camera presets, and a basic HUD. It exports:

- an SAP animation rig;
- an SAP animation controller;
- NeoForge entity animation clips.

The plugin only authors and exports resources. The mod must still register a `UseAnimationProfile` on the client, while item code remains responsible for trigger rules, state selection, and local animation time.

## 2. Requirements and installation

Requirements:

- Blockbench Desktop 5.0.6 or newer
- The Blockbench web app is not supported

Installation:

1. Open the Blockbench plugin window.
2. Choose the option to load a plugin from a file.
3. Select `tools/blockbench/sap_animation_exporter.js`.
4. The “SAP 使用动画” format will appear in the new-project list.

During plugin development, reload it from the plugin window or restart Blockbench after changing the source file.

The current plugin UI is Chinese. The menu labels quoted below match the actual UI.

## 3. Quick start

1. Select “文件 → 新建 → 新建 SAP 使用动画项目”.
2. Enable the first-person and/or third-person workspace.
3. Choose the Steve wide-arm or Alex slim-arm reference.
4. Enter the namespace and rig/controller path.
5. Use “文件 → 导入 → 设置 SAP 预览物品” to load an item model.
6. Switch to Animate mode, create an animation, and keyframe exported bones.
7. Configure events, speed, additive playback, and controller transitions if needed.
8. Use “文件 → 导出 → 导出 SAP 动画资源包” and select `src/main/resources` or `src/generated/resources`.
9. Register the exported resources from client-side Java code.

Save the Blockbench source project, normally as a `.bbmodel`. Exported JSON files are not a replacement for the editable source project.

## 4. Creating a project

The new-project dialog contains:

| Option | Meaning |
| --- | --- |
| 第一人称工作区 | Creates both first-person arms and their item sockets |
| 第三人称工作区 | Creates a full player reference and held-item preview anchors |
| 显示第一人称基础 HUD | Shows a crosshair, health, hunger, experience, and hotbar overlay |
| 原版玩家参考模型 | `Steve` uses 4-pixel arms; `Alex` uses 3-pixel arms |
| 命名空间 | Resource namespace, such as `shadowsandpetals` |
| 骨架/控制器路径 | Default path shared by the rig and controller, such as `animation/hammer` |

At least one perspective workspace is required.

> Changing the profile list later in “配置 SAP 使用动画项目” does not create or remove template bones. Select all required workspaces when creating the project. If a new perspective is needed later, recreate the project or manually reproduce the exact rig structure.

## 5. Workspace structure

### 5.1 Group roles

Every group has an `SAP Role`:

| Role | Exported | Purpose |
| --- | --- | --- |
| `animated_bone` | Yes | A runtime bone with position, rotation, and scale animation |
| `socket` | Yes | A runtime attachment point, such as a first-person held item |
| `reference` | No | Visual-only player or item geometry |
| `guide` | No | Workspace roots, helpers, and third-person item anchors |

Do not animate `reference` or `guide` groups. Export is rejected when preview item groups or third-person item anchors have keyframes.

### 5.2 First person

Default exported structure:

```text
SAP 第一人称 (guide)
├─ first_person_right_arm (animated_bone)
│  ├─ 第一人称右臂参考 (reference)
│  └─ first_person_right_item (socket)
└─ first_person_left_arm (animated_bone)
   ├─ 第一人称左臂参考 (reference)
   └─ first_person_left_item (socket)
```

- Animate `first_person_*_arm` to move the arm and item together.
- Animate `first_person_*_item` to move the item relative to its arm.
- The plugin switches Blockbench to global transform space for first-person authoring.
- First-person group-local origins become rest translations at export, while runtime pivots remain zero.

### 5.3 Third person

Default exported bones:

```text
SAP 第三人称 (guide)
└─ root
   ├─ body
   ├─ head
   ├─ right_arm
   ├─ left_arm
   ├─ right_leg
   └─ left_leg
```

Third-person held items always follow the corresponding player arm:

- the right-hand item follows `right_arm`;
- the left-hand item follows `left_arm`.

`main_hand_item`, `off_hand_item`, and their parent helpers are preview-only guides, not runtime sockets. Do not keyframe them.

The exporter automatically converts Blockbench third-person rotations to Minecraft coordinates. Do not manually negate the X or Z rotations.

## 6. Preview tools

### 6.1 Perspective cameras

The View menu contains:

- `SAP 相机：第一人称`
- `SAP 相机：第三人称`
- `切换 SAP 第一人称左手显示`
- `切换 SAP 第一人称基础 HUD`

Camera position, left-arm visibility, and HUD visibility affect the editor preview only. They are not exported. The front-facing third-person preview follows Minecraft renderer handedness, so anatomical left and right appear as they would on a real person facing the viewer.

### 6.2 Player skins

Use “文件 → 导入 → 加载 SAP 玩家皮肤” to replace the reference texture with a local PNG.

The file must be a valid `64 × 64` PNG. Loading a skin changes only the texture; it does not switch the geometry between Steve and Alex arm widths.

### 6.3 Held-item previews

Use “文件 → 导入 → 设置 SAP 预览物品”, then select a hand and a Minecraft Java item model JSON.

If both workspaces exist, the item is created in both. Setting the same hand again replaces the previous preview. Use “工具 → 清除 SAP 预览物品” to remove it.

The importer:

- resolves model parents;
- loads referenced textures;
- applies the `firstperson_*hand` and `thirdperson_*hand` display transforms;
- mirrors the right-hand display when a left-hand display is missing;
- creates a flat preview for `builtin/generated`, `item/generated`, and `item/handheld`.

Resource lookup rules:

- A model below `assets/<namespace>/...` lets the plugin infer both its resource root and namespace.
- Add resource packs or dependency-mod roots under “本地资源根目录” in the project settings.
- Separate multiple roots with an ASCII semicolon (`;`).
- Each root must directly contain the `assets` directory.

A direct MC 26 `minecraft:model` item wrapper is supported. Complex property-select, conditional, or range-dispatch item model entry points are not. For those items, select the final `assets/<namespace>/models/item/*.json` directly.

Preview items are never exported. Animate a first-person arm/socket or a third-person player arm instead of the imported item geometry.

## 7. Authoring animations

### 7.1 Basic workflow

1. Enter Animate mode.
2. Create an animation.
3. Name it after the desired resource path, such as `use/hammer`.
4. Set its length and loop mode.
5. Keyframe only `animated_bone` groups or first-person `socket` groups.
6. Edit the SAP animation properties as needed.

Position, rotation, and scale channels are exported. Supported interpolation modes are:

- Linear
- Catmull-Rom

Catmull-Rom is exported as `minecraft:catmullrom`. Every other interpolation type is exported as `minecraft:linear`; unsupported curves are not baked into intermediate keyframes.

Third-person preview items have no independent runtime transform. Supporting item motion relative to the arm requires adding a runtime socket/binding and extending both the rig and the authoring project.

### 7.2 Animation names

Animation names are normalized into lowercase resource paths:

- `animation.use_hammer` → `use_hammer`
- `animation.use.hammer` → `use/hammer`
- `UseHammer` → `use_hammer`

Normalized names must be unique. The normalized name becomes:

- the controller state name;
- the clip resource path;
- the path component of the clip ID.

The controller `initial` field is the first animation in the current export set. When exporting only the selected animation, that animation becomes the initial state.

### 7.3 Custom animation properties

Select an animation and edit:

| Property | Default | Meaning |
| --- | --- | --- |
| `SAP Events` / `sap_events` | `[]` | Event markers sorted by time |
| `SAP Speed` / `sap_speed` | `1` | Finite playback speed greater than or equal to 0; `0` freezes the clip |
| `SAP Additive` / `sap_additive` | `false` | Applies the state additively at runtime |

Example events:

```json
[
  {
    "time": 0.15,
    "id": "shadowsandpetals:hammer_swing"
  },
  {
    "time": 0.42,
    "id": "shadowsandpetals:hammer_hit"
  }
]
```

Event times are measured in seconds, must be sorted, and must fall between zero and the clip length. Each ID must be a complete `namespace:path`. Events are stored in the controller; runtime code must decide what each ID does.

### 7.4 Controller transitions

Enter transition JSON under “控制器过渡 JSON” in “工具 → 配置 SAP 使用动画项目”:

```json
[
  {
    "from": "windup",
    "to": "strike",
    "duration": 0.08
  },
  {
    "from": "strike",
    "to": "recover",
    "duration": 0.12
  }
]
```

`from` and `to` must match normalized state names. `duration` is a non-negative number of seconds. Duplicate directional state pairs are invalid.

Transitions only provide a blend duration between two named states. They do not automatically select or switch states; item or other runtime code owns that decision.

### 7.5 Rest pose

Select an `animated_bone` or `socket`, then use “工具 → 编辑 SAP 骨骼静止姿势” to edit rest translation and scale. The group's normal rotation supplies its rest rotation.

Scale components must be finite and non-zero. Editing a first-person rest translation also moves its reference tree. Avoid renaming or restructuring template bones and helpers, because runtime bindings and preview repair logic depend on their roles and canonical names.

## 8. Project settings

“工具 → 配置 SAP 使用动画项目” contains:

| Setting | Meaning |
| --- | --- |
| 人称配置 | `first_person`, `third_person`, or both separated by commas |
| 命名空间 | Export namespace |
| 骨架路径 | SAP rig resource path |
| 控制器路径 | SAP controller resource path |
| 本地资源根目录 | Roots used to resolve preview model parents and textures; separate with `;` |
| 显示第一人称基础 HUD | Editor-only preview setting |
| 控制器过渡 JSON | Array of `from`, `to`, and `duration` entries |

Namespaces may contain lowercase letters, digits, underscores, dots, and hyphens, and must begin with a letter or digit. Resource paths may also contain slashes. They cannot begin or end with a slash or contain empty, `.` or `..` segments.

## 9. Exporting

Use “文件 → 导出 → 导出 SAP 动画资源包” and configure:

- namespace;
- rig path;
- controller path;
- all animations or the selected animation only.

Choose one of these export roots:

```text
<project>/src/main/resources
```

or:

```text
<project>/src/generated/resources
```

For namespace `shadowsandpetals`, rig/controller path `animation/hammer`, and animation `use/hammer`, the output is:

```text
assets/
└─ shadowsandpetals/
   ├─ sap/
   │  └─ animations/
   │     ├─ rigs/
   │     │  └─ animation/hammer.json
   │     └─ controllers/
   │        └─ animation/hammer.json
   └─ neoforge/
      └─ animations/
         └─ entity/
            └─ use/hammer.json
```

Resource identifiers:

```text
rig:        shadowsandpetals:animation/hammer
controller: shadowsandpetals:animation/hammer
clip:       shadowsandpetals:use/hammer
state:      use/hammer
```

Before writing files, the plugin checks for missing or duplicate bones, missing animations, invalid resource paths, invalid animation lengths, missing rig targets, preview-only keyframes, invalid events, non-finite values, and zero rest scale.

The runtime loader additionally requires strictly increasing, non-duplicate keyframe times within the clip length.

## 10. Java runtime integration

Exported resources are not registered automatically. Create and register a `UseAnimationProfile` from `SAPAnimations.register()`. The complete bilingual example is in [Java 运行时接入](#10-java-运行时接入) above.

The registered rig, controller, clip, state, bone, and socket names must exactly match the exported resources. Set an unused perspective binding to `null`.

Item rendering code remains responsible for:

- deciding when playback is active;
- selecting a controller state;
- computing non-negative local animation time;
- calling `UseAnimationPlayer.applyFirstPerson(...)` or `UseAnimationPlayer.applyThirdPerson(...)`;
- consuming controller events when required.

Resources are loaded and validated during client resource reload. Missing files, unregistered clips, invalid bones/sockets, invalid states, or invalid transitions cause reload failure with a diagnostic in the log.

## 11. Troubleshooting

### A parent model or texture cannot be resolved

Ensure the selected file uses the standard `assets/<namespace>/models/...` layout, or add the correct resource root to the project settings. A resource root is the parent of `assets`.

### An MC 26 item JSON reports an unsupported dispatcher

Select the concrete `models/item/*.json` referenced by the dispatcher instead of a conditional, property-select, or range-dispatch entry point.

### Export reports keyframes on a preview item

Delete keyframes from the imported preview group, `main_hand_item`, or `off_hand_item`. Move the animation to a first-person arm/socket or the third-person `right_arm`/`left_arm`.

### The first-person reference arm disappears after setting an item

This is expected preview behavior. The plugin hides its built-in arm reference while a held-item preview exists to prevent the helper geometry from obscuring the item. Actual arm rendering is controlled by runtime code.

### Adding a profile does not create its workspace

Project settings do not generate templates. Select all perspectives when creating the project or recreate the source project.

### JSON files exist, but nothing animates in game

Verify that the `UseAnimationProfile` is registered in `SAPAnimations.register()` and that the item rendering path calls `UseAnimationPlayer`.

## 12. Blockbench scripting API

The plugin exposes an immutable global object:

```js
globalThis.SAPAnimationStudio
```

Available members:

- `version`
- `createProject(options)`
- `setPreviewItem(filePath, {hand})`
- `clearPreviewItem({hand})`
- `setFirstPersonHudVisible(visible)`
- `loadPlayerSkin(filePath)`
- `exportBundle(directory, options)`
- `activateProfile(profile)`
- `generateRig()`

See the runnable examples in [Blockbench 脚本 API](#12-blockbench-脚本-api) above.
