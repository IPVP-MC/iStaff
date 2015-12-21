package com.gmail.xd.zwander.istaff.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeleportRequest {

    private static final int PREFIX_LENGTH = 3;
    private static final String LOC_SEPARATOR = ",";
    private static final String PART_SEPARATOR = "~";

    public final UUID playerTarget;
    public final String playerName;
    public final Location locationTarget;
    public final String reason;

    public TeleportRequest(UUID uuid, Location location, String playerName, String reason) {
        this.playerTarget = uuid;
        this.locationTarget = location;
        this.playerName = playerName;
        this.reason = reason;
    }

    @SuppressWarnings("all")
    public String serialize() {
        StringBuilder builder = new StringBuilder();
        builder.append("pt:" + playerTarget.toString());
        builder.append(PART_SEPARATOR);
        builder.append("pn:" + playerName);
        builder.append(PART_SEPARATOR);
        builder.append("lt:" + locationTarget.getWorld().getName() + LOC_SEPARATOR + locationTarget.getX() + LOC_SEPARATOR + locationTarget.getY() +
                LOC_SEPARATOR + locationTarget.getZ() + LOC_SEPARATOR + locationTarget.getYaw() + LOC_SEPARATOR + locationTarget.getPitch());
        builder.append(PART_SEPARATOR);
        builder.append("re:" + reason);
        return builder.toString();
    }

    public static TeleportRequest deserialize(String string) {
        UUID playerTarget = null;
        String playerName = null;
        Location locationTarget = null;
        String reason = null;

        String[] parts = string.split(PART_SEPARATOR);
        if (parts.length == 4) {
            for (String part : parts) {
                // Not a valid serialised string
                if (part.length() <= PREFIX_LENGTH) {
                    return null;
                }

                String type = part.substring(0, PREFIX_LENGTH - 1);
                String section = part.substring(PREFIX_LENGTH, part.length());
                if (type.equals("pt")) {
                    try {
                        playerTarget = UUID.fromString(section);
                    } catch (IllegalArgumentException ignored) {
                    }
                } else if (type.equals("pn")) {
                    if (section.length() <= 16) playerName = section;
                } else if (type.equals("lt")) {
                    if (section.contains(LOC_SEPARATOR)) {
                        String[] locationParts = section.split(LOC_SEPARATOR);
                        try {
                            World world = Bukkit.getWorld(locationParts[0]);
                            double x = Double.parseDouble(locationParts[1]);
                            double y = Double.parseDouble(locationParts[2]);
                            double z = Double.parseDouble(locationParts[3]);
                            float yaw = Float.parseFloat(locationParts[4]);
                            float pitch = Float.parseFloat(locationParts[5]);
                            locationTarget = new Location(world, x, y, z, yaw, pitch);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                } else if (type.equals("re")) {
                    reason = section;
                }
            }
        }

        if (playerTarget != null && playerName != null && locationTarget != null && reason != null) {
            return new TeleportRequest(playerTarget, locationTarget, playerName, reason);
        }

        return null;
    }

    public void teleport(Player player) {
        if (playerTarget != null) {
            if (Bukkit.getPlayer(playerTarget) != null) {
                player.teleport(Bukkit.getPlayer(playerTarget));
            } else {
                player.sendMessage("The player you were attempting to TP to has disconnected...");
            }
        } else if (locationTarget != null)
            player.teleport(locationTarget);
        else {
            if (playerName != null) {
                if (Bukkit.getPlayer(playerName) != null) {
                    player.teleport(Bukkit.getPlayer(playerName));
                } else {
                    player.sendMessage("The player you were attempting to TP to has disconnected...");
                }
            }
        }
    }
}
