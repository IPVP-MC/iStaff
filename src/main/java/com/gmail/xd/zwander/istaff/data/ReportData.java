package com.gmail.xd.zwander.istaff.data;

import org.bson.types.ObjectId;

import java.util.Date;

public class ReportData {

    public final ObjectId reportID;
    public final Date reportedAt;
    public final String reason;
    public final String name;
    public final String reporter;
    public final String server;

    public ReportData(String reportedName, String reporterName, String serverName, ObjectId id, String reason, Date at) {
        this.name = reportedName;
        this.server = serverName;
        this.reporter = reporterName;
        reportID = id;
        reportedAt = at;
        this.reason = reason;
    }
}
