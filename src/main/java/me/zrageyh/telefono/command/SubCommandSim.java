package me.zrageyh.telefono.command;

import dev.lone.itemsadder.api.CustomStack;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.api.TelephoneAPI;
import me.zrageyh.telefono.exp.SimNumberAlreadyExistsException;
import me.zrageyh.telefono.exp.SimSaveException;
import me.zrageyh.telefono.manager.Database;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.remain.nbt.NBTItem;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.List;

public final class SubCommandSim extends SimpleSubCommand {


    public SubCommandSim(final String perm) {
        super("sim");
        setDescription("Ottieni una sim per telefono");
        setUsage("[numeroCustom]");
        setPermission(perm);
    }

    @Override
    protected void onCommand() {
        checkConsole();

        final ItemStack sim = new ItemBuilder(CustomStack.getInstance("iageneric:sim").getItemStack())
                .setDisplayName("§fSIM")
                .setLegacyLore(List.of(" ", "§7Numero: §f%number%", "§o§8Utilizzala su un telefono"))
                .setItemFlags(List.of(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE))
                .get();

        if (args.length == 0) {
            final String number = randomTelephoneNumber();
            checkAndSave(number, sim);
            return;
        }

        if (!getPlayer().hasPermission("telefono.sim.custom")) return;


        final String customNumber = args[0];
        if (customNumber.length() > 8) {
            tellError("&cIl numero di telefono deve essere inferiore a 8 cifre");
            return;
        }
        checkAndSave(customNumber, sim);
    }

    private void checkAndSave(String customNumber, ItemStack sim) {
        Common.runAsync(() -> {
            try {
                Database.getInstance().saveSim(customNumber);
                giveSim(customNumber, sim);
            } catch (SimNumberAlreadyExistsException e) {
                tellError("&cIl numero %s esiste già nel registro telefonico, ripeti il comando".formatted(customNumber));
            } catch (SimSaveException e) {
                Common.error(e, "Errore durante il salvataggio della sim");
            }
        });
    }

    private void giveSim(final String customNumber, final ItemStack sim) {
        final ItemStack item = TelephoneAPI.setSimNumber(sim, customNumber);
        final NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString("sim_number", customNumber);

        tellSuccess("&aHai generato una sim con il numero %s".formatted(customNumber));
        Telefono.getCacheNumeri().getNumbers().add(customNumber);

        Common.runLater(() -> {
            getPlayer().getInventory().addItem(nbtItem.getItem());
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        });
    }

    private String randomTelephoneNumber() {
        final long a = (long) Math.floor(Math.random() * 90000.0D) + 10000L;
        return "555" + a;
    }
}
