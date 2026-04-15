package edu.cit.barcenas.queuems.pattern.strategy;

import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutionException;

@Component
public class ContinuousNumberingStrategy implements QueueNumberStrategy {
    @Override
    public String generateNext(ServiceRequestRepository repository) throws ExecutionException, InterruptedException {
        String latestNumber = repository.findLatestQueueNumber();
        if (latestNumber == null) {
            return "Q001";
        }
        
        try {
            int currentNum = Integer.parseInt(latestNumber.substring(1));
            return String.format("Q%03d", currentNum + 1);
        } catch (Exception e) {
            return "Q001";
        }
    }
}
