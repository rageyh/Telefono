package me.zrageyh.telefono.inventories;

import lombok.AllArgsConstructor;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.events.SendMessage;
import me.zrageyh.telefono.items.ItemAddContatto;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Abbonamento;
import me.zrageyh.telefono.model.Call;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.model.history.HistoryChiamata;
import me.zrageyh.telefono.utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.Common;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class InventoryRubrica implements InventoryImpl {

    final Player player;
    final String sim;

    @Override
    public Gui getInventory() {

        final ItemStack item = player.getInventory().getItemInMainHand();
        final String sim = TelephoneAPI.getTelephoneNumber(item);

        final Optional<List<Contatto>> opt_contatti = Telefono.getCacheContatti().get(sim);


        final List<Contatto> contatti = opt_contatti.orElseGet(ArrayList::new);

        final List<Item> contattiItems = contatti.stream().sorted(Comparator.comparing(Contatto::getName)).map(contatto -> new SimpleItem(new ItemBuilder(Material.PAPER)
                .setDisplayName("§7ɴᴏᴍᴇ: §f%s".formatted(contatto.getFullName()))
                .setLegacyLore(List.of("§7ɴᴜᴍᴇʀᴏ: §f%s".formatted(contatto.getNumber()), " ", "§7ᴛᴀsᴛᴏ §fsɪɴɪsᴛʀᴏ §7ᴘᴇʀ ᴍᴀɴᴅᴀʀᴇ ᴜɴ §fᴍᴇssᴀɢɢɪᴏ", "§7ᴛᴀsᴛᴏ §fᴅᴇsᴛʀᴏ§r §7ᴘᴇʀ §fᴄʜɪᴀᴍᴀʀᴇ", "§7ᴛᴀsᴛᴏ §fᴄᴇɴᴛʀᴀʟᴇ§r §7ᴘᴇʀ §fʀɪɴᴏᴍɪɴᴀʀᴇ", "§7ᴛᴀsᴛᴏ §csʜɪғᴛ + ᴅᴇsᴛʀᴏ§r §7ᴘᴇʀ ᴇʟɪᴍɪɴᴀʀᴇ ɪʟ ᴄᴏɴᴛᴀᴛᴛᴏ"))) {
            @Override
            public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {

                event.setCancelled(true);


                switch (clickType) {
                    case MIDDLE -> {

                        final AnvilGUI.Builder surnameContatto = new AnvilGUI.Builder()
                                .title("ɴᴜᴏᴠᴏ ᴄᴏɢɴᴏᴍᴇ")
                                .plugin(Telefono.getInstance())
                                .itemLeft(Telefono.itemInvisible)
                                .itemRight(Telefono.itemInvisible)
                                .onClick((slot, state) -> {

                                    if (slot != AnvilGUI.Slot.OUTPUT) {
                                        return Collections.emptyList();
                                    }

                                    final String surname = state.getText().trim();
                                    contatto.setSurname(surname);
                                    Telefono.getCacheContatti().update(contatto);

                                    Database.getInstance().updateContatto(contatto);

                                    Common.tell(player, "&aHai rinominato con successo il contatto a %s %s".formatted(contatto.getName(), contatto.getSurname()));

                                    return List.of(AnvilGUI.ResponseAction.close());
                                });

                        new AnvilGUI.Builder().title("ɴᴜᴏᴠᴏ ɴᴏᴍᴇ")
                                .plugin(Telefono.getInstance())
                                .itemLeft(Telefono.itemInvisible)
                                .itemRight(Telefono.itemInvisible).onClick((slot, state) -> {

                                    if (slot != AnvilGUI.Slot.OUTPUT) {
                                        return Collections.emptyList();
                                    }

                                    final String name = state.getText().trim();
                                    contatto.setName(name);
                                    surnameContatto.open(player);

                                    return Collections.emptyList();
                                }).open(player);
                    }

                    case SHIFT_RIGHT -> {

                        player.closeInventory();
                        Telefono.getCacheContatti().remove(sim, contatto.getId());
                        Database.getInstance().deleteContatto(contatto.getId());
                        Common.tell(player, "&aHai rimosso con successo %s %s il contatto dalla tua rubrica".formatted(contatto.getName(), contatto.getSurname()));
                    }
                    case LEFT -> {

                        player.closeInventory();
                        Telefono.getCacheAbbonamento().get(sim).thenAccept(opt_abbonamento -> {
                            if (opt_abbonamento.isEmpty()) {
                                Common.tell(player, "&cNon hai abbastanza credito per mandare un messaggio a %s %s".formatted(contatto.getName(), contatto.getSurname()));
                                return;
                            }

                            final Abbonamento abbonamento = opt_abbonamento.get();
                            if (!abbonamento.hasCreditoToMessage()) {
                                Common.tell(player, "&cNon hai abbastanza credito per mandare un messaggio a %s %s".formatted(contatto.getName(), contatto.getSurname()));
                                return;
                            }

                            try {
                                new SendMessage(contatto).start(player);
                            } catch (final Exception ignored) {
                            }
                        });

                    }
                    case RIGHT -> {

                        player.closeInventory();

                        if (Telefono.getCacheChiamata().containsNumber(sim)) {
                            Common.tell(player, "&cPuoi efettuare soltanto una chiamata alla volta!");
                            return;
                        }


                        /**
                         *
                         * CHECK ABBONAMENTO
                         *
                         */


                        Telefono.getCacheAbbonamento().get(sim).thenAccept(opt_abbonamento -> {
                            if (opt_abbonamento.isEmpty()) {
                                Common.tell(player, "&cNon hai abbastanza credito per chiamare %s %s".formatted(contatto.getName(), contatto.getSurname()));
                                return;
                            }

                            final Abbonamento abbonamento = opt_abbonamento.get();
                            if (!abbonamento.hasCreditoToCall()) {
                                Common.tell(player, "&cNon hai abbastanza credito per chiamare %s %s".formatted(contatto.getName(), contatto.getSurname()));
                                return;
                            }
                            /**
                             *
                             * CHECK SE ESISTE O SE E' GIA' IN CHIAMATA
                             *
                             */


                            final Optional<Player> opt_target = TelephoneAPI.getPlayerByNumber(contatto.getNumber());
                            final String number = contatto.getNumber();

                            final Contatto contattoWhoCall = TelephoneAPI.getContattoByNumber(contatto.getNumber(), contatto.getSim())
                                    .orElseGet(() -> new Contatto(contatto.getNumber(), contatto.getSim(), contatto.getNumber()));


                            if (opt_target.isEmpty() || Telefono.getCacheChiamata().containsNumber(number)) {
                                Database.getInstance().saveChiamata(new HistoryChiamata(sim, number, Utils.getDateNow(), true));
                                final Call call = new Call(contatto, contattoWhoCall, abbonamento);
                                Telefono.getCacheHistoryChiamate().put(sim, call.getHistoryChiamata(true));
                                Telefono.getCacheHistoryChiamate().put(number, call.getHistoryChiamataReverse(true));
                                Common.tell(player, "&cIl contatto %s %s non è disponibile, riprova più tardi".formatted(contatto.getName(), contatto.getSurname()));
                                return;
                            }

                            final Player target = opt_target.get();
                            if (target.equals(player)) {
                                Common.tell(player, "&cNon puoi chiamare te stesso!");
                                return;
                            }


                            contattoWhoCall.setPlayer(player);
                            contatto.setPlayer(target);
                            final Call call = new Call(contatto, contattoWhoCall, abbonamento);

                            Telefono.getCacheChiamata().putData(Utils.toCallFormat(sim, number), call);

                            Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &7Chiamata in arrivo da §f%s§7, apri il telefono per rispondere".formatted(contattoWhoCall.getFullName()));
                            Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &7Stai chiamando &f%s...".formatted(contatto.getFullName()));

                            new BukkitRunnable() {

                                int time = 10;

                                @Override
                                public void run() {

                                    if (Telefono.getCacheChiamata().getData(number).isEmpty()) {
                                        Database.getInstance().saveChiamata(call.getHistoryChiamata(true));
                                        Telefono.getCacheHistoryChiamate().put(sim, call.getHistoryChiamata(true));
                                        Telefono.getCacheHistoryChiamate().put(number, call.getHistoryChiamataReverse(true));
                                        Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &cLa chiamata a %s è stata rifiutata e verrà salvata come persa".formatted(contatto.getFullName()));
                                        Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &cHai rifiutato la chiamata di %s, verrà salvata come persa".formatted(contattoWhoCall.getFullName()));
                                        cancel();
                                        return;

                                    }
                                    if (Telefono.getCacheChiamata().getData(number).get().isInCall()) {
                                        cancel();
                                        return;
                                    }

                                    if (time == 1 || time == 2 || time == 3 || time == 5 || time == 10)
                                        sendNotify();

                                    if (time == 0) {
                                        Database.getInstance().saveChiamata(call.getHistoryChiamata(true));
                                        Telefono.getCacheChiamata().removeData(number);
                                        Telefono.getCacheHistoryChiamate().put(sim, call.getHistoryChiamata(true));
                                        Telefono.getCacheHistoryChiamate().put(number, call.getHistoryChiamataReverse(true));
                                        Common.tellNoPrefix(player, "&7✉ &f&lCHIAMATA &8» &c%s non ha risposto alla tua chiamata, verrà salvata come persa".formatted(contatto.getFullName()));
                                        Common.tellNoPrefix(target, "&7✉ &f&lCHIAMATA &8» &cHai rifiutato la chiamata di %s, verrà salvata come persa".formatted(contattoWhoCall.getFullName()));
                                        cancel();
                                        return;
                                    }

                                    time--;

                                }

                                private void sendNotify() {
                                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1L, 1L);
                                    target.sendActionBar("§7Drinn.. Drinn.. hai §f" + time + "secondi §7per §frispondere §7alla chiamata");
                                }
                            }.runTaskTimerAsynchronously(Telefono.getInstance(), 0, 25L);


                        });
                    }
                }
            }
        }).collect(Collectors.toUnmodifiableList());

        return PagedGui.items().setStructure(
                        "# # # < # > # # a",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # # # # # # #")
                .addIngredient('a', new ItemAddContatto(null, sim)).setContent(contattiItems).build();
    }

    @Override
    public void open(final Player player) {
        Utils.openGui(getInventory(), player, "ʀᴜʙʀɪᴄᴀ");
    }
}