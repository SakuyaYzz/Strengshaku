package org.shaku.strengshaku;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strengshaku extends JavaPlugin {

    private static Strengshaku instance;
    private static List<String> stackableAttributes = new ArrayList<>();
    private static String stackableMarker = "";

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        GemParser.loadPatterns();

        stackableAttributes = getConfig().getStringList("stackable-attributes");
        stackableMarker = getConfig().getString("stackable-lore-marker", "");

        getServer().getPluginManager().registerEvents(new AllListeners(), this);

        getCommand("ss").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("此命令只能由玩家执行");
                return true;
            }
            Player p = (Player) sender;
            if (args.length == 0) {
                p.sendMessage(ChatColor.RED + "用法: /ss <玩家> <子命令>");
                p.sendMessage(ChatColor.GRAY + "子命令: qh(强化), qy(属性迁移), dy(属性叠影)");
                return true;
            }
            String targetName = args[0];
            String subCmd = (args.length > 1) ? args[1].toLowerCase() : "qh";
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "找不到玩家 " + targetName);
                return true;
            }
            Bukkit.getLogger().info("玩家 " + p.getName() + " 执行 /ss，目标: " + targetName + ", 子命令: " + subCmd);
            switch (subCmd) {
                case "qh": target.openInventory(QiangHuaGUI.createMainGUI(target)); break;
                case "qy": target.openInventory(QianYiGUI.createMainGUI(target)); break;
                case "dy": target.openInventory(DieYingGUI.createMainGUI(target)); break;
                default: p.sendMessage(ChatColor.RED + "未知子命令，可用: qh, qy, dy"); break;
            }
            return true;
        });

        getCommand("ssadmin").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("此命令只能由玩家执行");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("strengshaku.admin")) {
                p.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                return true;
            }
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "用法: /ssadmin addlore <玩家> <段类型> <lore内容>");
                p.sendMessage(ChatColor.GRAY + "段类型: equip (装备属性), extra (额外属性), skill (技能属性)");
                p.sendMessage(ChatColor.GRAY + "lore内容支持颜色代码 &");
                return true;
            }
            String sub = args[0].toLowerCase();
            if (!sub.equals("addlore")) {
                p.sendMessage(ChatColor.RED + "未知子命令，可用: addlore");
                return true;
            }
            String playerName = args[1];
            String sectionType = args[2].toLowerCase();
            StringBuilder loreBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) loreBuilder.append(" ");
                loreBuilder.append(args[i]);
            }
            String loreLine = loreBuilder.toString();
            if (loreLine.isEmpty()) {
                p.sendMessage(ChatColor.RED + "Lore内容不能为空！");
                return true;
            }
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "找不到玩家 " + playerName);
                return true;
            }
            ItemStack item = target.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                p.sendMessage(ChatColor.RED + "目标玩家手中没有物品！");
                return true;
            }
            String header;
            switch (sectionType) {
                case "equip":
                    header = "&7=======&9&l装备属性&7=======";
                    break;
                case "extra":
                    header = "&7=======&b&l额外属性&7=======";
                    break;
                case "skill":
                    header = "&7=======&a&l技能属性&7=======";
                    break;
                default:
                    p.sendMessage(ChatColor.RED + "无效的段类型，可用: equip, extra, skill");
                    return true;
            }
            boolean success = AdminHelper.addLoreToSection(item, header, loreLine);
            if (success) {
                p.sendMessage(ChatColor.GREEN + "已成功为 " + target.getName() + " 的物品添加Lore！");
            } else {
                p.sendMessage(ChatColor.RED + "添加失败，请检查物品Lore格式是否正确。");
            }
            return true;
        });

        getLogger().info("Strengshaku 已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("Strengshaku 已禁用。");
    }

    public static Strengshaku getInstance() {
        return instance;
    }

    public static List<String> getStackableAttributes() {
        return stackableAttributes;
    }

    public static String getStackableMarker() {
        return stackableMarker;
    }
}

// ========== 管理员辅助工具 ==========
class AdminHelper {
    public static boolean addLoreToSection(ItemStack item, String headerRaw, String loreLine) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        String header = ChatColor.translateAlternateColorCodes('&', headerRaw);
        String headerRawStrip = ChatColor.stripColor(header);

        int sepIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(headerRawStrip)) {
                sepIndex = i;
                break;
            }
        }
        if (sepIndex == -1) {
            lore.add(header);
            sepIndex = lore.size() - 1;
        }
        int insertIndex = sepIndex + 1;
        String newLine = ChatColor.translateAlternateColorCodes('&', loreLine);
        lore.add(insertIndex, newLine);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return true;
    }
}

// ========== 工具类 ==========
class Utils {
    public static boolean isArmor(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m.name().endsWith("_HELMET") || m.name().endsWith("_CHESTPLATE") ||
                m.name().endsWith("_LEGGINGS") || m.name().endsWith("_BOOTS") ||
                m == Material.ELYTRA;
    }

