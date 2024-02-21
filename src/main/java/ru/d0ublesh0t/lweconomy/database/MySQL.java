package ru.d0ublesh0t.lweconomy.database;

import org.bukkit.command.*;
import java.sql.*;
import java.util.UUID;
import org.bukkit.*;
import ru.d0ublesh0t.lweconomy.LWEconomy;

public class MySQL {
    public static Connection con;
    static ConsoleCommandSender console;

    public static void connect(String host, String port, String database, String username, String password) {
        if (!isConnected()) {
            try {
                con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true", username, password);
                console.sendMessage("[LWEconomy] Установлено соединение с MySQL!");
            } catch (SQLException e) {
                handleSQLException("Ошибка при подключении к MySQL:", e);
                disconnect();
            }
        }
    }

    public static boolean doesPlayerExist(UUID playerUUID) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT * FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet rs = selectStatement.executeQuery();
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            handleSQLException("Ошибка при проверке существования игрока:", e);
        }
        return false;
    }

    public static double getPlayerBalance(UUID playerUUID) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT balance FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble("balance");
            }
        } catch (SQLException e) {
            handleSQLException("Ошибка при получении баланса игрока:", e);
        }
        return 0;
    }

    public static void setPlayerBalance(UUID playerUUID, double newBalance) {
        try {
            PreparedStatement updateStatement = con.prepareStatement("UPDATE " + LWEconomy.table + " SET balance = ? WHERE uuid = ?");
            updateStatement.setDouble(1, newBalance);
            updateStatement.setString(2, playerUUID.toString());
            updateStatement.executeUpdate();
        } catch (SQLException e) {
            handleSQLException("Ошибка при обновлении баланса игрока:", e);
        }
    }

    public static void updatePlayerBalance(UUID playerUUID, double balanceToAdd) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT balance FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                double oldBalance = resultSet.getDouble("balance");
                double newBalance = oldBalance + balanceToAdd;

                PreparedStatement updateStatement = con.prepareStatement("UPDATE " + LWEconomy.table + " SET balance = ? WHERE uuid = ?");
                updateStatement.setDouble(1, newBalance);
                updateStatement.setString(2, playerUUID.toString());
                updateStatement.executeUpdate();
            }
        } catch (SQLException e) {
            handleSQLException("Ошибка при обновлении баланса игрока:", e);
        }
    }

    public static void addBalance(UUID playerUUID, double amount) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT balance FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                double currentBalance = resultSet.getDouble("balance");
                double newBalance = currentBalance + amount;
                setPlayerBalance(playerUUID, newBalance);
            }
        } catch (SQLException e) {
            handleSQLException("Ошибка при начислении средств:", e);
        }
    }

    public static void removeBalance(UUID playerUUID, double amount) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT balance FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                double currentBalance = resultSet.getDouble("balance");
                double newBalance = Math.max(0, currentBalance - amount); // Ensure balance doesn't go negative
                setPlayerBalance(playerUUID, newBalance);
            }
        } catch (SQLException e) {
            handleSQLException("Ошибка при вычитании средств:", e);
        }
    }

    public static boolean isPlayerBanned(UUID playerUUID) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT ban FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet rs = selectStatement.executeQuery();

            if (rs.next()) {
                String banStatus = rs.getString("ban");
                if (banStatus != null && banStatus.equalsIgnoreCase("yes")) {
                    return true; // Игрок заблокирован
                }
            }
            rs.close();
        } catch (SQLException e) {
            handleSQLException("Ошибка при проверке статуса блокировки игрока:", e);
        }
        return false; // Игрок не заблокирован
    }


    public static void setPlayerBanStatus(UUID playerUUID, boolean banStatus, String banAuthor) {
        try {
            PreparedStatement statement = con.prepareStatement("UPDATE " + LWEconomy.table + " SET ban = ?, ban_author = ? WHERE uuid = ?");
            statement.setString(1, banStatus ? "yes" : "no");
            statement.setString(2, banAuthor);
            statement.setString(3, playerUUID.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            handleSQLException("Ошибка при установке статуса блокировки игрока:", e);
        }
    }

    public static void setPlayerBanAuthor(UUID playerUUID, String banAuthor) {
        try {
            PreparedStatement statement = con.prepareStatement("UPDATE " + LWEconomy.table + " SET ban_author = ? WHERE uuid = ?");
            statement.setString(1, banAuthor);
            statement.setString(2, playerUUID.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            handleSQLException("Ошибка при установке автора блокировки игрока:", e);
        }
    }

    public static String getPlayerBanAuthor(UUID playerUUID) {
        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT ban_author FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet rs = selectStatement.executeQuery();

            if (rs.next()) {
                return rs.getString("ban_author");
            }
            rs.close();
        } catch (SQLException e) {
            handleSQLException("Ошибка при получении автора блокировки игрока:", e);
        }
        return null;
    }

    public static void setPlayerBanReason(UUID playerUUID, String banReason) {
        try {
            PreparedStatement statement = con.prepareStatement("UPDATE " +LWEconomy.table + " SET ban_reason = ? WHERE uuid = ?");
            statement.setString(1, banReason);
            statement.setString(2, playerUUID.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            handleSQLException("Ошибка при установке причины блокировки игрока:", e);
        }
    }

    public static String getPlayerBanReason(UUID playerUUID) {
        if (!isConnected()) {
            return ""; // Вернуть пустую строку, если нет соединения
        }

        try {
            PreparedStatement selectStatement = con.prepareStatement("SELECT ban_reason FROM " + LWEconomy.table + " WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet rs = selectStatement.executeQuery();

            if (rs.next()) {
                return rs.getString("ban_reason");
            }
            rs.close();
        } catch (SQLException e) {
            handleSQLException("Ошибка при получении причины блокировки игрока:", e);
        }
        return ""; // Вернуть пустую строку, если причина блокировки не найдена
    }

    public static void disconnect() {
        if (isConnected()) {
            try {
                con.close();
                console.sendMessage("[LWEconomy] Соединение с MySQL закрыто!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void Reconnect(final String host, final String port, final String database, final String username, final String password) {
        try {
            MySQL.console.sendMessage("");
            MySQL.con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true", username, password);
            MySQL.console.sendMessage("");
        }
        catch (SQLException e) {
            MySQL.console.sendMessage("");
            e.printStackTrace();
            disconnect();
        }
    }

    public static void reconnect(String host, String port, String database, String username, String password) {
        disconnect();
        connect(host, port, database, username, password);
    }

    public static boolean isConnected() {
        return con != null;
    }

    public static Connection getConnection() {
        return con;
    }

    private void executeCreateTableQuery(String createTableQuery) throws SQLException {
        try (Statement statement = MySQL.getConnection().createStatement()) {
            statement.executeUpdate(createTableQuery);
        }
    }

    public static ResultSet executeQuery(String query) throws SQLException {
        try (Statement statement = MySQL.getConnection().createStatement()) {
            // Для Select
            return statement.executeQuery(query);
        }
    }

    private static void handleSQLException(String message, SQLException e) {
        console.sendMessage("[LWEconomy] " + message);
        e.printStackTrace();
    }

    static {
        MySQL.console = Bukkit.getConsoleSender();
    }
}
