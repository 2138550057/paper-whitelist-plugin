package com.example.paperwhitelist.web;

import com.example.paperwhitelist.PaperWhitelistPlugin;
import com.example.paperwhitelist.database.DatabaseManager;
import com.example.paperwhitelist.model.Application;
import com.example.paperwhitelist.model.WhitelistEntry;
import com.example.paperwhitelist.util.CommandExecutor;
import com.example.paperwhitelist.util.PasswordUtil;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static spark.Spark.*;

public class WebServer {
    private final PaperWhitelistPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ThymeleafTemplateEngine templateEngine;
    private final String bedrockPrefix;
    private final String defaultUserType;

    public WebServer(PaperWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.templateEngine = new ThymeleafTemplateEngine();
        this.bedrockPrefix = plugin.getConfigManager().getString("whitelist.bedrock-prefix", "BE_");
        this.defaultUserType = plugin.getConfigManager().getString("whitelist.default-user-type", "TRIAL");
    }

    public void start() {
        int port = plugin.getConfigManager().getInt("web.port", 8080);
        String bindAddress = plugin.getConfigManager().getString("web.bind-address", "0.0.0.0");
        
        // Configure Spark
        port(port);
        ipAddress(bindAddress);
        staticFiles.location("/web");
        staticFiles.expireTime(60);

        // Session configuration
        int sessionTimeout = plugin.getConfigManager().getInt("web.session-timeout", 30);
        sessionTimeout(sessionTimeout * 60); // Convert minutes to seconds

        // Before filters
        before("/admin/*", this::adminAuthFilter);

        // Routes
        get("/", this::getHomePage);
        get("/apply", this::getApplicationPage);
        post("/apply", this::submitApplication);
        get("/login", this::getLoginPage);
        post("/login", this::processLogin);
        get("/logout", this::processLogout);
        
        // Admin routes
        get("/admin", (req, res) -> {
            res.redirect("/admin/applications");
            return null;
        });
        
        get("/admin/applications", this::getApplicationsPage);
        post("/admin/applications/:id/approve", this::approveApplication);
        post("/admin/applications/:id/deny", this::denyApplication);
        
        get("/admin/whitelist", this::getWhitelistPage);
        get("/admin/whitelist/:id/edit", this::getEditWhitelistEntryPage);
        post("/admin/whitelist/:id/edit", this::updateWhitelistEntry);
        post("/admin/whitelist/:id/remove", this::removeWhitelistEntry);

        plugin.getLogger().info("Web server started on " + bindAddress + ":" + port);
    }

    public void stop() {
        plugin.getLogger().info("Stopping web server...");
        spark.Spark.stop();
    }

    private void adminAuthFilter(Request req, Response res) {
        Session session = req.session(false);
        if (session == null || session.attribute("admin") == null) {
            res.redirect("/login?redirect=" + req.pathInfo());
            halt();
        }
    }

    private Object getHomePage(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        return templateEngine.render(new ModelAndView(model, "home"));
    }

    private Object getApplicationPage(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        return templateEngine.render(new ModelAndView(model, "apply"));
    }

    private Object submitApplication(Request req, Response res) {
        String gameId = req.queryParams("gameId");
        String contact = req.queryParams("contact");
        String version = req.queryParams("version");
        String bedrockId = req.queryParams("bedrockId");

        // Validate input
        if (gameId == null || gameId.trim().isEmpty() || contact == null || contact.trim().isEmpty()) {
            Map<String, Object> model = new HashMap<>();
            model.put("error", "游戏ID和联系方式不能为空");
            model.put("gameId", gameId);
            model.put("contact", contact);
            model.put("version", version);
            model.put("bedrockId", bedrockId);
            return templateEngine.render(new ModelAndView(model, "apply"));
        }

        // Create application
        Application application = new Application(gameId.trim(), contact.trim(), version, 
                ("BOTH".equals(version) && bedrockId != null) ? bedrockId.trim() : null);
        databaseManager.addApplication(application);

        res.redirect("/apply?success=true");
        return null;
    }

    private Object getLoginPage(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("redirect", req.queryParams("redirect"));
        return templateEngine.render(new ModelAndView(model, "login"));
    }

    private Object processLogin(Request req, Response res) {
        String password = req.queryParams("password");
        String storedHash = plugin.getConfigManager().getString("admin.password-hash", "");
        String defaultPassword = plugin.getConfigManager().getString("admin.default-password", "admin123");

        // Check password
        boolean passwordValid;
        if (!storedHash.isEmpty()) {
            passwordValid = PasswordUtil.verifyPassword(password, storedHash);
        } else {
            // Use default password if hash not set
            passwordValid = defaultPassword.equals(password);
        }

        if (passwordValid) {
            Session session = req.session();
            session.attribute("admin", true);
            
            String redirect = req.queryParams("redirect");
            res.redirect(redirect != null ? redirect : "/admin");
            return null;
        } else {
            Map<String, Object> model = new HashMap<>();
            model.put("error", "密码错误");
            return templateEngine.render(new ModelAndView(model, "login"));
        }
    }

