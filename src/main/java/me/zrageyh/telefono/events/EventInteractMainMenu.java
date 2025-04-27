package me.zrageyh.telefono.events;

import dev.lone.itemsadder.api.CustomStack;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.inventories.InventoryEmergency;
import me.zrageyh.telefono.inventories.InventoryGpsList;
import me.zrageyh.telefono.inventories.InventoryMessaggi;
import me.zrageyh.telefono.inventories.InventoryRubrica;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.model.history.Cronologia;
import me.zrageyh.telefono.model.history.HistoryChiamata;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static me.zrageyh.telefono.Telefono.GUI_TITLE_MAIN;


public class EventInteractMainMenu implements Listener {


    private static final Map<Integer, MenuAction> MENU_ACTIONS_UP = new HashMap<>();
    private static final Map<Integer, MenuAction> MENU_ACTIONS_DOWN = new HashMap<>();

    static {
        initializeMenuActions();
    }

    private static void initializeMenuActions() {
        // Top inventory actions
        MENU_ACTIONS_UP.put(21, (player, sim) -> new InventoryEmergency().open(player));
        MENU_ACTIONS_UP.put(22, (player, sim) -> Common.dispatchCommand(Bukkit.getConsoleSender(), "fatture " + player.getName()));
        MENU_ACTIONS_UP.put(23, (player, sim) -> {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "ticket");
        });

