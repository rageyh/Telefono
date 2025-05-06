package me.zrageyh.telefono;


import dev.lone.itemsadder.api.CustomStack;
import lombok.Getter;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.zrageyh.telefono.cache.*;
import me.zrageyh.telefono.events.EventInteractMainMenu;
import me.zrageyh.telefono.events.EventOpenTelephone;
import me.zrageyh.telefono.events.EventUseSim;
import me.zrageyh.telefono.items.paginated.BackItem;
import me.zrageyh.telefono.items.paginated.ForwardItem;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.setting.SettingsMySQL;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ASCIIUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.Arrays;
import java.util.List;

@Getter
public final class Telefono extends SimplePlugin {

    public static final String GUI_TITLE_MAIN = "§0.";
    public static ItemBuilder BORDER;
    public static ItemStack itemFattura;
    public static ItemStack itemGps;
    public static ItemStack itemRubrica;
    public static ItemStack itemHistoryCalls;
    public static ItemStack itemHistoryMessages;
    public static ItemStack itemRemoveSim;
    public static ItemStack itemEmergency;
    public static ItemStack itemInvisible;
    public static ItemStack itemDiscord;
    public static ItemStack itemStore;
    public static ItemStack itemImmobiliare;
    public static ItemStack itemDarkChat;
    public static ItemStack itemTwitch;
    public static ItemStack itemReport;
    @Getter
    private static CacheAbbonamento cacheAbbonamento;
    @Getter
    private static CacheContatti cacheContatti;
    @Getter
    private static CacheNumeri cacheNumeri;
    @Getter
    private static HeadDatabaseAPI headDatabaseAPI;
    @Getter
    private static CacheChiamata cacheChiamata;
    @Getter
    private static CacheHistoryChiamate cacheHistoryChiamate;
    @Getter
    private static CacheHistoryMessaggi cacheHistoryMessaggi;

