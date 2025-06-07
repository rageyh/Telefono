package me.zrageyh.telefono.inventories;

import lombok.AllArgsConstructor;
import me.zrageyh.telefono.items.ItemCallAmbulance;
import me.zrageyh.telefono.items.ItemCallPolice;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;

@AllArgsConstructor
public class InventoryEmergency implements InventoryImpl {


    @Override
    public Gui getInventory() {
        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . a . b . . #",
                        "# # # # # # # # #")
                .addIngredient('a', new ItemCallAmbulance(null))
                .addIngredient('b', new ItemCallPolice(null))
                .build();
    }

    @Override
    public void open(final Player player) {
        Utils.openGui(getInventory(), player, "ᴄʜɪᴀᴍᴀᴛᴀ ᴅɪ ᴇᴍᴇʀɢᴇɴᴢᴀ");
    }


}
