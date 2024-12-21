package edu.brown.cs.student.storage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** This interface declare storage operations */
public interface IStorage {
  /**
   * Adds a document to a specific collection.
   *
   * @param collection_id Collection ID.
   * @param doc_id Document ID.
   * @param data Data to store.
   * @throws IllegalArgumentException If any parameter is null.
   */
  void addDocument(String collection_id, String doc_id, Map<String, Object> data)
      throws IllegalArgumentException, ExecutionException, InterruptedException;

  /**
   * Updates an existing document without overwriting all of it
   *
   * @param collection_id Collection ID
   * @param doc_id Document ID
   * @param data the data to update the document with. If the field exists it will change it, if the
   *     field does not exist it will add it
   * @throws IllegalArgumentException If any parameter is null
   */
  void updateDocument(String collection_id, String doc_id, Map<String, Object> data)
      throws IllegalArgumentException;

  /**
   * Gets a collection
   *
   * @param collection_id collection id
   * @return the collection as a list of Maps
   * @throws InterruptedException
   * @throws ExecutionException
   */
  List<Map<String, Object>> getCollection(String collection_id)
      throws InterruptedException, ExecutionException;

  /**
   * Retrieves a documents from a specific collection.
   *
   * @param collection_id Collection ID.
   * @param doc_id the doc ID
   * @return List of documents as maps.
   * @throws InterruptedException
   * @throws ExecutionException
   */
  Map<String, Object> getDocument(String collection_id, String doc_id)
      throws InterruptedException, ExecutionException;

  /**
   * Deletes a specific room
   *
   * @param collectionID collection ID.
   * @param docID doc ID
   * @throws IllegalArgumentException If roomID is null.
   */
  void deleteDocument(String collectionID, String docID)
      throws InterruptedException, ExecutionException;

  /**
   * Return the document of a given collection
   *
   * @param collectionID id of the collection
   * @return number of documents in collection
   */
  public long getDocumentCount(String collectionID) throws InterruptedException, ExecutionException;

  public List<Map<String, Object>> getProblems(String difficulty, int number)
      throws InterruptedException, ExecutionException, IOException;

  public Map<String, Object> getProblem(String problemID)
      throws InterruptedException, ExecutionException, IOException;

  public List<Map<String, Object>> sortCollection(String collectionID, String field)
      throws InterruptedException, ExecutionException, IOException;
}
