package edu.cit.barcenas.queuems.feature.admin;

public class AdminCounterDTO {
    private String name;
    private String serviceType;
    private String status;
    private String assignedTellerId;

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
}
