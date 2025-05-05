package me.zrageyh.telefono.inventories;


import me.zrageyh.telefono.model.Abbonamento;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.Common;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static me.zrageyh.telefono.Telefono.*;


public class InventoryTelephoneShulker implements Listener {


    public void openInventory(final String sim, Player player) {
        getCacheAbbonamento().get(sim).thenAccept(opt_abbonamento -> {
            final ItemStack abbonamento = new ItemBuilder(itemInvisible)
                    .setDisplayName("§f§lᴀʙʙᴏɴᴀᴍᴇɴᴛᴏ")
                    .setLegacyLore(getAbbonamentoLore(opt_abbonamento))
                    .clearEnchantments()
                    .clearItemFlags()
                    .clearModifiers()
                    .get();

            final ItemStack shulkerItem = new ItemStack(Material.PURPLE_SHULKER_BOX);
            final BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
            final ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

            final Inventory shulkerInv = shulker.getInventory();
            shulker.setCustomName(GUI_TITLE_MAIN);

            shulkerInv.setItem(12, abbonamento);

            shulkerInv.setItem(13, itemDarkChat);
            shulkerInv.setItem(14, itemTwitch);
            // slot 13: illegale non disponibile
            // slot 14: tiwtch non disponibile
            // slot
            shulkerInv.setItem(21, itemEmergency);
            shulkerInv.setItem(22, itemFattura);
            shulkerInv.setItem(23, itemReport);
            // slot 23: report non disponibile

            meta.setBlockState(shulker);
            shulkerItem.setItemMeta(meta);

            Common.runLater(() -> player.openInventory(shulkerInv));

        });


    }

    private @NotNull List<String> getAbbonamentoLore(Optional<Abbonamento> opt_abbonamento) {
        final List<String> lore;

        if (opt_abbonamento.isPresent()) {
            Abbonamento abbonamento = opt_abbonamento.get();
            lore = Arrays.asList(
                    "§7ᴛɪᴘᴏ: §f%s".formatted(abbonamento.getAbbonamento()),
                    "§7ᴍᴇssᴀɢɢɪ ʀɪᴍᴀɴᴇɴᴛɪ: §f%d".formatted(abbonamento.getMessages()),
                    "§7ᴍɪɴᴜᴛɪ ᴄʜɪᴀᴍᴀᴛᴀ ʀɪᴍᴀɴᴇɴᴛɪ: §f%d".formatted(abbonamento.getCalls()));
        } else {
            lore = Arrays.asList(
                    "§7ᴛɪᴘᴏ: §fɴᴇssᴜɴᴏ",
                    "§7ᴍᴇssᴀɢɢɪ ʀɪᴍᴀɴᴇɴᴛɪ: §f0",
                    "§7ᴍɪɴᴜᴛɪ ᴄʜɪᴀᴍᴀᴛᴀ ʀɪᴍᴀɴᴇɴᴛɪ: §f0");
        }
        return lore;
    }


}
