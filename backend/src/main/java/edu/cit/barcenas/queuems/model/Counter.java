package edu.cit.barcenas.queuems.model;

import java.util.Date;

public class Counter {
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    private String id;
    private String name;
    private String serviceType;
    private String status = STATUS_OPEN;
    private String assignedTellerId;
    private String assignedTellerName;
    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    public Counter() {
    }

    public Counter(String id, String name, String serviceType, String status, String assignedTellerId,
            String assignedTellerName, Date createdAt, Date updatedAt) {
        this.id = id;
        this.name = name;
        this.serviceType = serviceType;
        this.status = status;
        this.assignedTellerId = assignedTellerId;
        this.assignedTellerName = assignedTellerName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static boolean isValidStatus(String status) {
        return STATUS_OPEN.equals(status) || STATUS_CLOSED.equals(status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTellerId() {
        return assignedTellerId;
    }

    public void setAssignedTellerId(String assignedTellerId) {
        this.assignedTellerId = assignedTellerId;
    }

    public String getAssignedTellerName() {
        return assignedTellerName;
    }

    public void setAssignedTellerName(String assignedTellerName) {
        this.assignedTellerName = assignedTellerName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
