package me.zrageyh.telefono.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;

@AllArgsConstructor
@Getter
public class GpsHead {

    private final String name;
    private final String id;
    private final String location;

    private final String world = "world";


    public String toStringLocation(final Location location) {
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
    }
}
