package me.zrageyh.telefono.events;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.inventories.InventoryCall;
import me.zrageyh.telefono.inventories.InventoryTelephoneShulker;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.collection.SerializedMap;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.zrageyh.telefono.Telefono.*;

public class EventOpenTelephone implements Listener {
    public static final SerializedMap serializedMap = new SerializedMap();
    private static final int COOLDOWN_SECONDS = 2;
    private static final int MAX_USES_PER_MINUTE = 20;
    private static final Set<Integer> VALID_TELEPHONE_SLOTS = Set.of(0, 8);

    // Rate limiting con Caffeine invece di Map custom
    private final Cache<UUID, Long> cooldownCache = Caffeine.newBuilder()
            .expireAfterWrite(COOLDOWN_SECONDS + 1, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();

    private final Cache<UUID, Integer> usageCountCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTelephoneInteract(final PlayerInteractEvent event) {
        if (!isValidInteraction(event)) return;

        final Player p = event.getPlayer();
        final ItemStack item = event.getItem();

        event.setCancelled(true);

        if (!TelephoneAPI.hasNumber(item)) {
            sendNoSimMessage(p);
            return;
        }

        final String sim = Utils.getNBTTag(item, "telephone_number");

        // Validazione numero SIM
        if (!isValidSimNumber(sim)) {
            Messenger.error(p, "&cNumero SIM non valido!");
            return;
        }

        // Handle chiamata in corso
        if (getCacheChiamata().containsNumber(sim)) {
            handleOngoingCall(p, sim);
            return;
        }

        // Validazione slot telefono
        if (!isValidTelephoneSlot(p)) {
            return;
        }

        // Apri interfaccia telefono
        openTelephoneInterface(p, sim);
    }

    private boolean isValidInteraction(final PlayerInteractEvent event) {
        return event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK
                && event.getItem() != null
                && event.getHand() == EquipmentSlot.HAND
                && TelephoneAPI.isTelephone(event.getItem());
    }

    /* Validazione numero SIM */
    private boolean isValidSimNumber(final String sim) {
        if (sim == null || sim.trim().isEmpty()) {
            return false;
        }

        // Whitelist caratteri permessi per SIM
        return sim.matches("^[0-9]{6,8}$");
    }

    private void sendNoSimMessage(final Player p) {
        p.sendMessage(" ");
        p.sendMessage("§9§l ɢ-ᴍᴏʙɪʟᴇ");
        p.sendMessage("§7 ᴘᴇʀ ᴘᴏᴛᴇʀ ᴜᴛɪʟɪᴢᴢᴀʀᴇ ɪʟ ᴛᴇʟᴇғᴏɴᴏ, è ɴᴇᴄᴇssᴀʀɪᴏ");
        p.sendMessage("§7 ᴀᴠᴇʀᴇ ᴜɴᴀ §fsɪᴍ §7ɪɴsᴛᴀʟʟᴀᴛᴀ ɴᴇʟ ᴛᴜᴏ ᴛᴇʟᴇғᴏɴᴏ");
        p.sendMessage("§7 ");
    }

    private void handleOngoingCall(final Player p, final String sim) {
        getCacheChiamata().getData(sim).ifPresent(call ->
                new InventoryCall(call).open(p));
    }

    private boolean isValidTelephoneSlot(final Player p) {
        final int slot = p.getInventory().getHeldItemSlot();
        if (!VALID_TELEPHONE_SLOTS.contains(slot)) {
            switchTelephoneSlot(p);
            Messenger.error(p, "&cDevi avere il telefono nel primo o ultimo slot per aprirlo!");
            return false;
        }
        return true;
    }

    /* Rate limiting avanzato con exponential backoff */
    private boolean canUsePhone(final Player p) {
        final UUID uuid = p.getUniqueId();
        final long currentTime = System.currentTimeMillis();

        // Controllo cooldown base
        final Long lastUse = cooldownCache.getIfPresent(uuid);
        if (lastUse != null && currentTime - lastUse < TimeUnit.SECONDS.toMillis(COOLDOWN_SECONDS)) {
            final long remainingTime = TimeUnit.SECONDS.toMillis(COOLDOWN_SECONDS) - (currentTime - lastUse);
            Messenger.error(p, "&cAspetta " + (remainingTime / 1000) + " secondi prima di aprire di nuovo il telefono!");
            return false;
        }

        // Rate limiting anti-spam
        final Integer currentUsage = usageCountCache.getIfPresent(uuid);
        final int newUsage = (currentUsage != null ? currentUsage : 0) + 1;

        if (newUsage > MAX_USES_PER_MINUTE) {
            Messenger.error(p, "&cTroppi tentativi! Riprova tra un minuto.");
            // Exponential backoff - aumenta il cooldown per spam
            cooldownCache.put(uuid, currentTime + TimeUnit.SECONDS.toMillis(COOLDOWN_SECONDS * 3));
            return false;
        }

        cooldownCache.put(uuid, currentTime);
        usageCountCache.put(uuid, newUsage);
        return true;
    }

    private void openTelephoneInterface(final Player p, final String sim) {
        serializedMap.put(p.getUniqueId().toString(), p.getInventory().getContents());

        Common.runLater(() -> {
            setupTelephoneInventory(p);
            new InventoryTelephoneShulker().openInventory(sim, p);
        });
    }

    private void switchTelephoneSlot(final Player p) {
        final ItemStack currentItem = p.getInventory().getItemInMainHand();
        final ItemStack firstSlotItem = p.getInventory().getItem(0);

        p.getInventory().setItem(0, currentItem);
        p.getInventory().setItemInMainHand(firstSlotItem != null ? firstSlotItem : new ItemStack(Material.AIR));
    }

    private void setupTelephoneInventory(final Player p) {
        final ItemStack mainHand = p.getInventory().getItemInMainHand();
        p.getInventory().clear();
        p.getInventory().setItemInMainHand(mainHand);

        setupHotbarItems(p);
        setupMainInventoryItems(p);
    }

    private void setupHotbarItems(final Player p) {
        p.getInventory().setItem(3, getServiceManager().getItemManager().getItemHistoryCalls());
        p.getInventory().setItem(4, getServiceManager().getItemManager().getItemRubrica());
        p.getInventory().setItem(5, getServiceManager().getItemManager().getItemHistoryMessages());
    }

    private void setupMainInventoryItems(final Player p) {
        p.getInventory().setItem(12, getServiceManager().getItemManager().getItemDiscord());
        p.getInventory().setItem(13, getServiceManager().getItemManager().getItemImmobiliare());
        p.getInventory().setItem(14, getServiceManager().getItemManager().getItemStore());
        p.getInventory().setItem(21, getServiceManager().getItemManager().getItemRemoveSim());
        p.getInventory().setItem(22, getServiceManager().getItemManager().getItemGps());
        p.getInventory().setItem(23, createSuoneriaItem(p));
    }

    private ItemStack createSuoneriaItem(final Player p) {
        final List<String> lore = p.hasPermission("telefono.vip.suoneria")
                ? List.of("§7Cʟɪᴄᴄᴀ ᴘᴇʀ ᴄᴀᴍʙɪᴀʀᴇ", "§7ʟᴀ §fsᴜᴏɴᴇʀɪᴀ §7ᴅᴇʟ ᴛᴜᴏ ᴛᴇʟᴇғᴏɴᴏ")
                : getSuoneriaDefaultLore();

        return new ItemBuilder(getServiceManager().getItemManager().getItemInvisible())
                .setDisplayName("§f§lsᴜᴏɴᴇʀɪᴀ")
                .setLegacyLore(lore)
                .setItemFlags(List.of(ItemFlag.values()))
                .clearEnchantments()
                .clearModifiers()
                .get();
    }

    private List<String> getSuoneriaDefaultLore() {
        return Arrays.asList(
                "§7Cʟɪᴄᴄᴀ ᴘᴇʀ ᴄᴀᴍʙɪᴀʀᴇ",
                "§7ʟᴀ §fsᴜᴏɴᴇʀɪᴀ §7ᴅᴇʟ ᴛᴜᴏ ᴛᴇʟᴇғᴏɴᴏ",
                " ",
                "§x§C§B§2§D§3§ED§x§C§F§3§0§3§Eᴇ§x§D§2§3§2§3§Dᴠ§x§D§6§3§5§3§Dɪ §x§D§D§3§A§3§Cᴀ§x§E§1§3§D§3§Cᴠ§x§E§4§3§F§3§Bᴇ§x§E§8§4§2§3§Bʀ§x§E§B§4§4§3§Aᴇ §x§E§F§4§7§3§Aɪ§x§E§F§4§7§3§Aʟ §x§F§3§9§0§4§F§lᴠ§x§D§F§8§7§5§3§lɪ§x§C§A§7§F§5§7§lᴘ §x§A§1§6§E§5§E§lʙ§x§8§D§6§5§6§2§lʀ§x§7§8§5§D§6§6§lᴏ§x§6§4§5§4§6§9§lɴ§x§4§F§4§C§6§D§lᴢ§x§3§B§4§3§7§1§lᴇ",
                "§x§C§B§2§D§3§Eᴘ§x§C§C§2§E§3§Eᴇ§x§C§D§2§F§3§Eʀ §x§C§F§3§0§3§Eᴘ§x§D§0§3§1§3§Dᴏ§x§D§1§3§2§3§Dᴛ§x§D§2§3§2§3§Dᴇ§x§D§3§3§3§3§Dʀ §x§D§6§3§5§3§Dᴜ§x§D§7§3§5§3§Ds§x§D§8§3§6§3§Dᴀ§x§D§9§3§7§3§Cʀ§x§D§A§3§8§3§Cᴇ §x§D§C§3§9§3§Cǫ§x§D§D§3§A§3§Cᴜ§x§D§E§3§B§3§Cᴇ§x§D§F§3§C§3§Cs§x§E§0§3§C§3§Cᴛ§x§E§1§3§D§3§Cᴀ §x§E§3§3§F§3§Bᴀ§x§E§4§3§F§3§Bᴘ§x§E§5§4§0§3§Bᴘ§x§E§7§4§1§3§Bʟ§x§E§8§4§2§3§Bɪ§x§E§9§4§2§3§Bᴄ§x§E§A§4§3§3§Bᴀ§x§E§B§4§4§3§Aᴢ§x§E§C§4§5§3§Aɪ§x§E§D§4§5§3§Aᴏ§x§E§E§4§6§3§Aɴ§x§E§F§4§7§3§Aᴇ"
        );
    }

    /* Cleanup cache al logout */
    public void cleanupPlayer(final UUID uuid) {
        cooldownCache.invalidate(uuid);
        usageCountCache.invalidate(uuid);
    }
}