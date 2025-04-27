package me.zrageyh.telefono.command;

import org.bukkit.ChatColor;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.DebugCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;

@AutoRegister
public final class TelefonoCommandGroup extends SimpleCommandGroup {

    @Override
    protected void registerSubcommands() {
        registerSubcommand(new DebugCommand("telefono.command.debug"));
        registerSubcommand(new SubCommandGive("telefono.command.give"));
        registerSubcommand(new SubCommandSim("telefono.command.sim"));
        registerSubcommand(new SubCommandAbbonamento("telefono.command.abbonamento"));
        registerSubcommand(new ReloadCommand("telefono.command.reload"));
    }

    @Override
    protected ChatColor getTheme() {
        return ChatColor.BLUE;
    }
}
