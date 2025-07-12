package com.example.paperwhitelist.database;

import com.example.paperwhitelist.PaperWhitelistPlugin;
import com.example.paperwhitelist.model.Application;
import com.example.paperwhitelist.model.WhitelistEntry;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final PaperWhitelistPlugin plugin;
    private Connection connection;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseManager(PaperWhitelistPlugin plugin) {
        this.plugin = plugin;
        String dbFile = plugin.getConfigManager().getString("database.file", "whitelist.db");
        this.username = plugin.getConfigManager().getString("database.username", "admin");
        this.password = plugin.getConfigManager().getString("database.password", "password");
        this.jdbcUrl = "jdbc:h2:" + plugin.getDataFolder() + "/" + dbFile + ";AUTO_SERVER=TRUE";
    }

    public void initialize() {
        try {
            // Load H2 driver
            Class.forName("org.h2.Driver");
            
            // Establish connection
            connect();
            
            // Create tables if not exist
            createTables();
            
            plugin.getLogger().info("Database initialized successfully");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public synchronized Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
        }
        return connection;
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    private void createTables() throws SQLException {
        // Create applications table
        String createApplicationsTable = """
            CREATE TABLE IF NOT EXISTS applications (
                id INT AUTO_INCREMENT PRIMARY KEY,
                game_id VARCHAR(50) NOT NULL,
                contact VARCHAR(50) NOT NULL,
                version VARCHAR(20) NOT NULL,
                bedrock_id VARCHAR(50),
                status VARCHAR(20) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        // Create whitelist table
        String createWhitelistTable = """
            CREATE TABLE IF NOT EXISTS whitelist (
                id INT AUTO_INCREMENT PRIMARY KEY,
                game_id VARCHAR(50) NOT NULL,
                uuid VARCHAR(100),
                version VARCHAR(20) NOT NULL,
                contact VARCHAR(50),
                user_type VARCHAR(20) DEFAULT 'TRIAL',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connect().createStatement()) {
            stmt.execute(createApplicationsTable);
            stmt.execute(createWhitelistTable);
        }
    }

    // Application CRUD operations
    public boolean addApplication(Application application) {
        String sql = "INSERT INTO applications (game_id, contact, version, bedrock_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, application.getGameId());
            pstmt.setString(2, application.getContact());
            pstmt.setString(3, application.getVersion());
            pstmt.setString(4, application.getBedrockId());
            pstmt.setString(5, application.getStatus());
            pstmt.setTimestamp(6, Timestamp.valueOf(application.getCreatedAt()));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add application: " + e.getMessage());
            return false;
        } finally {
            close();
        }
    }

    public List<Application> getApplicationsByStatus(String status) {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications WHERE status = ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, status);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Application app = new Application();
                    app.setId(rs.getInt("id"));
                    app.setGameId(rs.getString("game_id"));
                    app.setContact(rs.getString("contact"));
                    app.setVersion(rs.getString("version"));
                    app.setBedrockId(rs.getString("bedrock_id"));
                    app.setStatus(rs.getString("status"));
                    app.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    applications.add(app);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get applications: " + e.getMessage());
        } finally {
            close();
        }
        
        return applications;
    }

    public Application getApplicationById(int id) {
        String sql = "SELECT * FROM applications WHERE id = ?";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Application app = new Application();
                    app.setId(rs.getInt("id"));
                    app.setGameId(rs.getString("game_id"));
                    app.setContact(rs.getString("contact"));
                    app.setVersion(rs.getString("version"));
                    app.setBedrockId(rs.getString("bedrock_id"));
                    app.setStatus(rs.getString("status"));
                    app.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return app;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get application: " + e.getMessage());
        } finally {
            close();
        }
        
        return null;
    }

    public boolean updateApplicationStatus(int id, String status) {
        String sql = "UPDATE applications SET status = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to update application status: " + e.getMessage());
            return false;
        } finally {
            close();
        }
    }

    // Whitelist CRUD operations
    public boolean addWhitelistEntry(WhitelistEntry entry) {
        String sql = "INSERT INTO whitelist (game_id, uuid, version, contact, user_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, entry.getGameId());
            pstmt.setString(2, entry.getUuid());
            pstmt.setString(3, entry.getVersion());
            pstmt.setString(4, entry.getContact());
            pstmt.setString(5, entry.getUserType());
            pstmt.setTimestamp(6, Timestamp.valueOf(entry.getCreatedAt()));
            pstmt.setTimestamp(7, Timestamp.valueOf(entry.getUpdatedAt()));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add whitelist entry: " + e.getMessage());
            return false;
        } finally {
            close();
        }
    }

    public List<WhitelistEntry> getAllWhitelistEntries() {
        List<WhitelistEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM whitelist ORDER BY created_at DESC";
        
        try (Statement stmt = connect().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                WhitelistEntry entry = new WhitelistEntry();
                entry.setId(rs.getInt("id"));
                entry.setGameId(rs.getString("game_id"));
                entry.setUuid(rs.getString("uuid"));
                entry.setVersion(rs.getString("version"));
                entry.setContact(rs.getString("contact"));
                entry.setUserType(rs.getString("user_type"));
                entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                entry.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                entries.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get whitelist entries: " + e.getMessage());
        } finally {
            close();
        }
        
        return entries;
    }

    public WhitelistEntry getWhitelistEntryByGameId(String gameId) {
        String sql = "SELECT * FROM whitelist WHERE game_id = ?";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, gameId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    WhitelistEntry entry = new WhitelistEntry();
                    entry.setId(rs.getInt("id"));
                    entry.setGameId(rs.getString("game_id"));
                    entry.setUuid(rs.getString("uuid"));
                    entry.setVersion(rs.getString("version"));
                    entry.setContact(rs.getString("contact"));
                    entry.setUserType(rs.getString("user_type"));
                    entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    entry.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return entry;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get whitelist entry: " + e.getMessage());
        } finally {
            close();
        }
        
        return null;
    }

    public boolean updateWhitelistEntry(WhitelistEntry entry) {
        entry.setUpdatedAt(LocalDateTime.now());
        String sql = "UPDATE whitelist SET game_id = ?, uuid = ?, version = ?, contact = ?, user_type = ?, updated_at = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, entry.getGameId());
            pstmt.setString(2, entry.getUuid());
            pstmt.setString(3, entry.getVersion());
            pstmt.setString(4, entry.getContact());
            pstmt.setString(5, entry.getUserType());
            pstmt.setTimestamp(6, Timestamp.valueOf(entry.getUpdatedAt()));
            pstmt.setInt(7, entry.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to update whitelist entry: " + e.getMessage());
            return false;
        } finally {
            close();
        }
    }

    public boolean removeWhitelistEntry(int id) {
        String sql = "DELETE FROM whitelist WHERE id = ?";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove whitelist entry: " + e.getMessage());
            return false;
        } finally {
            close();
        }
    }

    public boolean removeWhitelistEntryByGameId(String gameId) {
        String sql = "DELETE FROM whitelist WHERE game_id = ?";
        
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, gameId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove whitelist entry by game id: " + e.getMessage());
            return false;
        } finally {
            close();
        }
    }
}