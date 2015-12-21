package com.connorl.istaff;

import com.connorl.istaff.commands.HackerCommand;
import com.connorl.istaff.commands.ReportCommand;
import com.connorl.istaff.listeners.BungeeMessager;
import com.connorl.istaff.listeners.ConnectionHandler;
import com.connorl.istaff.listeners.HackerModeListener;
import com.gmail.xd.zwander.menu.DisplayReportsMenu;
import com.gmail.xd.zwander.menu.Menu;
import com.gmail.xd.zwander.menu.MenuPage;
import com.gmail.xd.zwander.menu.MenuServer;
import com.gmail.xd.zwander.toolbox.ZToolBox;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import lombok.Getter;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class IStaff extends JavaPlugin {

    private MongoClient mongoClient;

    @Getter
    private Configuration configFile;

    @Getter
    private JedisPool pool;

    @Getter
    private static IStaff plugin;

    @Override
    public void onEnable() {
        plugin = this;

        setupConfig();
        Menu.initialize(this);

        getCommand("h").setExecutor(new HackerCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));

        getServer().getPluginManager().registerEvents(new HackerModeListener(), this);
        getServer().getPluginManager().registerEvents(new ConnectionHandler(this), this);

        new ZToolBox(this); // Create and register ZToolBox

        // Database stuff
        getMongoClient();

        DB mongoDatabase = mongoClient.getDB(configFile.getString("mongo.name"));
        getServer().getLogger().log(Level.INFO, "Database: " + mongoDatabase.getName());
        if (getServer().getOnlinePlayers().size() > 0) {
            BungeeMessager.sendPluginMessage("GetServer", null);
            getServer().getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    for (Player player : getServer().getOnlinePlayers()) {
                        HackerCommand.updatePlayerHackerMode(player, false);
                    }
                }
            }, 10L);
        }

        ISDataBaseManager.mdb = mongoDatabase;
        setupMenus();

        final JavaPlugin plugin = this;
        final BungeeMessager messager = new BungeeMessager(this);
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                String ip = configFile.getString("redis.host");
                int port = configFile.getInt("redis.port");
                String password = configFile.getString("redis.password");
                if (password == null || password.isEmpty()) {
                    pool = new JedisPool(new JedisPoolConfig(), ip, port, 0);
                } else {
                    pool = new JedisPool(new JedisPoolConfig(), ip, port, 0, password);
                }

                // Subscribe on server thread.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        messager.subscribeInit();
                    }
                }.runTask(plugin);
            }
        });
    }

    @Override
    public void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
        }

        BungeeMessager.close();
        if (pool != null) {
            pool.close();
        }

        HackerCommand.restoreAllPlayerInventories();
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
        lifespan = configFile.getInt("reports.lifespan");
        this.saveDefaultConfig();
        this.saveConfig();
    }

    public MongoClient getMongoClient() {
        String ip = configFile.getString("mongo.host");
        int port = configFile.getInt("mongo.port");

        try {
            mongoClient = new MongoClient(ip, port);
            mongoClient.getDatabaseNames();
        } catch (MongoException ex) {
            getServer().getLogger().log(Level.SEVERE, "Could not connect to mongo database", ex);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }

        return mongoClient;
    }

    private void setupMenus() {
        MenuPage reports = new DisplayReportsMenu(true);
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

    private int lifespan = 0;

    public int getReportLifespan() {
        return lifespan;
    }
}
