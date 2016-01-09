package org.ipvp.istaff.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PlayerHackerMode {

    private final UUID playerUUID;
    public boolean hackerMode; //TODO: Make private, currently compat for iHCF
}
