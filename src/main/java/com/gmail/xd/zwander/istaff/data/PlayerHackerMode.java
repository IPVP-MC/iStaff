package com.gmail.xd.zwander.istaff.data;

import java.util.UUID;

public class PlayerHackerMode {

    public UUID playerId;
    public boolean hackerMode;

    public PlayerHackerMode(UUID player, boolean hackerMode) {
        super();
        this.playerId = player;
        this.hackerMode = hackerMode;
    }
}