    @Override
    protected void onReloadablesStart() {
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::restoreInventory);
        initDatabase();
        Valid.checkBoolean(Common.doesPluginExist("ItemsAdder"), "ItemsAdder non trovato, controlla che sia inserito come plugin.");
        Valid.checkBoolean(Common.doesPluginExist("HeadDatabase"), "HeadDatabase non trovato, controlla che sia inserito come plugin.");
        Valid.checkBoolean(Common.doesPluginExist("RPGhuds"), "RPGhuds non trovato, controlla che sia inserito come plugin.");
    }

    @Override
    protected void onPluginPreReload() {
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::restoreInventory);
        Database.getInstance().close();
    }

    @Override
    protected void onPluginStart() {
        long startTime = System.currentTimeMillis();
        Common.log("————————————————————————————————————————————");
        ASCIIUtil.generate("Telefono", ASCIIUtil.SMALL, List.of("-")).forEach(Common::log);
        Common.log(" ");
        Messenger.ENABLED = false;
        headDatabaseAPI = new HeadDatabaseAPI();
        Common.runLater(20, this::loadGlobalIngredients);
        initDatabase();
        loadCache();
        registerEvents(new EventOpenTelephone(), new EventUseSim(), new EventInteractMainMenu());
        Common.log("Telefono v%s by %s avviato in %.2fms".formatted(getDescription().getVersion(), getDescription().getAuthors().get(0), (System.currentTimeMillis() - startTime) / 1000.0));
        Common.log("————————————————————————————————————————————");
    }

    private void initDatabase() {
        SettingsMySQL.init();
    }

    @Override
    protected void onPluginStop() {
        Bukkit.getOnlinePlayers().forEach(EventInteractMainMenu::restoreInventory);
        Database.getInstance().close();
    }


    private void loadCache() {
        cacheAbbonamento = new CacheAbbonamento();
        cacheContatti = new CacheContatti();
        cacheChiamata = new CacheChiamata();
        cacheHistoryChiamate = new CacheHistoryChiamate();
        cacheHistoryMessaggi = new CacheHistoryMessaggi();
        cacheNumeri = new CacheNumeri();
    }

    private void loadGlobalIngredients() {

        itemInvisible = new ItemBuilder(CustomStack.getInstance("iageneric:blue_ring").getItemStack())
                .setDisplayName("")
                .setLegacyLore(List.of(""))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,  ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers().get();

        itemDarkChat = buildItem("§f§lᴅᴀʀᴋ ᴄʜᴀᴛ", "§7§oIɴ ᴀʀʀɪᴠᴏ ᴄᴏɴ ɪʟ ᴘʀᴏssɪᴍᴏ ᴀɢɢɪᴏʀɴᴀᴍᴇɴᴛᴏ", "§7§oᴅᴇʟ sɪsᴛᴇᴍᴀ...");
        itemImmobiliare = buildItem("§f§lɪᴍᴍᴏʙɪʟɪᴀʀᴇ", "§7§oIɴ ᴀʀʀɪᴠᴏ ᴄᴏɴ ɪʟ ᴘʀᴏssɪᴍᴏ ᴀɢɢɪᴏʀɴᴀᴍᴇɴᴛᴏ", "§7§oᴅᴇʟ sɪsᴛᴇᴍᴀ...");
        itemTwitch = buildItem("§f§lᴛᴡɪᴛᴄʜ", "§7§oIɴ ᴀʀʀɪᴠᴏ ᴄᴏɴ ɪʟ ᴘʀᴏssɪᴍᴏ ᴀɢɢɪᴏʀɴᴀᴍᴇɴᴛᴏ", "§7§oᴅᴇʟ sɪsᴛᴇᴍᴀ...");


        itemReport = buildItem("§f§lʀᴇᴘᴏʀᴛ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ɪ §fᴛɪᴄᴋᴇᴛ", "§7ᴏ ᴄʜɪᴇᴅᴇʀᴇ §fᴀɪᴜᴛᴏ");

        itemFattura = buildItem("§f§lғᴀᴛᴛᴜʀᴇ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ʟᴇ ᴛᴜᴇ §fғᴀᴛᴛᴜʀᴇ", "§7ᴅᴀʟ ᴛᴇʟᴇғᴏɴᴏ");

        itemGps = buildItem("§f§lɢᴘs", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ɪʟ §fɴᴀᴠɪɢᴀᴛᴏʀᴇ", "§7ᴅᴀʟ ᴛᴇʟᴇғᴏɴᴏ");

        itemRubrica = buildItem("§f§lʀᴜʙʀɪᴄᴀ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴘʀɪʀᴇ ʟᴀ ʟɪsᴛᴀ", "§7ᴅᴇɪ ᴛᴜᴏɪ §fᴄᴏɴᴛᴀᴛᴛɪ");

        itemRemoveSim = buildItem("§f§lᴇsᴛʀᴀɪ sɪᴍ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ §fᴇsᴛʀᴀʀʀᴇ §7ʟᴀ sɪᴍ", "§7ᴅᴀʟ ᴛᴇʟᴇғᴏɴᴏ");

        itemHistoryMessages = buildItem("§f§lsᴍs", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ʟᴇ §fᴄʜᴀᴛ");

        itemHistoryCalls = buildItem("§f§lᴄʀᴏɴᴏʟᴏɢɪᴀ ᴄʜɪᴀᴍᴀᴛᴇ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ", "§7ʟᴀ §fᴄʀᴏɴᴏʟᴏɢɪᴀ ᴅᴇʟʟᴇ ᴄʜɪᴀᴍᴀᴛᴇ");

        itemEmergency = buildItem("§f§lᴇᴍᴇʀɢᴇɴᴢᴀ", "§7ᴜsᴀ ǫᴜᴇsᴛᴀ ᴀᴘᴘʟɪᴄᴀᴢɪᴏɴᴇ sᴏʟᴏ", "§7ɪɴ ᴄᴀsᴏ ᴅɪ §fᴇᴍᴇʀɢᴇɴᴢᴀ");

        itemDiscord = buildItem("§f§lᴅɪsᴄᴏʀᴅ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴇɴᴛʀᴀʀᴇ ɴᴇʟ ɴᴏsᴛʀᴏ", "§7sᴇʀᴠᴇʀ §fᴅɪsᴄᴏʀᴅ");

        itemStore = buildItem("§f§lsᴛᴏʀᴇ", "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀɴᴅᴀʀᴇ", "§7ɴᴇʟʟᴏ §Fsᴛᴏʀᴇ ᴜғғɪᴄɪᴀʟᴇ");


        BORDER = new ItemBuilder(CompMaterial.BLACK_STAINED_GLASS_PANE.getMaterial())
                .setDisplayName("")
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,  ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers()
                .clearLore();


        Structure.addGlobalIngredient('#', BORDER);
        Structure.addGlobalIngredient('<', new BackItem());
        Structure.addGlobalIngredient('>', new ForwardItem());

        Structure.addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL);
        Structure.addGlobalIngredient('y', Markers.CONTENT_LIST_SLOT_VERTICAL);


    }

    private void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }

    private ItemStack buildItem(String name, String... lore) {
        return new ItemBuilder(itemInvisible)
                .setDisplayName(name)
                .setLegacyLore(List.of(lore))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,  ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers().get();
    }

}
