package edu.cit.barcenas.queuems.pattern.observer;

import edu.cit.barcenas.queuems.model.ServiceRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * A concrete observer that broadcasts queue status changes via WebSockets.
 */
@Component
public class WebSocketQueueObserver implements QueueStatusObserver {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketQueueObserver(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onStatusChange(ServiceRequest request) {
        // Broadcast the updated request to a specific topic
        // Clients can subscribe to /topic/queue to get real-time updates
        messagingTemplate.convertAndSend("/topic/queue", request);
        
        // Also broadcast to a user-specific topic if needed
        messagingTemplate.convertAndSend("/topic/user/" + request.getUserId(), request);
        
        // Also broadcast to a counter-specific topic
        messagingTemplate.convertAndSend("/topic/counter/" + request.getCounterId(), request);
    }
}