    public static boolean isWeapon(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.WOODEN_SWORD || m == Material.STONE_SWORD ||
                m == Material.IRON_SWORD || m == Material.GOLDEN_SWORD ||
                m == Material.DIAMOND_SWORD || m == Material.NETHERITE_SWORD ||
                m == Material.WOODEN_AXE || m == Material.STONE_AXE ||
                m == Material.IRON_AXE || m == Material.GOLDEN_AXE ||
                m == Material.DIAMOND_AXE || m == Material.NETHERITE_AXE ||
                m == Material.BOW || m == Material.CROSSBOW ||
                m == Material.TRIDENT || m == Material.MACE;
    }

    // 判断两个物品是否属于同一类型（剑斧可互转，弓/弩/三叉戟/重锤各自同类）
    public static boolean isSameItemType(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        // 武器分类
        if (isWeapon(a) && isWeapon(b)) {
            Material ma = a.getType();
            Material mb = b.getType();
            // 剑类 ↔ 斧类 可以互转
            if ((isSword(ma) || isAxe(ma)) && (isSword(mb) || isAxe(mb))) return true;
            // 弓
            if (ma == Material.BOW && mb == Material.BOW) return true;
            // 弩
            if (ma == Material.CROSSBOW && mb == Material.CROSSBOW) return true;
            // 三叉戟
            if (ma == Material.TRIDENT && mb == Material.TRIDENT) return true;
            // 重锤 (MACE)
            if (ma == Material.MACE && mb == Material.MACE) return true;
            return false;
        }
        // 护甲分类
        if (isArmor(a) && isArmor(b)) {
            Material ma = a.getType();
            Material mb = b.getType();
            // 头盔类
            if (ma.name().endsWith("_HELMET") && mb.name().endsWith("_HELMET")) return true;
            // 胸甲类
            if (ma.name().endsWith("_CHESTPLATE") && mb.name().endsWith("_CHESTPLATE")) return true;
            // 护腿类
            if (ma.name().endsWith("_LEGGINGS") && mb.name().endsWith("_LEGGINGS")) return true;
            // 靴子类
            if (ma.name().endsWith("_BOOTS") && mb.name().endsWith("_BOOTS")) return true;
            // 鞘翅
            if (ma == Material.ELYTRA && mb == Material.ELYTRA) return true;
            return false;
        }
        return false;
    }

    private static boolean isSword(Material m) {
        return m.name().endsWith("_SWORD");
    }

    private static boolean isAxe(Material m) {
        return m.name().endsWith("_AXE");
    }

    public static boolean isSameCategory(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        return (isWeapon(a) && isWeapon(b)) || (isArmor(a) && isArmor(b));
    }

    public static boolean isWeaponOnlyAttribute(String attr) {
        return attr.contains("攻击力") || attr.contains("真实伤害") ||
                attr.contains("暴击率") || attr.contains("雷击") || attr.contains("吸血");
    }

    public static boolean isArmorOnlyAttribute(String attr) {
        return attr.contains("生命力") || attr.contains("闪避率") || attr.contains("防御力");
    }

    public static boolean isDecorationItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = item.getItemMeta().getDisplayName();
                if (name.contains("可爱的小挡板")) return true;
            }
        }
        return false;
    }
}

// ========== 普通强化 GUI ==========
class QiangHuaGUI {
    public static final String TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "强化装备";
    private static final int SIZE = 54;
    private static final int EQUIP_SLOT = 20, GEM_SLOT = 24, STRENGTHEN_SLOT = 39, TIP_SLOT = 40, REQUIRE_SLOT = 41;

    private static ItemStack getBackgroundItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "可爱的小挡板~");
        meta.setLore(Collections.singletonList(" "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getStrengthenButton(Player player, Inventory inv) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "点击强化");
        int rate = StrengthenManager.calculateSuccessRate(inv);
        meta.setLore(Arrays.asList(" ", ChatColor.AQUA + "成功率: " + ChatColor.WHITE + rate + "%", " "));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getTipItem() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "小贴士");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "左边放装备，右边放宝石即可强化"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getRequireItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "要求");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "根据宝石类型对应进行强化"));
        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createMainGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) {
            if (i == EQUIP_SLOT || i == GEM_SLOT || i == STRENGTHEN_SLOT || i == TIP_SLOT || i == REQUIRE_SLOT) continue;
            inv.setItem(i, getBackgroundItem());
        }
        inv.setItem(STRENGTHEN_SLOT, getStrengthenButton(player, inv));
        inv.setItem(TIP_SLOT, getTipItem());
        inv.setItem(REQUIRE_SLOT, getRequireItem());
        return inv;
    }

    public static void updateButton(Inventory inv, Player player) {
        if (inv != null) inv.setItem(STRENGTHEN_SLOT, getStrengthenButton(player, inv));
    }
}

// ========== 属性迁移 GUI（提示已更新剑斧互转） ==========
class QianYiGUI {
    public static final String TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "属性迁移";
    private static final int SIZE = 54;
    private static final int OLD_SLOT = 20, NEW_SLOT = 24, MIGRATE_SLOT = 39, TIP_SLOT = 40, REQ_SLOT = 41;
    public static final int PREVIEW_SLOT = 22;

