package com.gmail.xd.zwander.istaff.data;

import org.bson.types.ObjectId;

import java.util.Date;

public class ReportData {

    public final ObjectId reportID;
    public final Date reportedAt;
    public final String reason;
    public final String reportedName;
    public final String reporterName;
    public final String serverName;

    public ReportData(String reportedName, String reporterName, String serverName, ObjectId reportID, String reason, Date reportedAt) {
        this.reportID = reportID;
        this.reportedAt = reportedAt;
        this.reason = reason;
        this.reportedName = reportedName;
        this.reporterName = reporterName;
        this.serverName = serverName;
    }
}
