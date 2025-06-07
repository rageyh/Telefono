package me.zrageyh.telefono.command;

import me.zrageyh.telefono.Telefono;
import org.mineacademy.fo.command.SimpleSubCommand;

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
        getPlayer().getInventory().addItem(Telefono.getServiceManager().getItemManager().getItemTelephone());
        tellSuccess("&aHai ricevuto un telefono");
    }
}
