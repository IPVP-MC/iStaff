package com.connorl.istaff.listeners;

import com.connorl.istaff.IStaff;
import com.gmail.xd.zwander.istaff.data.PlayerConnectionData;
import com.gmail.xd.zwander.istaff.data.TeleportRequest;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Zwander on 24/11/2015.
 */
public class BungeeMessager extends JedisPubSub implements PluginMessageListener {

    static IStaff plugin;
    static String[] serverList;
    static BungeeMessager instance;

    @Getter
    private static String serverName;

    @Getter
    private static boolean updateInProgress;

    @Getter
    private static Map<String, PlayerConnectionData> playerList = new HashMap<>();

    private static int uuidCounter = 0;

    public BungeeMessager(IStaff plugin) {
        BungeeMessager.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "RedisBungee");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "RedisBungee", this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        instance = this;
    }

    private static BukkitTask subscription;

    public void subscribeInit() {
        subscription = new BukkitRunnable() {
            @Override
            public void run() {
                Jedis jedis = plugin.getPool().getResource();
                try {
                    Bukkit.getLogger().info("Subscribing to \"iStaff\"...");
                    jedis.subscribe(instance, "istaff");
                    Bukkit.getLogger().info("iStaff subscription ended.");
                    plugin.getPool().returnResource(jedis);
                    subscription = null;
                    subscribe();
                } catch (Exception ex) {
                    plugin.getPool().returnResource(jedis);
                    Bukkit.getLogger().log(Level.SEVERE, "Subscribing failed. Please restart the server.", ex);
                    subscription = null;
                    subscribe();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public static void close() {
        instance.unsubscribe();
        subscription.cancel();
    }

    private static void sendChannelMessage(String channel, String message) {
        plugin.getLogger().info("---Channel Message Start---");
        Jedis jedis = plugin.getPool().getResource();
        plugin.getLogger().info("Resource located");
        message = channel + DELIMITER + message;
        jedis.publish("istaff", message);
        plugin.getLogger().info("Message published");
        plugin.getPool().returnResourceObject(jedis);
        plugin.getLogger().info("Resource returned");
        plugin.getLogger().info("---Channel Message Sent---");
    }

    private static final String DELIMITER = Character.toString((char) 31);

    @Override
    public void onMessage(String channel, String message) {
        plugin.getLogger().info("Message: " + message);
        String[] items = message.split(DELIMITER);
        String subChannel = items[0];
        items = Arrays.copyOfRange(items, 1, items.length);
        if (subChannel.equals("AdminMessage")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("istaff.true")) {
                    player.sendMessage(items[0]);
                }
            }

            return;
        }

        if (subChannel.equals("TeleRequest")) {
            plugin.getLogger().info("Teleport request has been received.");

            // Read the data in the same way you wrote it
            String id = "";
            String stringReq = "";
            String fromServer = "";
            String toServer = "";

            try {
                toServer = items[0];
                id = items[1];
                stringReq = items[2];
                fromServer = items[3];
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }

            if (!toServer.equals(serverName) || fromServer.isEmpty()) {
                return;
            }

            try {
                UUID uuid = UUID.fromString(id);
                TeleportRequest teleportRequest = TeleportRequest.deserialize(stringReq);
                ConnectionHandler.addTeleRequest(uuid, teleportRequest);
                sendTeleportRequestResponse(uuid, fromServer, true);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                return;
            }

            return;
        }

        if (subChannel.equals("TeleResponse")) {
            plugin.getLogger().info("Teleport response has been recieved");

            String id = "";
            Boolean success = false;
            String toServer = "";
            String fromServer = "";
            try {
                toServer = items[0];
                success = Boolean.parseBoolean(items[1]);
                id = items[2];
                fromServer = items[3];
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Read the data in the same way you wrote it
            if (!toServer.equals(serverName)) {
                return;
            }

            try {
                Player player = Bukkit.getPlayer(UUID.fromString(id));
                if (player != null) {
                    if (!success) {
                        player.sendMessage(ChatColor.RED + "Failed to teleport :(");
                    } else {
                        telePlayer(player, fromServer);
                    }
                }
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void sendPluginMessage(String subChannel, String argument) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        if (argument != null) {
            out.writeUTF(argument);
        }

        Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player != null) {
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }

    public void telePlayer(final Player player, String serverName) {
        plugin.getLogger().info("Attempting to teleport..");
        if (serverName.equals(BungeeMessager.serverName)) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    ConnectionHandler.forceTele(player);
                    plugin.getLogger().info("Local teleport attempted");
                }
            }, 1L);
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        plugin.getLogger().info("Server change attempted");
    }

    /**
     * DO NOT CALL FROM MAIN THREAD
     */
    public static void updatePlayerList() {
        if (!updateInProgress) {
            updateInProgress = true;
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServers");
            Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (player != null) {
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    updatePlayerListDirect();
                }
            }.runTaskLaterAsynchronously(plugin, 20L);
        }
    }

    public static void sendTeleReq(Player player, String server, TeleportRequest teleportRequest) {
        sendTeleReq(player, server, teleportRequest.serialize());
    }

    public static void sendTeleReq(Player player, String serverName, String serialisedTeleportRequest) {
        plugin.getLogger().info("Sending teleport request");
        String message = serverName + DELIMITER + player.getUniqueId().toString() + DELIMITER + serialisedTeleportRequest + DELIMITER + BungeeMessager.serverName;
        new MessageSender("TeleRequest", message).start();
    }

    /**
     * async
     *
     * @param message the message
     */
    public static void sendAdminMessage(String message) {
        Bukkit.getLogger().info("Admin message..");
        new MessageSender("AdminMessage", message).start();
    }

    public static void sendMessage(Player from, String playerName, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF(playerName);
        out.writeUTF(message);
        from.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private static void sendTeleportRequestResponse(UUID id, String server, boolean success) {
        String message = server + DELIMITER + success + DELIMITER + id.toString() + DELIMITER + serverName;
        plugin.getLogger().info("Teleport response is being sent.");
        new MessageSender("TeleResponse", message).start();
    }

    private static void updatePlayerListDirect() {
        for (String server : serverList) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerList");
            out.writeUTF(server);
            Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (player != null) {
                player.sendPluginMessage(plugin, "RedisBungee", out.toByteArray());
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("RedisBungee") && !channel.equals("BungeeCord")) {
            return;
        }

        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        String subChannel = input.readUTF();
        switch (subChannel) {
            case "GetServers":
                serverList = input.readUTF().split(", ");
                updatePlayerListDirect();
                break;
            case "PlayerList":
                String server = input.readUTF(); // The name of the server you got the player list of, as given in args.

                String[] playerList = input.readUTF().split(", ");
                for (String playerName : playerList) {
                    BungeeMessager.playerList.put(playerName, new PlayerConnectionData(playerName, server, null));
                    sendPluginMessage("UUIDOther", playerName);
                }

                break;
            case "UUIDOther":
                String playerName = input.readUTF();
                String uuid = input.readUTF();

                uuidCounter++;
                if (uuid.length() < 10) {
                    return;
                }

                StringBuilder sb = new StringBuilder(uuid);
                sb.insert(8, '-');
                sb.insert(13, '-');
                sb.insert(18, '-');
                sb.insert(23, '-');

                BungeeMessager.playerList.get(playerName).uuid = UUID.fromString(sb.toString());
                if (uuidCounter == BungeeMessager.playerList.size()) {
                    updateInProgress = false;
                    uuidCounter = 0;
                }

                break;
            case "GetServer":
                serverName = input.readUTF();
                plugin.getLogger().info("Server name set! : " + serverName);
                break;
        }
    }

    private static class MessageSender extends Thread {

        final String channel;
        final String message;

        MessageSender(String channel, String message) {
            this.message = message;
            this.channel = channel;
        }

        @Override
        public void run() {
            sendChannelMessage(channel, message);
            plugin.getLogger().info("Message has been sent: " + channel + " : " + message);
        }
    }
}