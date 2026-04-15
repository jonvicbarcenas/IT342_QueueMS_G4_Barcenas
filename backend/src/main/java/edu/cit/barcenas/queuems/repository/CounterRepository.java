package edu.cit.barcenas.queuems.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import edu.cit.barcenas.queuems.model.Counter;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class CounterRepository {

    private static final String COLLECTION_NAME = "counters";

    private final Firestore firestore;

    public CounterRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public void save(Counter counter) throws ExecutionException, InterruptedException {
        DocumentReference docRef;
        if (counter.getId() == null || counter.getId().isEmpty()) {
            docRef = firestore.collection(COLLECTION_NAME).document();
            counter.setId(docRef.getId());
        } else {
            docRef = firestore.collection(COLLECTION_NAME).document(counter.getId());
        }

        ApiFuture<?> future = docRef.set(counter);
        future.get();
    }

    public Counter findById(String id) throws ExecutionException, InterruptedException {
        var doc = firestore.collection(COLLECTION_NAME).document(id).get().get();
        if (!doc.exists()) {
            return null;
        }
        return doc.toObject(Counter.class);
    }

    public List<Counter> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Counter.class))
                .sorted(Comparator.comparing(Counter::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Counter> findByStatus(String status) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Counter.class))
                .sorted(Comparator.comparing(Counter::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION_NAME).document(id).delete().get();
    }
}
