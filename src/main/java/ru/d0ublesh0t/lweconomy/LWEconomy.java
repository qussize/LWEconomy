package ru.d0ublesh0t.lweconomy;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.plugin.java.*;
import java.util.logging.*;
import java.net.*;
import java.io.*;
import org.bukkit.plugin.*;
import java.util.*;
import org.bukkit.command.*;
import java.sql.*;
import org.bukkit.event.player.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import ru.d0ublesh0t.lweconomy.database.MySQL;
import ru.d0ublesh0t.lweconomy.economy.EconomyCMD;
import ru.d0ublesh0t.lweconomy.economy.EconomyCheck;
import ru.d0ublesh0t.lweconomy.events.Placeholders;

public final class LWEconomy extends JavaPlugin implements Listener {

    private static LWEconomy plugin;

    private Placeholders placeholders;

    private Logger logger = Logger.getLogger("Minecraft");

    public static final String table = "DonateBalance";
    private String host;

    private String port;

    private String database;

    private String username;

    private String password;

    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        this.logger.info("[LWEconomy] Плагин успешно запущен!");
        this.host = getConfig().getString("mysql.host");
        this.port = getConfig().getString("mysql.port");
        this.database = getConfig().getString("mysql.database");
        this.username = getConfig().getString("mysql.username");
        this.password = getConfig().getString("mysql.password");

        initializeDatabase();

        // Создайте экземпляр PlaceholderAPI и передайте this (LMEconomy) в качестве аргумента.
        Placeholders placeholderAPI = new Placeholders(this);

        // Регистрация PlaceholderAPI
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderAPI.register();
        }

        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        MySQL.connect(this.host, this.port, this.database, this.username, this.password);
        Objects.requireNonNull(this.getCommand("coins")).setExecutor((CommandExecutor)new EconomyCheck(this));
        Objects.requireNonNull(this.getCommand("lazuraedit")).setExecutor((CommandExecutor)new EconomyCMD(this));
    }

    private void initializeDatabase() {
        try {
            MySQL.connect(this.host, this.port, this.database, this.username, this.password);
            createTableIfNotExists(table, "CREATE TABLE IF NOT EXISTS " + this.database + "." + table + " (`id` INT(11) NOT NULL AUTO_INCREMENT, `uuid` VARCHAR(255) NOT NULL, `nick` VARCHAR(255) NOT NULL, `balance` DOUBLE NOT NULL DEFAULT 0.0, `ban` VARCHAR(10) NOT NULL DEFAULT 'no', `ban_author` VARCHAR(255) DEFAULT NULL, `ban_reason` VARCHAR(255) DEFAULT NULL, PRIMARY KEY (`id`)) ENGINE = InnoDB");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTableIfNotExists(String tableName, String createTableQuery) throws SQLException {
        try (Statement statement = MySQL.getConnection().createStatement()) {
            statement.executeUpdate(createTableQuery);
        }
    }

    public void onDisable() {
        MySQL.disconnect();
        // Отменяем регистрацию класса Placeholders в плагине
        new Placeholders(this).unregister();
        this.logger.info(String.format("[%s] Disabled Version %s", this.getDescription().getName(), this.getDescription().getVersion()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        try {
            PreparedStatement checkStatement = MySQL.getConnection().prepareStatement("SELECT * FROM " + table + " WHERE uuid = ?");
            checkStatement.setString(1, uuid);
            ResultSet rsCoins = checkStatement.executeQuery();
            if (!rsCoins.next()) {
                String name = player.getName();
                String insertQuery = "INSERT INTO " + table + " (uuid, nick, balance, ban, ban_author, ban_reason) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStatement = MySQL.getConnection().prepareStatement(insertQuery)) {
                    insertStatement.setString(1, uuid);
                    insertStatement.setString(2, name);
                    insertStatement.setDouble(3, 0.0D);
                    insertStatement.setString(4, "no"); // Устанавливаем ban в "no"
                    insertStatement.setString(5, null); // Устанавливаем ban_author в NULL
                    insertStatement.setString(6, null); // Устанавливаем ban_reason в NULL
                    insertStatement.executeUpdate();
                }
            }
            rsCoins.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onReload() {
        this.saveDefaultConfig();
        MySQL.Reconnect(this.host, this.port, this.database, this.username, this.password);
    }
}
