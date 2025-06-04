package me.zrageyh.telefono.cache;

import lombok.Getter;
import me.zrageyh.telefono.Telefono;
import me.zrageyh.telefono.manager.Database;
import org.mineacademy.fo.Common;

import java.util.LinkedList;
import java.util.List;

@Getter
public class CacheNumeri {

    @Getter
    private final List<String> numbers = new LinkedList<>();

    public CacheNumeri() {
        Common.runAsync(() -> {
            Database.getInstance().getAllSim().forEach(s -> Telefono.getCacheNumeri().getNumbers().add(s));
        });

    }


}
