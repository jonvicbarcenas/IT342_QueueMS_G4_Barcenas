package edu.cit.barcenas.queuems.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class ServiceRequestRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "service_requests";

    public ServiceRequestRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public void save(ServiceRequest request) throws ExecutionException, InterruptedException {
        DocumentReference docRef;
        if (request.getId() == null || request.getId().isEmpty()) {
            docRef = firestore.collection(COLLECTION_NAME).document();
            request.setId(docRef.getId());
        } else {
            docRef = firestore.collection(COLLECTION_NAME).document(request.getId());
        }
        ApiFuture<?> future = docRef.set(request);
        future.get();
    }

    public ServiceRequest findById(String id) throws ExecutionException, InterruptedException {
        var doc = firestore.collection(COLLECTION_NAME).document(id).get().get();
        if (!doc.exists())
            return null;
        return doc.toObject(ServiceRequest.class);
    }

    public List<ServiceRequest> findByUserId(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ServiceRequest.class))
                .collect(Collectors.toList());
    }

    public String findLatestQueueNumber() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .get();
        
        if (querySnapshot.isEmpty()) {
            return null;
        }
        
        return querySnapshot.getDocuments().get(0).getString("queueNumber");
    }

    public List<ServiceRequest> findByCounterIdAndStatus(String counterId, String status) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("counterId", counterId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ServiceRequest.class))
                .collect(Collectors.toList());
    }
}
