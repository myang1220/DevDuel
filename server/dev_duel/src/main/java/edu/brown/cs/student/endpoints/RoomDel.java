package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import spark.Request;
import spark.Response;
import spark.Route;

// deletes a room
public class RoomDel implements Route {

  private IStorage storageManager;

  public RoomDel(IStorage storage) {
    this.storageManager = storage;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();

    // collect params
    String roomID = request.queryParams("roomID");

    try {
      if (roomID == null) {
        throw new IllegalArgumentException("Must specify roomID");
      }
      // retrieve the user doc
      Map<String, Object> data = this.storageManager.getDocument("Rooms", roomID);
      if (data == null) { // check to make sure the room exists
        throw new IllegalArgumentException(
            "User document for [roomID: "
                + roomID
                + "] not found. Check spelling or if the room exists");
      }
      this.storageManager.deleteDocument("Rooms", roomID); // delete the document
      responseMap.put("data", data); // save the data just to return for the user
      responseMap.put("response_type", "success");
    } catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
      responseMap.put("response_type", "failure");
      responseMap.put("error", e.toString());
      e.printStackTrace();
    }
    return JsonUtil.toMoshiJson(responseMap);
  }
}
