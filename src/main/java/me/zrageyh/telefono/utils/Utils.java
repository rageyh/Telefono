package me.zrageyh.telefono.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.remain.nbt.NBTItem;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

import java.text.SimpleDateFormat;

public class Utils {

    public static String getNBTTag(ItemStack itemStack, String tag) {
        NBTItem nbtItem = new NBTItem(itemStack);
        return (nbtItem.hasTag(tag)) ? nbtItem.getString(tag) : null;
    }

    public static boolean hasNBTTag(ItemStack itemStack, String tag) {
        NBTItem nbtItem = new NBTItem(itemStack);
        return nbtItem.hasTag(tag);
    }

    public static ItemStack setNBTTag(ItemStack itemStack, String tag, String value) {
        NBTItem nbtItem = new NBTItem(itemStack);
        nbtItem.setString(tag, value);
        return nbtItem.getItem();
    }

    public static void openGui(Gui gui, Player player, String title) {
        Window.single()
                .setGui(gui)
                .setViewer(player)
                .setTitle(title)
                .open(player);
    }

    public static Location toLocation(String location) {
        String[] parts = location.split(",");
        return new Location(Bukkit.getWorld("world"), Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    public static String getDateNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.ITALY);
        return dateFormat.format(new java.util.Date());
    }

    public static String toCallFormat(String sim, String number) {
        return "%s,%s".formatted(sim, number);
    }


}
