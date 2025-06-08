package me.zrageyh.telefono.manager;

import dev.lone.itemsadder.api.CustomStack;
import lombok.Getter;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.nbt.NBTItem;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.List;

/*
 * Gestisce tutti gli ItemStack del plugin
 * Sostituisce gli static per dependency injection
 */
@Getter
public class ItemManager {

    public static final String GUI_TITLE_MAIN = "§0.";

    private final ItemStack itemTelephone;
    private final ItemBuilder border;
    private final ItemStack itemFattura;
    private final ItemStack itemGps;
    private final ItemStack itemRubrica;
    private final ItemStack itemHistoryCalls;
    private final ItemStack itemHistoryMessages;
    private final ItemStack itemRemoveSim;
    private final ItemStack itemEmergency;
    private final ItemStack itemInvisible;
    private final ItemStack itemDiscord;
    private final ItemStack itemStore;
    private final ItemStack itemImmobiliare;
    private final ItemStack itemDarkChat;
    private final ItemStack itemTwitch;
    private final ItemStack itemReport;

    public ItemManager() {
        this.itemTelephone = getTelephoneItem();
        this.border = createBorder();
        this.itemInvisible = createInvisibleItem();
        this.itemFattura = buildItem("§f§lғᴀᴛᴛᴜʀᴇ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ʟᴇ ᴛᴜᴇ §fғᴀᴛᴛᴜʀᴇ", "§7ᴅᴀʟ ᴛᴇʟᴇғᴏɴᴏ");
        this.itemGps = buildItem("§f§lɢᴘs", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ɪʟ §fɴᴀᴠɪɢᴀᴛᴏʀᴇ", "§7ᴅᴀʟ ᴛᴇʟᴇғᴏɴᴏ");
        this.itemRubrica = buildItem("§f§lʀᴜʙʀɪᴄᴀ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ʟᴀ ʟɪsᴛᴀ", "§7ᴅᴇɪ ᴛᴜᴏɪ §fᴄᴏɴᴛᴀᴛᴛɪ");
        this.itemHistoryCalls = buildItem("§f§lᴄʀᴏɴᴏʟᴏɢɪᴀ ᴄʜɪᴀᴍᴀᴛᴇ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ʟᴀ ᴄʀᴏɴᴏʟᴏɢɪᴀ", "§7ᴅᴇʟʟᴇ ᴛᴜᴇ §fᴄʜɪᴀᴍᴀᴛᴇ");
        this.itemHistoryMessages = buildItem("§f§lᴄʀᴏɴᴏʟᴏɢɪᴀ ᴍᴇssᴀɢɢɪ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ʟᴀ ᴄʀᴏɴᴏʟᴏɢɪᴀ", "§7ᴅᴇɪ ᴛᴜᴏɪ §fᴍᴇssᴀɢɢɪ");
        this.itemRemoveSim = buildItem("§f§lᴇsᴛʀᴀɪ sɪᴍ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ §fᴇsᴛʀᴀʀʀᴇ §7ʟᴀ sɪᴍ", "§7ᴅᴀʟ ᴛᴇʟᴇғᴏɴᴏ");
        this.itemEmergency = buildItem("§f§lᴇᴍᴇʀɢᴇɴᴢᴀ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ɪʟ ᴍᴇɴù", "§7ᴅᴇʟʟᴇ §fᴇᴍᴇʀɢᴇɴᴢᴇ");
        this.itemDiscord = buildItem("§f§lᴅɪsᴄᴏʀᴅ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ɪʟ ʟɪɴᴋ", "§7ᴅᴇʟ §fᴅɪsᴄᴏʀᴅ §7ᴜғғɪᴄɪᴀʟᴇ");
        this.itemStore = buildItem("§f§lsᴛᴏʀᴇ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ɪʟ ʟɪɴᴋ", "§7ᴅᴇʟ §fsᴛᴏʀᴇ §7ᴜғғɪᴄɪᴀʟᴇ");
        this.itemImmobiliare = buildItem("§f§lɪᴍᴍᴏʙɪʟɪᴀʀᴇ", "§7§oIɴ ᴀʀʀɪᴠᴏ ᴄᴏɴ ɪʟ ᴘʀᴏssɪᴍᴏ ᴀɢɢɪᴏʀɴᴀᴍᴇɴᴛᴏ", "§7§oᴅᴇʟ sɪsᴛᴇᴍᴀ...");
        this.itemDarkChat = buildItem("§f§lᴅᴀʀᴋ ᴄʜᴀᴛ", "§7§oIɴ ᴀʀʀɪᴠᴏ ᴄᴏɴ ɪʟ ᴘʀᴏssɪᴍᴏ ᴀɢɢɪᴏʀɴᴀᴍᴇɴᴛᴏ", "§7§oᴅᴇʟ sɪsᴛᴇᴍᴀ...");
        this.itemTwitch = buildItem("§f§lᴛᴡɪᴛᴄʜ", "§7§oIɴ ᴀʀʀɪᴠᴏ ᴄᴏɴ ɪʟ ᴘʀᴏssɪᴍᴏ ᴀɢɢɪᴏʀɴᴀᴍᴇɴᴛᴏ", "§7§oᴅᴇʟ sɪsᴛᴇᴍᴀ...");
        this.itemReport = buildItem("§f§lʀᴇᴘᴏʀᴛ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ɪ §fᴛɪᴄᴋᴇᴛ", "§7ᴏ ᴄʜɪᴇᴅᴇʀᴇ §fᴀɪᴜᴛᴏ");
    }

    /* Crea border con ItemsAdder */
    private ItemBuilder createBorder() {
        return new ItemBuilder(CompMaterial.BLACK_STAINED_GLASS_PANE.toItem())
            .setDisplayName(" ")
            .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS));
    }

    /* Crea item invisibile per AnvilGUI */
    private ItemStack createInvisibleItem() {
        return new ItemBuilder(CustomStack.getInstance("mcicons:icon_time_day").getItemStack())
            .setDisplayName("")
            .setLegacyLore(List.of(""))
            .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
            .clearEnchantments()
            .clearModifiers()
            .get();
    }

    private ItemStack getTelephoneItem() {
        final ItemStack telephone =ItemCreator.of(CustomStack.getInstance("iageneric:telephone").getItemStack())
                .name("&fTelefono")
                .lore("  ", "&7Numero: &fnessuno")
                .make();

        final NBTItem nbtItem = new NBTItem(telephone);
        nbtItem.setString("telephone_number", "nessuno");
        return nbtItem.getItem();
    }

    /* Factory method per creare item standard */
    private ItemStack buildItem(final String name, final String... lore) {
        return new ItemBuilder(CustomStack.getInstance("mcicons:icon_time_day").getItemStack())
            .setDisplayName(name)
            .setLegacyLore(List.of(lore))
            .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
            .clearEnchantments()
            .clearModifiers()
            .get();
    }

    /* Validazione sicura ItemStack - controlla NBT + material invece di solo display name */
    public boolean isSpecificItem(final ItemStack item, final ItemStack reference) {
        if (item == null || reference == null) return false;

        // Controllo material type
        if (!item.getType().equals(reference.getType())) return false;

        // Controllo display name se presente
        if (item.hasItemMeta() && reference.hasItemMeta()) {
                    final String itemName = item.getItemMeta().getDisplayName();
        final String refName = reference.getItemMeta().getDisplayName();
            if (!itemName.equals(refName)) return false;
        }

        // Per CustomStack, verifica l'ID univoco
        final CustomStack itemCustom = CustomStack.byItemStack(item);
        final CustomStack refCustom = CustomStack.byItemStack(reference);

        if (itemCustom != null && refCustom != null) {
            return itemCustom.getId().equals(refCustom.getId());
        }

        return true;
    }

    /* Controllo ottimizzato per telefono - usa CustomStack ID per performance */
    public boolean isTelephone(final ItemStack item) {
        if (item == null) return false;
        final CustomStack customStack = CustomStack.byItemStack(item);
        return customStack != null && "telephone".equals(customStack.getId());
    }

    /* Controllo ottimizzato per SIM - usa CustomStack ID */
    public boolean isSim(final ItemStack item) {
        if (item == null) return false;

        final CustomStack customStack = CustomStack.byItemStack(item);
        return customStack != null && "sim".equals(customStack.getId());
    }
} 