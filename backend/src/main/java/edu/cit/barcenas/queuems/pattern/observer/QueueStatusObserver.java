package edu.cit.barcenas.queuems.pattern.observer;

import edu.cit.barcenas.queuems.model.ServiceRequest;

/**
 * Interface for observers that watch for status changes in the queue.
 */
public interface QueueStatusObserver {
    void onStatusChange(ServiceRequest request);
}
