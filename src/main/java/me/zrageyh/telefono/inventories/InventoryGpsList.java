package me.zrageyh.telefono.inventories;


import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.model.GpsHead;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.nbt.NBTItem;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryGpsList implements InventoryImpl {

    private static List<ItemBuilder> cachedHeads = null;
    private final HeadDatabaseAPI headDatabaseAPI = Telefono.getHeadDatabaseAPI();

    private ItemBuilder getHead(final GpsHead objGpsHead) {
        return new ItemBuilder(CompMetadata.setMetadata(headDatabaseAPI.getItemHead(objGpsHead.getId()), "telefono_gps", objGpsHead.getLocation()))
                .setDisplayName(objGpsHead.getName())
                .setLegacyLore(List.of("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴀᴠᴠɪᴀʀᴇ ɪʟ ɢᴘs ᴘᴇʀ §e" + objGpsHead.getName()))
                .clearEnchantments()
                .clearModifiers()
                .clearItemFlags();
    }


    private List<ItemBuilder> initializeHeads() {
        return new ArrayList<>() {{
            add(getHead(new GpsHead("§eᴀʀᴍᴇʀɪᴀ", "14482", "-58,69,-5")));
            add(getHead(new GpsHead("§eʙᴀɴᴄᴀ", "3005", "76,79,327")));
            add(getHead(new GpsHead("§eᴄᴀsɪɴᴏ", "23852", "381,70,-405")));
            add(getHead(new GpsHead("ᴄᴏɴᴄᴇssᴏɴᴀʀɪᴀ", "45000", "-20,67,462")));
            add(getHead(new GpsHead("§eᴄᴇɴᴛʀᴀʟᴇ ᴘᴏʟɪᴢɪᴀ", "24214", "111,67,171")));
            add(getHead(new GpsHead("§eᴄʜɪᴇsᴀ", "51293", "273,67,-3")));
            add(getHead(new GpsHead("§eɢᴀʀᴀɢᴇ ᴄᴇɴᴛʀᴀʟᴇ", "48775", "24,66,-40")));
            add(getHead(new GpsHead("§eɢᴏᴠᴇʀɴᴏ", "12040", "98,67,-99")));
            add(getHead(new GpsHead("§eɪᴍᴘᴏʀᴛ&ᴇxᴘᴏʀᴛ", "21492", "369,67,132")));
            add(getHead(new GpsHead("§eʟᴀᴠᴏʀɪ ʙᴀsᴇ", "87924", "-623,67,30")));
            add(getHead(new GpsHead("§eᴍᴇᴄᴄᴀɴɪᴄᴏ", "50985", "-213,67, 55")));
            add(getHead(new GpsHead("§eᴍᴜɴɪᴄɪᴘɪᴏ", "78754", "36,70,165")));
            add(getHead(new GpsHead("§eᴏsᴘᴇᴅᴀʟᴇ", "35114", "137,67,-11")));
            add(getHead(new GpsHead("§esᴄᴇʀɪғғᴀᴛᴏ", "24214", "-40,67,165")));
            add(getHead(new GpsHead("§esᴄᴜᴏʟᴀ", "4104", "-176,72,167")));
            add(getHead(new GpsHead("§esᴛᴀʀʙᴜᴄᴋs", "12040", "290,67,66")));
            add(getHead(new GpsHead("§eᴛᴀʙᴀᴄᴄʜᴇʀɪᴀ", "14482", "-207,67,272")));

        }};
    }

    public List<ItemBuilder> heads() {
        if (cachedHeads == null) {
            cachedHeads = initializeHeads();
        }
        return cachedHeads;
    }

    @Override
    public Gui getInventory() {
        final List<Item> items = heads().stream()
                .map(s -> new SimpleItem(s) {
                    @Override
                    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {
                        final NBTItem nbtItem = new NBTItem(event.getCurrentItem());
                        final Location location = Utils.toLocation(nbtItem.getString("telefono_gps"));
                        startGps(player, location);
                        player.sendMessage("§aGps avviato, segui la bussola per arrivare a destinazione");
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                        player.closeInventory();
                    }
                })
                .collect(Collectors.toList());

        return PagedGui.items()
                .setStructure(
                        "# # # < # > # # #",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "# # # # # # # # #")
                .setContent(items)
                .build();
    }

    @Override
    public void open(final Player player) {
        Utils.openGui(getInventory(), player, "ʟɪsᴛᴀ ᴅᴇsᴛɪɴᴀᴢɪᴏɴɪ");
    }

    private void startGps(final Player player, final Location location) {
       /* CompassHud.Destination destination = new CompassHud.Destination(location);
        RPGHuds rpgHuds = RPGHuds.inst();

        Hud<?> compassHud = rpgHuds.getPlayerHud(player, "rpghuds:compass");
        CompassHud compassHud1 = (CompassHud) compassHud;
        compassHud1.setDestination(destination);*/
    }
}