    private static ItemStack getBackgroundItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "可爱的小挡板~");
        meta.setLore(Collections.singletonList(" "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getMigrateButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "迁移属性");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "将左边属性段复制到右边装备（附魔同步转移，可叠加属性保留）"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getTipItem() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "小贴士");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "左边旧装备，右边新装备（剑斧可互转，其他需同类）"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getReqItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "要求");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "剑类↔斧类可互转；弓/弩/三叉戟/重锤需同类；护甲需同部位"));
        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createMainGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) {
            if (i == OLD_SLOT || i == NEW_SLOT || i == MIGRATE_SLOT || i == TIP_SLOT || i == REQ_SLOT || i == PREVIEW_SLOT) continue;
            inv.setItem(i, getBackgroundItem());
        }
        inv.setItem(MIGRATE_SLOT, getMigrateButton());
        inv.setItem(TIP_SLOT, getTipItem());
        inv.setItem(REQ_SLOT, getReqItem());
        inv.setItem(PREVIEW_SLOT, getDefaultPreviewItem());
        return inv;
    }

    private static ItemStack getDefaultPreviewItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "预览");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "↔️ 未放入装备"));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getPreviewItem(ItemStack oldItem, ItemStack newItem) {
        if (oldItem == null || newItem == null) {
            return getDefaultPreviewItem();
        }
        if (!Utils.isSameItemType(oldItem, newItem)) {
            ItemStack error = new ItemStack(Material.BARRIER);
            ItemMeta meta = error.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "预览");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "↔️ 类型不匹配"));
            error.setItemMeta(meta);
            return error;
        }
        ItemStack preview = newItem.clone();
        // 迁移附魔
        if (oldItem.hasItemMeta() && oldItem.getItemMeta().hasEnchants()) {
            Map<Enchantment, Integer> enchants = oldItem.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                preview.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }
        // 迁移属性段
        if (oldItem.hasItemMeta() && oldItem.getItemMeta().hasLore()) {
            List<String> oldLore = oldItem.getItemMeta().getLore();
            List<String> newLore = preview.hasItemMeta() && preview.getItemMeta().hasLore() ?
                    preview.getItemMeta().getLore() : new ArrayList<>();
            List<String> merged = simulateMigration(oldLore, newLore);
            ItemMeta previewMeta = preview.getItemMeta();
            previewMeta.setLore(merged);
            preview.setItemMeta(previewMeta);
        }
        ItemMeta meta = preview.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(0, ChatColor.GRAY + "===== 迁移预览 =====");
        meta.setLore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    private static List<String> simulateMigration(List<String> oldLore, List<String> newLore) {
        List<String> result = new ArrayList<>(newLore);
        String[] sections = {
                "&7=======&9&l装备属性&7=======",
                "&7=======&b&l额外属性&7=======",
                "&7=======&a&l技能属性&7======="
        };
        List<String> stackableAttrs = Strengshaku.getStackableAttributes();
        String stackableMarker = Strengshaku.getStackableMarker();

        for (String sec : sections) {
            String header = ChatColor.translateAlternateColorCodes('&', sec);
            String headerRaw = ChatColor.stripColor(header);

            List<String> stackableLines = new ArrayList<>();
            int newStart = -1, newEnd = -1;
            for (int i = 0; i < result.size(); i++) {
                String raw = ChatColor.stripColor(result.get(i));
                if (raw.contains(headerRaw)) {
                    newStart = i;
                    int j = i + 1;
                    while (j < result.size()) {
                        boolean isNextHeader = false;
                        for (String s : sections) {
                            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s)).equals(ChatColor.stripColor(result.get(j)))) {
                                isNextHeader = true;
                                break;
                            }
                        }
                        if (isNextHeader) break;
                        j++;
                    }
                    newEnd = j;
                    for (int k = i + 1; k < newEnd; k++) {
                        String line = result.get(k);
                        if (isStackableLine(line, stackableAttrs, stackableMarker)) {
                            stackableLines.add(line);
                        }
                    }
                    break;
                }
            }
            if (newStart != -1) {
                for (int i = newEnd - 1; i >= newStart; i--) {
                    result.remove(i);
                }
            }
            List<String> oldContent = new ArrayList<>();
            int oldStart = -1;
            for (int i = 0; i < oldLore.size(); i++) {
                String raw = ChatColor.stripColor(oldLore.get(i));
                if (raw.contains(headerRaw)) {
                    oldStart = i;
                    int j = i + 1;
                    while (j < oldLore.size()) {
                        boolean isNextHeader = false;
                        for (String s : sections) {
                            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s)).equals(ChatColor.stripColor(oldLore.get(j)))) {
                                isNextHeader = true;
                                break;
                            }
                        }
                        if (isNextHeader) break;
                        j++;
                    }
                    for (int k = i + 1; k < j; k++) {
                        oldContent.add(oldLore.get(k));
                    }
                    break;
                }
            }
            if (oldContent.isEmpty() && stackableLines.isEmpty()) continue;
            result.add(header);
            result.addAll(stackableLines);
            result.addAll(oldContent);
        }
        return result;
    }

    private static boolean isStackableLine(String line, List<String> stackableAttrs, String marker) {
        if (line == null) return false;
        String converted = line.replace('§', '&');
        if (!marker.isEmpty() && converted.contains(marker)) return true;
        Pattern p = Pattern.compile("&8\\[\\*\\]&7([^&]+)&c[+-]?\\d+\\.?\\d*");
        Matcher m = p.matcher(converted);
        if (m.find()) {
            String attrName = m.group(1);
            return stackableAttrs.contains(attrName);
        }
        return false;
    }
}

