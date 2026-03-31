package edu.cit.barcenas.queuems.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {
    private String id;
    private String userId;
    private String counterId;
    private String status = STATUS_PENDING;
    private String queueNumber;
    private Date createdAt = new Date();

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
}
