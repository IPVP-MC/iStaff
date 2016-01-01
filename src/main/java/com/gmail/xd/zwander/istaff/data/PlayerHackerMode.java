package com.gmail.xd.zwander.istaff.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PlayerHackerMode {

    private final UUID playerUUID;
    public final boolean hackerMode; //TODO: Make private, currently compat for iHCF
}
