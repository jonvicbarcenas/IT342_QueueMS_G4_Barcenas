package edu.cit.barcenas.queuems.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import edu.cit.barcenas.queuems.model.User;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {

    private final Firestore firestore;

    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public void save(User user) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("users").document(user.getUid());
        ApiFuture<?> future = docRef.set(user);
        future.get();
    }

    public User findByUid(String uid) throws ExecutionException, InterruptedException {
        var doc = firestore.collection("users").document(uid).get().get();
        if (!doc.exists())
            return null;
        return doc.toObject(User.class);
    }
}
