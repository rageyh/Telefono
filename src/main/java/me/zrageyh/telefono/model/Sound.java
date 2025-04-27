package me.zrageyh.telefono.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class Sound {
    private final String sound;
    private final Location loc;
    private final Player p;
    private final float volume;
    private final float pitch;
    private final List<String> sounds = Arrays.asList(
            "iaalchemy:telephoneringtone",
            "iaalchemy:alieno",
            "iaalchemy:feelgood",
            "iaalchemy:huawei",
            "iaalchemy:nokia",
            "iaalchemy:pele",
            "iaalchemy:vader",
            "iaalchemy:message");


    public Sound(String sound, Location loc, Player p, float volume, float pitch) {
        if (sounds.stream().noneMatch(s -> s.equalsIgnoreCase(sound))) {
            throw new IllegalStateException("Il suono " + sound + " non è custom, scegli tra " + Arrays.toString(sounds.toArray()).replace("[", "").replace("]", ""));
        }
        this.sound = sound;
        this.loc = loc;
        this.p = p;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void playSound() {
        p.playSound(loc, sound, volume, pitch);
    }

    public void stopSound() {
        p.stopSound(sound);
    }
}
