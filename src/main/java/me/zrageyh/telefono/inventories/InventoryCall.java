package me.zrageyh.telefono.inventories;

import lombok.AllArgsConstructor;
import me.zrageyh.telefono.items.ItemCallAccept;
import me.zrageyh.telefono.items.ItemCallEnd;
import me.zrageyh.telefono.model.Call;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;

@AllArgsConstructor
public class InventoryCall implements InventoryImpl {

    private Call call;

    @Override
    public Gui getInventory() {
        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . a . b . . #",
                        "# . . . . . . . #",
                        "# # # # # # # # #")
                .addIngredient('a', new ItemCallAccept(null, call))
                .addIngredient('b', new ItemCallEnd(null, call))
                .build();
    }

    @Override
    public void open(Player player) {
        Utils.openGui(getInventory(), player, "ɢᴇsᴛɪᴏɴᴇ ᴄʜɪᴀᴍᴀᴛᴀ");
    }
}
