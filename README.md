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
