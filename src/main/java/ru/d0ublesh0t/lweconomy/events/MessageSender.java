package ru.d0ublesh0t.lweconomy.events;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class MessageSender {
    public static final long[] TELEGRAM_USER_IDS = {};; // Юзеры ТГ на подобии 000000000L, 000000000L
    public static final long[] VK_USER_IDS = {}; // Юзеры ВК на подобии 000000000L, 000000000L
    private static final String VK_ACCESS_TOKEN = "ТОКЕН ВК";
    private static final String TELEGRAM_BOT_TOKEN = "ТОКЕН ТГ";

    public static void sendVkMessageToUsers(String message, List<Long> userIds) {
        try {
            for (Long userId : userIds) {
                String urlString = "https://api.vk.com/method/messages.send?user_id=" + userId + "&random_id=" + generateRandomId() + "&message=" + URLEncoder.encode(message, "UTF-8") + "&v=5.81&access_token=" + VK_ACCESS_TOKEN;
                getRequest(urlString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendTelegramMessageToUsers(String message, List<Long> userIds) {
        try {
            for (Long userId : userIds) {
                String urlString = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + userId + "&text=" + URLEncoder.encode(message, "UTF-8");
                getRequest(urlString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int generateRandomId() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    private static void getRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        connection.disconnect();
    }
}
