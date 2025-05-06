package me.zrageyh.telefono.items;

import dev.lone.itemsadder.api.CustomStack;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.model.Call;
import me.zrageyh.telefono.model.Contatto;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.Messenger;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.List;

public class ItemCallEnd extends SimpleItem {

    private final Call call;

    public ItemCallEnd(final ItemStack is, final Call call) {
        super(is);
        this.call = call;
    }

    @Override
    public ItemProvider getItemProvider() {
        //TODO METTERE ITEM IA icon_confirm
        return new ItemBuilder(CustomStack.getInstance("_iainternal:icon_cancel").getItemStack())
                .setDisplayName("§c§lʀɪғɪᴜᴛᴀ/ᴛᴇʀᴍɪɴᴀ ᴄʜɪᴀᴍᴀᴛᴀ")
                .setLegacyLore(List.of("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ʀɪғɪᴜᴛᴀʀᴇ ᴏ ᴛᴇʀᴍɪɴᴀᴛᴀ ᴜɴᴀ ᴄʜɪᴀᴍᴀᴛᴀ"))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers();
    }

    @Override
    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {

        player.closeInventory();


        if (call == null) {
            Messenger.error(player, "&cNon hai chiamate in sospeso");
            return;
        }

        final Contatto contattoWhoCall = call.getContattoWhoCall();

        Telefono.getCacheChiamata().removeData(contattoWhoCall.getSim());


    }
}
