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
import me.zrageyh.telefono.utils.ValidationUtils;
import me.zrageyh.telefono.security.SecurityManager;
import me.zrageyh.telefono.security.RateLimiter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.concurrent.TimeUnit;

import static me.zrageyh.telefono.manager.ItemManager.GUI_TITLE_MAIN;


/**
 * ARCHITECTURE FIX: Convertito da static abuse a instance-based pattern
 * - Eliminati 12 metodi statici
 * - Dependency injection ready
 * - Foundation singleton pattern compliant
 */
public class EventInteractMainMenu implements Listener {

    // ARCHITECTURE FIX: Instance-based invece di static
    private final Map<Integer, MenuAction> menuActionsUp = new HashMap<>();
    private final Map<Integer, MenuAction> menuActionsDown = new HashMap<>();
    private final Telefono plugin;
    private final SecurityManager securityManager;
    
    // CRITICAL FIX: Click debouncing per prevenire duplicazioni
    private final Cache<UUID, Long> clickDebounceCache = Caffeine.newBuilder()
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .maximumSize(1000)
            .build();
    
    public EventInteractMainMenu() {
        this.plugin = Telefono.getInstance();
        this.securityManager = SecurityManager.getInstance();
        initializeMenuActions();
    }

    // ARCHITECTURE FIX: Instance methods invece di static
    private void initializeMenuActions() {
        // Top inventory actions
        menuActionsUp.put(21, (player, sim) -> new InventoryEmergency().open(player));
        menuActionsUp.put(22, (player, sim) -> Common.dispatchCommand(Bukkit.getConsoleSender(), "fatture " + player.getName()));
        menuActionsUp.put(23, (player, sim) -> {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "ticket");
        });

        // Bottom inventory actions - ARCHITECTURE FIX: Usa instance methods
        menuActionsDown.put(3, this::handleCallHistory);
        menuActionsDown.put(4, this::handleContacts);
        menuActionsDown.put(5, this::handleMessages);
        menuActionsDown.put(12, this::handleDiscord);
        menuActionsDown.put(14, this::handleStore);
        menuActionsDown.put(21, this::handleSimRemoval);
        menuActionsDown.put(22, (player, sim) -> new InventoryGpsList().open(player));
    }

    // ARCHITECTURE FIX: Static method for backward compatibility (for now)
    public static void restoreInventory(final Player player) {
        // CRITICAL FIX: Usa il delay intelligente nel PlayerDataManager
        Telefono.getServiceManager().getPlayerDataManager().restoreInventory(player);
    }
    
    // CRITICAL FIX: Metodo per restore forzato (quit/close definitivi)
    public static void forceRestoreInventory(final Player player) {
        Telefono.getServiceManager().getPlayerDataManager().forceRestoreInventory(player);
    }

    // ARCHITECTURE FIX: Instance method invece di static
    private void handleCallHistory(final Player player, final String sim) {
        if (!securityManager.validateAndAttemptAction(player, RateLimiter.ActionType.OPEN_HISTORY, sim, "sim")) {
            return;
        }
        
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

    private ChatPaginator createCallHistoryPaginator(final List<SimpleComponent> calls) {
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

    private void handleContacts(final Player player, final String sim) {
        if (!securityManager.validateAndAttemptAction(player, RateLimiter.ActionType.OPEN_CONTACTS, sim, "sim")) {
            player.closeInventory();
            return;
        }

        Telefono.getCacheContatti().get(sim).thenAcceptAsync(contatti -> {
            if (contatti.isEmpty()) {
                Common.runLater(() -> {
                    Messenger.error(player, "&cNessun contatto disponibile");
                    player.closeInventory();
                });
                return;
            }
            
            Common.runLater(() -> new InventoryRubrica(player, sim).open(player));
        }).exceptionally(throwable -> {
            Common.error(throwable, "Errore caricamento contatti per " + player.getName());
            Common.runLater(() -> {
                Messenger.error(player, "&cErrore caricamento contatti");
                player.closeInventory();
            });
            return null;
        });
    }

    private void handleMessages(final Player player, final String sim) {
        if (!securityManager.validateAndAttemptAction(player, RateLimiter.ActionType.OPEN_CONTACTS, sim, "sim")) {
            player.closeInventory();
            return;
        }
        
        Telefono.getCacheContatti().get(sim).thenAccept(contacts -> {
            Common.runLater(() -> {
                if (contacts.isEmpty()) {
                    Messenger.error(player, "&cNon ci sono messaggi da mostrare");
                    player.closeInventory();
                    return;
                }
                new InventoryMessaggi(contacts.get(), sim).open(player);
            });
        });
    }

    private void handleDiscord(final Player player, final String sim) {
        sendDiscordMessage(player);
    }

    private void handleStore(final Player player, final String sim) {
        sendStoreMessage(player);
    }

    private void handleSimRemoval(final Player player, final String sim) {
        removeSim(player, sim);
    }

    private void sendDiscordMessage(final Player player) {
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

    private void sendStoreMessage(final Player player) {
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

    private void removeSim(final Player player, final String sim) {
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
        final Player player = (Player) e.getPlayer();
        
        // CRITICAL FIX: Controlla se il giocatore ha davvero l'interfaccia telefono aperta
        // Verifica il titolo dell'inventario per distinguere diverse GUI
        final String title = e.getView().getTitle();
        
        // Se chiude l'interfaccia del telefono principale, fai restore
        if (title.equalsIgnoreCase(GUI_TITLE_MAIN)) {
            restoreInventory(player);
        } else {
            // Per altre GUI (che non sono del telefono), fai restore normale
            restoreInventory(player);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        forceRestoreInventory(player);
        securityManager.getRateLimiter().clearPlayer(uuid);
        clickDebounceCache.invalidate(uuid); // Cleanup debounce cache
    }

    @EventHandler
    public void onClick(final InventoryClickEvent e) {
        if (!e.getView().getTitle().equalsIgnoreCase(GUI_TITLE_MAIN)) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || e.getClickedInventory() == null) {
            return;
        }

        final Player player = (Player) e.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        final long currentTime = System.currentTimeMillis();
        
        // CRITICAL FIX: Click debouncing per prevenire duplicazioni
        final Long lastClick = clickDebounceCache.getIfPresent(uuid);
        if (lastClick != null && currentTime - lastClick < 300) {
            return; // Ignora click troppo rapidi
        }
        clickDebounceCache.put(uuid, currentTime);
        
        final String sim = TelephoneAPI.getTelephoneNumber(player.getInventory().getItemInMainHand());

        // ARCHITECTURE FIX: Usa instance-based maps invece di static
        final Inventory bottomInventory = e.getView().getBottomInventory();
        final MenuAction action = e.getClickedInventory().equals(bottomInventory) ? menuActionsDown.get(e.getSlot()) : menuActionsUp.get(e.getSlot());
        if (action != null) {
            action.execute(player, sim);
        }
    }

    @FunctionalInterface
    private interface MenuAction {
        void execute(Player player, String sim);
    }
}
