package ru.d0ublesh0t.lweconomy.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.d0ublesh0t.lweconomy.LWEconomy;
import ru.d0ublesh0t.lweconomy.database.MySQL;
import ru.d0ublesh0t.lweconomy.events.MessageSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static ru.d0ublesh0t.lweconomy.events.MessageSender.TELEGRAM_USER_IDS;
import static ru.d0ublesh0t.lweconomy.events.MessageSender.VK_USER_IDS;

public class EconomyCheck implements CommandExecutor {

    private final LWEconomy plugin;

    private final List<String> commandAliases = Arrays.asList("coins", "coin"); // Список алиасов команды

    public EconomyCheck(LWEconomy plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String senderNameDisplay;
        if (sender instanceof Player) {
            senderNameDisplay = ((Player) sender).getName();
        } else if (sender instanceof ConsoleCommandSender) {
            senderNameDisplay = "Консоль";
        } else if (sender instanceof RemoteConsoleCommandSender) {
            senderNameDisplay = "Ркон";
        } else if (sender.getName().equalsIgnoreCase("@EasyPayments")) {
            senderNameDisplay = "@EasyPayments";
        } else {
            senderNameDisplay = sender.getName();
        }
        String senderNameDisplayWithColor = (sender instanceof Player) ? "§fИгрок §c" + senderNameDisplay : "§c" + senderNameDisplay;
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.AQUA + "§m------------------------");
            sender.sendMessage(ChatColor.RED + "Команды для работы с балансом:");

            if (sender.hasPermission("lazuraworld.economy.balance")) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/" + commandAliases.get(0) + " balance " + ChatColor.WHITE + " - узнать свой баланс");
            }

            if (sender.hasPermission("lazuraworld.economy.balance.others")) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/" + commandAliases.get(0) + " balance <ник> " + ChatColor.WHITE + " - узнать баланс другого игрока");
            }

            if (sender.hasPermission("lazuraworld.economy.pay")) {
                sender.sendMessage(ChatColor.AQUA + "§m------------------------");
                sender.sendMessage(ChatColor.RED + "Команды для перевода монет:");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/" + commandAliases.get(0) + " pay <ник> <сумма> " + ChatColor.WHITE + " - перевести монеты другому игроку");
            }

            sender.sendMessage(ChatColor.AQUA + "§m------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player)) {
                String errorMessage = plugin.getConfig().getString("message.no-console");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cИспользование: §d/" + commandAliases.get(0) + " pay <ник> <сумма>");
                return true;
            }

            String receiverName = args[1];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.pay")) {
                String errorMessage = plugin.getConfig().getString("message.no-permission");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            double money;
            try {
                money = Double.parseDouble(args[2].replace(",", "."));
                if (money < 0.0D) {
                    String errorMessage = plugin.getConfig().getString("message.negative-balance");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                    return true;
                }
            } catch (NumberFormatException e) {
                String errorMessage = plugin.getConfig().getString("message.negative-format");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            if (!MySQL.doesPlayerExist(receiverUUID)) {
                String errorMessage = plugin.getConfig().getString("message.player-not-found");
                errorMessage = errorMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            UUID senderUUID = ((Player) sender).getUniqueId();

            if (!MySQL.doesPlayerExist(senderUUID)) {
                String errorMessage = plugin.getConfig().getString("message.player-not-found-sender");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            if (MySQL.isPlayerBanned(senderUUID)) {
                String successMessage = plugin.getConfig().getString("message.balance-blocked");
                successMessage = successMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
                return true;
            }

            if (MySQL.isPlayerBanned(receiverUUID)) {
                String successMessage = plugin.getConfig().getString("message.balance-blocked");
                successMessage = successMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
                return true;
            }

            double senderBalance = MySQL.getPlayerBalance(senderUUID);

            if (senderBalance < money) {
                String errorMessage = plugin.getConfig().getString("message.no-money");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            // Снимаем средства у отправителя
            MySQL.removeBalance(senderUUID, money);

            // Пополняем баланс получателя
            MySQL.addBalance(receiverUUID, money);

            String successMessageSender = plugin.getConfig().getString("message.balance-updated-sender");
            successMessageSender = successMessageSender.replace("{receiver}", receiverName)
                    .replace("{money}", String.valueOf(money));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessageSender));

            Player receiverOnline = Bukkit.getPlayer(receiverUUID);
            if (receiverOnline != null) {
                String successMessageReceiver = plugin.getConfig().getString("message.balance-updated-receiver");
                successMessageReceiver = successMessageReceiver.replace("{receiver}", senderNameDisplayWithColor)
                        .replace("{money}", String.valueOf(money));
                receiverOnline.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessageReceiver));
            }

            String alertMessage = senderNameDisplay + " перевел Игроку " + receiverName + ": " + money + " Лазур(-ов)";
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("balance")) {

            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    String errorMessage = plugin.getConfig().getString("message.no-console");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                    return true;
                }

                if (!sender.hasPermission("lazuraworld.economy.balance")) {
                    String errorMessage = plugin.getConfig().getString("message.no-permission");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                    return true;
                }

                Player player = (Player) sender;
                UUID playerUUID = player.getUniqueId();
                double playerBalance = MySQL.getPlayerBalance(playerUUID);

                player.sendMessage(ChatColor.GREEN + "§fНа вашем счету: §d" + playerBalance + " Лазур(-ов)");
                return true;
            } else {
                if (!sender.hasPermission("lazuraworld.economy.balance.others")) {
                    String errorMessage = plugin.getConfig().getString("message.no-permission");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                    return true;
                }

                String receiverName = args[1];
                OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);

                if (receiverPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "§cИспользование: §d/" + commandAliases.get(0) + " balance Ник");
                    return true;
                }

                UUID receiverUUID = receiverPlayer.getUniqueId();

                if (!MySQL.doesPlayerExist(receiverUUID)) {
                    String errorMessage = plugin.getConfig().getString("message.player-not-found");
                    errorMessage = errorMessage.replace("{receiver}", receiverName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                    return true;
                }

                double receiverBalance = MySQL.getPlayerBalance(receiverUUID);

                sender.sendMessage(ChatColor.GREEN + "§fНа счету Игрока §c" + receiverName + "§f: §d" + receiverBalance + " Лазур(-ов)");
                return true;
            }
        }

        return false;
    }

    private void sendAlertToVk(String message) {
        for (long userId : VK_USER_IDS) {
            List<Long> userIdList = new ArrayList<>();
            userIdList.add(userId);
            MessageSender.sendVkMessageToUsers(message, userIdList);
        }
    }

    private void sendAlertToTelegram(String message) {
        for (long userId : TELEGRAM_USER_IDS) {
            List<Long> userIdList = new ArrayList<>();
            userIdList.add(userId);
            MessageSender.sendTelegramMessageToUsers(message, userIdList);
        }
    }
}
