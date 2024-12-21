package edu.brown.cs.student.storage;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Query.Direction;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/** Utility Class for interacting with Firebase Firestore */
public class FirestoreUtil implements IStorage {

  private Firestore db;
  private final String firebaseConfigPath = "src/main/resources/firebase_config.json";

  public FirestoreUtil() throws IOException {
    try {
      if (FirebaseApp.getApps().isEmpty()) {
        // Input stream creation
        InputStream serviceAccount = new FileInputStream(this.firebaseConfigPath);

        // More detailed credential loading
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();

        // initialize firebase and get firestore
        FirebaseApp.initializeApp(options);
        this.db = FirestoreClient.getFirestore();
        System.out.println("Firestore successfully set up.");
      }

    } catch (IOException e) {
      System.err.println("Firebase Initialization Error:");
      e.printStackTrace();
      throw e; // Re-throw to maintain original method signature
    }
  }

  // adds a document or OVERWRITES it if it already exists
  @Override
  public void addDocument(String collection_id, String doc_id, Map<String, Object> data)
      throws IllegalArgumentException, ExecutionException, InterruptedException {
    if (collection_id == null || doc_id == null || data == null) {
      throw new IllegalArgumentException(
          "addDocument: collection_id, doc_id, or data cannot be null");
    }
    DocumentReference docRef = this.db.collection(collection_id).document(doc_id);
    ApiFuture<WriteResult> future = docRef.set(data);
    System.out.println("Update time : " + future.get().getUpdateTime());
  }

  // Differs from adding a document because it ADDS to the existing documents, and only overwrites
  // specified fields
  @Override
  public void updateDocument(String collection_id, String doc_id, Map<String, Object> data)
      throws IllegalArgumentException {
    if (collection_id == null || doc_id == null || data == null) {
      throw new IllegalArgumentException(
          "updateDocument: collection_id, doc_id, or data cannot be null");
    }
    DocumentReference docRef = this.db.collection(collection_id).document(doc_id);
    docRef.set(data, SetOptions.merge());
  }

  @Override
  public Map<String, Object> getDocument(String collection_id, String doc_id)
      throws InterruptedException, ExecutionException {
    DocumentReference docRef = this.db.collection(collection_id).document(doc_id);
    // asynchronously retrieve the document
    ApiFuture<DocumentSnapshot> future = docRef.get();
    // future.get() blocks on response
    DocumentSnapshot document = future.get();
    if (document.exists()) {
      return document.getData();
    } else {
      System.out.println("No such document: " + doc_id + " in collection " + collection_id);
    }
    return null;
  }

  @Override
  public void deleteDocument(String collectionID, String docID)
      throws InterruptedException, ExecutionException {
    if (docID == null) {
      throw new IllegalArgumentException("deleteDocument: docID cannot be null");
    }
    if (collectionID == null) {
      throw new IllegalArgumentException("deleteDocument: collectionID cannot be null");
    }
    DocumentReference document = this.db.collection(collectionID).document(docID);
    ApiFuture<WriteResult> deleteFuture = document.delete(); // Firestore returns a future
    WriteResult result = deleteFuture.get(); // Wait for the operation to complete
    System.out.println("Document deleted at: " + result.getUpdateTime());
  }

  @Override
  public List<Map<String, Object>> getCollection(String collectionID)
      throws ExecutionException, InterruptedException {
    if (collectionID == null) {
      throw new IllegalArgumentException("getCollection: collectionId cannot be null");
    }
    // asynchronously retrieve all documents
    ApiFuture<QuerySnapshot> future = this.db.collection(collectionID).get();
    // future.get() blocks on response
    List<QueryDocumentSnapshot> documents = future.get().getDocuments();
    List<Map<String, Object>> collection = new ArrayList<>();
    for (QueryDocumentSnapshot document : documents) {
      collection.add(document.getData());
    }
    return collection; // note this could return null I think
  }

  @Override
  public long getDocumentCount(String collectionID)
      throws InterruptedException, ExecutionException {
    // Create an aggregation query
    ApiFuture<AggregateQuerySnapshot> future = this.db.collection(collectionID).count().get();
    // Get the count
    AggregateQuerySnapshot snapshot = future.get();
    return snapshot.getCount();
  }

  @Override
  public List<Map<String, Object>> getProblems(String difficulty, int number)
      throws InterruptedException, ExecutionException, IOException {

    // Reference to the collection and create a query
    Query query = db.collection("Problems").whereEqualTo("difficulty", difficulty);
    // Execute the query
    ApiFuture<QuerySnapshot> querySnapshot = query.get();
    // Get the query results
    List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

    List<Map<String, Object>> selectedProblems = new ArrayList<>();
    // get the size of the document
    int docSize = documents.size();
    // if no question of this difficulty found at all,
    if (docSize == 0) {
      return new ArrayList<>();
    }
    // return all that we have if not enough questions of this difficulty level.
    number = docSize < number ? docSize : number;

    IntStream randomInts = new Random().ints(number, 0, docSize);
    for (int num : randomInts.toArray()) {
      // get document and convert it to string and then deserialize it
      Map<String, Object> doc = documents.get(num).getData();
      selectedProblems.add(doc);
    }
    return selectedProblems;
  }

  @Override
  public Map<String, Object> getProblem(String problemID)
      throws InterruptedException, ExecutionException, IOException {
    Query query = db.collection("Problems").whereEqualTo("problemID", problemID);
    ApiFuture<QuerySnapshot> querySnapshot = query.get();
    List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
    return documents.get(0).getData();
  }

  @Override
  public List<Map<String, Object>> sortCollection(String collectionID, String field)
      throws InterruptedException, ExecutionException, IOException {
    if (field == null) {
      throw new IllegalArgumentException("sortCollection: field cannot be null");
    }
    if (collectionID == null) {
      throw new IllegalArgumentException("collectionID: collectionID cannot be null");
    }
    Query query = db.collection(collectionID).orderBy(field, Direction.DESCENDING);
    List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();
    List<Map<String, Object>> resultList = new ArrayList<>();
    for (QueryDocumentSnapshot document : documents) {
      resultList.add(document.getData());
    }
    return resultList;
  }
}