        // Bottom inventory actions
        MENU_ACTIONS_DOWN.put(3, EventInteractMainMenu::handleCallHistory);
        MENU_ACTIONS_DOWN.put(4, EventInteractMainMenu::handleContacts);
        MENU_ACTIONS_DOWN.put(5, EventInteractMainMenu::handleMessages);
        MENU_ACTIONS_DOWN.put(12, EventInteractMainMenu::handleDiscord);
        MENU_ACTIONS_DOWN.put(14, EventInteractMainMenu::handleStore);
        MENU_ACTIONS_DOWN.put(21, EventInteractMainMenu::handleSimRemoval);
        MENU_ACTIONS_DOWN.put(22, (player, sim) -> new InventoryGpsList().open(player));
    }

    public static void restoreInventory(final Player player) {
        final String uuid = player.getUniqueId().toString();
        if (!EventOpenTelephone.serializedMap.containsKey(uuid)) {
            return;
        }
        final Object contents = EventOpenTelephone.serializedMap.getObject(uuid);
        if (contents != null) {
            player.getInventory().setContents((ItemStack[]) contents);
            EventOpenTelephone.serializedMap.remove(uuid);
        }
    }

    private static void handleCallHistory(final Player player, final String sim) {
        final CompletableFuture<Optional<List<HistoryChiamata>>> historyFuture = Telefono.getCacheHistoryChiamate().get(sim);

        historyFuture.thenAccept(opt_chiamate -> {
            if (opt_chiamate.isEmpty() || opt_chiamate.get().isEmpty()) {
                Messenger.error(player, "&cNon ci sono chiamate da mostrare");
                player.closeInventory();
                return;
            }

            final List<SimpleComponent> formattedCalls = opt_chiamate.get().stream()
                    .sorted(Comparator.comparing(Cronologia::getDate).reversed())
                    .map(call -> SimpleComponent.of(call.isLost() ? call.getTextFormatLost() : call.getTextFormat()))
                    .toList();

            final ChatPaginator paginator = createCallHistoryPaginator(formattedCalls);
            paginator.send(player);
            player.closeInventory();
        });
    }

    private static ChatPaginator createCallHistoryPaginator(final List<SimpleComponent> calls) {
        final ChatPaginator paginator = new ChatPaginator(7);
        paginator.setFoundationHeader("§x§2§9§F§B§0§8§lᴄ§x§3§1§F§B§1§1§lʀ§x§3§8§F§B§1§A§lᴏ§x§4§0§F§B§2§4§lɴ§x§4§7§F§B§2§D§lᴏ§x§4§F§F§C§3§6§lʟ§x§5§6§F§C§3§F§lᴏ§x§5§E§F§C§4§8§lɢ§x§6§5§F§C§5§1§lɪ§x§6§D§F§C§5§B§lᴀ §x§7§C§F§C§6§D§lᴄ§x§8§4§F§C§7§6§lʜ§x§8§B§F§C§7§F§lɪ§x§9§3§F§D§8§8§lᴀ§x§9§A§F§D§9§2§lᴍ§x§A§2§F§D§9§B§lᴀ§x§A§9§F§D§A§4§lᴛ§x§B§1§F§D§A§D§lᴇ§6");
        paginator.setHeader(
                "§7[§a✔§7] = ᴄʜɪᴀᴍᴀᴛᴀ ʀɪsᴘᴏsᴛᴀ",
                "§7[§c❌§7] = ᴄʜɪᴀᴍᴀᴛᴀ ᴘᴇʀsᴀ",
                "§7[§e←§7] = ᴄʜɪᴀᴍᴀᴛᴀ ʀɪᴄᴇᴠᴜᴛᴀ",
                "§7[§e→§7] = ᴄʜɪᴀᴍᴀᴛᴀ ᴇғғᴇᴛᴛᴜᴀᴛᴀ",
                " "
        );
        paginator.setPages(calls);
        return paginator;
    }

    private static void handleContacts(final Player player, final String sim) {
        if (!Telefono.getCacheContatti().getCache().asMap().containsKey(player.getUniqueId().toString())) {
            Common.runAsync(() -> {
                List<Contatto> contatti = Database.getInstance().getContattiBySim(sim);
                if (contatti == null) contatti = new ArrayList<>();
                Telefono.getCacheContatti().getCache().put(player.getUniqueId().toString(), contatti);
            });
        }
        new InventoryRubrica(player, sim).open(player);
    }

    private static void handleMessages(final Player player, final String sim) {
        final Optional<List<Contatto>> contacts = Telefono.getCacheContatti().get(sim);
        if (contacts.isEmpty()) {
            Messenger.error(player, "&cNon ci sono messaggi da mostrare");
            player.closeInventory();
            return;
        }
        new InventoryMessaggi(contacts.get(), sim).open(player);
    }

    private static void handleDiscord(final Player player, final String sim) {
        sendDiscordMessage(player);
    }

    private static void handleStore(final Player player, final String sim) {
        sendStoreMessage(player);
    }

    private static void handleSimRemoval(final Player player, final String sim) {
        removeSim(player, sim);
    }

    private static void sendDiscordMessage(final Player player) {
        player.closeInventory();
        player.sendMessage(" ");
        player.sendMessage(" §x§0§8§4§C§F§B§lᴅ§x§2§4§6§8§F§B§lɪ§x§3§F§8§4§F§C§ls§x§5§B§A§0§F§C§lᴄ§x§7§6§B§B§F§C§lᴏ§x§9§2§D§7§F§D§lʀ§x§A§D§F§3§F§D§lᴅ");
        player.sendMessage("§7 ʜᴀɪ ʙɪsᴏɢɴᴏ ᴅɪ §fᴀssɪsᴛᴇɴᴢᴀ §7ᴏ ᴠᴜᴏɪ ʀɪᴍᴀɴᴇʀᴇ §fᴀɢɢɪᴏʀɴᴀᴛᴏ§7?");
        player.sendMessage("§7 ᴇɴᴛʀᴀ sᴜʙɪᴛᴏ ɴᴇʟ ɴᴏsᴛʀᴏ §fᴅɪsᴄᴏʀᴅ ᴜғғɪᴄɪᴀʟᴇ§7:");
        SimpleComponent.of("§f  ➥ [§x§0§8§4§C§F§Bᴅ§x§2§4§6§8§F§Bɪ§x§3§F§8§4§F§Cs§x§5§B§A§0§F§Cᴄ§x§7§6§B§B§F§Cᴏ§x§9§2§D§7§F§Dʀ§x§A§D§F§3§F§Dᴅ§f]")
                .onClickOpenUrl("https://discord.gg/J5Jqa6yExY")
                .onHover("§f(ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴇɴᴛʀᴀʀᴇ ɴᴇʟ ɴᴏsᴛʀᴏ ᴅɪsᴄᴏʀᴅ)")
                .send(player);
        player.sendMessage(" ");
    }

    private static void sendStoreMessage(final Player player) {
        player.closeInventory();
        player.sendMessage(" ");
        player.sendMessage(" §x§F§3§9§0§4§F§ls§x§C§5§7§D§5§8§lᴛ§x§9§7§6§A§6§0§lᴏ§x§6§9§5§6§6§9§lʀ§x§3§B§4§3§7§1§lᴇ");
        player.sendMessage("§7 ᴠᴜᴏɪ §fᴍɪɢʟɪᴏʀᴀʀᴇ §7ʟᴀ ᴛᴜᴀ ᴇsᴘᴇʀɪᴇɴᴢᴀ ᴇ ᴄᴏɴᴛʀɪʙᴜɪʀᴇ ᴀʟ ᴍɪɢʟɪᴏʀᴀᴍᴇɴᴛᴏ ᴅᴇʟ sᴇʀᴠᴇʀ?");
        player.sendMessage("§7 §fᴀᴄǫᴜɪsᴛᴀ §7ᴜɴ ᴠɪᴘ ɴᴇʟ ɴᴏsᴛʀᴏ sᴛᴏʀᴇ ᴜғғɪᴄɪᴀʟᴇ§7:");
        SimpleComponent.of("§f  ➥ [§x§F§3§9§0§4§F§ns§x§C§5§7§D§5§8§nᴛ§x§9§7§6§A§6§0§nᴏ§x§6§9§5§6§6§9§nʀ§x§3§B§4§3§7§1§nᴇ§f]")
                .onClickOpenUrl("https://store.greenfieldrp.it")
                .onHover("§f(ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀɴᴅᴀʀᴇ ɴᴇʟʟᴏ sᴛᴏʀᴇ)")
                .send(player);
        player.sendMessage(" ");
    }

    private static void removeSim(final Player player, final String sim) {
        player.closeInventory();
        final ItemStack simItem = new ItemBuilder(CustomStack.getInstance("iageneric:sim").getItemStack())
                .setDisplayName("§fSIM")
                .setLegacyLore(Arrays.asList(" ", "§7Numero: §f%number%", "§o§8Utilizzala su un telefono"))
                .clearEnchantments()
                .clearItemFlags()
                .clearModifiers()
                .get();

        Common.runLater(() -> {
            final ItemStack simNumber = TelephoneAPI.setSimNumber(simItem, sim);
            player.getInventory().addItem(simNumber);

            final ItemStack hand = player.getInventory().getItemInMainHand();
            final ItemStack telephone = TelephoneAPI.setTelephoneNumber(hand, "nessuno");
            player.getInventory().setItemInMainHand(telephone);

            Messenger.success(player, "&aHai rimosso la sim dal tuo telefono");
        });
    }

    @EventHandler
    public void onClose(final InventoryCloseEvent e) {
        restoreInventory((Player) e.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        restoreInventory(e.getPlayer());
    }

    @EventHandler
    public void onClick(final InventoryClickEvent e) {
        if (!e.getView().getTitle().equalsIgnoreCase(GUI_TITLE_MAIN)) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || e.getClickedInventory() == null) {
            return;
        }

        final Player player = (Player) e.getWhoClicked();
        final String sim = TelephoneAPI.getTelephoneNumber(player.getInventory().getItemInMainHand());

        final Inventory bottomInventory = e.getView().getBottomInventory();
        final MenuAction action = e.getClickedInventory().equals(bottomInventory) ? MENU_ACTIONS_DOWN.get(e.getSlot()) : MENU_ACTIONS_UP.get(e.getSlot());
        if (action != null) {
            action.execute(player, sim);
        }
    }

    @FunctionalInterface
    private interface MenuAction {
        void execute(Player player, String sim);
    }
}
