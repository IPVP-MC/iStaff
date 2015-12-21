package com.connorl.istaff;

import com.connorl.istaff.listeners.BungeeMessager;
import com.connorl.istaff.listeners.HackerModeListener;
import com.gmail.xd.zwander.istaff.data.FauxPlayerInventory;
import com.gmail.xd.zwander.istaff.data.PlayerHackerMode;
import com.gmail.xd.zwander.istaff.data.ReportData;
import com.gmail.xd.zwander.istaff.data.TeleportRequest;
import com.gmail.xd.zwander.menu.MenuServer;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Allows external objects to reference the database without fear of changing it.
 * Contains relevant methods for modifying the iStaff DB
 *
 * @author zwander.xd@gmail.com
 */
public abstract class ISDataBaseManager {

    protected static DB mdb;

    public static boolean hasSentRecentReports(CommandSender sender, int maxCount, long inTime) {
        BasicDBObject object = new BasicDBObject("reported_by", sender.getName());
        object.append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - inTime)));

        long count = mdb.getCollection("reports").count(object);
        return count >= maxCount;
    }

    public static void addReport(Player reported, CommandSender reporter, String reason) {
        BasicDBObject object = new BasicDBObject()
                .append("user_name", reported.getName())
                .append("server", BungeeMessager.getServerName())
                .append("reported_by", reporter.getName())
                .append("reason", reason)
                .append("at", new Date(System.currentTimeMillis()));

        mdb.getCollection("reports").insert(object);
    }

    public static void resolveReport(ObjectId reportID) {
        mdb.getCollection("reports").findAndRemove(new BasicDBObject("_id", reportID));
    }

    public static void resolveReport(String reportID) {
        ObjectId oid = new ObjectId(reportID);
        resolveReport(oid);
    }

    public static ReportData[] getUnresolvedReports() {
        DBCursor iterable = mdb.getCollection("reports").find();
        final List<ReportData> ret = new ArrayList<>();
        while (iterable.hasNext()) {
            DBObject object = iterable.next();
            ObjectId objectId = (ObjectId) object.get("_id");
            String reporterName = ((String) object.get("reported_by"));
            Date at = (Date) object.get("at");
            String reason = (String) object.get("reason");
            String name = (String) object.get("user_name");
            String server = (String) object.get("server");
            ReportData reportData = new ReportData(name, reporterName, server, objectId, reason, at);
            ret.add(reportData);
        }

        return ret.toArray(new ReportData[ret.size()]);
    }

    public static Map<String, List<ReportData>> getUnresolvedReportsGroupPlayer() {
        DBCursor iterable = mdb.getCollection("reports").find();
        final Map<String, List<ReportData>> ret = new HashMap<>();
        while (iterable.hasNext()) {
            DBObject object = iterable.next();
            ObjectId objectId = (ObjectId) object.get("_id");
            String reporterName = ((String) object.get("reported_by"));
            Date at = (Date) object.get("at");
            String reason = (String) object.get("reason");
            String name = (String) object.get("user_name");
            String server = (String) object.get("server");
            ReportData rdi = new ReportData(name, reporterName, server, objectId, reason, at);
            if (ret.get(name) == null) {
                ret.put(name, new ArrayList<>());
            }

            ret.get(name).add(rdi);
        }

        return ret;
    }

    /**
     * Delete reports older than set set time.
     */
    public static void closeOldReports() {
        long lifespan = IStaff.getPlugin().getReportLifespan() * 60000;
        mdb.getCollection("reports").remove(new BasicDBObject("at", new BasicDBObject("$lt", new Date(System.currentTimeMillis() - lifespan))));
    }

    public static ReportData getReport(ObjectId objectId) {
        BasicDBObject object = new BasicDBObject("_id", objectId);
        DBCursor iterable = mdb.getCollection("reports").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        if (res == null) {
            return null;
        }

        Date at = (Date) res.get("at");
        String reason = (String) res.get("reason");
        String name = (String) res.get("user_name");
        String serverName = (String) res.get("server");
        String reporterName = ((String) res.get("reported_by"));
        return new ReportData(name, reporterName, serverName, objectId, reason, at);
    }

    public static void savePlayerInventory(Player player) throws TimeoutException {
        PlayerInventory inventory = player.getInventory();
        String[] serialisedInventory = BukkitSerialization.playerInventoryToBase64(inventory);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        UUID uuid = player.getUniqueId();
        long time = System.currentTimeMillis();
        while (BungeeMessager.getServerName() == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }

            if (System.currentTimeMillis() - time > 5000) {
                Bukkit.getLogger().log(Level.SEVERE, "COULD NOT RETRIEVE SERVER NAME. THIS MAY RESULT IN LOSS OF INVENTORIES");
                throw new TimeoutException("Servername can not be null!");
            }
        }

        BasicDBObject object = new BasicDBObject()
                .append("player_id", uuid.toString())
                .append("server", BungeeMessager.getServerName());
        BasicDBObject object2 = new BasicDBObject()
                .append("player_id", uuid.toString())
                .append("inv", serialisedInventory[0])
                .append("armor", serialisedInventory[1])
                .append("server", BungeeMessager.getServerName())
                .append("at", timeStamp);
        mdb.getCollection("saved_inventories").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public static FauxPlayerInventory loadPlayerInventory(UUID uuid, String serverName) {
        BasicDBObject object = new BasicDBObject()
                .append("player_id", uuid.toString())
                .append("server", (serverName));

        DBCursor iterable = mdb.getCollection("saved_inventories").find(object);
        DBObject object2 = iterable.hasNext() ? iterable.next() : null;

        try {
            return new FauxPlayerInventory(BukkitSerialization.fromBase64((String) object2.get("inv")), BukkitSerialization.itemStackArrayFromBase64((String) object2.get("armor")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static FauxPlayerInventory loadPlayerInventory(Player player) throws TimeoutException {
        long time = System.currentTimeMillis();
        while (BungeeMessager.getServerName() == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }

            if (System.currentTimeMillis() - time > 5000) {
                Bukkit.getLogger().log(Level.SEVERE, "COULD NOT RETRIEVE SERVER NAME. THIS MAY RESULT IN LOSS OF INVENTORIES");
                throw new TimeoutException("Servername can not be null!");
            }
        }

        BasicDBObject object = new BasicDBObject();
        //		.append("player_id",new BasicDBObject("$eq",(p.getUniqueId().toString())))
        //		.append("server", (BungeeMessager.getServerName()));
        object.put("player_id", player.getUniqueId().toString());
        object.put("server", BungeeMessager.getServerName());

        DBCursor iterable = mdb.getCollection("saved_inventories").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        if (res == null) {
            return null;
        }

        try {
            return new FauxPlayerInventory(BukkitSerialization.fromBase64((String) res.get("inv")), BukkitSerialization.itemStackArrayFromBase64((String) res.get("armor")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static void setHackerMode(Player player, boolean mode) {
        UUID uuid = player.getUniqueId();
        setHackerMode(uuid, mode);
    }

    public static void setURL(Player p, String url) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        UUID uuid = p.getUniqueId();
        BasicDBObject object = new BasicDBObject()
                .append("_id", uuid.toString());
        BasicDBObject object2 = new BasicDBObject()
                //.append("$setOnInsert",new BasicDBObject("_id",(id.toString())))
                .append("url", (url))
                .append("at", (timeStamp));
        mdb.getCollection("player_url").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public static void setHackerMode(PlayerHackerMode playerHackerMode) {
        setHackerMode(playerHackerMode.playerId, playerHackerMode.hackerMode);
    }

    private static void setHackerMode(UUID uuid, boolean mode) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        BasicDBObject object = new BasicDBObject()
                .append("_id", uuid.toString());
        BasicDBObject object2 = new BasicDBObject()
                //.append("$setOnInsert",new BasicDBObject("_id",(id.toString())))
                .append("hacker_mode", (mode))
                .append("at", (timeStamp));
        mdb.getCollection("player_hacker_modes").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public static PlayerHackerMode getHackerMode(Player player) {
        return getHackerMode(player.getUniqueId());
    }

    public static PlayerHackerMode getHackerMode(UUID uuid) {
        BasicDBObject d = new BasicDBObject();//("_id",new BasicDBObject("$eq",(p.toString())));
        d.put("_id", uuid.toString());
        DBCursor iterable = mdb.getCollection("player_hacker_modes").find(d);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        return res == null ? null : new PlayerHackerMode(UUID.fromString((String) res.get("_id")), (Boolean) res.get("hacker_mode"));
    }

    public static void setPlayerReturnLocation(UUID playerUUID, TeleportRequest teleportRequest, String server) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        BasicDBObject object = new BasicDBObject()
                .append("_id", playerUUID.toString());
        BasicDBObject object2 = new BasicDBObject()
                //.append("$setOnInsert",new BasicDBObject("_id",(playerid.toString())))
                .append("telerequest", teleportRequest.serialize())
                .append("server", server)
                .append("at", (timeStamp));
        mdb.getCollection("player_returns").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public static String[] getPlayerReturnLocation(UUID playerUUID) {
        BasicDBObject object = new BasicDBObject("_id", playerUUID.toString());
        DBCursor iterable = mdb.getCollection("player_returns").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        try {
            return new String[]{(String) res.get("telerequest"), (String) res.get("server")};
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getPlayerComplaints(UUID uuid) {
        BasicDBObject object = new BasicDBObject().append("user_id", uuid.toString());
        DBCursor iterable = mdb.getCollection("reports").find(object);
        final List<String> ret = new ArrayList<>();

        DBObject current;
        while (iterable.hasNext()) {
            current = iterable.next();
            String reason = (String) current.get("reason");
            ret.add(reason);
        }

        return ret;
    }

    public static void resolveAllReports(String playername) {
        BasicDBObject object = new BasicDBObject("user_name", playername);
        mdb.getCollection("reports").remove(object);
    }

    /**
     * Runs async
     *
     * @param player the player
     */
    public static void serveSavedMenu(final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String url = ISDataBaseManager.getURL(player);
                MenuServer.serveMenu(player, url, false);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    protected static String getURL(Player player) {
        BasicDBObject object = new BasicDBObject("_id", player.getUniqueId().toString());
        DBCursor iterable = mdb.getCollection("player_url").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        return res == null ? null : (String) res.get("url");
    }

    public static void addResolvedReportTally(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString())
                .append("player_name", player.getName().toLowerCase())
                .append("at", new Date(System.currentTimeMillis()));
        mdb.getCollection("admin_report_tally").insert(object);
    }

    public static long getTotalResolvedReports(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString());
        return mdb.getCollection("admin_report_tally").count(object);
    }

    public static long getMonthResolvedReports(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(30)))));
        return mdb.getCollection("admin_report_tally").count(object);
    }

    public static long getWeekResolvedReports(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(7)))));
        return mdb.getCollection("admin_report_tally").count(object);
    }

    public static long getTotalResolvedReports(String playerName) {
        BasicDBObject object = new BasicDBObject("player_name", playerName.toLowerCase());
        return mdb.getCollection("admin_report_tally").count(object);
    }

    public static long getMonthResolvedReports(String playerName) {
        BasicDBObject object = new BasicDBObject("player_name", playerName.toLowerCase())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(30)))));
        return mdb.getCollection("admin_report_tally").count(object);
    }

    public static long getWeekResolvedReports(String playerName) {
        BasicDBObject object = new BasicDBObject("player_name", playerName.toLowerCase())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(7)))));
        return mdb.getCollection("admin_report_tally").count(object);
    }
}
