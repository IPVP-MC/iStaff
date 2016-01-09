package org.ipvp.istaff.data;

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
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.menu.MenuServer;
import org.ipvp.istaff.util.BukkitSerialization;

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
public class IStaffDataBaseManager {

    private final DB db;

    public IStaffDataBaseManager(DB db) {
        this.db = db;
    }

    public boolean hasSentRecentReports(CommandSender sender, int maxCount, long inTime) {
        BasicDBObject object = new BasicDBObject("reported_by", sender.getName());
        object.append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - inTime)));

        long count = db.getCollection("reports").count(object);
        return count >= maxCount;
    }

    public void addReport(Player reported, CommandSender reporter, String reason) {
        BasicDBObject object = new BasicDBObject()
                .append("user_name", reported.getName())
                .append("server", IStaff.getPlugin().getBungeeMessager().getServerName())
                .append("reported_by", reporter.getName())
                .append("reason", reason)
                .append("at", new Date(System.currentTimeMillis()));

        db.getCollection("reports").insert(object);
    }

    public void resolveReport(ObjectId reportID) {
        db.getCollection("reports").findAndRemove(new BasicDBObject("_id", reportID));
    }

    public void resolveReport(String reportID) {
        resolveReport(new ObjectId(reportID));
    }

    public ReportData[] getUnresolvedReports() {
        DBCursor iterable = db.getCollection("reports").find();
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

    public Map<String, List<ReportData>> getUnresolvedReportsGroupPlayer() {
        DBCursor iterable = db.getCollection("reports").find();
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

            List<ReportData> list = ret.get(name);
            if (list == null) {
                list = new ArrayList<>();
                ret.put(name, list);
            }

            list.add(rdi);
        }

        return ret;
    }

    /**
     * Delete reports older than set set time.
     */
    public void closeOldReports() {
        long lifespan = IStaff.getPlugin().getReportLifespan() * TimeUnit.MINUTES.toMillis(1L);
        db.getCollection("reports").remove(new BasicDBObject("at", new BasicDBObject("$lt", new Date(System.currentTimeMillis() - lifespan))));
    }

    public ReportData getReport(ObjectId objectId) {
        BasicDBObject object = new BasicDBObject("_id", objectId);
        DBCursor iterable = db.getCollection("reports").find(object);
        if (!iterable.hasNext()) {
            return null;
        }

        DBObject res = iterable.next();
        Date at = (Date) res.get("at");
        String reason = (String) res.get("reason");
        String name = (String) res.get("user_name");
        String serverName = (String) res.get("server");
        String reporterName = ((String) res.get("reported_by"));
        return new ReportData(name, reporterName, serverName, objectId, reason, at);
    }

    public void savePlayerInventory(Player player) throws TimeoutException {
        PlayerInventory inventory = player.getInventory();
        String[] serialisedInventory = BukkitSerialization.playerInventoryToBase64(inventory);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        UUID uuid = player.getUniqueId();
        long time = System.currentTimeMillis();
        while (IStaff.getPlugin().getBungeeMessager().getServerName() == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                break;
            }

            if (System.currentTimeMillis() - time > 5000) {
                Bukkit.getLogger().log(Level.SEVERE, "COULD NOT RETRIEVE SERVER NAME. THIS MAY RESULT IN LOSS OF INVENTORIES");
                throw new TimeoutException("Server name can not be null!");
            }
        }

        BasicDBObject object = new BasicDBObject()
                .append("player_id", uuid.toString())
                .append("server", IStaff.getPlugin().getBungeeMessager().getServerName());

        BasicDBObject object2 = new BasicDBObject()
                .append("player_id", uuid.toString())
                .append("inv", serialisedInventory[0])
                .append("armor", serialisedInventory[1])
                .append("server", IStaff.getPlugin().getBungeeMessager().getServerName())
                .append("at", timeStamp);

        db.getCollection("saved_inventories").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public FauxPlayerInventory loadPlayerInventory(UUID uuid, String serverName) {
        BasicDBObject object = new BasicDBObject()
                .append("player_id", uuid.toString())
                .append("server", (serverName));

        DBCursor iterable = db.getCollection("saved_inventories").find(object);
        DBObject object2 = iterable.hasNext() ? iterable.next() : null;

        try {
            return new FauxPlayerInventory(BukkitSerialization.fromBase64((String) object2.get("inv")), BukkitSerialization.itemStackArrayFromBase64((String) object2.get("armor")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public FauxPlayerInventory loadPlayerInventory(Player player) throws TimeoutException {
        if (DEBUG) {
            return null;
        }

        long time = System.currentTimeMillis();
        while (IStaff.getPlugin().getBungeeMessager().getServerName() == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                break;
            }

            if (System.currentTimeMillis() - time > 5000) {
                Bukkit.getLogger().log(Level.SEVERE, "COULD NOT RETRIEVE SERVER NAME. THIS MAY RESULT IN LOSS OF INVENTORIES");
                throw new TimeoutException("Servername can not be null!");
            }
        }

        BasicDBObject object = new BasicDBObject();
        object.put("player_id", player.getUniqueId().toString());
        object.put("server", IStaff.getPlugin().getBungeeMessager().getServerName());

        DBCursor iterable = db.getCollection("saved_inventories").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        if (res != null) {
            try {
                return new FauxPlayerInventory(BukkitSerialization.fromBase64((String) res.get("inv")), BukkitSerialization.itemStackArrayFromBase64((String) res.get("armor")));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    public void setHackerMode(UUID playerUUID, boolean mode) {
        if (true) {
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        BasicDBObject object = new BasicDBObject().append("_id", playerUUID.toString());
        BasicDBObject object2 = new BasicDBObject().append("hacker_mode", (mode)).append("at", (timeStamp));
        db.getCollection("player_hacker_modes").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public void setURL(UUID playerUUID, String url) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        BasicDBObject object = new BasicDBObject().append("_id", playerUUID.toString());
        BasicDBObject object2 = new BasicDBObject().append("url", (url)).append("at", (timeStamp));
        db.getCollection("player_url").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public static boolean DEBUG = true;

    public PlayerHackerMode getHackerMode(UUID playerUUID) {
        if (DEBUG) {
            return null;
        }

        BasicDBObject object = new BasicDBObject();
        object.put("_id", playerUUID.toString());
        DBCursor iterable = db.getCollection("player_hacker_modes").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        return res == null ? null : new PlayerHackerMode(UUID.fromString((String) res.get("_id")), (Boolean) res.get("hacker_mode"));
    }

    public void setPlayerReturnLocation(UUID playerUUID, TeleportRequest teleportRequest, String server) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        BasicDBObject object = new BasicDBObject()
                .append("_id", playerUUID.toString());
        BasicDBObject object2 = new BasicDBObject()
                .append("telerequest", teleportRequest.serialize())
                .append("server", server)
                .append("at", (timeStamp));
        db.getCollection("player_returns").update(object, new BasicDBObject("$set", object2), true, false);
    }

    public String[] getPlayerReturnLocation(UUID playerUUID) {
        BasicDBObject object = new BasicDBObject("_id", playerUUID.toString());
        DBCursor iterable = db.getCollection("player_returns").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        try {
            return new String[]{(String) res.get("telerequest"), (String) res.get("server")};
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public List<String> getPlayerComplaints(UUID uuid) {
        BasicDBObject object = new BasicDBObject().append("user_id", uuid.toString());
        DBCursor iterable = db.getCollection("reports").find(object);
        final List<String> ret = new ArrayList<>(iterable.size());

        DBObject current;
        while (iterable.hasNext()) {
            current = iterable.next();
            String reason = (String) current.get("reason");
            ret.add(reason);
        }

        return ret;
    }

    public void resolveAllReports(String playername) {
        BasicDBObject object = new BasicDBObject("user_name", playername);
        db.getCollection("reports").remove(object);
    }

    /**
     * Runs async
     *
     * @param player the player
     */
    public void serveSavedMenu(final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String url = getURL(player);
                MenuServer.serveMenu(player, url, false);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    protected String getURL(Player player) {
        BasicDBObject object = new BasicDBObject("_id", player.getUniqueId().toString());
        DBCursor iterable = db.getCollection("player_url").find(object);
        DBObject res = iterable.hasNext() ? iterable.next() : null;
        return res != null ? (String) res.get("url") : null;
    }

    public void addResolvedReportTally(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString())
                .append("player_name", player.getName().toLowerCase())
                .append("at", new Date(System.currentTimeMillis()));
        db.getCollection("admin_report_tally").insert(object);
    }

    public long getTotalResolvedReports(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString());
        return db.getCollection("admin_report_tally").count(object);
    }

    public long getMonthResolvedReports(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(30)))));
        return db.getCollection("admin_report_tally").count(object);
    }

    public long getWeekResolvedReports(Player player) {
        BasicDBObject object = new BasicDBObject("player_id", player.getUniqueId().toString())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(7)))));
        return db.getCollection("admin_report_tally").count(object);
    }

    public long getTotalResolvedReports(String playerName) {
        BasicDBObject object = new BasicDBObject("player_name", playerName.toLowerCase());
        return db.getCollection("admin_report_tally").count(object);
    }

    public long getMonthResolvedReports(String playerName) {
        BasicDBObject object = new BasicDBObject("player_name", playerName.toLowerCase())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(30)))));
        return db.getCollection("admin_report_tally").count(object);
    }

    public long getWeekResolvedReports(String playerName) {
        BasicDBObject object = new BasicDBObject("player_name", playerName.toLowerCase())
                .append("at", new BasicDBObject("$gt", new Date(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(7)))));
        return db.getCollection("admin_report_tally").count(object);
    }
}
