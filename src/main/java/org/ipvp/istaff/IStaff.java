package org.ipvp.istaff;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import lombok.Getter;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.istaff.commands.CheckCommand;
import org.ipvp.istaff.commands.HackerCommand;
import org.ipvp.istaff.commands.ReportCommand;
import org.ipvp.istaff.data.HackerModeHandler;
import org.ipvp.istaff.data.IStaffDataBaseManager;
import org.ipvp.istaff.data.PlayerHackerMode;
import org.ipvp.istaff.listeners.BungeeMessager;
import org.ipvp.istaff.listeners.ConnectionHandler;
import org.ipvp.istaff.listeners.HackerModeListener;
import org.ipvp.istaff.menu.DisplayReportsMenu;
import org.ipvp.istaff.menu.Menu;
import org.ipvp.istaff.menu.MenuPage;
import org.ipvp.istaff.menu.MenuServer;
import org.ipvp.istaff.toolbox.ZToolBox;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class IStaff extends JavaPlugin {

    @Getter
    private BungeeMessager bungeeMessager;

    private MongoClient mongoClient;

    @Getter
    private Configuration configFile;

    @Getter
    private IStaffDataBaseManager iStaffDataBaseManager;

    @Getter
    private HackerModeHandler hackerModeHandler;

    @Getter
    private JedisPool pool;

    @Getter
    private static IStaff plugin;

    @Override
    public void onEnable() {
        plugin = this;
        setupConfig();

        Menu.initialize(this);
        setupMenus();

        hackerModeHandler = new HackerModeHandler(this);

        getCommand("h").setExecutor(new HackerCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));

        getServer().getPluginManager().registerEvents(new HackerModeListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionHandler(this), this);

        new ZToolBox(this); // Create and register ZToolBox

        // Database stuff
        final JavaPlugin plugin = this;
        bungeeMessager = new BungeeMessager(this);
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                // Try setting up Mongo.
                String mongoName = configFile.getString("mongo.name");
                String mongoIP = configFile.getString("mongo.host");
                int mongoPort = configFile.getInt("mongo.port");
                try {
                    getServer().getLogger().info("Attempting to connect to mongo database");
                    mongoClient = new MongoClient(mongoIP, mongoPort);
                    mongoClient.getDatabaseNames();

                    DB mongoDatabase = mongoClient.getDB(mongoName);
                    iStaffDataBaseManager = new IStaffDataBaseManager(mongoDatabase);
                    getServer().getLogger().log(Level.INFO, "Database: " + mongoDatabase.getName());
                    if (getServer().getOnlinePlayers().size() > 0) {
                        bungeeMessager.sendPluginMessage("GetServer", null);
                        getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
                            @Override
                            public void run() {
                                for (Player player : getServer().getOnlinePlayers()) {
                                    PlayerHackerMode playerHackerMode = iStaffDataBaseManager.getHackerMode(player.getUniqueId());
                                    final boolean on = playerHackerMode != null && playerHackerMode.isHackerMode();
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            hackerModeHandler.updatePlayerHackerMode(player, false, on);
                                        }
                                    }.runTask(plugin);
                                }
                            }
                        }, 10L);
                    }

                    getServer().getLogger().info("Successfully connected to mongo database");
                } catch (MongoException ex) {
                    getServer().getLogger().log(Level.SEVERE, "Could not connect to mongo database", ex);
                } catch (UnknownHostException ex) {
                    ex.printStackTrace();
                }

                // Connect to redis now.
                String redisIP = configFile.getString("redis.host");
                int redisPort = configFile.getInt("redis.port");
                String redisPassword = configFile.getString("redis.password");
                if (redisPassword == null || redisPassword.isEmpty()) {
                    pool = new JedisPool(new JedisPoolConfig(), redisIP, redisPort, 0);
                } else {
                    pool = new JedisPool(new JedisPoolConfig(), redisIP, redisPort, 0, redisPassword);
                }
                try {
                    getServer().getLogger().info("Attempting to connect to Redis");
                    pool.getResource();
                    bungeeMessager.subscribeInit();
                    getServer().getLogger().info("Successfully connected to Redis");
                } catch (Exception ex) {
                    getServer().getLogger().log(Level.SEVERE, "Could not connect to Redis", ex);
                }
            }
        });
    }

    @Override
    public void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
        }

        bungeeMessager.close();
        if (pool != null) {
            pool.close();
        }

        hackerModeHandler.restoreAllPlayerInventories();
    }

    private void setupConfig() {
        configFile = this.getConfig();
        configFile.options().copyDefaults(true);
        configFile.addDefault("reports.max.characters", 16);
        configFile.addDefault("reports.max.count", 1);
        configFile.addDefault("redis.host", "localhost");
        configFile.addDefault("redis.port", 6379);
        configFile.addDefault("redis.password", null);
        configFile.addDefault("reports.max.in_time_ms", (long) 120000);
        configFile.addDefault("mongo.host", "localhost");
        configFile.addDefault("mongo.port", 27017);
        configFile.addDefault("mongo.name", "iStaff");
        configFile.addDefault("reports.lifespan", 6379);
        reportLifespan = configFile.getInt("reports.lifespan");

        this.saveDefaultConfig();
        this.saveConfig();
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    private void setupMenus() {
        MenuPage reports = new DisplayReportsMenu(this, true);
        MenuServer.urlMenupages.put("reports", reports);
    }

    public static Map<String, String> splitQuery(String url) throws UnsupportedEncodingException {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        try {
            String query = url.split("\\?", 2)[1];
            String[] pairs = query.split("\\&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        } catch (IndexOutOfBoundsException ignored) {
        }

        return queryPairs;
    }

    @Getter
    private int reportLifespan = 0;
}
