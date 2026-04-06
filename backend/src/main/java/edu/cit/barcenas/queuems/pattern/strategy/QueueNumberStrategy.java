package edu.cit.barcenas.queuems.pattern.strategy;

import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import java.util.concurrent.ExecutionException;

public interface QueueNumberStrategy {
    String generateNext(ServiceRequestRepository repository) throws ExecutionException, InterruptedException;
}
