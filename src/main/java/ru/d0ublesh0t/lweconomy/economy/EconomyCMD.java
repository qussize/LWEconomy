package ru.d0ublesh0t.lweconomy.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.d0ublesh0t.lweconomy.LWEconomy;
import ru.d0ublesh0t.lweconomy.database.MySQL;
import ru.d0ublesh0t.lweconomy.events.MessageSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static ru.d0ublesh0t.lweconomy.events.MessageSender.TELEGRAM_USER_IDS;
import static ru.d0ublesh0t.lweconomy.events.MessageSender.VK_USER_IDS;

public class EconomyCMD implements CommandExecutor {

    private final LWEconomy plugin;

    public EconomyCMD(LWEconomy plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length < 2) {
            return true;
        }

        String senderNameDisplay;
        if (sender instanceof Player) {
            senderNameDisplay = ((Player) sender).getName();
        } else if (sender instanceof ConsoleCommandSender) {
            senderNameDisplay = "Консоль";
        } else if (sender instanceof RemoteConsoleCommandSender) {
            senderNameDisplay = "Ркон";
        } else if (sender.getName().equalsIgnoreCase("@LazuraPayments")) {
            senderNameDisplay = "@LazuraPayments";
        } else {
            senderNameDisplay = sender.getName();
        }
        String senderNameDisplayWithColor = (sender instanceof Player) ? "§fИгрок §c" + senderNameDisplay : "§c" + senderNameDisplay;

        if (!args[0].equalsIgnoreCase("money")) {
            return true;
        }

