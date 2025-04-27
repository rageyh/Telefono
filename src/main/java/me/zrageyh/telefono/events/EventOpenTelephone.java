package me.zrageyh.telefono.events;

import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.inventories.InventoryCall;
import me.zrageyh.telefono.inventories.InventoryTelephoneShulker;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static me.zrageyh.telefono.Telefono.*;

public class EventOpenTelephone implements Listener {
    public static final SerializedMap serializedMap = new SerializedMap();
    private static final int COOLDOWN_SECONDS = 3;
    private static final Set<Integer> VALID_TELEPHONE_SLOTS = Set.of(0, 8);
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    @EventHandler
    public void onTelephoneInteract(final PlayerInteractEvent event) {
        if (!isValidInteraction(event)) return;

        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        event.setCancelled(true);

        if (!TelephoneAPI.hasNumber(item)) {
            sendNoSimMessage(player);
            return;
        }

        final String sim = Utils.getNBTTag(item, "telephone_number");

        // Handle ongoing call
        if (getCacheChiamata().containsNumber(sim)) {
            handleOngoingCall(player, sim);
            return;
        }

        // Validate telephone slot and cooldown
        if (!isValidTelephoneSlot(player) || !canUsePhone(player)) {
            return;
        }

        // Open telephone interface
        openTelephoneInterface(player, sim);
    }

    private boolean isValidInteraction(final PlayerInteractEvent event) {
        return event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK
                && event.getItem() != null
                && event.getHand() == EquipmentSlot.HAND
                && TelephoneAPI.isTelephone(event.getItem());
    }

    private void sendNoSimMessage(final Player player) {
        player.sendMessage(" ");
        player.sendMessage("§9§l ɢ-ᴍᴏʙɪʟᴇ");
        player.sendMessage("§7 ᴘᴇʀ ᴘᴏᴛᴇʀ ᴜᴛɪʟɪᴢᴢᴀʀᴇ ɪʟ ᴛᴇʟᴇғᴏɴᴏ, è ɴᴇᴄᴇssᴀʀɪᴏ");
        player.sendMessage("§7 ᴀᴠᴇʀᴇ ᴜɴᴀ §fsɪᴍ §7ɪɴsᴛᴀʟʟᴀᴛᴀ ɴᴇʟ ᴛᴜᴏ ᴛᴇʟᴇғᴏɴᴏ");
        player.sendMessage("§7 ");
    }

    private void handleOngoingCall(final Player player, final String sim) {
        getCacheChiamata().getData(sim).ifPresent(call ->
                new InventoryCall(call).open(player));
    }

    private boolean isValidTelephoneSlot(final Player player) {
        final int slot = player.getInventory().getHeldItemSlot();
        if (!VALID_TELEPHONE_SLOTS.contains(slot)) {
            switchTelephoneSlot(player);
            Messenger.error(player, "&cDevi avere il telefono nel primo o ultimo slot per aprirlo!");
            return false;
        }
        return true;
    }

    private boolean canUsePhone(final Player player) {
        final long currentTime = System.currentTimeMillis();
        final long lastUse = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastUse < TimeUnit.SECONDS.toMillis(COOLDOWN_SECONDS)) {
            Messenger.error(player, "&cAspetta prima di aprire di nuovo il tuo telefono!");
            return false;
        }

        playerCooldowns.put(player.getUniqueId(), currentTime);
        return true;
    }

    private void openTelephoneInterface(final Player player, final String sim) {
        serializedMap.put(player.getUniqueId().toString(), player.getInventory().getContents());

        Common.runLater(() -> {
            setupTelephoneInventory(player);
            new InventoryTelephoneShulker().openInventory(sim, player);
        });
    }

    private void switchTelephoneSlot(final Player player) {
        final ItemStack currentItem = player.getInventory().getItemInMainHand();
        final ItemStack firstSlotItem = player.getInventory().getItem(0);

        player.getInventory().setItem(0, currentItem);
        player.getInventory().setItemInMainHand(firstSlotItem != null ? firstSlotItem : new ItemStack(Material.AIR));
    }

    private void setupTelephoneInventory(final Player player) {
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        player.getInventory().clear();
        player.getInventory().setItemInMainHand(mainHand);

        setupHotbarItems(player);
        setupMainInventoryItems(player);
    }

    private void setupHotbarItems(final Player player) {
        player.getInventory().setItem(3, itemHistoryCalls);
        player.getInventory().setItem(4, itemRubrica);
        player.getInventory().setItem(5, itemHistoryMessages);
    }

    private void setupMainInventoryItems(final Player player) {
        player.getInventory().setItem(12, itemDiscord);
        player.getInventory().setItem(13, itemImmobiliare);
        player.getInventory().setItem(14, itemStore);
        player.getInventory().setItem(21, itemRemoveSim);
        player.getInventory().setItem(22, itemGps);
        player.getInventory().setItem(23, createSuoneriaItem(player));
    }

    private ItemStack createSuoneriaItem(final Player player) {
        final List<String> lore = player.hasPermission("telefono.vip.suoneria")
                ? List.of("§7Cʟɪᴄᴄᴀ ᴘᴇʀ ᴄᴀᴍʙɪᴀʀᴇ", "§7ʟᴀ §fsᴜᴏɴᴇʀɪᴀ §7ᴅᴇʟ ᴛᴜᴏ ᴛᴇʟᴇғᴏɴᴏ")
                : getSuoneriaDefaultLore();

        return new ItemBuilder(itemInvisible)
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
}