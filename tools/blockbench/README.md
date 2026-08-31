# SAP Animation Exporter 使用文档 / User Guide

- [中文](#中文)
- [English](#english)

---

# 中文

## 1. 插件简介

`sap_animation_exporter.js` 是 Shadows And Petals 的 Blockbench 桌面版插件，用于制作玩家使用物品时的第一人称/第三人称动画，也用于制作方块实体的骨骼动画。

插件按项目模式创建专用工作区：玩家模式带有玩家参考模型、手持物品预览、相机预设和基础 HUD；方块实体模式带有可配置的 rig 骨骼、方块模型预览和方块相机。两种模式都导出以下三类资源：

- SAP 骨架（rig）
- SAP 动画控制器（controller）
- NeoForge 实体动画片段（clip）

插件只负责制作与导出资源。导出后仍需在模组客户端代码中注册对应的 `UseAnimationProfile` 或 `BlockAnimationDefinition`；动画触发条件、状态选择和播放时间由运行时代码决定。

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
2. 在“动画类型”中选择“玩家使用动画”或“方块实体动画”。
3. 玩家模式下勾选需要的第一人称和/或第三人称工作区，选择 Steve/Alex；方块实体模式下填写方块骨骼 JSON。
4. 填写命名空间及骨架/控制器路径。
5. 玩家模式可用“文件 → 导入 → 设置 SAP 预览物品”；方块实体模式可用“设置 SAP 方块模型预览”把模型导入选中的骨骼。
6. 进入 Blockbench 的动画模式，新建动画并给 `animated_bone` 或 `socket` 制作关键帧。
7. 需要时填写事件、速度、叠加模式和控制器过渡。
8. 选择“文件 → 导出 → 导出 SAP 动画资源包”，将结果导出到 `src/main/resources` 或 `src/generated/resources`。
9. 在 Java 客户端代码中注册导出的骨架、控制器和动画片段。

请保存 Blockbench 工程文件（通常为 `.bbmodel`）。导出的 JSON 不能代替工程源文件。

## 4. 新建项目

新建项目对话框包含以下选项：

| 选项 | 说明 |
| --- | --- |
| 动画类型 | `玩家使用动画` 或 `方块实体动画` |
| 第一人称工作区 | 创建第一人称双臂及左右物品 socket |
| 第三人称工作区 | 创建完整玩家参考模型及左右手物品预览锚点 |
| 显示第一人称基础 HUD | 在第一人称相机中显示准星、生命值、饱食度、经验条和快捷栏 |
| 原版玩家参考模型 | `Steve` 为 4 像素宽手臂，`Alex` 为 3 像素宽手臂 |
| 命名空间 | 资源命名空间，例如 `shadowsandpetals` |
| 骨架/控制器路径 | 骨架和控制器的默认资源路径，例如 `animation/hammer` |
| 方块骨骼 JSON | 方块实体模式下创建的骨骼数组，包含 `name`、`pivot`、`parent` 和可选 `rest` |

玩家模式至少选择一个人称工作区；方块实体模式会忽略玩家工作区选项，并创建一个 `SAP 方块实体` 根节点。

方块骨骼 JSON 示例（坐标使用 Blockbench/Minecraft 的 16 像素方块坐标）：

```json
[
  {"name": "root", "pivot": [8, 0, 8]},
  {"name": "body", "pivot": [8, 16, 8], "parent": "root"},
  {"name": "main", "pivot": [8, 16, 8], "parent": "root"}
]
```

`pivot` 是运行时骨骼枢轴；`rest.translation`、`rest.rotation`、`rest.scale` 是可选的静止变换。名称必须唯一，父级必须存在且不能形成循环。

> “配置 SAP 使用动画项目”中的人称列表会为缺失的人称补充模板，但不会删除已有骨骼。因此应在新建项目时选好所需工作区；切换到方块实体模式也不会删除玩家骨骼。

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

### 5.4 方块实体

方块实体模式的结构由新建项目中的骨骼 JSON 生成：

```text
SAP 方块实体 (guide)
└─ root (animated_bone)
   ├─ body (animated_bone)
   └─ main (animated_bone)
```

每个 `animated_bone` 都能在动画模式中制作位置、旋转和缩放关键帧。方块实体工作区使用一个中心化坐标层：运行时 `pivot` 的 X/Z 会在编辑器中减去 `[8, 0, 8]`，因此方块中心位于 Blockbench 原点；导出 rig 时插件会自动加回 `[8, 0, 8]`，游戏仍收到 Minecraft 的 `0..16` 坐标。`SAP Rest Translation/Scale` 和组旋转对应 rig 的 `rest`。根节点和导入的模型预览不会导出为几何体；游戏仍使用 BER 的 baked `BlockStateModelPart`。

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

### 6.4 方块模型预览

在方块实体模式中，先在 Outliner 选中目标 `animated_bone`，再选择“文件 → 导入 → 设置 SAP 方块模型预览”。支持：

- 直接的 `assets/<namespace>/models/*.json` 方块模型；
- `assets/<namespace>/blockstates/*.json`（选择第一个 variant/multipart 模型作为编辑器预览）；
- Blockbench `.bbmodel`（仅导入 cube 元素）。

模型会作为 `reference` 预览组挂到目标骨骼下，不会写入 rig，也不会替代游戏中由方块状态烘焙的模型。重复导入同一骨骼会替换旧预览；“工具 → 清除 SAP 方块模型预览”可以按骨骼或全部清除。这样可以在可见模型上直接给骨骼制作关键帧，同时保留运行时 `AnimatedBlockModel` 的绑定方式。

导入方块模型时，模型的 `0..16` X/Z 坐标会自动映射到编辑器的 `-8..8`，Y 坐标保持不变；这样模型中心和骨骼枢轴都落在 BB 中轴线附近。导出时会自动还原到 Minecraft 的 `0..16` 坐标，请不要手动再平移。骨骼 JSON 中的 `pivot` 仍填写运行时方块坐标，例如 `[8, 9, 9]`；打开旧的方块项目时插件会自动完成一次坐标迁移。

例如当前仓库的风铃可以把 `models/block/wind_chimes/block.json` 导入 `body`。如果 `main_ribbon.json` 和 `vane.json` 都需要同时显示在 `main` 上，应先把它们合并为一个 `.bbmodel`，或者为它们建立两个独立的预览骨骼；同一骨骼上的后一次导入会替换前一次导入。

### 6.5 示例：静态模型 A 与动画模型 B

模型 A、B 在插件中只是用于观察动画效果的 `reference` 几何；真正被写入动画资源的是骨骼。对于“A 保持静止、B 参与动画”的方块，可以创建一个公共根骨骼、一个静态骨骼和一个动画骨骼：

```json
[
  {
    "name": "root",
    "pivot": [8, 0, 8]
  },
  {
    "name": "static",
    "pivot": [8, 0, 8],
    "parent": "root"
  },
  {
    "name": "moving",
    "pivot": [8, 16, 8],
    "parent": "root"
  }
]
```

创建方块实体项目后，Outliner 结构如下：

```text
SAP 方块实体
└─ root
   ├─ static
   └─ moving
```

`pivot` 使用模型自身的 `0..16` 方块坐标。B 从顶部悬挂时可使用 `[8,16,8]`；转轴位于模型中央时可使用 `[8,8,8]`。如果 B 还应继承 A 的动画，就把 `moving` 的父级改为 `static`。

导入模型：

1. 选中 `static`，执行“文件 → 导入 → 设置 SAP 方块模型预览”，选择模型 A 的具体 `models/*.json` 或 `.bbmodel`。
2. 选中 `moving`，再次执行同一命令并选择模型 B。
3. 确认模型分别出现在 `static` 和 `moving` 下方的 `reference` 预览组中。

一个骨骼同时只保存一个导入模型；再次导入会替换该骨骼的旧预览。如果 A 或 B 由多个 JSON 组成，应先合并为一个 `.bbmodel`，或者为每个部分建立独立骨骼。

制作动画时进入 Animate 模式，新建动画并使用最终资源路径作为名称，例如 `animation/my_block`。只给 `moving` 添加位置、旋转或缩放关键帧；不要给 `moving (方块预览)`、Cube 或其他 `reference` 组制作关键帧。`static` 没有关键帧时会保持静止。例如一个循环摆动可以使用：

```text
0.0 秒：moving rotation Z = -10
0.5 秒：moving rotation Z =  10
1.0 秒：moving rotation Z = -10
```

将循环模式设置为 `Loop`，然后使用“文件 → 导出 → 导出 SAP 动画资源包”并选择 `src/main/resources`。当命名空间为 `shadowsandpetals`、骨架/控制器路径和动画名均为 `animation/my_block` 时，会生成：

```text
assets/shadowsandpetals/sap/animations/rigs/animation/my_block.json
assets/shadowsandpetals/sap/animations/controllers/animation/my_block.json
assets/shadowsandpetals/neoforge/animations/entity/animation/my_block.json
```

注册路径必须与导出名称一致：

```java
SAPAnimationRegistries.blockAnimation("my_block")
        .clip("animation/my_block")
        .defaultState("animation/my_block")
        .register();
```

如果 A、B 都由 BER 的 `AnimatedBlockModel` 提交，可以分别绑定到对应骨骼：

```java
AnimatedBlockModel animatedModel = new AnimatedBlockModel(
        rig,
        List.of(
                new AnimatedBlockModel.Binding(
                        rig, "static", staticParts, staticHasTranslucency, tints),
                new AnimatedBlockModel.Binding(
                        rig, "moving", movingParts, movingHasTranslucency, tints)
        )
);

animatedModel.submit(
        state.animationPose,
        poseStack,
        collector,
        state.lightCoords
);
```

如果 A 完全不需要受到 rig 层级影响，也可以继续用普通方式提交 A，只把 B 绑定到 `moving`。此时仍可在插件中把 A 导入 `static` 或 `root`，作为编辑 B 动画时的视觉参照。

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

通常，控制器的 `initial` 是本次导出列表中的第一个动画。如果没有配置自定义过渡，并且导出器恰好找到一组完整的 `base_intro`、`base`、`base_outro` 动画，则会自动以 intro 作为 initial，并生成标准使用动画过渡。只导出当前动画时，该动画会成为初始 state。

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

对于约定的 `intro -> loop -> outro` 工作流，三个动画应使用同一基础名称，例如 `use/hammer_intro`、`use/hammer`、`use/hammer_outro`。将自定义过渡 JSON 留空时，导出器会自动生成 intro 到 loop、intro/loop 到 outro，以及快速重用时 outro 到 intro 的过渡。只要填写了任意自定义过渡，就会关闭自动推断并原样导出配置。

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
| 动画类型 | `玩家使用动画` 或 `方块实体动画`；切换后会只导出当前模式的骨骼 |
| 人称配置 | 仅允许 `first_person`、`third_person`，多个值以逗号分隔 |
| 命名空间 | 导出资源的 namespace |
| 骨架路径 | SAP rig 的资源路径 |
| 控制器路径 | SAP controller 的资源路径 |
| 本地资源根目录 | 解析预览模型父级和纹理时使用，多个目录以 `;` 分隔 |
| 显示第一人称基础 HUD | 编辑器预览设置 |
| 控制器过渡 JSON | `from`、`to`、`duration` 数组 |
| 方块骨骼 JSON | 方块实体模式下补充尚不存在的骨骼，并保存模板元数据 |

命名空间只能使用小写字母、数字、下划线、点和连字符，并必须以字母或数字开头。资源路径允许小写字母、数字、下划线、点、斜杠和连字符；不能以斜杠开头或结尾，也不能包含空路径段、`.` 或 `..` 段。

## 9. 导出

选择“文件 → 导出 → 导出 SAP 动画资源包”，设置：

- 命名空间；
- 骨架路径；
- 控制器路径；
- 导出全部动画或仅导出当前动画。

玩家与方块实体使用相同的导出按钮和资源目录布局。方块实体模式下只会收集 `SAP 方块实体` 根节点内的 `animated_bone`/`socket`；导入的方块模型预览组不会导出。

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

导出资源不会自动注册。应在 `SAPAnimations` 中通过 builder 创建并注册一个 `UseAnimationProfile`。下面是第一与第三人称同时启用的三段动画示例：

```java
public static final UseAnimationProfile HAMMER =
        SAPAnimationRegistries.useAnimation("hammer")
                .clip("use/hammer_intro")
                .clip("use/hammer")
                .clip("use/hammer_outro")
                .sequence(
                        "use/hammer_intro",
                        "use/hammer",
                        "use/hammer_outro")
                .firstPerson()
                .thirdPerson()
                .register();
```

注册时使用的 rig、controller、clip、state、bone 和 socket 名称必须与导出结果完全一致。只需要某一个人称时，仅调用对应的 binding 方法。

物品渲染代码负责：

- 判断动画是否应当播放；
- 在客户端 tick 中把使用状态交给 `UseAnimationPlaybackManager`；
- 从第一、第三人称渲染入口读取同一个 playback；
- 将 playback 采样的 `RigPose` 交给 `UseAnimationPlayer`；
- 根据需要消费 controller 事件。

资源在客户端资源重载时加载并校验。缺失文件、未注册 clip、错误 bone/socket、非法 state 或非法过渡都会使资源重载失败，并在日志中给出原因。

### 方块实体动画

方块实体也可以复用同一套 rig、controller 和 NeoForge clip。先在 `SAPAnimations` 中注册方块动画：

```java
public static final BlockAnimationDefinition WIND_CHIME =
        SAPAnimationRegistries.blockAnimation("wind_chime")
                .clip("animation/wind_chime")
                .defaultState("animation/wind_chime")
                .register();
```

在插件方块实体模式中，建议把默认动画命名为 `animation/wind_chime`，这样会同时生成上面示例中的 clip 和 controller state；如果把动画命名为 `idle`，则将 builder 的 clip/state 改成 `idle`。BER 在 `extractRenderState` 中采样 `RigPose`，并用 `AnimatedBlockModel.Binding` 将每个 rig bone 绑定到已烘焙的 `BlockStateModelPart`。`AnimatedBlockModel.submit(...)` 会自动处理父骨骼链、pivot、旋转、缩放、透明层、tint 以及 push/pop；`submit` 中只需保留方块朝向或整体偏移。流体、火焰等非方块模型几何仍可作为独立的自定义层提交。

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

### 方块模型预览为空或无法导入

请直接选择具体的 `models/*.json`、包含可解析 variant 的 `blockstates/*.json` 或只含 cube 的 `.bbmodel`。如果模型引用了其他资源包，请把资源根目录（直接包含 `assets` 的目录）加入项目配置；预览模型的几何体仍然是非导出 reference。

### 方块动画导出后骨骼不生效

确认关键帧加在 `SAP 方块实体` 根节点下的 `animated_bone` 上，并且 Java `BlockAnimationDefinition` 使用了相同的 rig/controller/clip 路径和 bone 名称。方块模型几何体由 BER 的 `AnimatedBlockModel.Binding` 绑定，不能通过给 reference 组打关键帧来改变导出 rig。

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

SAPAnimationStudio.createProject({
    mode: "block_entity",
    namespace: "shadowsandpetals",
    rigPath: "animation/wind_chime",
    controllerPath: "animation/wind_chime",
    blockBones: [
        {name: "root", pivot: [8, 0, 8]},
        {name: "body", pivot: [8, 16, 8], parent: "root"},
        {name: "main", pivot: [8, 16, 8], parent: "root"}
    ],
    blockModelPath: "D:/path/to/assets/shadowsandpetals/models/block/wind_chimes/block.json",
    blockModelBone: "body"
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

SAPAnimationStudio.setBlockModel(
    "D:/path/to/assets/shadowsandpetals/models/block/wind_chime.json",
    {bone: "body"}
);
SAPAnimationStudio.clearBlockModel({bone: "body"});
```

其他可用成员：

- `version`
- `generateRig()`
- `parseBlockBones(value)`
- `setBlockModel(filePath, {bone})`
- `clearBlockModel({bone})`

---

# English

## 1. Overview

`sap_animation_exporter.js` is a desktop Blockbench plugin for authoring first-person/third-person player use animations and block-entity bone animations for Shadows And Petals.

It creates a mode-specific workspace: player mode has player references, held-item previews, camera presets, and a basic HUD; block-entity mode has configurable rig bones, block-model previews, and a block camera. Both modes export:

- an SAP animation rig;
- an SAP animation controller;
- NeoForge entity animation clips.

The plugin only authors and exports resources. The mod must still register a `UseAnimationProfile` or `BlockAnimationDefinition`, while runtime code remains responsible for trigger rules, state selection, and local animation time.

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
2. Choose `玩家使用动画` or `方块实体动画` in `动画类型`.
3. In player mode, enable the required perspective workspaces and choose Steve/Alex; in block mode, enter the block-bone JSON.
4. Enter the namespace and rig/controller path.
5. In player mode use “文件 → 导入 → 设置 SAP 预览物品”; in block mode use “设置 SAP 方块模型预览” for the selected bone.
6. Switch to Animate mode, create an animation, and keyframe `animated_bone` or `socket` groups.
7. Configure events, speed, additive playback, and controller transitions if needed.
8. Use “文件 → 导出 → 导出 SAP 动画资源包” and select `src/main/resources` or `src/generated/resources`.
9. Register the exported resources from client-side Java code.

Save the Blockbench source project, normally as a `.bbmodel`. Exported JSON files are not a replacement for the editable source project.

## 4. Creating a project

The new-project dialog contains:

| Option | Meaning |
| --- | --- |
| 动画类型 | `玩家使用动画` (player use) or `方块实体动画` (block entity) |
| 第一人称工作区 | Creates both first-person arms and their item sockets |
| 第三人称工作区 | Creates a full player reference and held-item preview anchors |
| 显示第一人称基础 HUD | Shows a crosshair, health, hunger, experience, and hotbar overlay |
| 原版玩家参考模型 | `Steve` uses 4-pixel arms; `Alex` uses 3-pixel arms |
| 命名空间 | Resource namespace, such as `shadowsandpetals` |
| 骨架/控制器路径 | Default path shared by the rig and controller, such as `animation/hammer` |
| 方块骨骼 JSON | Bone array for block mode with `name`, `pivot`, `parent`, and optional `rest` |

Player mode requires at least one perspective workspace. Block mode ignores the player workspace options and creates a `SAP 方块实体` root.

Example block-bone JSON (coordinates use the 16-pixel Minecraft/Blockbench block space):

```json
[
  {"name": "root", "pivot": [8, 0, 8]},
  {"name": "body", "pivot": [8, 16, 8], "parent": "root"},
  {"name": "main", "pivot": [8, 16, 8], "parent": "root"}
]
```

`pivot` is the runtime bone pivot. `rest.translation`, `rest.rotation`, and `rest.scale` are optional rest transforms. Bone names must be unique, parents must exist, and the hierarchy must be acyclic.

> Changing the profile list later in “配置 SAP 使用动画项目” adds missing perspective templates but never deletes existing bones. Switching to block mode also keeps the player bones in the source file; export is isolated to the active mode.

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

### 5.4 Block entities

Block mode builds its hierarchy from the bone JSON entered when the project is created:

```text
SAP 方块实体 (guide)
└─ root (animated_bone)
   ├─ body (animated_bone)
   └─ main (animated_bone)
```

Every `animated_bone` can receive position, rotation, and scale keyframes. Block-entity workspaces use a centered coordinate layer: runtime bone pivot X/Z values are **reduced by** `[8, 0, 8]` for the editor, so the block center sits on the Blockbench origin; rig export adds `[8, 0, 8]` back and Minecraft still receives its `0..16` coordinates. `SAP Rest Translation/Scale` and the group rotation become the rig `rest` transform. The root and imported preview geometry are not exported; the game still uses its baked `BlockStateModelPart` geometry.

## 6. Preview tools

### 6.1 Perspective cameras

The View menu contains:

- `SAP 相机：第一人称`
- `SAP 相机：第三人称`
- `切换 SAP 第一人称左手显示`
- `切换 SAP 第一人称基础 HUD`

Camera position, left-arm visibility, and HUD visibility affect the editor preview only. They are not exported. The front-facing third-person preview follows Minecraft renderer handedness, so anatomical left and right appear as they would on a real person facing the viewer.

Block mode adds `SAP 相机：方块实体`, which frames the 16×16×16 block space.

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

### 6.4 Block-model previews

In block mode, select an `animated_bone` in the Outliner and choose “文件 → 导入 → 设置 SAP 方块模型预览”. The importer accepts:

- a concrete `assets/<namespace>/models/*.json` block model;
- an `assets/<namespace>/blockstates/*.json` file (the first variant/multipart model is used for the editor preview);
- a Blockbench `.bbmodel` containing cube elements.

The model is attached as a `reference` preview group under the selected bone. It is not written into the rig and does not replace the baked model used by the game. Importing again on the same bone replaces the previous preview; “工具 → 清除 SAP 方块模型预览” removes one bone or all previews. This lets you keyframe the visible block while retaining the runtime `AnimatedBlockModel` binding.

When importing a block model, its `0..16` X/Z coordinates are mapped to `-8..8` in the editor while Y is unchanged, putting the model center and bone pivots on the BB axes. Export restores Minecraft's `0..16` coordinates automatically; do not add another manual translation. Bone JSON `pivot` values remain runtime block coordinates, such as `[8, 9, 9]`. Opening a block project saved by an older plugin version migrates its coordinates once.

For the repository's wind chime, import `models/block/wind_chimes/block.json` into `body`. To display both `main_ribbon.json` and `vane.json` on `main`, first merge them into one `.bbmodel` or create separate preview bones; a later import on the same bone replaces the previous one.

### 6.5 Example: static model A and animated model B

Models A and B are `reference` geometry used to inspect the animation in the editor. The bones, rather than the preview geometry, are written to the animation resources. For a block where A remains static and B moves, create a common root, a static bone, and an animated bone:

```json
[
  {
    "name": "root",
    "pivot": [8, 0, 8]
  },
  {
    "name": "static",
    "pivot": [8, 0, 8],
    "parent": "root"
  },
  {
    "name": "moving",
    "pivot": [8, 16, 8],
    "parent": "root"
  }
]
```

The resulting Outliner structure is:

```text
SAP 方块实体
└─ root
   ├─ static
   └─ moving
```

`pivot` uses the model's own `0..16` block coordinates. Use `[8,16,8]` for a part suspended from the top, or `[8,8,8]` for a central axle. Make `moving` a child of `static` instead if B must inherit animation authored on A.

Import the models as follows:

1. Select `static`, run “文件 → 导入 → 设置 SAP 方块模型预览”, and choose model A's concrete `models/*.json` or `.bbmodel`.
2. Select `moving`, run the same command, and choose model B.
3. Confirm that the models appear in separate `reference` preview groups below `static` and `moving`.

Each bone stores one imported model at a time; another import replaces that bone's previous preview. If A or B consists of multiple JSON files, merge them into a `.bbmodel` first or create a separate bone for each part.

In Animate mode, create an animation whose name is its final resource path, such as `animation/my_block`. Add position, rotation, or scale keyframes only to `moving`; do not keyframe `moving (方块预览)`, cubes, or other `reference` groups. With no keyframes on `static`, A remains stationary. A simple loop could use:

```text
0.0 s: moving rotation Z = -10
0.5 s: moving rotation Z =  10
1.0 s: moving rotation Z = -10
```

Set the loop mode to `Loop`, choose “文件 → 导出 → 导出 SAP 动画资源包”, and export to `src/main/resources`. With namespace `shadowsandpetals` and `animation/my_block` used for the rig, controller, and animation name, the result is:

```text
assets/shadowsandpetals/sap/animations/rigs/animation/my_block.json
assets/shadowsandpetals/sap/animations/controllers/animation/my_block.json
assets/shadowsandpetals/neoforge/animations/entity/animation/my_block.json
```

The registration paths must match the exported names:

```java
SAPAnimationRegistries.blockAnimation("my_block")
        .clip("animation/my_block")
        .defaultState("animation/my_block")
        .register();
```

If the BER submits both A and B through `AnimatedBlockModel`, bind the baked parts to their corresponding bones:

```java
AnimatedBlockModel animatedModel = new AnimatedBlockModel(
        rig,
        List.of(
                new AnimatedBlockModel.Binding(
                        rig, "static", staticParts, staticHasTranslucency, tints),
                new AnimatedBlockModel.Binding(
                        rig, "moving", movingParts, movingHasTranslucency, tints)
        )
);

animatedModel.submit(
        state.animationPose,
        poseStack,
        collector,
        state.lightCoords
);
```

If A must remain completely outside the rig hierarchy, submit A normally and bind only B to `moving`. A can still be imported under `static` or `root` as visual context while authoring B's animation.

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

Normally, the controller `initial` field is the first animation in the current export set. When the exporter finds exactly one complete `base_intro`, `base`, and `base_outro` triplet and no custom transitions are configured, it emits an automatic use sequence: intro is initial, intro advances to loop, release can blend from intro or loop into outro, and rapid reuse blends from outro back to intro. When exporting only the selected animation, that animation becomes the initial state.

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

For the conventional `intro -> loop -> outro` workflow, name the animations with one shared base, for example `use/hammer_intro`, `use/hammer`, and `use/hammer_outro`. Leave the custom transition JSON empty to generate the standard transitions automatically. Supplying any custom transition entries disables this inference and exports those entries unchanged.

### 7.5 Rest pose

Select an `animated_bone` or `socket`, then use “工具 → 编辑 SAP 骨骼静止姿势” to edit rest translation and scale. The group's normal rotation supplies its rest rotation.

Scale components must be finite and non-zero. Editing a first-person rest translation also moves its reference tree. Avoid renaming or restructuring template bones and helpers, because runtime bindings and preview repair logic depend on their roles and canonical names.

## 8. Project settings

“工具 → 配置 SAP 使用动画项目” contains:

| Setting | Meaning |
| --- | --- |
| 动画类型 | `玩家使用动画` or `方块实体动画`; export collects only the active mode's bones |
| 人称配置 | `first_person`, `third_person`, or both separated by commas |
| 命名空间 | Export namespace |
| 骨架路径 | SAP rig resource path |
| 控制器路径 | SAP controller resource path |
| 本地资源根目录 | Roots used to resolve preview model parents and textures; separate with `;` |
| 显示第一人称基础 HUD | Editor-only preview setting |
| 控制器过渡 JSON | Array of `from`, `to`, and `duration` entries |
| 方块骨骼 JSON | Adds missing block-mode bones and stores the template metadata |

Namespaces may contain lowercase letters, digits, underscores, dots, and hyphens, and must begin with a letter or digit. Resource paths may also contain slashes. They cannot begin or end with a slash or contain empty, `.` or `..` segments.

## 9. Exporting

Use “文件 → 导出 → 导出 SAP 动画资源包” and configure:

- namespace;
- rig path;
- controller path;
- all animations or the selected animation only.

Player and block-entity projects use the same export command and resource layout. In block mode only `animated_bone`/`socket` groups under `SAP 方块实体` are collected; imported block-model preview groups are excluded.

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

Exported resources are not registered automatically. Create and register a `UseAnimationProfile` in `SAPAnimations` with `SAPAnimationRegistries.useAnimation(...)`. The complete sequence builder example is in [Java 运行时接入](#10-java-运行时接入) above.

The registered rig, controller, clip, state, bone, and socket names must exactly match the exported resources. Set an unused perspective binding to `null`.

Item rendering code remains responsible for:

- deciding when playback is active;
- observing use state through `UseAnimationPlaybackManager` on client ticks;
- resolving the same playback from first- and third-person render paths;
- applying its sampled `RigPose` through `UseAnimationPlayer`;
- consuming controller events when required.

Resources are loaded and validated during client resource reload. Missing files, unregistered clips, invalid bones/sockets, invalid states, or invalid transitions cause reload failure with a diagnostic in the log.

### Block-entity animations

Block entities can reuse the same SAP rig, controller, and NeoForge clip resources. Register the block animation in `SAPAnimations`:

```java
public static final BlockAnimationDefinition WIND_CHIME =
        SAPAnimationRegistries.blockAnimation("wind_chime")
                .clip("animation/wind_chime")
                .defaultState("animation/wind_chime")
                .register();
```

In block mode, name the default animation `animation/wind_chime` to generate the clip and controller state used above; if you use `idle`, change both builder values to `idle`. In `extractRenderState`, sample the `RigPose` and bind each rig bone to its baked `BlockStateModelPart` with `AnimatedBlockModel.Binding`. `AnimatedBlockModel.submit(...)` owns the parent chain, pivots, rotation, scale, translucent layers, tints, and pose push/pop; the BER `submit` method only keeps block-facing or whole-model offsets. Fluids, flames, and other non-block-model geometry can remain separate custom layers.

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

### A block-model preview is empty or cannot be imported

Select a concrete `models/*.json`, a `blockstates/*.json` with a resolvable variant, or a `.bbmodel` containing only cubes. Add a resource root (the directory that directly contains `assets`) when the model references another pack. Preview geometry remains a non-exported `reference` group.

### Exported block animation does not move the model

Keyframe an `animated_bone` under `SAP 方块实体` and make sure the Java `BlockAnimationDefinition` uses matching rig/controller/clip paths and bone names. The geometry is bound by the BER's `AnimatedBlockModel.Binding`; keyframing a reference preview group cannot change the exported rig.

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
- `parseBlockBones(value)`
- `setBlockModel(filePath, {bone})`
- `clearBlockModel({bone})`

See the runnable examples in [Blockbench 脚本 API](#12-blockbench-脚本-api) above.
