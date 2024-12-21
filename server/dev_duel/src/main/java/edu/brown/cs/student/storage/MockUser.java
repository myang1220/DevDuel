package edu.brown.cs.student.storage;

import java.util.HashMap;
import java.util.Map;

public class MockUser {

  public String userID;
  public String displayName;
  public String email;
  public String startDate;
  public Double wins;
  public Map<String, ProblemInfo> code; // map from string (problemID) to its info

  // Nested class to represent user info
  public static class ProblemInfo {
    public String code;
    public String date;
    public String score;

    // Method to update user info fields from a map
    public void updateFields(Map<String, Object> updates) {
      if (updates.containsKey("code")) {
        this.code = (String) updates.get("code");
      }
      if (updates.containsKey("date")) {
        this.date = (String) updates.get("date");
      }
      if (updates.containsKey("score")) {
        this.score = (String) updates.get("score");
      }
    }

    @Override
    public String toString() {
      return "code{"
          + "code='"
          + code
          + '\''
          + ", date='"
          + date
          + '\''
          + ", score='"
          + score
          + '\''
          + '}';
    }
  }

  // Method to update room fields from a map
  public void updateFields(Map<String, Object> updates) {
    if (updates.containsKey("userID")) {
      this.userID = (String) updates.get("userID");
    }
    if (updates.containsKey("displayName")) {
      this.displayName = (String) updates.get("displayName");
    }
    if (updates.containsKey("email")) {
      this.email = (String) updates.get("email");
    }
    if (updates.containsKey("startDate")) {
      this.startDate = (String) updates.get("startDate");
    }
    if (updates.containsKey("wins")) {
      this.wins = Double.parseDouble(updates.get("wins").toString());
    }
    if (updates.containsKey("code")) {
      if (this.code == null) {
        this.code = new HashMap<>();
      }
      Map<String, Map<String, Object>> codeUpdates =
          (Map<String, Map<String, Object>>) updates.get("code");
      for (Map.Entry<String, Map<String, Object>> entry : codeUpdates.entrySet()) {
        String problemID = entry.getKey();
        Map<String, Object> codeUpdate = entry.getValue();
        ProblemInfo currProbInfo = this.code.get(problemID);
        if (currProbInfo == null) {
          currProbInfo = new ProblemInfo();
          this.code.put(problemID, currProbInfo);
        }
        currProbInfo.updateFields(codeUpdate);
      }
    }
  }

  @Override
  public String toString() {
    return "MockRoom{"
        + "userID='"
        + userID
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", email='"
        + email
        + '\''
        + ", startDate='"
        + startDate
        + '\''
        + ", wins='"
        + wins
        + '\''
        + '}';
  }
}
