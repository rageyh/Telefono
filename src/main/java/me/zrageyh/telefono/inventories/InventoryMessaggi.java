package me.zrageyh.telefono.inventories;

import lombok.AllArgsConstructor;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.model.history.Cronologia;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class InventoryMessaggi implements InventoryImpl {

    private final List<Contatto> contatti;
    private final String sim;


    @Override
    public Gui getInventory() {

        return PagedGui.items()
                .setStructure(
                        ". . . . . . . . .",
                        ". x x x x x x . >",
                        ". x x x x x x . .",
                        ". x x x x x x . <",
                        ". . . . . . . . .")
                .setContent(toItem())
                .build();
    }

    @Override
    public void open(final Player player) {
        Utils.openGui(getInventory(), player, "§f\uF808\uE899\uF80B\uF80A\uF809\uF805\uF80A\uF804");
    }

    public List<Item> toItem() {

        return contatti.stream()
                .sorted(Comparator.comparing(Contatto::getName))
                .map(contatto -> new SimpleItem(new ItemBuilder(Material.PAPER)
                        .setDisplayName("§f%s".formatted(contatto.getFullName()))
                        .setLegacyLore(List.of(
                                "§7ɴᴜᴍᴇʀᴏ: §f%s".formatted(contatto.getNumber()),
                                " ",
                                "§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴠɪsᴜᴀʟɪᴢᴢᴀʀᴇ ɪ ᴍᴇssᴀɢɢɪ"))) {
                    @Override
                    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {

                        player.closeInventory();

                        Telefono.getCacheHistoryMessaggi().getForNumber(sim, contatto.getNumber()).thenAcceptAsync(opt_historymessages -> {

                            if (opt_historymessages.isEmpty()) {
                                Messenger.error(player, "&cLa chat con %s è vuota".formatted(contatto.getFullName()));
                                return;
                            }

                            final List<SimpleComponent> messaggi = opt_historymessages.get().stream()
                                    .sorted(Comparator.comparing(Cronologia::getDate).reversed())
                                    .map((s) -> s.isLost() ? s.getTextFormatLost() : s.getTextFormat())
                                    .map(SimpleComponent::of)
                                    .toList();

                            if (messaggi.isEmpty()) {
                                Messenger.error(player, "&cLa chat con %s è vuota".formatted(contatto.getFullName()));
                                return;
                            }

                            final ChatPaginator c = new ChatPaginator(7);
                            c.setFoundationHeader("§x§2§9§F§B§0§8§lᴄ§x§3§C§F§B§2§0§lʜ§x§5§0§F§C§3§7§lᴀ§x§6§3§F§C§4§F§lᴛ §x§8§A§F§C§7§E§lᴄ§x§9§E§F§D§9§5§lᴏ§x§B§1§F§D§A§D§lɴ %s§6".formatted(contatto.getFullName()));
                            c.setHeader("§7[§a✔§7] = ᴍᴇssᴀɢɢɪᴏ ʟᴇᴛᴛᴏ", "§7[§c❌§7] = ᴍᴇssᴀɢɢɪᴏ ɴᴏɴ ʀɪᴄᴇᴠᴜᴛᴏ", "§7[§e←§7] = ᴍᴇssᴀɢɢɪᴏ ʀɪᴄᴇᴠᴜᴛᴏ", "§7[§e→§7] = ᴍᴇssᴀɢɢɪᴏ ɪɴᴠɪᴀᴛᴏ", " ");
                            c.setPages(messaggi);
                            c.send(player);
                        });

                    }
                }).collect(Collectors.toUnmodifiableList());

    }

}
