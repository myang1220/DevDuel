package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * This class sets room data based on a room ID. If no room exists it will create one If parameter
 * values are unspecified they will be ignored, and will not be updated, but certain groups of data
 * must be given together (for example you cant update a score without a roomID)and specified values
 * will just update the current room data. This makes it good for adding players into a room without
 * having to first query the current players as well as updating score. This is the one stop shop
 * for editing anything room related, an attempt to make the backend efficient.
 */
public class RoomSet implements Route {

  private IStorage storageManager;

  public RoomSet(IStorage storage) {
    this.storageManager = storage;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();

    // collect params
    String roomName = request.queryParams("roomName");
    String roomID = request.queryParams("roomID");
    String difficulty = request.queryParams("difficulty");
    String timeCreated = request.queryParams("timeCreated");
    String problemID = request.queryParams("problemID");
    String duration = request.queryParams("duration");

    String userName = request.queryParams("userName");
    String userID = request.queryParams("userID");
    String userScore = request.queryParams("userScore");
    String timeSubmitted = request.queryParams("timeSubmitted");

    try {
      if (roomID == null) {
        throw new IllegalArgumentException("Must specify roomID");
      }
      Map<String, Object> data = new HashMap<>();
      data.put("roomID", roomID);
      if (roomName != null
          && difficulty != null
          && timeCreated != null
          && problemID != null
          && duration != null) {
        data.put("roomName", roomName);
        data.put("difficulty", difficulty);
        data.put("timeCreated", timeCreated);
        data.put("problemID", problemID);
        data.put("duration", duration);
      }
      if (userName != null && userID != null && userScore != null && timeSubmitted != null) {
        Map<String, Object> players = new HashMap<>();
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("displayName", userName);
        playerData.put("userID", userID);
        playerData.put("userScore", userScore);
        playerData.put("timeSubmitted", timeSubmitted);

        players.put(userID, playerData);
        data.put("players", players);
      }

      responseMap.put("data", data);
      this.storageManager.updateDocument("Rooms", roomID, data);
      responseMap.put("response_type", "success");
    } catch (IllegalArgumentException e) {
      responseMap.put("response_type", "failure");
      responseMap.put("error", e.toString());
      e.printStackTrace();
    }
    return JsonUtil.toMoshiJson(responseMap);
  }
}
