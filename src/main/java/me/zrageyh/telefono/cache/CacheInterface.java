package me.zrageyh.telefono.cache;

public interface CacheInterface<V> {

    void update(String sim, int id, V data);

    void remove(String sim, int id);

    void loadDataToCache();


}
