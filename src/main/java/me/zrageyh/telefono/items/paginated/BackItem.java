package me.zrageyh.telefono.items.paginated;


import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemFlag;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.List;

public class BackItem extends PageItem {

    public BackItem() {
        super(false);
    }

    @Override
    public ItemProvider getItemProvider(final PagedGui<?> gui) {
        final ItemBuilder builder = new ItemBuilder(CustomStack.getInstance("mcicons:icon_back_white").getItemStack())
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE));
        builder.setDisplayName("§7ᴘᴀɢɪɴᴀ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ")
                .addLoreLines(gui.hasPreviousPage()
                        ? "§7ᴠᴀɪ ᴀʟʟᴀ ᴘᴀɢɪɴᴀ §e" + gui.getCurrentPage() + "§7/§e" + gui.getPageAmount()
                        : "§cɴᴇssᴜɴᴀ ᴘᴀɢɪɴᴀ ᴅɪsᴘᴏɴɪʙɪʟᴇ");

        return builder;
    }

}
