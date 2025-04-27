package me.zrageyh.telefono.events;

import me.zrageyh.telefono.api.TelephoneAPI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;

public class EventUseSim implements Listener {

    @EventHandler
    public void onClick(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getAction().equals(Action.LEFT_CLICK_AIR)
                || event.getAction().equals(Action.LEFT_CLICK_BLOCK)
                || event.getItem() == null
                || event.getHand() != EquipmentSlot.HAND)
            return;

        final ItemStack item = event.getItem();


        if (!TelephoneAPI.isSim(item)) return;

        event.setCancelled(true);

        final ItemStack offhand = player.getInventory().getItemInOffHand();

        if (offhand == null || offhand.getType() == Material.AIR
                || !TelephoneAPI.isTelephone(offhand) || TelephoneAPI.hasNumber(offhand) || offhand.getAmount() != 1) {
            player.sendMessage(" ");
            player.sendMessage("§9§l ɢ-ᴍᴏʙɪʟᴇ");
            player.sendMessage("§7 ᴘᴇʀ ᴘᴏᴛᴇʀ ɪɴsᴇʀɪʀᴇ ʟᴀ sɪᴍ ɴᴇʟ ᴛᴇʟᴇғᴏɴᴏ, è ɴᴇᴄᴇssᴀʀɪᴏ");
            player.sendMessage("§7 ᴀᴠᴇʀᴇ ᴜɴ §fᴛᴇʟᴇғᴏɴᴏ sᴇɴᴢᴀ sɪᴍ §7ɪɴ §fsᴇᴄᴏɴᴅᴀ ᴍᴀɴᴏ");
            player.sendMessage("§7 ");
            return;
        }

        final String number = TelephoneAPI.getSimNumber(item);
        player.getInventory().addItem(TelephoneAPI.setTelephoneNumber(offhand, number));

        player.getInventory().setItemInOffHand(null);
        player.getInventory().setItemInMainHand(null);


        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        Messenger.success(player, "&aHai inserito la sim %s nel tuo telefono!".formatted(number));


        // TODO codice inserimento sim
    }
}
