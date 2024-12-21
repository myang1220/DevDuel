package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import spark.Request;
import spark.Response;
import spark.Route;

/** This class lists the rooms in a database */
public class RoomList implements Route {

  private IStorage storageManager;

  public RoomList(IStorage storage) {
    this.storageManager = storage;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();

    try {
      List<Map<String, Object>> rooms = this.storageManager.getCollection("Rooms");
      responseMap.put("data", rooms); // note this can return an empty list of rooms!
      // This is intentional as sometimes there are no rooms

      responseMap.put("response_type", "success");
    } catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
      responseMap.put("response_type", "failure");
      responseMap.put("error", e.toString());
      e.printStackTrace();
    }
    return JsonUtil.toMoshiJson(responseMap);
  }
}
