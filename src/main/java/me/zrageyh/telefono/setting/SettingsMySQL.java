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
        final String host = mysqlConfig.getString("Host");
        final String database = mysqlConfig.getString("Database");
        final String user = mysqlConfig.getString("User");
        final String password = mysqlConfig.getString("Password");
        final String line = mysqlConfig.getString("Line");

        final String jdbcUrl = line.replace("{host}", host).replace("{database}", database);
        Database.getInstance().initConnectionPool(jdbcUrl, user, password);
        Database.getInstance().connect(jdbcUrl, user, password, "Telefono");

        mysqlConfig.save();
    }

}
