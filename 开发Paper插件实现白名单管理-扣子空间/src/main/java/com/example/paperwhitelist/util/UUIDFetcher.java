package com.example.paperwhitelist.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UUIDFetcher {
    private static final String MINECRAFT_API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    /**
     * 获取玩家UUID（优先从本地缓存，本地没有则从Mojang API获取）
     * @param playerName 玩家名称
     * @return UUID或null（如果获取失败）
     */
    public static UUID getPlayerUUID(String playerName) {
        // 先尝试从本地获取
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.getUniqueId().toString().equals("00000000-0000-0000-0000-000000000000")) {
            return offlinePlayer.getUniqueId();
        }

        // 本地没有则尝试从Mojang API获取
        try {
            return fetchUUIDFromMojangAPI(playerName);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 异步获取玩家UUID
     * @param playerName 玩家名称
     * @return 包含UUID的CompletableFuture
     */
    public static CompletableFuture<UUID> getPlayerUUIDAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> getPlayerUUID(playerName));
    }

    /**
     * 从Mojang API获取UUID
     */
    private static UUID fetchUUIDFromMojangAPI(String playerName) throws IOException {
        URL url = new URL(MINECRAFT_API_URL + playerName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            String id = jsonObject.get("id").getAsString();
            
            // 格式化UUID（Mojang API返回的UUID没有连字符）
            return UUID.fromString(id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
            ));
        }
    }
}