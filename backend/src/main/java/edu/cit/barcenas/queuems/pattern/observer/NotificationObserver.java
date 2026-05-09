package edu.cit.barcenas.queuems.pattern.observer;

import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.repository.UserRepository;
import edu.cit.barcenas.queuems.infrastructure.notification.EmailService;
import edu.cit.barcenas.queuems.infrastructure.notification.FcmService;
import org.springframework.stereotype.Component;

/**
 * A concrete observer that sends email and push notifications on status changes.
 */
@Component
public class NotificationObserver implements QueueStatusObserver {

    private final EmailService emailService;
    private final FcmService fcmService;
    private final UserRepository userRepository;

    public NotificationObserver(EmailService emailService, FcmService fcmService, UserRepository userRepository) {
        this.emailService = emailService;
        this.fcmService = fcmService;
        this.userRepository = userRepository;
    }

    @Override
    public void onStatusChange(ServiceRequest request) {
        try {
            if (ServiceRequest.STATUS_PENDING.equals(request.getStatus())) {
                return;
            }

            User user = userRepository.findByUid(request.getUserId());
            if (user == null) return;

            String subject = "QueueMS: Request Status Update";
            String message = String.format("Hello %s, your request #%s status has been updated to: %s",
                    user.getFirstname(), request.getQueueNumber(), request.getStatus());

            // Send Email
            emailService.sendSimpleMessage(user.getEmail(), subject, message);

            // Send Push Notification
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                fcmService.sendNotification(user.getFcmToken(), subject, message);
            }
        } catch (Exception e) {
            System.err.println("Failed to send notifications: " + e.getMessage());
        }
    }
}