    private Object processLogout(Request req, Response res) {
        Session session = req.session(false);
        if (session != null) {
            session.invalidate();
        }
        res.redirect("/");
        return null;
    }

    private Object getApplicationsPage(Request req, Response res) {
        List<Application> pendingApplications = databaseManager.getApplicationsByStatus("PENDING");
        List<Application> processedApplications = databaseManager.getApplicationsByStatus("APPROVED");
        processedApplications.addAll(databaseManager.getApplicationsByStatus("DENIED"));
        
        Map<String, Object> model = new HashMap<>();
        model.put("pendingApplications", pendingApplications);
        model.put("processedApplications", processedApplications);
        return templateEngine.render(new ModelAndView(model, "admin/applications"));
    }

    private Object approveApplication(Request req, Response res) {
        try {
            int applicationId = Integer.parseInt(req.params(":id"));
            Application application = databaseManager.getApplicationById(applicationId);
            
            if (application != null) {
                // Update application status
                databaseManager.updateApplicationStatus(applicationId, "APPROVED");
                
                // Add to whitelist based on version
                String javaGameId = application.getGameId();
                String bedrockGameId = application.getBedrockId();
                
                // Add Java edition if selected
                if ("JAVA".equals(application.getVersion()) || "BOTH".equals(application.getVersion())) {
                    addToWhitelist(javaGameId, "JAVA", application.getContact());
                }
                
                // Add Bedrock edition if selected
                if ("BEDROCK".equals(application.getVersion()) || "BOTH".equals(application.getVersion())) {
                    String actualBedrockId = "BEDROCK".equals(application.getVersion()) ? 
                            javaGameId : bedrockGameId;
                    addToWhitelist(bedrockPrefix + actualBedrockId, "BEDROCK", application.getContact());
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid application ID: " + req.params(":id"));
        }
        
        res.redirect("/admin/applications");
        return null;
    }

    private Object denyApplication(Request req, Response res) {
        try {
            int applicationId = Integer.parseInt(req.params(":id"));
            databaseManager.updateApplicationStatus(applicationId, "DENIED");
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid application ID: " + req.params(":id"));
        }
        
        res.redirect("/admin/applications");
        return null;
    }

    private void addToWhitelist(String gameId, String version, String contact) {
        // Execute whitelist command
        CommandExecutor.executeWhitelistAdd(plugin, gameId);
        
        // Get UUID (for future use, currently not implemented)
        String uuid = null; // Could be implemented with Minecraft API or web API
        
        // Add to database
        WhitelistEntry entry = new WhitelistEntry(gameId, uuid, version, contact, defaultUserType);
        databaseManager.addWhitelistEntry(entry);
    }

    private Object getWhitelistPage(Request req, Response res) {
        List<WhitelistEntry> whitelistEntries = databaseManager.getAllWhitelistEntries();
        
        Map<String, Object> model = new HashMap<>();
        model.put("whitelistEntries", whitelistEntries);
        return templateEngine.render(new ModelAndView(model, "admin/whitelist"));
    }

    private Object getEditWhitelistEntryPage(Request req, Response res) {
        try {
            int entryId = Integer.parseInt(req.params(":id"));
            WhitelistEntry entry = databaseManager.getWhitelistEntryByGameId(req.params(":id"));
            
            if (entry != null) {
                Map<String, Object> model = new HashMap<>();
                model.put("entry", entry);
                return templateEngine.render(new ModelAndView(model, "admin/edit-whitelist"));
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid whitelist entry ID: " + req.params(":id"));
        }
        
        res.redirect("/admin/whitelist");
        return null;
    }

    private Object updateWhitelistEntry(Request req, Response res) {
        try {
            int entryId = Integer.parseInt(req.params(":id"));
            WhitelistEntry entry = databaseManager.getWhitelistEntryByGameId(req.params(":id"));
            
            if (entry != null) {
                String oldGameId = entry.getGameId();
                String newGameId = req.queryParams("gameId");
                String contact = req.queryParams("contact");
                String userType = req.queryParams("userType");
                
                // Update entry
                entry.setGameId(newGameId);
                entry.setContact(contact);
                entry.setUserType(userType);
                
                // If game ID changed, remove old one from whitelist
                if (!oldGameId.equals(newGameId)) {
                    CommandExecutor.executeWhitelistRemove(plugin, oldGameId);
                    CommandExecutor.executeWhitelistAdd(plugin, newGameId);
                }
                
                databaseManager.updateWhitelistEntry(entry);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid whitelist entry ID: " + req.params(":id"));
        }
        
        res.redirect("/admin/whitelist");
        return null;
    }

    private Object removeWhitelistEntry(Request req, Response res) {
        try {
            int entryId = Integer.parseInt(req.params(":id"));
            WhitelistEntry entry = databaseManager.getWhitelistEntryByGameId(req.params(":id"));
            
            if (entry != null) {
                // Remove from whitelist
                CommandExecutor.executeWhitelistRemove(plugin, entry.getGameId());
                
                // Remove from database
                databaseManager.removeWhitelistEntry(entryId);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid whitelist entry ID: " + req.params(":id"));
        }
        
        res.redirect("/admin/whitelist");
        return null;
    }
}