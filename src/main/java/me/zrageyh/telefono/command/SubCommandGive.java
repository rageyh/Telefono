package me.zrageyh.telefono.command;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.nbt.NBTItem;

public final class SubCommandGive extends SimpleSubCommand {


    public SubCommandGive(final String perm) {
        super("ottieni");
        setDescription("Ottieni un telefono");
        setUsage("");
        setPermission(perm);
    }

    @Override
    protected void onCommand() {
        checkConsole();

        //TODO CAMBIARE ITEM
		/*ItemStack telephone = ItemCreator.of(new ItemStack(Material.STICK))
				.name("&fTelefono")
				.lore(" ", "&7Numero: &fnessuno")
				.make();*/
        final ItemStack telephone = ItemCreator.of(CustomStack.getInstance("iageneric:phone").getItemStack())
                .name("&fTelefono")
                .lore("  ", "&7Numero: &fnessuno")
                .make();

        final NBTItem nbtItem = new NBTItem(telephone);
        nbtItem.setString("telephone_number", "nessuno");

        getPlayer().getInventory().addItem(nbtItem.getItem());

        tellSuccess("&aHai ricevuto un telefono");
    }
}
