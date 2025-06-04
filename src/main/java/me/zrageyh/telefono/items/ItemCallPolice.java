package me.zrageyh.telefono.items;


import me.zrageyh.telefono.Telefono;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.model.SimpleComponent;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.Collections;
import java.util.List;

public class ItemCallPolice extends SimpleItem {


    public ItemCallPolice(ItemStack itemStack) {
        super(itemStack);
    }

    public static void callPolice(Player player, String reason) {
        player.sendMessage("§f ");
        player.sendMessage("§f §9§lGFPD");
        player.sendMessage("§f §7Hai §fcorrettamente §7inviato una richiesta di soccorso");
        player.sendMessage("§f §7Attendi l'arrivo delle §fforze dell'ordine.");
        player.sendMessage("§f ");
        Bukkit.getOnlinePlayers().stream()
                .filter((s) -> s.hasPermission("gfpd.notify.emergenza"))
                .filter((s) -> !s.equals(player)).forEach((target) -> {
                    target.sendMessage("§f ");
                    target.sendMessage("§f §9§lGFPD");
                    target.sendMessage("§7 Il cittadino §f%s §7ha richiesto l'intervento delle §fforze dell'ordine!".formatted(player.getName()));
               //     target.sendMessage("§7 Luogo: §f%s".formatted(Utils.getRegionByPlayer(player)));
                    target.sendMessage("§7 Segnalazione: §f%s".formatted(reason));
                    target.sendMessage("§7 X: §f" + player.getLocation().getX() + "§7, Y: §f" + player.getLocation().getY() + "§7, Z: §f" + player.getLocation().getZ());
                    SimpleComponent.of("§f §9[AVVIA GPS]").onHover("§7Clicca per avviare il gps").onClickRunCmd("/cadaveri startgps %d/%d/%d true".formatted((int) player.getLocation().getX(), (int) player.getLocation().getY(), (int) player.getLocation().getZ())).send(target);
                    target.sendMessage("§7 ");
                });
    }

    @Override
    public ItemProvider getItemProvider() {
        return new ItemBuilder(Telefono.getHeadDatabaseAPI().getItemHead("16669"))
                .setDisplayName("§9§lʟsᴘᴅ")
                .setLegacyLore(List.of("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ sᴇɢɴᴀʟᴀʀᴇ ᴜɴ'ᴇᴍᴇʀɢᴇɴᴢᴀ", "§7ᴀʟʟᴇ §fғᴏʀᴢᴇ ᴅᴇʟʟ'ᴏʀᴅɪɴᴇ"))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,  ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

        new AnvilGUI.Builder()
                .title("ᴍᴏᴛɪᴠᴏ ᴄʜɪᴀᴍᴀᴛᴀ")
                .plugin(Telefono.getInstance())
                .itemLeft(Telefono.itemInvisible)
                .itemRight(Telefono.itemInvisible)
                .onClick((slot, state) -> {

                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String reason = state.getText().trim();
                    callPolice(player, reason);
                    return List.of(AnvilGUI.ResponseAction.close());
                }).open(player);


    }
}
