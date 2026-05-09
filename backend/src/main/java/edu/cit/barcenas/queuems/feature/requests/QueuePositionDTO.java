package edu.cit.barcenas.queuems.feature.requests;

public class QueuePositionDTO {
    private String requestId;
    private int position;
    private int totalActive;
    private int peopleAhead;
    private int estimatedWaitMinutes;

    public QueuePositionDTO() {
    }

    public QueuePositionDTO(String requestId, int position, int totalActive, int peopleAhead, int estimatedWaitMinutes) {
        this.requestId = requestId;
        this.position = position;
        this.totalActive = totalActive;
        this.peopleAhead = peopleAhead;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getTotalActive() {
        return totalActive;
    }

    public void setTotalActive(int totalActive) {
        this.totalActive = totalActive;
    }

    public int getPeopleAhead() {
        return peopleAhead;
    }

    public void setPeopleAhead(int peopleAhead) {
        this.peopleAhead = peopleAhead;
    }

    public int getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }
}