        String subCommand = args[1];
        if (subCommand.equalsIgnoreCase("=")) {
            if (args.length < 4) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            // Проверка прав доступа
            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            double money;
            try {
                money = Double.parseDouble(args[3].replace(",", "."));
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

            if (MySQL.isPlayerBanned(receiverUUID)) {
                String successMessage = plugin.getConfig().getString("message.balance-blocked");
                successMessage = successMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
                return true;
            }

            MySQL.setPlayerBalance(receiverUUID, money);

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

            String alertMessage = plugin.getConfig().getString("message.balance-alert");
            alertMessage = alertMessage.replace("{senderNameDisplay}", senderNameDisplay)
                    .replace("{receiverName}", receiverName)
                    .replace("{money}", String.valueOf(money));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', alertMessage));
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }

        if (subCommand.equalsIgnoreCase("+")) {
            if (args.length < 4) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            double money;
            try {
                money = Double.parseDouble(args[3].replace(",", "."));
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

            if (MySQL.isPlayerBanned(receiverUUID)) {
                String successMessage = plugin.getConfig().getString("message.balance-blocked");
                successMessage = successMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
                return true;
            }

            // Обновление баланса в базе данных
            MySQL.updatePlayerBalance(receiverUUID, money);

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

            String alertMessage = senderNameDisplay + " выдал Игроку " + receiverName + ": " + money + " Лазур(-ов)";
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }

        if (subCommand.equalsIgnoreCase("-")) {
            if (args.length < 4) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            double money;
            try {
                money = Double.parseDouble(args[3].replace(",", "."));
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

            // Снимаем средства у цели
            MySQL.removeBalance(receiverUUID, money);

            String successMessage = plugin.getConfig().getString("message.balance-success");
            successMessage = successMessage.replace("{money}", String.valueOf(money))
                    .replace("{receiverName}", receiverName)
                    .replace("{senderNameDisplay}", senderNameDisplay);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));

            if (receiverPlayer != null && receiverPlayer.isOnline()) {
                String decreaseMessage = plugin.getConfig().getString("message.balance-decrease");
                decreaseMessage = decreaseMessage.replace("{money}", String.valueOf(money));
                Player onlineReceiver = receiverPlayer.getPlayer();
                onlineReceiver.sendMessage(ChatColor.translateAlternateColorCodes('&', decreaseMessage));
            }

            String alertMessage = plugin.getConfig().getString("message.balance-alert");
            alertMessage = alertMessage.replace("{senderNameDisplay}", senderNameDisplay)
                    .replace("{receiverName}", receiverName)
                    .replace("{money}", String.valueOf(money));
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }
        if (subCommand.equalsIgnoreCase("0")) {
            if (args.length < 3) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            if (!MySQL.doesPlayerExist(receiverUUID)) {
                String errorMessage = plugin.getConfig().getString("message.player-not-found");
                errorMessage = errorMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            // Сбрасываем баланс игрока
            MySQL.setPlayerBalance(receiverUUID, 0.0D);

            String successMessageSender = plugin.getConfig().getString("message.balance-updated-sender");
            successMessageSender = successMessageSender.replace("{receiver}", receiverName);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessageSender));

            Player receiverOnline = Bukkit.getPlayer(receiverUUID);
            if (receiverOnline != null) {
                String successMessageReceiver = plugin.getConfig().getString("message.negative-balance");
                receiverOnline.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessageReceiver));
            }

            String alertMessage = senderNameDisplay + " сбросил баланс Игрока " + receiverName;
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }
        if (subCommand.equalsIgnoreCase("b")) {
            if (args.length < 4) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            if (!MySQL.doesPlayerExist(receiverUUID)) {
                String errorMessage = plugin.getConfig().getString("message.player-not-found");
                errorMessage = errorMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            // Проверяем, заблокирован ли уже баланс игрока
            if (MySQL.isPlayerBanned(receiverUUID)) {
                String currentBanReason = MySQL.getPlayerBanReason(receiverUUID);
                String currentBanAuthor = MySQL.getPlayerBanAuthor(receiverUUID);
                String errorMessage = plugin.getConfig().getString("message.balance-blocked");
                errorMessage = errorMessage.replace("{BanAuthor}", currentBanAuthor + ". ")
                        .replace("{BanReason}", currentBanReason);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            String banReason = args[3];

            // Блокируем баланс игрока навсегда
            MySQL.setPlayerBanStatus(receiverUUID, true, sender.getName());
            MySQL.setPlayerBanAuthor(receiverUUID, sender.getName()); // Сначала устанавливаем BanAuthor
            MySQL.setPlayerBanReason(receiverUUID, banReason); // Затем устанавливаем BanReason

            sender.sendMessage("§aУспешно §7» §fВы заблокировали баланс Игрока §c" + receiverName + " §cнавсегда §fс причиной: " + banReason);

            if (receiverPlayer != null && receiverPlayer.isOnline()) {
                Player onlineReceiver = receiverPlayer.getPlayer();
                onlineReceiver.sendMessage("§aБаланс §7» §fВаш баланс был заблокирован. Обратитесь к администратору для разблокировки.");
            }

            String alertMessage = senderNameDisplay + " заблокировал баланс Игрока " + receiverName + " с причиной: " + banReason;
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }

        if (subCommand.equalsIgnoreCase("ub")) {
            if (args.length < 3) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            if (!MySQL.doesPlayerExist(receiverUUID)) {
                String errorMessage = plugin.getConfig().getString("message.player-not-found");
                errorMessage = errorMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            // Разблокируем баланс игрока
            MySQL.setPlayerBanStatus(receiverUUID, false, null);
            MySQL.setPlayerBanAuthor(receiverUUID, null);
            MySQL.setPlayerBanReason(receiverUUID, null);

            sender.sendMessage("§aУспешно §7» §fВы разблокировали баланс Игрока §c" + receiverName + "§f.");

            Player targetOnline = Bukkit.getPlayer(receiverUUID);
            if (targetOnline != null) {
                targetOnline.sendMessage("§aБаланс §7» §fВаш баланс разблокирован. Теперь вы можете снова использовать баланс.");
            }

            String alertMessage = senderNameDisplay + " разблокировал баланс Игрока " + receiverName;
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
        }

        if (subCommand.equalsIgnoreCase("cb")) {
            if (args.length < 3) {
                return true;
            }

            String receiverName = args[2];
            OfflinePlayer receiverPlayer = Bukkit.getOfflinePlayer(receiverName);
            UUID receiverUUID = receiverPlayer.getUniqueId();

            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }

            if (!MySQL.doesPlayerExist(receiverUUID)) {
                String errorMessage = plugin.getConfig().getString("message.player-not-found");
                errorMessage = errorMessage.replace("{receiver}", receiverName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                return true;
            }

            String banStatus = MySQL.isPlayerBanned(receiverUUID) ? "заблокирован" : "не заблокирован";
            String banAuthor = MySQL.getPlayerBanAuthor(receiverUUID);
            String banReason = MySQL.getPlayerBanReason(receiverUUID);

            sender.sendMessage("§7Статус блокировки игрока §c" + receiverName + "§7: " + (MySQL.isPlayerBanned(receiverUUID) ? "§cзаблокирован" : "§aне заблокирован"));

            if (MySQL.isPlayerBanned(receiverUUID)) {
                sender.sendMessage("§7Автор блокировки: " + (banAuthor != null ? "§f" + banAuthor : "§fне указан"));
                sender.sendMessage("§7Причина блокировки: " + (banReason != null ? "§f" + banReason : "§fне указана"));
            }

            return true;
        }

        if (subCommand.equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lazuraworld.economy.admin")) {
                return true;
            }
            plugin.onReload();
            sender.sendMessage(ChatColor.GREEN + "§aУспешно §7» §fВы перезагрузили плагин.");

            String alertMessage = senderNameDisplay + " перезагрузил плагин LWEconomy ";
            sendAlertToVk(alertMessage);
            sendAlertToTelegram(alertMessage);
            return true;
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

