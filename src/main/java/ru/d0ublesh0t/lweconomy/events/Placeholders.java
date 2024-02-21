package ru.d0ublesh0t.lweconomy.events;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.d0ublesh0t.lweconomy.LWEconomy;
import ru.d0ublesh0t.lweconomy.database.MySQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Placeholders extends PlaceholderExpansion {
    private LWEconomy plugin;
    private boolean errorLogged = false;

    public Placeholders(LWEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lweconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "d0ublesh0t";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(final Player player, final @NotNull String params) {
        if (player == null) {
            return null;
        }

        String[] parames = params.split("_");

        if (parames.length == 1) {
            try {
                ResultSet rs = getPlayerData(player);
                if (rs.next()) {
                    return rs.getString(params);
                }
            } catch (SQLException e) {
                if (!errorLogged) {
                    handleSQLException(e);
                    System.err.println("Ошибка выполнения запроса к базе данных: " + e.getMessage());
                    errorLogged = true;
                }
            }
        } else if (parames.length == 2 && parames[0].equalsIgnoreCase("checkban")) {
            try {
                ResultSet rs = getPlayerData(player);
                if (rs.next()) {
                    String banStatus = rs.getString("ban");
                    String banAuthor = rs.getString("ban_author");
                    String banReason = rs.getString("ban_reason");

                    if (banStatus != null && banStatus.equalsIgnoreCase("yes")) {
                        return processBanParam(parames[1], banAuthor, banReason);
                    } else {
                        return "Нет";
                    }
                }
            } catch (SQLException e) {
                if (!errorLogged) {
                    handleSQLException(e);
                    System.err.println("Ошибка выполнения запроса к базе данных: " + e.getMessage());
                    errorLogged = true;
                }
            }
        }

        return null;
    }

    private ResultSet getPlayerData(Player player) throws SQLException {
        String query = "SELECT * FROM " + LWEconomy.table + " WHERE uuid = ?";
        try {
            PreparedStatement statement = MySQL.getConnection().prepareStatement(query);
            statement.setString(1, player.getUniqueId().toString());
            return statement.executeQuery();
        } catch (SQLException e) {
            if (!errorLogged) {
                handleSQLException(e);
                System.err.println("Ошибка выполнения запроса к базе данных: " + e.getMessage());
                errorLogged = true;
            }
            throw e;
        }
    }

    private String processBanParam(String param, String banAuthor, String banReason) {
        if (param.equalsIgnoreCase("status")) {
            return banAuthor.equals("") ? "Нет" : "Да";
        } else if (param.equalsIgnoreCase("reason")) {
            return banReason.equals("none") ? "Не указана" : banReason;
        } else if (param.equalsIgnoreCase("author")) {
            return banAuthor.equals("") ? "Неизвестно" : banAuthor;
        }
        return "";
    }

    private void handleSQLException(SQLException e) {
        e.printStackTrace();
        // Дополнительные действия при ошибке SQL
    }
}