// ========== 属性叠影 GUI ==========
class DieYingGUI {
    public static final String TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "属性叠影";
    private static final int SIZE = 54;
    private static final int EQUIP_SLOT = 20, WASTE_SLOT = 24, DIEYING_SLOT = 39, TIP_SLOT = 40, REQ_SLOT = 41;

    private static ItemStack getBackgroundItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "可爱的小挡板~");
        meta.setLore(Collections.singletonList(" "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getDieYingButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "执行叠影");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "消耗废弃装备，转化对应属性"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getTipItem() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "小贴士");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "左边目标，右边消耗品（武器任意，护甲任意）"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getReqItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "要求");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "两者必须同为武器或同为护甲"));
        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createMainGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) {
            if (i == EQUIP_SLOT || i == WASTE_SLOT || i == DIEYING_SLOT || i == TIP_SLOT || i == REQ_SLOT) continue;
            inv.setItem(i, getBackgroundItem());
        }
        inv.setItem(DIEYING_SLOT, getDieYingButton());
        inv.setItem(TIP_SLOT, getTipItem());
        inv.setItem(REQ_SLOT, getReqItem());
        return inv;
    }
}

// ========== 所有监听器 ==========
class AllListeners implements Listener {

    @EventHandler
    public void onQHClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (inv == null) return;
        if (!event.getView().getTitle().equals(QiangHuaGUI.TITLE)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        event.setCancelled(true);

        if (slot == 20 || slot == 24) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTask(Strengshaku.getInstance(), () -> QiangHuaGUI.updateButton(inv, p));
            return;
        }
        if (slot == 39) {
            ItemStack equip = inv.getItem(20);
            ItemStack gem = inv.getItem(24);
            if (equip == null || equip.getType() == Material.AIR) {
                p.sendMessage(ChatColor.RED + "请放入装备！");
                return;
            }
            if (gem == null || gem.getType() == Material.AIR) {
                p.sendMessage(ChatColor.RED + "请放入宝石！");
                return;
            }
            if (gem.getType() != Material.MAGMA_CREAM) {
                p.sendMessage(ChatColor.RED + "宝石必须是岩浆膏！");
                return;
            }
            List<GemParser.AttributeResult> attrs = GemParser.parseGem(gem);
            if (attrs.isEmpty()) {
                p.sendMessage(ChatColor.RED + "宝石无可识别属性！");
                return;
            }
            for (GemParser.AttributeResult attr : attrs) {
                if (Utils.isWeaponOnlyAttribute(attr.attribute) && Utils.isArmor(equip)) {
                    p.sendMessage(ChatColor.RED + "该属性不能强化在装备上！");
                    return;
                }
                if (Utils.isArmorOnlyAttribute(attr.attribute) && !Utils.isArmor(equip)) {
                    p.sendMessage(ChatColor.RED + "该属性不能强化在武器上！");
                    return;
                }
            }
            StrengthenManager.performStrengthen(p, equip, gem, inv);
            Bukkit.getScheduler().runTask(Strengshaku.getInstance(), () -> QiangHuaGUI.updateButton(inv, p));
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onQHDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (inv == null) return;
        if (!event.getView().getTitle().equals(QiangHuaGUI.TITLE)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && slot != 20 && slot != 24) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTask(Strengshaku.getInstance(), () -> QiangHuaGUI.updateButton(inv, (Player) event.getWhoClicked()));
    }

