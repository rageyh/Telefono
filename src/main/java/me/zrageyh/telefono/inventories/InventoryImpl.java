package me.zrageyh.telefono.inventories;

import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;

public interface InventoryImpl {

    Gui getInventory();

    void open(Player player);
}
