package me.zrageyh.telefono.setting;

import me.zrageyh.telefono.manager.Database;
import org.mineacademy.fo.database.SimpleDatabase;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.YamlConfig;

public class SettingsMySQL {

    public static void init() {
        if (!SimplePlugin.getInstance().isEnabled()) {
            return;
        }
        final YamlConfig mysqlConfig = YamlConfig.fromInternalPath("mysql.yml");
        SimpleDatabase.setConnectUsingHikari(mysqlConfig.getBoolean("Hikari", true));
        final String host = mysqlConfig.getString("Host");
        final String database = mysqlConfig.getString("Database");
        final String user = mysqlConfig.getString("User");
        final String password = mysqlConfig.getString("Password");
        final String line = mysqlConfig.getString("Line");
        Database.getInstance().connect(line.replace("{host}", host).replace("{database}", database), user, password, "Telefono");


        mysqlConfig.save();
    }

}