    @EventHandler
    public void onQYClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (inv == null) return;
        if (!event.getView().getTitle().equals(QianYiGUI.TITLE)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        event.setCancelled(true);

        if (slot == 20 || slot == 24) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTask(Strengshaku.getInstance(), () -> updatePreview(inv));
            return;
        }
        if (slot == 39) {
            ItemStack oldItem = inv.getItem(20);
            ItemStack newItem = inv.getItem(24);
            if (oldItem == null || newItem == null) {
                p.sendMessage(ChatColor.RED + "请放入两个物品！");
                return;
            }
            if (!Utils.isSameItemType(oldItem, newItem)) {
                p.sendMessage(ChatColor.RED + "剑类↔斧类可互转，其他需同类！");
                return;
            }
            boolean success = QianYiManager.migrateAttributes(oldItem, newItem);
            if (success) {
                p.sendMessage(ChatColor.GREEN + "属性迁移成功！");
                inv.setItem(20, null);
                inv.setItem(24, newItem);
                updatePreview(inv);
            } else {
                p.sendMessage(ChatColor.RED + "迁移失败，旧装备没有属性段！");
            }
            return;
        }
        if (slot == QianYiGUI.PREVIEW_SLOT) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onQYDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (inv == null) return;
        if (!event.getView().getTitle().equals(QianYiGUI.TITLE)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && slot != 20 && slot != 24) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTask(Strengshaku.getInstance(), () -> updatePreview(inv));
    }

    private void updatePreview(Inventory inv) {
        ItemStack oldItem = inv.getItem(20);
        ItemStack newItem = inv.getItem(24);
        ItemStack preview = QianYiGUI.getPreviewItem(oldItem, newItem);
        inv.setItem(QianYiGUI.PREVIEW_SLOT, preview);
    }

    @EventHandler
    public void onDYClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (inv == null) return;
        if (!event.getView().getTitle().equals(DieYingGUI.TITLE)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        event.setCancelled(true);

        if (slot == 20 || slot == 24) {
            event.setCancelled(false);
            return;
        }
        if (slot == 39) {
            ItemStack equip = inv.getItem(20);
            ItemStack waste = inv.getItem(24);
            if (equip == null || waste == null) {
                p.sendMessage(ChatColor.RED + "请放入两个物品！");
                return;
            }

            boolean equipWeapon = Utils.isWeapon(equip);
            boolean wasteWeapon = Utils.isWeapon(waste);
            boolean equipArmor = Utils.isArmor(equip);
            boolean wasteArmor = Utils.isArmor(waste);

            if ((equipWeapon && wasteWeapon) || (equipArmor && wasteArmor)) {
                // 通过
            } else {
                p.sendMessage(ChatColor.RED + "两者必须同为武器或同为护甲！");
                return;
            }

            boolean success = DieYingManager.applyDieYing(equip, waste);
            inv.setItem(24, null);
            if (success) {
                p.sendMessage(ChatColor.GREEN + "叠影成功！");
                inv.setItem(20, equip);
            } else {
                p.sendMessage(ChatColor.RED + "叠影失败，消耗品已消耗！");
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDYDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (inv == null) return;
        if (!event.getView().getTitle().equals(DieYingGUI.TITLE)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && slot != 20 && slot != 24) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player p = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();

        if (!title.equals(QiangHuaGUI.TITLE) &&
                !title.equals(QianYiGUI.TITLE) &&
                !title.equals(DieYingGUI.TITLE)) {
            return;
        }

        int[] slots = {20, 24};
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (Utils.isDecorationItem(item)) continue;
            ItemStack toGive = item.clone();
            inv.setItem(slot, null);
            HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(toGive);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    p.getWorld().dropItem(p.getLocation(), drop);
                }
            }
        }
    }
}

// ========== 宝石解析 ==========
class GemParser {
    private static List<AttributePattern> patterns = new ArrayList<>();
    private static List<ComboGem> comboGems = new ArrayList<>();
    private static String superIndicator;

