package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import spark.Request;
import spark.Response;
import spark.Route;

// this class sets the data for a user
public class UserSet implements Route {

  private IStorage storageManager;

  public UserSet(IStorage storage) {
    this.storageManager = storage;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();

    // collect params
    String displayName = request.queryParams("displayName");
    String userID = request.queryParams("userID");
    String email = request.queryParams("email");
    String wins = request.queryParams("wins");
    String startDate = request.queryParams("date");

    try {
      // makes sure no params are null
      if (displayName == null
          || userID == null
          || email == null
          || wins == null
          || startDate == null) {
        throw new IllegalArgumentException("One or more required parameters are missing or null.");
      }

      double winDouble;
      winDouble = Double.parseDouble(wins);

      // puts the input data into a map which will be sent to firestore
      HashMap<String, Object> data = new HashMap<>();
      data.put("displayName", displayName);
      data.put("userID", userID);
      data.put("email", email);
      data.put("wins", winDouble);
      data.put("startDate", startDate);

      Map<String, Object> currentData = this.storageManager.getDocument("Users", userID);
      if (currentData != null && currentData.containsKey("code")) {
        data.put("code", currentData.get("code"));
      }

      // puts the data in a document under collection Users and with name User+userID
      this.storageManager.addDocument("Users", userID, data);

      // returns success and displays the data inputted to make sure
      responseMap.put("response_type", "success");
      responseMap.put("data", data);
    } catch (IllegalArgumentException e) {
      // returns failure if something went wrong
      responseMap.put("response_type", "failure");
      responseMap.put("error", e.toString());
      e.printStackTrace();
    }
    return JsonUtil.toMoshiJson(responseMap);
  }
}
