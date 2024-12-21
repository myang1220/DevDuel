package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import spark.Request;
import spark.Response;
import spark.Route;

/** This class returns the info for a user given only their userID */
public class UserInfo implements Route {

  private IStorage storageManager;

  public UserInfo(IStorage storage) {
    this.storageManager = storage;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();

    // collect params
    String userID = request.queryParams("userID");

    try {
      if (userID == null) {
        throw new IllegalArgumentException("Must specify userID");
      }

      // retrieve the user doc
      Map<String, Object> data = this.storageManager.getDocument("Users", userID);
      if (data == null) { // check to make sure something was retrieved. getDocument for firestore
        // returns null if nothing
        throw new IllegalArgumentException(
            "User document for [userID: "
                + userID
                + "] not found. Check spelling or if the user exists");
      }
      responseMap.put("data", data);
      responseMap.put("response_type", "success");
    } catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
      responseMap.put("response_type", "failure");
      responseMap.put("error", e.toString());
      e.printStackTrace();
    }
    return JsonUtil.toMoshiJson(responseMap);
  }
}
