package org.ipvp.istaff.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.data.TeleportRequest;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

public class ConnectionHandler implements Listener {

    public ConnectionHandler(IStaff plugin) {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                Iterator<Entry<UUID, Entry<Long, TeleportRequest>>> iterator = HackerModeListener.teleportRequests.entrySet().iterator();
                long currentTime = System.currentTimeMillis();
                while (iterator.hasNext()) {
                    Entry<UUID, Entry<Long, TeleportRequest>> entry = iterator.next();
                    if (currentTime - entry.getValue().getKey() > 30000) {
                        iterator.remove();
                    }
                }
            }
        }, 60L, 60L);
    }

    public static void addTeleportRequest(UUID playerUUID, TeleportRequest teleportRequest) {
        Bukkit.getLogger().info("Added teleport request");
        HackerModeListener.teleportRequests.put(playerUUID, new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), teleportRequest));
    }

    public static void forceTeleport(Player player) {
        Entry<Long, TeleportRequest> entry = HackerModeListener.teleportRequests.remove(player.getUniqueId());
        if (entry != null) {
            TeleportRequest teleportRequest = entry.getValue();
            if (teleportRequest != null) {
                teleportRequest.teleport(player);
            }
        }
    }
}
