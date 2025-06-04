package me.zrageyh.telefono.items;

import dev.lone.itemsadder.api.CustomStack;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.events.StartCall;
import me.zrageyh.telefono.model.Call;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.utils.Utils;
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

public class ItemCallAccept extends SimpleItem {

    private final Call call;

    public ItemCallAccept(final ItemStack is, final Call call) {
        super(is);
        this.call = call;
    }

    @Override
    public ItemProvider getItemProvider() {
        return new ItemBuilder(CustomStack.getInstance("mcicons:icon_confirm").getItemStack())
                .setDisplayName("§a§lᴀᴄᴄᴇᴛᴛᴀ ᴄʜɪᴀᴍᴀᴛᴀ")
                .setLegacyLore(List.of("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴄᴄᴇᴛᴛᴀʀᴇ ʟᴀ ᴄʜɪᴀᴍᴀᴛᴀ ɪɴ ᴀʀʀɪᴠᴏ"))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers();
    }

    @Override
    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {

        player.closeInventory();


        if (call == null || call.getContattoWhoCall().getPlayer().equals(player)) {
            Messenger.error(player, "&cNon hai chiamate in sospeso");
            return;
        }

        //E' IL CONTATTO CHE CHIAMA
        final Contatto contattoWhoCall = call.getContattoWhoCall();

        final String sim = contattoWhoCall.getSim();
        final String number = contattoWhoCall.getNumber();

        call.startCall();
        Telefono.getCacheChiamata().putData(Utils.toCallFormat(sim, number), call);

        final Player target = contattoWhoCall.getPlayer();

        new StartCall(player, call).start(player);
        new StartCall(target, call).start(target);


    }
}
