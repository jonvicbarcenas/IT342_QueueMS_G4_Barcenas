package edu.cit.barcenas.queuems.model;

import java.util.Date;

public class ServiceRequest {
    private String id;
    private String userId;
    private String counterId;
    private String counterName;
    private String serviceType;
    private String assignedTellerId;
    private String assignedTellerName;
    private String notes;
    private String status = STATUS_PENDING;
    private String queueNumber;
    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    public ServiceRequest() {
    }

    public ServiceRequest(
            String id,
            String userId,
            String counterId,
            String counterName,
            String serviceType,
            String assignedTellerId,
            String assignedTellerName,
            String notes,
            String status,
            String queueNumber,
            Date createdAt,
            Date updatedAt) {
        this.id = id;
        this.userId = userId;
        this.counterId = counterId;
        this.counterName = counterName;
        this.serviceType = serviceType;
        this.assignedTellerId = assignedTellerId;
        this.assignedTellerName = assignedTellerName;
        this.notes = notes;
        this.status = status;
        this.queueNumber = queueNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Possible Statuses
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SERVING = "SERVING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * Validates if the given status is a valid status
     * @param status the status to validate
     * @return true if the status is valid, false otherwise
     */
    public static boolean isValidStatus(String status) {
        return STATUS_PENDING.equals(status) || 
               STATUS_SERVING.equals(status) || 
               STATUS_COMPLETED.equals(status) || 
               STATUS_CANCELLED.equals(status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCounterId() {
        return counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(String queueNumber) {
        this.queueNumber = queueNumber;
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
