package com.example.paperwhitelist.model;

import java.time.LocalDateTime;

public class Application {
    private int id;
    private String gameId;
    private String contact;
    private String version;
    private String bedrockId;
    private String status;
    private LocalDateTime createdAt;

    public Application() {}

    public Application(String gameId, String contact, String version, String bedrockId) {
        this.gameId = gameId;
        this.contact = contact;
        this.version = version;
        this.bedrockId = bedrockId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBedrockId() {
        return bedrockId;
    }

    public void setBedrockId(String bedrockId) {
        this.bedrockId = bedrockId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}