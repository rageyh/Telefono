package me.zrageyh.telefono.items.paginated;


import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemFlag;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.List;

public class ForwardItem extends PageItem {

    public ForwardItem() {
        super(true);
    }

    @Override
    public ItemProvider getItemProvider(final PagedGui<?> gui) {
        final ItemBuilder builder = new ItemBuilder(CustomStack.getInstance("mcicons:icon_next_white").getItemStack())
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_POTION_EFFECTS));
        builder.setDisplayName("§7ᴘʀᴏssɪᴍᴀ ᴘᴀɢɪɴᴀ")
                .addLoreLines(gui.hasNextPage()
                        ? "§7ᴠᴀɪ ᴀʟʟᴀ ᴘᴀɢɪɴᴀ §e" + (gui.getCurrentPage() + 2) + "§7/§e" + gui.getPageAmount()
                        : "§cɴᴇssᴜɴᴀ ᴘᴀɢɪɴᴀ ᴅɪsᴘᴏɴɪʙɪʟᴇ");

        return builder;
    }

}
