package edu.brown.cs.student.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MockStorage implements IStorage {

  private Map<String, Map<String, Object>> rooms;
  private Map<String, Map<String, Object>> users;

  public MockStorage() {
    this.rooms = new HashMap<>();
    this.users = new HashMap<>();
  }

  // this should add an element to one of the instance variable maps. The collection_id is what
  // tells you which one it should go to. Then the doc ID is the string value in the map, and
  // the data becomes the object.
  @Override
  public void addDocument(String collection_id, String doc_id, Map<String, Object> data)
      throws IllegalArgumentException {
    if (collection_id == null || doc_id == null || data == null) {
      throw new IllegalArgumentException("Collection ID, document ID, and data cannot be null.");
    }

    Map<String, Map<String, Object>> targetCollection = getCollectionById(collection_id);

    // Add or overwrite the document
    targetCollection.put(doc_id, new HashMap<>(data));
  }

  @Override
  public void updateDocument(String collection_id, String doc_id, Map<String, Object> data)
      throws IllegalArgumentException {
    if (collection_id == null || doc_id == null || data == null) {
      throw new IllegalArgumentException(
          "updateDocument: collection_id, doc_id, or data cannot be null");
    }

    Map<String, Map<String, Object>> targetCollection = getCollectionById(collection_id);

    // If the document exists, update the fields in the document
    Map<String, Object> existingDoc = targetCollection.get(doc_id);
    if (existingDoc != null) {
      // Merge the existing document data with the new data
      mergeFields(existingDoc, data); // Adds or updates the fields in the existing document
    } else {
      // If the document does not exist, create a new document if needed
      targetCollection.put(doc_id, new HashMap<>(data));
    }
  }

  @Override
  public List<Map<String, Object>> getCollection(String collection_id)
      throws InterruptedException, ExecutionException {
    if (collection_id == null) {
      throw new IllegalArgumentException("Collection ID cannot be null.");
    }

    Map<String, Map<String, Object>> targetCollection = getCollectionById(collection_id);

    // Return all documents in the collection as a list
    return new ArrayList<>(targetCollection.values());
  }

  @Override
  public Map<String, Object> getDocument(String collection_id, String doc_id)
      throws InterruptedException, ExecutionException {
    if (collection_id == null || doc_id == null) {
      throw new IllegalArgumentException("Collection ID and document ID cannot be null.");
    }

    Map<String, Map<String, Object>> targetCollection = getCollectionById(collection_id);

    return targetCollection.get(doc_id); // could be null btw
  }

  @Override
  public void deleteDocument(String collectionID, String docID)
      throws InterruptedException, ExecutionException {
    if (collectionID == null || docID == null) {
      throw new IllegalArgumentException("deleteDocument: collectionID and docID cannot be null");
    }

    Map<String, Map<String, Object>> targetCollection = getCollectionById(collectionID);

    // Check if the document exists in the collection
    if (targetCollection.containsKey(docID)) {
      // Remove the document from the collection
      targetCollection.remove(docID);
      System.out.println(
          "Document with ID " + docID + " has been deleted from collection: " + collectionID);
    } else {
      // If the document does not exist, throw an exception
      throw new IllegalArgumentException(
          "Document with ID " + docID + " does not exist in collection: " + collectionID);
    }
  }

  @Override
  public long getDocumentCount(String collectionID)
      throws InterruptedException, ExecutionException {
    return 0;
  }

  @Override
  public List<Map<String, Object>> getProblems(String difficulty, int number)
      throws InterruptedException, ExecutionException, IOException {
    return null;
  }

  @Override
  public Map<String, Object> getProblem(String problemID)
      throws InterruptedException, ExecutionException, IOException {
    return null;
  }

  @Override
  public List<Map<String, Object>> sortCollection(String collectionID, String field)
      throws InterruptedException, ExecutionException, IOException {
    List<Map<String, Object>> collection = this.getCollection(collectionID);

    // Sort the collection based on the specified field
    collection.sort(
        (map1, map2) -> {
          Comparable<Object> value1 = (Comparable<Object>) map1.get(field);
          Comparable<Object> value2 = (Comparable<Object>) map2.get(field);

          if (value1 == null || value2 == null) {
            throw new IllegalArgumentException("Field value is missing or null.");
          }

          return value1.compareTo(value2) * -1;
        });

    return collection;
  }

  // helper to make getting collection easier
  private Map<String, Map<String, Object>> getCollectionById(String collection_id)
      throws IllegalArgumentException {
    switch (collection_id) {
      case "Rooms":
        return this.rooms;
      case "Users":
        return this.users;
      default:
        throw new IllegalArgumentException("Invalid collection ID: " + collection_id);
    }
  }

  /**
   * Helper for update Recursively merges the fields from the source map into the target map. -
   * Overwrites primitive values. - Updates nested maps instead of replacing them entirely.
   *
   * @param target The target map to update.
   * @param source The source map with new data.
   */
  private void mergeFields(Map<String, Object> target, Map<String, Object> source) {
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map && target.get(key) instanceof Map) {
        // If both target and source have maps at this key, merge them
        Map<String, Object> targetNestedMap = (Map<String, Object>) target.get(key);
        Map<String, Object> sourceNestedMap = (Map<String, Object>) value;
        mergeFields(targetNestedMap, sourceNestedMap);
      } else {
        // Otherwise, overwrite the value in the target map
        target.put(key, value);
      }
    }
  }
}
