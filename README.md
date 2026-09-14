# Strengshaku

[中文](#中文) | [English](#english)

[![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## 中文

### 简介

**Strengshaku** 是一款适用于 **Paper 1.21.4** 的强化插件，提供装备强化、属性迁移和属性叠影三大功能。插件通过 GUI 操作，支持从宝石（岩浆膏）的 Lore 中解析属性词条，并按照可配置的概率曲线进行强化。所有属性均以 Lore 形式添加到装备上，并严格插入到指定分隔线下方，避免干扰其他插件的 Lore。

### 功能特性

- **装备强化**：将装备放入左侧槽位，宝石（岩浆膏）放入右侧槽位，点击强化按钮即可。成功率根据装备上对应属性的数值动态计算，所有普通宝石共用同一套概率曲线；特级宝石必定成功。
- **复合宝石**：支持雷击、吸血等复合词条。首次强化增加两条 Lore（如“雷击伤害增加”和“雷击几率增加”），后续强化只增加第一条属性。
- **属性迁移**：将旧装备的属性段与附魔迁移到新装备。要求两件物品类型匹配（剑类↔斧类可互转；弓、弩、三叉戟、重锤需同类；护甲需同部位）。可叠加属性会保留在新装备上。迁移界面提供实时预览。
- **属性叠影**：消耗废弃装备，将其攻击力或生命力按 10% 转化为目标装备的“暴伤倍率增加”（武器）或“暴伤抵抗增加”（护甲）。成功率根据目标已有的暴伤属性值计算。
- **管理员指令**：可向玩家手持物品的指定属性段（装备属性/额外属性/技能属性）直接添加自定义 Lore。
- **高度可配置**：概率曲线、属性正则、复合宝石、可叠加属性等均在 `config.yml` 中配置。

### 命令与权限

| 命令 | 说明 | 权限 |
|------|------|------|
| `/ss <玩家> qh` | 为指定玩家打开强化装备菜单 | `strengshaku.use`（默认所有玩家） |
| `/ss <玩家> qy` | 为指定玩家打开属性迁移菜单 | `strengshaku.use` |
| `/ss <玩家> dy` | 为指定玩家打开属性叠影菜单 | `strengshaku.use` |
| `/ssadmin addlore <玩家> <段类型> <lore内容>` | 向玩家手持物品的指定段添加 Lore。段类型：`equip`、`extra`、`skill` | `strengshaku.admin`（默认 OP） |

> 子命令 `qh` 可省略，即 `/ss <玩家>` 默认打开强化菜单。

### 配置文件 `config.yml`

```yaml
# 特级宝石标识（Lore 中包含此文本即视为特级，强化必定成功）
super-gem-indicator: "&8[*]&b特级宝石"

# 最大强化等级
max-level: 10

# 属性分隔线
attribute-separator: "&7=======&9&l装备属性&7======="

# 属性显示格式
attribute-format: "&8[*]&7{attribute}&c{value}"

# 普通宝石成功率曲线（基于装备上对应属性的数值）
attack-gem-probability:
  - max-attack: 15
    rate: 0.5
  - max-attack: 20
    rate: 0.85
  - max-attack: 30
    rate: 0.55
  - max-attack: 40
    rate: 0.35
  - max-attack: 50
    rate: 0.25
  - default: 0.05

# 叠影成功率曲线（基于目标已有暴伤倍率/抵抗值）
dieying-probability:
  - max-attribute: 100
    rate: 0.5
  - max-attribute: 120
    rate: 0.3
  - max-attribute: 150
    rate: 0.2
  - max-attribute: 200
    rate: 0.1
  - max-attribute: 300
    rate: 0.08
  - default: 0.05

# 属性解析正则
attribute-patterns:
  attack:
    regex: "&7攻击力增加&c([+-]?\\d+)"
    attribute: "攻击力增加"
  defense:
    regex: "&7防御力增加&c([+-]?\\d+)"
    attribute: "防御力增加"
  health:
    regex: "&7生命力增加&c([+-]?\\d+)"
    attribute: "生命力增加"
  true_damage:
    regex: "&7真实伤害增加&c([+-]?\\d+)"
    attribute: "真实伤害增加"
  crit_rate:
    regex: "&7暴击率增加&c([+-]?\\d+)"
    attribute: "暴击率增加"
  dodge_rate:
    regex: "&7闪避率增加&c([+-]?\\d+)"
    attribute: "闪避率增加"

# 复合宝石（首次强化增加两条 Lore，后续只增加第一条）
combo-gems:
  - trigger: "雷击伤害增加"
    attributes: ["雷击伤害增加", "雷击几率增加"]
    increments: [1, 10]
  - trigger: "吸血倍率增加"
    attributes: ["吸血倍率增加", "吸血几率增加"]
    increments: [1, 10]

# 可叠加属性（迁移时保留）
stackable-attributes:
  - "恢复效率增加"
  - "生命偷取"

# 可叠加 Lore 标记
stackable-lore-marker: "&7(可叠加)"
```

### 安装与使用

1. 将 `Strengshaku.jar` 放入服务器 `plugins` 文件夹。
2. 启动或重启服务器，插件会自动生成 `config.yml`。
3. 根据需求修改 `config.yml`，然后重启或重载插件。
4. 使用 `/ss <玩家> qh/qy/dy` 打开对应菜单。
5. 强化：左侧放装备，右侧放宝石（岩浆膏），点击浅绿色玻璃板。
6. 迁移：左侧放旧装备，右侧放新装备，点击迁移按钮。
7. 叠影：左侧放目标装备，右侧放消耗品，点击执行叠影。

### 构建方法

本项目使用 Maven 构建。确保已安装 JDK 21 和 Maven。

```bash
mvn clean package
```

构建完成后，可在 `target/` 目录找到 `Strengshaku-1.0.jar`。

**依赖：**
- Paper API 1.21.4
- Java 21

### 注意事项

- 强化属性会严格添加到 `attribute-separator` 指定的分隔线下方，不会干扰其他插件的 Lore。
- 迁移要求两件物品类型匹配：剑类↔斧类可互转；弓、弩、三叉戟、重锤需同类；护甲需同部位。
- 叠影要求两件物品同为武器或同为护甲；武器叠影使用攻击力计算，护甲叠影使用生命力计算。
- 关闭 GUI 时，槽位 20 和 24 中的物品会返还给玩家；背景玻璃板不会返还。
- 概率曲线中的 `max-attack` 实际代表对应属性的阈值，所有普通宝石共用同一套曲线。
- 若装备没有对应属性值，则按曲线最低档（`max-attack` 最小值）计算成功率，请合理设置基础概率。

---

## English

### Introduction

**Strengshaku** is a strengthening plugin for **Paper 1.21.4** that provides gear enhancement, attribute migration, and shadow stacking. It uses GUIs for operation, parses attribute entries from gem (magma cream) lore, and applies configurable probability curves. All attributes are added as lore and strictly inserted below a specified separator line to avoid interfering with other plugins' lore.

### Features

- **Gear Enhancement**: Place gear in the left slot and a gem (magma cream) in the right slot, then click the enhance button. The success rate is dynamically calculated based on the corresponding attribute value on the gear. All normal gems share the same probability curve; super gems always succeed.
- **Combo Gems**: Supports combo entries like lightning and lifesteal. The first enhancement adds two lore lines (e.g., "Lightning Damage Increase" and "Lightning Chance Increase"); subsequent enhancements only increase the first attribute.
- **Attribute Migration**: Migrates attribute sections and enchantments from old gear to new gear. The two items must match in type (swords ↔ axes are interchangeable; bow, crossbow, trident, mace must be the same type; armor must be the same piece). Stackable attributes are preserved. A live preview is provided in the migration GUI.
- **Shadow Stacking**: Consumes a waste item and converts 10% of its attack damage or health into "Crit Damage Multiplier" (weapons) or "Crit Resistance" (armor) on the target item. The success rate is based on the target's existing crit attribute value.
- **Admin Command**: Directly add custom lore to a player's held item under a specified section (equip/extra/skill).
- **Highly Configurable**: Probability curves, attribute regex, combo gems, and stackable attributes are all configured in `config.yml`.

### Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/ss <player> qh` | Opens the enhancement GUI for the target player | `strengshaku.use` (default: everyone) |
| `/ss <player> qy` | Opens the attribute migration GUI | `strengshaku.use` |
| `/ss <player> dy` | Opens the shadow stacking GUI | `strengshaku.use` |
| `/ssadmin addlore <player> <section> <lore>` | Adds lore to the player's held item. Sections: `equip`, `extra`, `skill` | `strengshaku.admin` (default: OP) |

> The `qh` subcommand can be omitted; `/ss <player>` opens the enhancement menu by default.

### Configuration `config.yml`

```yaml
# Super gem indicator (if lore contains this text, enhancement always succeeds)
super-gem-indicator: "&8[*]&b特级宝石"

# Max enhancement level
max-level: 10

# Attribute separator
attribute-separator: "&7=======&9&l装备属性&7======="

# Attribute format
attribute-format: "&8[*]&7{attribute}&c{value}"

# Normal gem success rate curve (based on the corresponding attribute value on gear)
attack-gem-probability:
  - max-attack: 15
    rate: 0.5
  - max-attack: 20
    rate: 0.85
  - max-attack: 30
    rate: 0.55
  - max-attack: 40
    rate: 0.35
  - max-attack: 50
    rate: 0.25
  - default: 0.05

# Shadow stacking success rate curve (based on target's existing crit value)
dieying-probability:
  - max-attribute: 100
    rate: 0.5
  - max-attribute: 120
    rate: 0.3
  - max-attribute: 150
    rate: 0.2
  - max-attribute: 200
    rate: 0.1
  - max-attribute: 300
    rate: 0.08
  - default: 0.05

# Attribute regex patterns
attribute-patterns:
  attack:
    regex: "&7攻击力增加&c([+-]?\\d+)"
    attribute: "攻击力增加"
  defense:
    regex: "&7防御力增加&c([+-]?\\d+)"
    attribute: "防御力增加"
  health:
    regex: "&7生命力增加&c([+-]?\\d+)"
    attribute: "生命力增加"
  true_damage:
    regex: "&7真实伤害增加&c([+-]?\\d+)"
    attribute: "真实伤害增加"
  crit_rate:
    regex: "&7暴击率增加&c([+-]?\\d+)"
    attribute: "暴击率增加"
  dodge_rate:
    regex: "&7闪避率增加&c([+-]?\\d+)"
    attribute: "闪避率增加"

# Combo gems (first enhancement adds two lore lines; subsequent only the first)
combo-gems:
  - trigger: "雷击伤害增加"
    attributes: ["雷击伤害增加", "雷击几率增加"]
    increments: [1, 10]
  - trigger: "吸血倍率增加"
    attributes: ["吸血倍率增加", "吸血几率增加"]
    increments: [1, 10]

# Stackable attributes (preserved during migration)
stackable-attributes:
  - "恢复效率增加"
  - "生命偷取"

# Stackable lore marker
stackable-lore-marker: "&7(可叠加)"
```

### Installation & Usage

1. Place `Strengshaku.jar` into the server's `plugins` folder.
2. Start or restart the server; `config.yml` will be generated automatically.
3. Modify `config.yml` as needed, then restart or reload the plugin.
4. Use `/ss <player> qh/qy/dy` to open the corresponding GUI.
5. Enhancement: Place gear on the left and a gem (magma cream) on the right, then click the lime green glass pane.
6. Migration: Place old gear on the left and new gear on the right, then click the migrate button.
7. Shadow Stacking: Place the target item on the left and a waste item on the right, then click the execute button.

### Build

This project uses Maven. Ensure JDK 21 and Maven are installed.

```bash
mvn clean package
```

After building, the `Strengshaku-1.0.jar` will be in the `target/` directory.

**Dependencies:**
- Paper API 1.21.4
- Java 21

### Notes

- Enhanced attributes are strictly inserted below the separator defined by `attribute-separator`, without interfering with other plugins' lore.
- Migration requires matching types: swords ↔ axes are interchangeable; bow, crossbow, trident, mace must be the same type; armor must be the same piece.
- Shadow stacking requires both items to be weapons or both to be armor. Weapon stacking uses attack damage; armor stacking uses health.
- When closing the GUI, items in slots 20 and 24 are returned to the player; background glass panes are not returned.
- The `max-attack` in the probability curve actually represents the threshold of the corresponding attribute; all normal gems share the same curve.
- If the gear does not have the corresponding attribute value, the lowest tier of the curve is used. Please set a reasonable base probability.
