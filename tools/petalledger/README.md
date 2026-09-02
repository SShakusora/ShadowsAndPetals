# Shadows & Petals · Petal Ledger

> 本文件是 Petal Ledger 的使用说明，使用 CommonMark Markdown 编写。

Petal Ledger 是供译者、校对者和维护者使用的 LangKey 编辑工作台。它把
`src/generated/resources/assets/shadowsandpetals/lang/*.json` 中的语言键整理成可搜索、可对照、可预览和可导出的网页，重点覆盖物品/方块名称、结构化 Tooltip，以及 Jade、JEI、进度、容器、消息、字幕和创造模式标签等普通语言键。

当前网页是一个静态页面，入口为 [`Petal-Ledger.html`](./Petal-Ledger.html)。页面首次打开时使用同目录的 manifest 快照；网络可用时会尝试读取仓库分支上的最新生成语言资源。

## 目录

- [1. 文件和数据来源](#1-文件和数据来源)
- [2. 快速开始](#2-快速开始)
- [3. URL 参数](#3-url-参数)
- [4. 更新语言键清单](#4-更新语言键清单)
- [5. 工作台界面](#5-工作台界面)
- [6. Tooltip 的键结构和游戏读取规则](#6-tooltip-的键结构和游戏读取规则)
- [7. 三种预览状态](#7-三种预览状态)
- [8. 高亮、换行和占位符](#8-高亮换行和占位符)
- [9. 普通语言键预览](#9-普通语言键预览)
- [10. 草稿、撤销和导出](#10-草稿撤销和导出)
- [11. 导入文件格式](#11-导入文件格式)
- [12. 校对与提交流程](#12-校对与提交流程)
- [13. 常见问题](#13-常见问题)
- [14. 代码参考](#14-代码参考)

## 1. 文件和数据来源

| 文件 | 作用 | 是否手工编辑 |
| --- | --- | --- |
| `tools/petalledger/Petal-Ledger.html` | 网页界面、编辑器、Tooltip/普通语言键预览、导入导出和本地草稿 | 是，功能变更时编辑 |
| `tools/petalledger/data/repository-manifest.js` | 供网页离线启动的 `window.__TEXTLEDGER_MANIFEST__` 快照 | 否，由脚本生成 |
| `tools/petalledger/generate-manifest.mjs` | 扫描生成的 locale JSON，整理键、Tooltip 行、来源文件、纹理和特殊组件 | 仅在数据模型变更时修改 |
| `tools/petalledger/package-site.mjs` | 将页面和实际纹理打包成不依赖仓库源码的线上静态站点 | 仅在打包格式变更时修改 |
| `build.gradle` 中的 `generateTextLedgerManifest` | 先运行数据生成器，再调用 manifest 脚本 | 否，使用 Gradle 任务 |
| `src/generated/resources/assets/shadowsandpetals/lang/*.json` | Minecraft 实际使用的生成语言资源，是网页同步的事实来源 | 由 data generator 生成 |
| `src/main/java/.../data/BuiltinLanguageKeys.java` | 普通语言键、全局 Tooltip 提示和动态提示文案 | 修改文案后重新生成资源 |
| `src/main/java/.../data/lang/TooltipLangBuilder.java` | 物品 Tooltip 的 `summary`、`conditionN`、`behaviourN`、`controlN`、`actionN` 键构造器 | 修改结构时阅读/修改 |
| `src/main/java/.../client/tooltip/ItemDescription.java` | 游戏内 Tooltip 的行读取顺序、Shift/Ctrl 状态和缺键终止规则 | 预览不一致时的首要参考 |
| `src/main/java/.../client/tooltip/TooltipHelper.java` | 游戏内宽度、换行、下划线高亮和颜色规则 | 检查格式/换行时的参考 |
| `src/main/java/.../client/tooltip/ClientRockeryTooltip.java` | 石山 Shift 预览组件的尺寸和显示条件 | 石山预览不一致时的参考 |
| `src/main/java/.../item/chime/WindChimeTooltipModifier.java` | 风铃 Shift 动态颜色、彩带和风舌文案 | 风铃预览不一致时的参考 |

manifest 当前包含基准语言 `en_us`、已生成的 `zh_cn`，以及约 867 个语言键。清单中的 `source`、`displayNameKey`、`tooltip`、`dynamic`、`component` 和 `iconSource` 字段用于让网页知道如何编辑和预览，并不取代 Minecraft 的语言 JSON。

网页同步的优先级如下：

1. 在线读取 manifest 记录的 GitHub 分支中的基准 locale。
2. 尝试读取目标 locale；不存在时保留空值并显示缺失数量。
3. 任一网络请求失败时，回退到 `repository-manifest.js` 的内嵌快照。

## 2. 快速开始

### 2.1 准备环境

- 使用支持 ES modules、`Intl.Segmenter` 和现代 CSS 的浏览器（当前版 Chrome、Edge 或 Firefox 均可）。
- 维护清单时需要 Node.js（建议 Node 20 或更高版本）和仓库中的 `gradlew.bat`。
- 如果要显示仓库纹理或访问 GitHub，建议通过本地静态服务器打开页面，而不是直接双击 `file://` 文件。

### 2.2 启动网页

在仓库根目录打开 PowerShell：

```powershell
python -m http.server 8765
```

如果系统没有 `python` 命令，可以使用 `py -m http.server 8765`。然后访问：

```text
http://127.0.0.1:8765/tools/petalledger/Petal-Ledger.html
```

网页仍可直接以 `file://` 打开，但浏览器可能阻止相对纹理、模块脚本或跨域请求；离线快照能否加载取决于浏览器的本地文件策略。

### 2.3 第一次使用

1. 在左侧“资源库”选择“物品名称”“方块名称”或“其他语言键”，也可以保留“全部语言键”。
2. 用搜索框按显示名、完整键、注册表 ID、上下文或任意 locale 文案过滤。
3. 点击一行，在右侧查看基准值和目标值；目标值输入框才是可编辑区域。
4. 对有 Tooltip 的物品/方块，使用“默认”“Shift”“Ctrl”切换游戏状态，检查预览中的顺序、颜色和占位符。
5. 修改后先点“保存草稿”，再用“导出补丁”生成可审阅文件；需要放回语言目录时再导出 `locale JSON`。

## 3. URL 参数

页面支持在打开时指定分支和目标语言：

```text
Petal-Ledger.html?branch=26.1.2%2FDev&locale=ja_jp
```

| 参数 | 说明 | 示例 |
| --- | --- | --- |
| `branch` | 覆盖 manifest 中记录的 Git 分支；包含 `/` 时应进行 URL 编码 | `branch=26.1.2%2FDev` |
| `locale` | 进入页面后使用的目标 locale；会统一为小写，并把非法字符转换为 `_` | `locale=ja_jp` |

基准 locale 由 manifest 的 `baseLocale` 决定，当前为 `en_us`。目标 locale 不能与基准相同；如果目标 locale 尚未存在，网页会创建一个空的目标列，在线同步时也会保留空值并提示“目标 locale 不存在”。

## 4. 更新语言键清单

### 4.1 推荐命令

当注册表、`BuiltinLanguageKeys`、Tooltip builder 或语言生成逻辑发生变化时，在仓库根目录运行：

```powershell
.\gradlew.bat generateTextLedgerManifest
```

这个任务会自动依赖 `runClientData`，因此会先重新生成 `src/generated/resources`，再写入 `tools/petalledger/data/repository-manifest.js`。

若需要分别观察两个阶段，也可以运行：

```powershell
.\gradlew.bat runClientData
node tools/petalledger/generate-manifest.mjs
```

脚本默认把 JavaScript 输出到标准输出；在 PowerShell 中若单独运行脚本，应明确重定向到快照文件：

```powershell
node tools/petalledger/generate-manifest.mjs > tools/petalledger/data/repository-manifest.js
```

### 4.2 脚本选项

`generate-manifest.mjs` 不解析 Java 源码，也不通过正则猜测注册表；它只读取 data generator 生成的 locale JSON，再按键名整理结构。可选参数如下：

```text
--resources <目录>   语言资源根目录，默认 src/generated/resources
--namespace <命名空间> 默认 shadowsandpetals
--locales <列表>     逗号分隔的 locale，默认 en_us,zh_cn
```

例如：

```powershell
node tools/petalledger/generate-manifest.mjs `
  --resources src/generated/resources `
  --namespace shadowsandpetals `
  --locales en_us,zh_cn,ja_jp `
  > tools/petalledger/data/repository-manifest.js
```

### 4.3 生成结果检查

提交前至少检查以下内容：

- `repository.branch`、`repository.commit` 是否对应本次生成资源的 Git 状态。
- `locales` 和 `baseLocale` 是否符合预期。
- 语言键数量和各 `context` 数量是否出现异常大幅变化。
- 新物品/方块是否有 `displayNameKey`，Tooltip 子键是否挂在正确的 owner 上。
- 有纹理的物品/方块是否生成了正确的 `iconSource` 相对路径。
- 风铃是否带有 `dynamic.kind = "wind_chime_colors"`，石山是否带有 `component.kind = "rockery_preview"`。

不要手工修改 `repository-manifest.js` 来修复文案。应修改 Java 注册表或语言 builder，重新运行数据生成和 manifest 任务，并将生成资源与快照一起提交。

### 4.4 生成线上静态站点

部署到独立域名时运行：

```powershell
.\gradlew.bat generateTextLedgerSite
```

任务会生成 `build/textledger-site`，其中包含 `index.html`、`data/repository-manifest.js` 和 manifest 实际引用的本地纹理。线上快照使用 `assets/` 前缀，因此不需要公开仓库的 `src/` 目录；每次更新语言资源后重新生成并上传新的 release 即可。

## 5. 工作台界面

### 5.1 左侧语言键选择器

左侧列表独立滚动，不会再把整个页面无限拉长。筛选后仍保留当前列表的滚动区域；窄屏时列表会变成有限高度的滚动面板。

每一行显示：

- 显示名称和完整 LangKey。
- 物品、方块或普通语言键的图标。
- 状态圆点：已完成、缺失、已编辑或校验失败。

Tooltip 子键（例如 `...tooltip.condition1`）会显示在“其他语言键”中，但点击后会跳转到所属物品/方块的编辑器，以避免把一条 Tooltip 拆成互不相关的页面。

### 5.2 顶部语言和数据来源

- “基准”下拉框：用于对照的只读语言，默认 `en_us`。
- “目标”输入框：当前要翻译的 locale，例如 `zh_cn`、`ja_jp`。
- “应用”：切换目标 locale；切换前若有未保存修改，网页会先写入当前目标的本地草稿。
- 状态栏：显示在线读取、离线快照、缺失语言文件或保存结果。

在线同步只读 GitHub 资源，不会通过网页直接提交仓库。真正的提交仍应由维护者审阅导出的文件后完成。

### 5.3 编辑器字段

基准列始终只读，目标列可编辑。对于当前快照中尚不存在的 Tooltip 字段，编辑器会提供“添加翻译键”；保存非空文本后会把它作为新的目标键加入编辑集。

修改会立即更新右侧预览，并记录在当前页面的撤销历史中。只有与基准快照不同的目标键才会进入“脏键”集合并被导出。

## 6. Tooltip 的键结构和游戏读取规则

### 6.1 键布局

对一个物品或方块 `item.shadowsandpetals.example`，Tooltip 语言键采用以下布局：

| 顺序 | 键后缀 | 含义 |
| --- | --- | --- |
| 1 | `.tooltip.summary` | Shift 描述区的摘要 |
| 2 | `.tooltip.condition1` + `.tooltip.behaviour1` | 第一条 Shift 行的条件和行为 |
| 3 | `.tooltip.condition2` + `.tooltip.behaviour2` | 第二条 Shift 行，编号连续递增 |
| 4 | `.tooltip.control1` + `.tooltip.action1` | 第一条 Ctrl 操作和动作 |
| 5 | `.tooltip.control2` + `.tooltip.action2` | 第二条 Ctrl 行，编号连续递增 |

`TooltipLangBuilder` 对应的写法如下（示意）：

```java
TooltipLangBuilder.of(item)
    .summary("A quiet tool.", "一件安静的工具。")
    .behaviour("When held", "手持时", "It listens to nearby stones.", "它会聆听附近的石头。")
    .action("Right click", "右键", "Carve the matching shape.", "雕刻匹配的形状。")
    .register();
```

builder 会从 1 开始自动编号；如果手工添加键，也必须保持编号连续，并且每个 `conditionN` 都要有对应的 `behaviourN`，每个 `controlN` 都要有对应的 `actionN`。

### 6.2 游戏中的读取顺序

`ItemDescription.currentLines()` 的状态优先级是 **Shift 高于 Ctrl**：

- Shift 按下时，如果存在行为行，读取摘要、空行、每个 `conditionN` 和 `behaviourN`。
- Ctrl 按下且没有 Shift 时，读取 `controlN` 和 `actionN`。
- 没有对应详情时，该状态回退到基础 Tooltip，而不是显示半截空列表。
- 游戏从编号 1 开始读取；遇到第一个缺失的成对键就停止继续读取后面的编号。

因此，`condition1` 存在但 `behaviour1` 缺失，会使后面的 `condition2/behaviour2` 在游戏中不可见。网页校验会把不成对或不连续的字段标红；请删除多余编号，或补齐完整的一对。

### 6.3 提示行和空行

游戏会在 Tooltip 前部添加按键提示：Ctrl 提示在前，Shift 提示在后；有相应详情时，提示与详情之间插入空行。网页的默认、Shift、Ctrl 预览遵循同样的顺序，但它是 HTML 近似渲染，不会调用 Minecraft 的真实字体渲染器。

## 7. 三种预览状态

| 状态 | 游戏含义 | 网页显示 |
| --- | --- | --- |
| 默认 | 未按特殊按键时的物品 Tooltip | 标题和 Ctrl/Shift 提示；详情需按住对应按键 |
| Shift | 查看描述/行为 | 摘要、行为行、动态风铃信息或石山组件预览 |
| Ctrl | 查看操作 | 操作行；若没有操作则回退到默认内容 |

可以点击预览上方的状态按钮，也可以按住键盘上的 Shift/Ctrl。真实游戏中 Shift 优先于 Ctrl；网页同时检测到两者时也采用 Shift 模式。

### 7.1 风铃

`WindChimeTooltipModifier` 只在 Shift 状态插入动态行，位置在基础 Tooltip 的第二行附近：空行、`Current colors:`/`当前颜色：`、彩带颜色和风舌颜色。颜色名称来自 Minecraft 的 `color.minecraft.<dye>` 键，因此会随目标 locale 显示对应颜色名。

### 7.2 石山

`ClientRockeryTooltip` 默认只显示“按住 Shift 显示预览”提示；按住 Shift 时显示约 64px 的自定义组件和尺寸标签。尺寸由物品 ID 的后缀推导，标签使用 `tooltip.shadowsandpetals.rockery.dimensions_label`。网页提供同样的状态和尺寸信息，但石山图像是静态 HTML 近似，不是游戏内真实的 PIP 模型渲染。

## 8. 高亮、换行和占位符

### 8.1 `_文字_` 高亮标记

Tooltip 文案使用成对下划线标记高亮，例如：

```text
主手持锤子对_石头_长按右键，雕刻匹配形状的石山。
```

游戏的 `TooltipHelper.cutTextComponent` 会去掉标记本身，再以高亮颜色渲染“石头”。网页预览也会去掉下划线，并将完整高亮词作为不可拆分片段处理，因此 `_石头_` 不应在预览中拆成多行。下划线必须成对出现；奇数个下划线会被校验标红。

颜色和宽度来自游戏实现：普通文字使用主色，高亮文字使用高亮色，单行最大宽度约为 200 像素。网页用浏览器字体和宽度估算值模拟该规则，字体度量与游戏资源不同，所以长句的具体断行仍可能有少量差异。

不要用下划线包住整句来代替局部高亮，也不要在高亮标记中插入无关空格。修改后请同时检查 Shift 和 Ctrl 两种预览，因为两者的文本来源不同。

### 8.2 `%s` 和位置占位符

文案中的 `%s`、`%1$s` 等占位符由游戏运行时填充。翻译时必须：

- 保留与基准文案相同的占位符数量。
- 需要换序时使用位置形式（例如 `%1$s`、`%2$s`），不要凭空删除或新增参数。
- 不要把占位符拆到 `_` 高亮标记内部，除非 Java 调用方明确要求这样做。

普通语言键预览会使用“示例值”“12”“红色”替换占位符，只用于检查句法和大致布局。

### 8.3 HTML 安全

网页会对编辑文本进行 HTML 转义后再插入预览；不要输入 HTML 标签来实现颜色或换行。Tooltip 的颜色、缩进和换行应通过游戏约定的下划线标记与普通空格表达。

## 9. 普通语言键预览

除物品/方块和 Tooltip 外，manifest 会按键前缀分配上下文：

| 前缀/上下文 | 网页预览 |
| --- | --- |
| `advancements.` | 进度提示卡片 |
| `config.` | 配置项普通文本 |
| `container.` | 容器标题样式 |
| `entity.` | 实体名称 |
| `fluid_type.` | 流体名称 |
| `itemGroup.` | 创造模式标签 |
| `jade.` | Jade 信息行 |
| `jei.` | JEI 卡片 |
| `message.` | 消息框 |
| `subtitles.` | 字幕行 |
| 其他 | 通用语言键行 |

这些卡片只说明文案放置的上下文，不模拟完整的游戏 UI、字体、动画或第三方模组界面。最终校对仍应在实际客户端中检查。

## 10. 草稿、撤销和导出

### 10.1 本地草稿

“保存草稿”只写入当前浏览器的 `localStorage`，不会修改 Git 工作区或 GitHub。草稿键按分支和目标 locale 隔离，形如：

```text
petal-ledger-v2:<branch>:<target-locale>
```

切换目标 locale 时，若当前有修改，网页会先保存当前草稿；重新打开相同分支和 locale 后会尝试恢复。浏览器隐私模式、清理站点数据或存储空间不足时可能无法保存，请改用导出补丁。

“清空草稿”会删除当前分支/目标 locale 的本地草稿并重新载入页面状态；执行前请确认已经导出需要保留的内容。

### 10.2 撤销、恢复和差异

- “撤销”或 `Ctrl/Cmd+Z`：撤销最近一次字段修改；输入框正在获得焦点时交给浏览器处理原生撤销。
- “恢复快照”：将当前物品/方块及其 Tooltip 字段恢复到同步时的基准快照，并把删除/新增标记重新计算。
- “差异”：查看当前目标值相对于基准快照的修改，适合导出前复核。

### 10.3 导出补丁

“导出补丁”只包含已修改的键，文件名类似：

```text
shadows-and-petals-zh_cn-patch.json
```

补丁包含仓库名、分支、基准提交、基准/目标 locale、导出时间，以及每项的 key、类型、上下文、来源、基准值、目标值和删除标记。它适合代码审阅、翻译协作和后续合并。

### 10.4 导出 locale JSON

“locale JSON”输出扁平的 Minecraft 语言对象，例如：

```json
{
  "item.shadowsandpetals.example": "示例物品",
  "item.shadowsandpetals.example.tooltip.summary": "一件安静的工具。"
}
```

导出前请检查空值。网页允许空值表示“尚未翻译”，但直接把空值提交到语言目录通常会造成游戏中显示为空。

导出成功后，文件名、目标 locale 和时间会写入浏览器中的导出记录；记录只用于本地提示，不是仓库历史。

## 11. 导入文件格式

网页接受两种格式。

### 11.1 TextLedger 补丁

补丁顶层应包含 `format: "shadowsandpetals-language-patch"`、`schemaVersion: 2` 和 `entries` 数组。最小示例：

```json
{
  "format": "shadowsandpetals-language-patch",
  "schemaVersion": 2,
  "repository": "ShadowsAndPetals",
  "branch": "26.1.2/Dev",
  "baseLocale": "en_us",
  "targetLocale": "zh_cn",
  "entries": [
    {
      "key": "item.shadowsandpetals.example.tooltip.summary",
      "type": "language",
      "context": "tooltip",
      "fieldType": "summary",
      "baseValue": "A quiet tool.",
      "value": "一件安静的工具。",
      "deleted": false
    }
  ]
}
```

网页会校验仓库、分支、基准提交、locale、字段类型和删除标记。未知键会被忽略，非法项会被跳过并显示数量；基准提交不一致时应先重新同步或重新生成 manifest，而不是强行导入。

将 `deleted` 设为 `true` 可标记删除；删除项的 `value` 应为 `null`。普通翻译项的 `value` 必须是字符串。

### 11.2 标准 locale JSON

也可以直接导入 Minecraft 的扁平 locale JSON：

```json
{
  "item.shadowsandpetals.example": "示例物品",
  "tooltip.shadowsandpetals.holdKey.shift": "按住 Shift 显示描述"
}
```

只会应用当前 manifest 中已知的键；未知键和非字符串值会被分别统计。导入后仍需点击“保存草稿”或导出补丁，导入本身不会写回仓库。

## 12. 校对与提交流程

推荐按以下顺序工作：

1. 更新生成资源和 manifest，确认分支/提交信息正确。
2. 选择目标 locale，先按“缺失”筛选，再按“已编辑/校验失败”复核。
3. 先完成物品/方块显示名，再完成对应 Tooltip；不要只翻译 Tooltip 子键而遗漏标题。
4. 对每个 Tooltip 检查默认、Shift、Ctrl 三态，确认提示顺序、颜色、空行、下划线高亮和占位符。
5. 检查风铃和石山等特殊组件；这些组件的动态文字不一定来自物品自己的 Tooltip 键。
6. 导出补丁交给另一位译者或维护者审阅；必要时再导出 locale JSON。
7. 将审阅后的 JSON 放入对应语言目录或交给合并脚本，运行数据生成和测试。
8. 提交时同时包含 Java 文案变更、生成语言资源和 manifest 快照；不要提交浏览器 localStorage 内容。

提交前检查清单：

- [ ] 目标 locale 没有与基准 locale 混淆。
- [ ] 新增 Tooltip 行的编号从 1 开始且连续。
- [ ] 每个 `conditionN`/`behaviourN`、`controlN`/`actionN` 都成对存在。
- [ ] `_高亮_` 标记成对，且高亮词不会被不必要的空格拆开。
- [ ] `%s`、`%1$s` 等占位符数量和语义正确。
- [ ] 默认、Shift、Ctrl 预览都没有错误或意外空行。
- [ ] 没有把“尚未翻译”的空值直接当作已完成翻译提交。
- [ ] 导出补丁的 `baseCommit` 与当前 manifest 一致。

## 13. 常见问题

### 页面显示“未找到 manifest”

确认 [`Petal-Ledger.html`](./Petal-Ledger.html) 与 `data/repository-manifest.js` 的相对位置未改变，并从 `tools/petalledger` 的上级目录通过 HTTP 服务器访问。若快照不存在，先运行：

```powershell
.\gradlew.bat generateTextLedgerManifest
```

### 一直显示“离线 manifest”

这表示 GitHub 请求失败，网页正在使用本地快照。离线编辑仍然可用；联网并刷新后才会获取生成资源的最新内容。检查网络、分支 URL 和浏览器控制台的跨域错误。

### 新增语言文件没有出现在列表中

manifest 默认只包含 `en_us,zh_cn`。使用 `--locales` 重新生成，或在网页的“目标”输入框应用新的 locale；网页会为不存在的目标语言创建空值，但不会自动生成服务器上的 JSON 文件。

### Tooltip 行在游戏中消失

优先检查第一个缺失的成对键：例如有 `condition1` 却没有 `behaviour1`，游戏会在这里停止读取。还要检查编号是否从 1 连续递增，以及是否误把 Ctrl 的 `controlN/actionN` 写成了 Shift 的 `conditionN/behaviourN`。

### 下划线或高亮显示异常

确保使用成对的半角 `_`，不要混用全角下划线或 Markdown 的反引号。高亮词会被网页作为一个不可拆分片段；如果整句仍然过长，网页和游戏可能在不同位置换行，这是浏览器字体与 Minecraft 字体度量不同造成的近似误差。

### 预览和游戏仍有差异

网页没有启动 Minecraft，也没有调用真实的 `Font`、`BreakIterator`、Tooltip 渲染器或第三方模组 UI。它主要验证键结构、读取顺序、状态切换、占位符和特殊组件。最终视觉效果必须在 `runClient` 中复核。

### 纹理图标缺失

从本地静态服务器访问，确认 manifest 的 `iconSource` 相对路径指向实际 PNG。没有纹理或浏览器拒绝本地资源时，网页会退回到字母图标；这不影响语言键编辑。

### 导入被拒绝或大量键被忽略

补丁必须对应当前仓库、分支和基准提交；标准 locale JSON 必须是扁平的“键到字符串”对象。先查看状态栏中的未知/非法数量，再重新同步或更新 manifest。不要把嵌套 JSON、数组或整份 Minecraft 配置文件直接导入。

### 草稿找不到

草稿存储在浏览器站点的 localStorage 中，并按分支与目标 locale 隔离。检查是否更换了端口、浏览器、隐私窗口或 URL 分支参数；必要时从导出的补丁恢复。

## 14. 代码参考

阅读或修改行为时，建议按下面的调用链定位：

```text
ModLanguageProvider
  └─ DatagenLangRegistry / BuiltinLanguageKeys
       └─ src/generated/resources/.../lang/*.json
            └─ generate-manifest.mjs
                 └─ repository-manifest.js
                      └─ Petal-Ledger.html

TooltipLangBuilder
  └─ ItemDescription.currentLines()
       ├─ TooltipHelper.cutTextComponent()
       ├─ WindChimeTooltipModifier
       └─ ClientRockeryTooltip
```

当“键存在但预览不对”时，先查看 `Petal-Ledger.html` 的 manifest 分类和 Tooltip 组装；当“预览正确但游戏不显示”时，查看 `ItemDescription.currentLines()` 的成对键和连续编号；当“换行/高亮不一致”时，查看 `TooltipHelper.cutTextComponent()` 的宽度、下划线切分和实际 Minecraft 字体。

最后，网页是翻译协作工具，不是语言资源的第二个事实来源：Java 注册/语言代码和 data-generator 输出始终优先，manifest 应通过脚本更新，导出文件应经过人工审阅后再合并。
