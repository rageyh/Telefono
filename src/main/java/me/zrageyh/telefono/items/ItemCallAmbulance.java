package me.zrageyh.telefono.items;


import me.zrageyh.telefono.Telefono;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.Collections;
import java.util.List;

public class ItemCallAmbulance extends SimpleItem {


    public ItemCallAmbulance(final ItemStack itemStack) {
        super(itemStack);
    }

    @Override
    public ItemProvider getItemProvider() {
        return new ItemBuilder(Telefono.getHeadDatabaseAPI().getItemHead("23265"))
                .setDisplayName("§c§lᴇᴍs")
                .setLegacyLore(List.of("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴄʜɪᴀᴍᴀʀᴇ ᴜɴ §fᴀᴍʙᴜʟᴀɴᴢᴀ", "§7ᴀʟʟᴀ ᴛᴜᴀ ᴘᴏsɪᴢɪᴏɴᴇ"))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers();
    }

    @Override
    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {

        new AnvilGUI.Builder()
                .title("ᴍᴏᴛɪᴠᴏ ᴄʜɪᴀᴍᴀᴛᴀ")
                .plugin(Telefono.getInstance())
                .itemLeft(Telefono.itemInvisible)
                .itemRight(Telefono.itemInvisible)
                .onClick((slot, state) -> {

                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    final String reason = state.getText().trim();
                 //TODO   CadaveriAPI.callAmbulance(player, reason);
                    return List.of(AnvilGUI.ResponseAction.close());
                }).open(player);


    }
}
