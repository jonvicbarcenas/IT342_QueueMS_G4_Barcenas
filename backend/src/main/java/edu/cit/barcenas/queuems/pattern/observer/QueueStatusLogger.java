package edu.cit.barcenas.queuems.pattern.observer;

import edu.cit.barcenas.queuems.model.ServiceRequest;
import org.springframework.stereotype.Component;

/**
 * A concrete observer that logs status changes to the console.
 */
@Component
public class QueueStatusLogger implements QueueStatusObserver {
    @Override
    public void onStatusChange(ServiceRequest request) {
        System.out.println("Status of request " + request.getId() + " changed to: " + request.getStatus());
    }
}
