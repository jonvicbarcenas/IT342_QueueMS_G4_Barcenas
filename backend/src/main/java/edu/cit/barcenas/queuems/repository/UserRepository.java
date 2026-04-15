package edu.cit.barcenas.queuems.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import edu.cit.barcenas.queuems.model.User;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    public User findByEmail(String email) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .get();
        
        if (querySnapshot.isEmpty()) {
            return null;
        }
        
        return querySnapshot.getDocuments().get(0).toObject(User.class);
    }

    public List<User> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection("users").get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(User.class))
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<User> findByRole(String role) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection("users")
                .whereEqualTo("role", role)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(User.class))
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public void delete(String uid) throws ExecutionException, InterruptedException {
        firestore.collection("users").document(uid).delete().get();
    }
}
