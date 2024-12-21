package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * This class updates the user code history in a IStorage database. It takes in a userID, problemID
 * date, score, and the code. This class will add the code under the problem if there is no
 * submission for that problem already available. If the user has done that problem before, then it
 * will check to see which submission has a higher score, and choose that one. It will prioritize
 * more recent submissions if they are both equal.
 */
public class UserUpdateHist implements Route {

  private IStorage storageManager;

  public UserUpdateHist(IStorage storage) {
    this.storageManager = storage;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, Object> responseMap = new HashMap<>();

    // collect params
    String userID = request.queryParams("userID");
    String problemID = request.queryParams("problemID");
    String date = request.queryParams("date");
    String score = request.queryParams("score");
    String code = request.queryParams("code");
    String winString = request.queryParams("win");

    try {
      if (winString == null) {
        throw new IllegalArgumentException("The 'win' parameter is required.");
      }

      // Normalize and convert to a boolean
      winString = winString.trim().toLowerCase();
      if (!winString.equals("true") && !winString.equals("false")) {
        throw new IllegalArgumentException("The 'win' parameter must be 'true' or 'false'.");
      }
      boolean win = Boolean.parseBoolean(winString);
      Double wins;

      // makes sure no params are null
      if (userID == null || date == null || score == null || code == null || problemID == null) {
        throw new IllegalArgumentException("One or more required parameters are missing or null.");
      }

      Map<String, Object> data = this.storageManager.getDocument("Users", userID);
      if (data == null) {
        throw new IllegalArgumentException(
            "User document not found. Check spelling or if the user exists");
      }
      if (win) { // update wins before anything else
        if (data.containsKey("wins")) {
          try {
            wins = Double.parseDouble(data.get("wins").toString());
            wins++;
            data.put("wins", wins);
          } catch (NumberFormatException e) {
            throw new InterruptedException("Value of wins in user " + userID + "not a number");
          }
        }
      }
      if (data.containsKey("code")) {
        try {
          // Attempt to cast the value of "code" to a Map
          Map<String, Object> codeMap = (Map<String, Object>) data.get("code");
          if (codeMap.containsKey("problem" + problemID)) {
            // problem already has a code history, must check if we need to update
            Map<String, Object> problemMap =
                (Map<String, Object>) codeMap.get("problem" + problemID);
            if (problemMap.containsKey("score")) {
              double oldScore = this.fracToDouble(problemMap.get("score").toString());
              double newScore = this.fracToDouble(score);
              if (newScore >= oldScore) {
                problemMap.put("score", score);
                problemMap.put("code", code);
                problemMap.put("date", date);

                codeMap.put("problem" + problemID, problemMap);
                data.put("code", codeMap);
                this.storageManager.updateDocument("Users", userID, data);
              } else {
                // new score not as good as old score, only need to update win
                this.storageManager.updateDocument("Users", userID, data);
              }
            } else {
              throw new IllegalArgumentException(
                  "for some reason problem map does not contain field score. Uh oh");
            }
          } else {
            // if the problem doesnt exist, add the entry for this problem
            Map<String, Object> additionalMap = new HashMap<>();
            additionalMap.put("code", code);
            additionalMap.put("date", date);
            additionalMap.put("score", score);

            codeMap.put("problem" + problemID, additionalMap);
            data.put("code", codeMap);
            this.storageManager.updateDocument("Users", userID, data);
          }
        } catch (ClassCastException | NullPointerException e) {
          throw new IllegalArgumentException("'code' exists but is not properly structured.");
        }
      } else {
        // if there is no code field, add one
        Map<String, Object> codeMap = new HashMap<>();
        Map<String, Object> problemMap = new HashMap<>();

        problemMap.put("score", score);
        problemMap.put("code", code);
        problemMap.put("date", date);

        codeMap.put("problem" + problemID, problemMap);
        data.put("code", codeMap);
        this.storageManager.updateDocument("Users", userID, data);
      }

      // returns success and displays the data inputted to make sure
      responseMap.put("response_type", "success");
      responseMap.put("updated data", data);
    } catch (IllegalArgumentException | ExecutionException | InterruptedException e) {
      // returns failure if something went wrong
      responseMap.put("response_type", "failure");
      responseMap.put("error", e.toString());
      e.printStackTrace();
    }
    return JsonUtil.toMoshiJson(responseMap);
  }

  /**
   * Helper function to reduce clutter when checking scores. Scores are in string format as a
   * fraction (like 4/5), and this converts to decimal so it can be easily compared.
   *
   * @param score
   * @return
   */
  private double fracToDouble(String score) {
    double scoreDouble;
    try {
      if (score.contains("/")) {
        String[] fractionParts = score.split("/");
        scoreDouble = Double.parseDouble(fractionParts[0]) / Double.parseDouble(fractionParts[1]);
      } else {
        scoreDouble = Double.parseDouble(score);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid score format: " + score);
    }
    return scoreDouble;
  }
}