    public static void loadPatterns() {
        patterns.clear();
        ConfigurationSection section = Strengshaku.getInstance().getConfig().getConfigurationSection("attribute-patterns");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String regex = section.getString(key + ".regex");
                String attr = section.getString(key + ".attribute");
                if (regex != null && attr != null) {
                    patterns.add(new AttributePattern(Pattern.compile(regex), attr));
                }
            }
        }

        comboGems.clear();
        List<Map<?, ?>> comboList = Strengshaku.getInstance().getConfig().getMapList("combo-gems");
        for (Map<?, ?> map : comboList) {
            String trigger = (String) map.get("trigger");
            List<String> attrs = (List<String>) map.get("attributes");
            List<?> incs = (List<?>) map.get("increments");
            if (trigger != null && attrs != null && incs != null && attrs.size() == incs.size()) {
                List<Integer> increments = new ArrayList<>();
                for (Object o : incs) increments.add(Integer.parseInt(o.toString()));
                comboGems.add(new ComboGem(trigger, attrs, increments));
            }
        }

        superIndicator = Strengshaku.getInstance().getConfig().getString("super-gem-indicator", "&8[*]&b特级宝石");
    }

    public static List<AttributeResult> parseGem(ItemStack gem) {
        List<AttributeResult> results = new ArrayList<>();
        if (gem == null || !gem.hasItemMeta()) return results;
        ItemMeta meta = gem.getItemMeta();
        if (!meta.hasLore()) return results;

        List<String> lore = meta.getLore();
        List<String> converted = new ArrayList<>();
        for (String line : lore) converted.add(line.replace('§', '&'));

        for (ComboGem combo : comboGems) {
            for (String line : converted) {
                if (line.contains(combo.trigger)) {
                    for (int i = 0; i < combo.attributes.size(); i++) {
                        results.add(new AttributeResult(combo.attributes.get(i), combo.increments.get(i)));
                    }
                    return results;
                }
            }
        }

        for (String line : converted) {
            for (AttributePattern pattern : patterns) {
                Matcher m = pattern.pattern.matcher(line);
                if (m.find()) {
                    try {
                        int val = Integer.parseInt(m.group(1));
                        results.add(new AttributeResult(pattern.attributeName, val));
                        return results;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return results;
    }

    public static boolean isSuperGem(ItemStack gem) {
        if (gem == null || !gem.hasItemMeta()) return false;
        ItemMeta meta = gem.getItemMeta();
        if (!meta.hasLore()) return false;
        String target = superIndicator;
        for (String line : meta.getLore()) {
            if (line.replace('§', '&').contains(target)) return true;
        }
        return false;
    }

    static class AttributePattern { Pattern pattern; String attributeName; AttributePattern(Pattern p, String n) { pattern = p; attributeName = n; } }
    static class ComboGem { String trigger; List<String> attributes; List<Integer> increments; ComboGem(String t, List<String> a, List<Integer> i) { trigger = t; attributes = a; increments = i; } }
    static class AttributeResult { String attribute; int value; AttributeResult(String a, int v) { attribute = a; value = v; } }
}

// ========== 强化管理器 ==========
class StrengthenManager {
    private static final Random RANDOM = new Random();
    private static final String SEPARATOR = ChatColor.translateAlternateColorCodes('&',
            Strengshaku.getInstance().getConfig().getString("attribute-separator",
                    "&7=======&9&l装备属性&7======="));
    private static final String ATTR_FORMAT = Strengshaku.getInstance().getConfig().getString("attribute-format",
            "&8[*]&7{attribute}&c{value}");

    public static int calculateSuccessRate(Inventory inv) {
        ItemStack gem = inv.getItem(24);
        ItemStack equip = inv.getItem(20);
        if (gem == null || equip == null) return 0;
        if (GemParser.isSuperGem(gem)) return 100;

        List<GemParser.AttributeResult> attrs = GemParser.parseGem(gem);
        if (attrs.isEmpty()) return 0;
        String attrName = attrs.get(0).attribute;

        int attrValue = getAttributeValue(equip, attrName);
        return getProbabilityFromValue(attrValue);
    }

    private static int getAttributeValue(ItemStack item, String attributeName) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return 0;
        ItemMeta meta = item.getItemMeta();
        Pattern p = Pattern.compile("&8\\[\\*\\]&7" + Pattern.quote(attributeName) + "&c([+-]?\\d+)");
        for (String line : meta.getLore()) {
            String conv = line.replace('§', '&');
            Matcher m = p.matcher(conv);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static int getProbabilityFromValue(int value) {
        FileConfiguration config = Strengshaku.getInstance().getConfig();
        List<Map<?, ?>> probList = config.getMapList("attack-gem-probability");
        if (probList == null || probList.isEmpty()) return 50;
        for (Map<?, ?> entry : probList) {
            Object maxObj = entry.get("max-attack");
            if (maxObj != null) {
                try {
                    int max = Integer.parseInt(maxObj.toString());
                    if (value < max) {
                        Object rateObj = entry.get("rate");
                        if (rateObj != null) {
                            double rate = Double.parseDouble(rateObj.toString());
                            return (int) (rate * 100);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Object def = config.get("attack-gem-probability.default");
        if (def != null) {
            try { return (int) (Double.parseDouble(def.toString()) * 100); } catch (NumberFormatException ignored) {}
        }
        return 5;
    }

    public static void performStrengthen(Player player, ItemStack equip, ItemStack gem, Inventory gui) {
        List<GemParser.AttributeResult> attrs = GemParser.parseGem(gem);
        if (attrs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "宝石无可识别属性！");
            return;
        }

        int currentLevel = getCurrentLevel(equip);
        int maxLevel = Strengshaku.getInstance().getConfig().getInt("max-level", 10);
        if (currentLevel >= maxLevel) {
            player.sendMessage(ChatColor.RED + "已达最大强化等级！");
            return;
        }

        if (attrs.size() > 1) {
            String firstAttr = attrs.get(0).attribute;
            if (hasAttribute(equip, firstAttr)) {
                attrs = Collections.singletonList(attrs.get(0));
            }
        }

        int rate = calculateSuccessRate(gui);
        boolean success = RANDOM.nextInt(100) < rate;

        gem.setAmount(gem.getAmount() - 1);
        if (gem.getAmount() <= 0) gui.setItem(24, null);
        else gui.setItem(24, gem);

        if (success) {
            for (GemParser.AttributeResult attr : attrs) {
                applyAttribute(equip, attr.attribute, attr.value);
            }
            player.sendMessage(ChatColor.GREEN + "强化成功！");
        } else {
            player.sendMessage(ChatColor.RED + "强化失败，宝石已消耗！");
        }
    }

    private static boolean hasAttribute(ItemStack item, String attributeName) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        ItemMeta meta = item.getItemMeta();
        Pattern p = Pattern.compile("&8\\[\\*\\]&7" + Pattern.quote(attributeName) + "&c([+-]?\\d+)");
        for (String line : meta.getLore()) {
            String conv = line.replace('§', '&');
            Matcher m = p.matcher(conv);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    private static int getCurrentLevel(ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        List<String> lore = meta.getLore();
        boolean in = false; int count = 0;
        for (String line : lore) {
            String raw = ChatColor.stripColor(line);
            if (!in) {
                if (raw.contains(ChatColor.stripColor(SEPARATOR))) in = true;
            } else {
                if (!line.trim().isEmpty()) count++;
            }
        }
        return count;
    }

    private static void applyAttribute(ItemStack item, String attr, int inc) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        int sepIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(ChatColor.stripColor(SEPARATOR))) {
                sepIndex = i;
                break;
            }
        }
        if (sepIndex == -1) {
            lore.add(0, SEPARATOR);
            sepIndex = 0;
        }

        Pattern p = Pattern.compile("&8\\[\\*\\]&7" + Pattern.quote(attr) + "&c([+-]?\\d+)");
        boolean found = false;
        for (int i = sepIndex + 1; i < lore.size(); i++) {
            String line = lore.get(i);
            String conv = line.replace('§', '&');
            Matcher m = p.matcher(conv);
            if (m.find()) {
                try {
                    int cur = Integer.parseInt(m.group(1));
                    int nval = cur + inc;
                    String newLine = ATTR_FORMAT.replace("{attribute}", attr).replace("{value}", (nval >= 0 ? "+" : "") + nval);
                    lore.set(i, ChatColor.translateAlternateColorCodes('&', newLine));
                    found = true;
                    break;
                } catch (NumberFormatException ignored) {}
            }
        }
        if (!found) {
            int insert = sepIndex + 1;
            String newLine = ATTR_FORMAT.replace("{attribute}", attr).replace("{value}", (inc >= 0 ? "+" : "") + inc);
            lore.add(insert, ChatColor.translateAlternateColorCodes('&', newLine));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}

// ========== 属性迁移管理器 ==========
class QianYiManager {
    private static final String[] SECTIONS = {
            "&7=======&9&l装备属性&7=======",
            "&7=======&b&l额外属性&7=======",
            "&7=======&a&l技能属性&7======="
    };

    public static boolean migrateAttributes(ItemStack oldItem, ItemStack newItem) {
        if (oldItem.hasItemMeta() && oldItem.getItemMeta().hasEnchants()) {
            Map<Enchantment, Integer> enchants = oldItem.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                newItem.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        if (!oldItem.hasItemMeta() || !oldItem.getItemMeta().hasLore()) {
            return true;
        }
        List<String> oldLore = oldItem.getItemMeta().getLore();
        List<String> newLore = newItem.hasItemMeta() && newItem.getItemMeta().hasLore() ?
                newItem.getItemMeta().getLore() : new ArrayList<>();

        for (String sec : SECTIONS) {
            String header = ChatColor.translateAlternateColorCodes('&', sec);
            String headerRaw = ChatColor.stripColor(header);

            String newHeader = null;
            List<String> stackableLines = new ArrayList<>();
            int newStart = -1, newEnd = -1;
            for (int i = 0; i < newLore.size(); i++) {
                String raw = ChatColor.stripColor(newLore.get(i));
                if (raw.contains(headerRaw)) {
                    newStart = i;
                    newHeader = newLore.get(i);
                    int j = i + 1;
                    while (j < newLore.size()) {
                        boolean isNextHeader = false;
                        for (String s : SECTIONS) {
                            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s)).equals(ChatColor.stripColor(newLore.get(j)))) {
                                isNextHeader = true;
                                break;
                            }
                        }
                        if (isNextHeader) break;
                        j++;
                    }
                    newEnd = j;
                    for (int k = i + 1; k < newEnd; k++) {
                        String line = newLore.get(k);
                        if (isStackableLine(line)) {
                            stackableLines.add(line);
                        }
                    }
                    break;
                }
            }

            if (newStart != -1) {
                for (int i = newEnd - 1; i >= newStart; i--) {
                    newLore.remove(i);
                }
            }

            List<String> oldContent = new ArrayList<>();
            int oldStart = -1;
            for (int i = 0; i < oldLore.size(); i++) {
                String raw = ChatColor.stripColor(oldLore.get(i));
                if (raw.contains(headerRaw)) {
                    oldStart = i;
                    int j = i + 1;
                    while (j < oldLore.size()) {
                        boolean isNextHeader = false;
                        for (String s : SECTIONS) {
                            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s)).equals(ChatColor.stripColor(oldLore.get(j)))) {
                                isNextHeader = true;
                                break;
                            }
                        }
                        if (isNextHeader) break;
                        j++;
                    }
                    for (int k = i + 1; k < j; k++) {
                        oldContent.add(oldLore.get(k));
                    }
                    break;
                }
            }

            if (oldContent.isEmpty() && stackableLines.isEmpty()) continue;

            String finalHeader = (newHeader != null) ? newHeader : header;
            newLore.add(finalHeader);
            newLore.addAll(stackableLines);
            newLore.addAll(oldContent);
        }

        ItemMeta meta = newItem.getItemMeta();
        meta.setLore(newLore);
        newItem.setItemMeta(meta);
        return true;
    }

    private static boolean isStackableLine(String line) {
        if (line == null) return false;
        String converted = line.replace('§', '&');

        String marker = Strengshaku.getStackableMarker();
        if (!marker.isEmpty() && converted.contains(marker)) {
            return true;
        }

        Pattern p = Pattern.compile("&8\\[\\*\\]&7([^&]+)&c[+-]?\\d+\\.?\\d*");
        Matcher m = p.matcher(converted);
        if (m.find()) {
            String attrName = m.group(1);
            return Strengshaku.getStackableAttributes().contains(attrName);
        }
        return false;
    }
}

// ========== 属性叠影管理器 ==========
class DieYingManager {
    private static final Random RANDOM = new Random();
    private static final String EXTRA_SEP = "&7=======&b&l额外属性&7=======";
    private static final String CRIT_DMG_FORMAT = "&8[*]&7暴伤倍率增加&c+{value}";
    private static final String CRIT_RESIST_FORMAT = "&8[*]&7暴伤抵抗增加&c+{value}";

    public static boolean applyDieYing(ItemStack equip, ItemStack waste) {
        boolean isWeapon = Utils.isWeapon(equip);
        boolean isArmor = Utils.isArmor(equip);
        if (!isWeapon && !isArmor) return false;

        int baseValue = getBaseValue(waste, isWeapon);
        if (baseValue == 0) return false;

        double bonus = baseValue * 0.1;
        String bonusStr = String.format("%.1f", bonus);

        int mainAttr = getMainAttribute(equip);
        double rate = (mainAttr == 0) ? 1.0 : getDieYingProbability(mainAttr);
        boolean success = RANDOM.nextDouble() < rate;

        if (!success) return false;

        String attrFormat = isWeapon ? CRIT_DMG_FORMAT : CRIT_RESIST_FORMAT;
        String attrName = isWeapon ? "暴伤倍率增加" : "暴伤抵抗增加";

        ItemMeta meta = equip.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        String header = ChatColor.translateAlternateColorCodes('&', EXTRA_SEP);
        int sepIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(ChatColor.stripColor(header))) {
                sepIndex = i;
                break;
            }
        }

        if (sepIndex == -1) {
            lore.add(header);
            sepIndex = lore.size() - 1;
            String newLine = attrFormat.replace("{value}", bonusStr);
            lore.add(ChatColor.translateAlternateColorCodes('&', newLine));
        } else {
            Pattern p = Pattern.compile("&8\\[\\*\\]&7" + Pattern.quote(attrName) + "&c\\+([+-]?\\d+\\.?\\d*)");
            boolean found = false;
            for (int i = sepIndex + 1; i < lore.size(); i++) {
                String line = lore.get(i);
                String conv = line.replace('§', '&');
                Matcher m = p.matcher(conv);
                if (m.find()) {
                    try {
                        double current = Double.parseDouble(m.group(1));
                        double newVal = current + bonus;
                        String newLine = attrFormat.replace("{value}", String.format("%.1f", newVal));
                        lore.set(i, ChatColor.translateAlternateColorCodes('&', newLine));
                        found = true;
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (!found) {
                int insert = sepIndex + 1;
                String newLine = attrFormat.replace("{value}", bonusStr);
                lore.add(insert, ChatColor.translateAlternateColorCodes('&', newLine));
            }
        }

        meta.setLore(lore);
        equip.setItemMeta(meta);
        return true;
    }

    private static int getBaseValue(ItemStack item, boolean isWeaponTarget) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        Pattern p;
        if (isWeaponTarget) {
            p = Pattern.compile("&8\\[\\*\\]&7攻击力增加&c([+-]?\\d+)");
        } else {
            p = Pattern.compile("&8\\[\\*\\]&7生命力增加&c([+-]?\\d+)");
        }
        for (String line : meta.getLore()) {
            String conv = line.replace('§', '&');
            Matcher m = p.matcher(conv);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static int getMainAttribute(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return 0;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        Pattern p;
        if (Utils.isWeapon(item)) {
            p = Pattern.compile("&8\\[\\*\\]&7暴伤倍率增加&c\\+([+-]?\\d+\\.?\\d*)");
        } else if (Utils.isArmor(item)) {
            p = Pattern.compile("&8\\[\\*\\]&7暴伤抵抗增加&c\\+([+-]?\\d+\\.?\\d*)");
        } else {
            return 0;
        }
        for (String line : lore) {
            String conv = line.replace('§', '&');
            Matcher m = p.matcher(conv);
            if (m.find()) {
                try {
                    double val = Double.parseDouble(m.group(1));
                    return (int) Math.round(val);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static double getDieYingProbability(int mainAttr) {
        FileConfiguration config = Strengshaku.getInstance().getConfig();
        List<Map<?, ?>> probList = config.getMapList("dieying-probability");
        if (probList == null || probList.isEmpty()) return 0.5;
        for (Map<?, ?> entry : probList) {
            Object maxObj = entry.get("max-attribute");
            if (maxObj != null) {
                try {
                    int max = Integer.parseInt(maxObj.toString());
                    if (mainAttr < max) {
                        Object rateObj = entry.get("rate");
                        if (rateObj != null) {
                            return Double.parseDouble(rateObj.toString());
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Object def = config.get("dieying-probability.default");
        if (def != null) {
            try { return Double.parseDouble(def.toString()); } catch (NumberFormatException ignored) {}
        }
        return 0.05;
    }
}