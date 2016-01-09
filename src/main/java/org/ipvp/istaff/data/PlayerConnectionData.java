package org.ipvp.istaff.data;

import java.util.UUID;

public class PlayerConnectionData {

    public String playerName;
    public String serverName;
    public UUID uuid;

    public PlayerConnectionData(String playerName, String serverName, UUID uuid) {
        this.playerName = playerName;
        this.serverName = serverName;
        this.uuid = uuid;
    }
}
