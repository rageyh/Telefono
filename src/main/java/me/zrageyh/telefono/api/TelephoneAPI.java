package me.zrageyh.telefono.api;

import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.cache.CacheNumeri;
import me.zrageyh.telefono.model.Contatto;
import me.zrageyh.telefono.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TelephoneAPI {


    public static final Telefono instance = ((Telefono) Telefono.getInstance());

    public static ItemStack setTelephoneNumber(final ItemStack item, final String number) {

        final ItemStack newItem = Utils.setNBTTag(item, "telephone_number", number);
        final List<String> lore = newItem.getLore();
        lore.set(1, "§7Numero: §f%s".formatted(number));
        newItem.setLore(lore);
        return newItem;
    }

    public static ItemStack setSimNumber(final ItemStack item, final String number) {
        final ItemStack newItem = Utils.setNBTTag(item, "sim_number", number);
        final List<String> lore = newItem.getLore();
        lore.set(1, "§7Numero: §f%s".formatted(number));
        newItem.setLore(lore);
        return newItem;
    }

    public static boolean hasNumber(final ItemStack item) {
        final String number = Utils.getNBTTag(item, "telephone_number");
        return number != null && !number.equals("nessuno");
    }

    public static String getTelephoneNumber(final ItemStack item) {
        final String number = Utils.getNBTTag(item, "telephone_number");
        return number != null ? number : "nessuno";
    }

    public static String getSimNumber(final ItemStack item) {
        return Utils.getNBTTag(item, "sim_number");
    }

    public static boolean isTelephone(final ItemStack item) {
        return item != null && item.getType() != Material.AIR && Utils.hasNBTTag(item, "telephone_number");
    }

    public static boolean isSim(final ItemStack item) {
        return item != null && item.getType() != Material.AIR && Utils.hasNBTTag(item, "sim_number");
    }

    public static boolean numberExists(final String number) {
        final CacheNumeri cacheNumeri = Telefono.getCacheNumeri();
        return cacheNumeri.getNumbers().contains(number);
    }

    public static Optional<Player> getPlayerByNumber(final String number) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getPlayer)
                .filter(TelephoneAPI::hasTelephoneInInventory)
                .filter(player -> {
                    final List<ItemStack> telephones = getTelephonesInInventory(player);
                    for (final ItemStack item : telephones) {
                        final String numberTelephone = TelephoneAPI.getTelephoneNumber(item);
                        if (numberTelephone.equalsIgnoreCase(number))
                            return true;
                    }
                    return false;
                })
                .findAny();

    }


    public static Optional<Contatto> getContattoByNumber(final String sim, final String number) {
        final Optional<List<Contatto>> opt_contatti_target = Telefono.getCacheContatti().get(sim);
        if (opt_contatti_target.isEmpty()) return Optional.empty();

        final List<Contatto> contatti = opt_contatti_target.get();
        return contatti.stream()
                .filter(c -> c.getNumber().equalsIgnoreCase(number))
                .findFirst();
    }


    public static List<ItemStack> getTelephonesInInventory(final Player player) {
        final Inventory inv = player.getInventory();
        return Arrays.stream(inv.getContents())
                .filter(i -> i != null && i.getType() != Material.AIR)
                .filter(TelephoneAPI::isTelephone)
                .toList();

    }

    public static boolean hasTelephoneInInventory(final Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(s -> s != null && s.getType() != Material.AIR)
                .anyMatch(TelephoneAPI::isTelephone);
    }


}
