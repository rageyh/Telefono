package me.zrageyh.telefono.items;

import dev.lone.itemsadder.api.CustomStack;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.cache.CacheContatti;
import me.zrageyh.telefono.manager.Database;
import me.zrageyh.telefono.model.Contatto;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ItemAddContatto extends SimpleItem {

    final HeadDatabaseAPI headDatabaseAPI = Telefono.getHeadDatabaseAPI();
    final String sim;
    final CacheContatti cacheContatti;

    public ItemAddContatto(final ItemStack item, final String sim) {
        super(item);
        this.sim = sim;
        cacheContatti = Telefono.getCacheContatti();
    }

    @Override
    public ItemProvider getItemProvider() {
        return new ItemBuilder(CustomStack.getInstance("mcicons:icon_plus").getItemStack())
                .setDisplayName("§f§lᴀɢɢɪᴜɴɢɪ ᴄᴏɴᴛᴀᴛᴛᴏ")
                .setLegacyLore(List.of("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ §fᴀɢɢɪᴜɴɢᴇʀᴇ §7ᴜɴ ᴄᴏɴᴛᴀᴛᴛᴏ", "§7ᴀʟʟᴀ ᴛᴜᴀ ʀᴜʙʀɪᴄᴀ"))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .clearEnchantments()
                .clearModifiers();
    }

    @Override
    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {

        final Contatto contatto = new Contatto(sim);
        Optional<List<Contatto>> opt_contatti = Telefono.getCacheContatti().get(sim);

        final AnvilGUI.Builder numberGui = new AnvilGUI.Builder()
                .title("ɴᴜᴍᴇʀᴏ ᴄᴏɴᴛᴀᴛᴛᴏ")
                .plugin(Telefono.getInstance())
                .itemLeft(Telefono.itemInvisible)
                .itemRight(Telefono.itemInvisible)
                .onClick((slot, state) -> {

                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    final String number = ChatColor.stripColor(state.getText().replace(" ", ""));

                    if (opt_contatti.isPresent()) {
                        final List<Contatto> contatti = opt_contatti.get();

                        if (contatti.stream().anyMatch(c -> c.getNumber().equals(number))) {
                            Messenger.error(player, "&cUn contatto con il numero %s già esiste, prova con un altro numero di telefono".formatted(number));
                            return List.of(AnvilGUI.ResponseAction.close());
                        }
                    }

                    if (number.equals(sim)) {
                        Messenger.error(player, "&cNon puoi salvare &c%s &c%s &ccon il tuo stesso numero".formatted(contatto.getName(), contatto.getSurname()));
                        return List.of(AnvilGUI.ResponseAction.close());
                    }


                    if (!TelephoneAPI.numberExists(number)) {
                        Messenger.error(player, "&cIl numero %s è inesistente, perfavore inserisci un numero valido".formatted(number));
                        return List.of(AnvilGUI.ResponseAction.close());
                    }

                    contatto.setNumber(number);

                    cacheContatti.put(sim, contatto);
                    Database.getInstance().saveContatto(contatto);

                    Messenger.success(player, "&aHai aggiunto il contatto %s %s alla tua rubrica".formatted(contatto.getName(), contatto.getSurname()));
                    return List.of(AnvilGUI.ResponseAction.close());
                });

        final AnvilGUI.Builder surnameGui = new AnvilGUI.Builder()
                .title("ᴄᴏɢɴᴏᴍᴇ ᴄᴏɴᴛᴀᴛᴛᴏ")
                .plugin(Telefono.getInstance())
                .itemLeft(Telefono.itemInvisible)
                .itemRight(Telefono.itemInvisible)
                .onClick((slot, state) -> {

                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }


                    final String surname = ChatColor.stripColor(state.getText().replace(" ", ""));
                    final String name = contatto.getName();

                    if (surname.length() > 15) {
                        Messenger.error(player, "&cIl cognome è troppo lungo, riprova");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }

                    if (opt_contatti.isPresent()) {
                        final List<Contatto> contatti = opt_contatti.get();

                        if (contatti.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name)) && contatti.stream().anyMatch(c -> c.getSurname().equalsIgnoreCase(surname))) {
                            Messenger.error(player, "&cIl contatto %s %s è già presente nella tua rubrica!".formatted(name, surname));
                            return List.of(AnvilGUI.ResponseAction.close());
                        }
                    }
                    contatto.setSurname(surname);
                    numberGui.open(player);
                    return Collections.emptyList();
                });


        final AnvilGUI.Builder nameGui = new AnvilGUI.Builder()
                .title("ɴᴏᴍᴇ ᴄᴏɴᴛᴀᴛᴛᴏ")
                .plugin(Telefono.getInstance())
                .itemLeft(Telefono.itemInvisible)
                .itemRight(Telefono.itemInvisible)
                .onClick((slot, state) -> {

                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    final String name = ChatColor.stripColor(state.getText().replace(" ", ""));

                    if (name.length() > 15) {
                        Messenger.error(player, "&cIl nome è troppo lungo, riprova");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }

                    contatto.setName(name);
                    surnameGui.open(player);
                    return Collections.emptyList();
                });

        nameGui.open(player);
    }

}
