package edu.brown.cs.student.storage;

import java.util.HashMap;
import java.util.Map;

public class MockRoom {

  public String roomID;
  public String roomName;
  public String problemID;
  public String duration;
  public String difficulty;
  public String timeCreated;
  public Map<String, UserInfo> players; // map from string (userID) to their info

  // Nested class to represent user info
  public static class UserInfo {
    public String displayName;
    public String timeSubmitted;
    public String userID;
    public String userScore;

    // Method to update user info fields from a map
    public void updateFields(Map<String, Object> updates) {
      if (updates.containsKey("displayName")) {
        this.displayName = (String) updates.get("displayName");
      }
      if (updates.containsKey("timeSubmitted")) {
        this.timeSubmitted = (String) updates.get("timeSubmitted");
      }
      if (updates.containsKey("userID")) {
        this.userID = (String) updates.get("userID");
      }
      if (updates.containsKey("userScore")) {
        this.userScore = (String) updates.get("userScore");
      }
    }

    @Override
    public String toString() {
      return "UserInfo{"
          + "displayName='"
          + displayName
          + '\''
          + ", timeSubmitted='"
          + timeSubmitted
          + '\''
          + ", userID='"
          + userID
          + '\''
          + ", userScore='"
          + userScore
          + '\''
          + '}';
    }
  }

  // Method to update room fields from a map
  public void updateFields(Map<String, Object> updates) {
    if (updates.containsKey("roomID")) {
      this.roomID = (String) updates.get("roomID");
    }
    if (updates.containsKey("roomName")) {
      this.roomName = (String) updates.get("roomName");
    }
    if (updates.containsKey("problemID")) {
      this.problemID = (String) updates.get("problemID");
    }
    if (updates.containsKey("duration")) {
      this.duration = (String) updates.get("duration");
    }
    if (updates.containsKey("difficulty")) {
      this.difficulty = (String) updates.get("difficulty");
    }
    if (updates.containsKey("timeCreated")) {
      this.timeCreated = (String) updates.get("timeCreated");
    }
    if (updates.containsKey("players")) {
      if (this.players == null) {
        this.players = new HashMap<>();
      }
      Map<String, Map<String, Object>> playersUpdates =
          (Map<String, Map<String, Object>>) updates.get("players");
      for (Map.Entry<String, Map<String, Object>> entry : playersUpdates.entrySet()) {
        String userID = entry.getKey();
        Map<String, Object> playerUpdates = entry.getValue();
        UserInfo userInfo = this.players.get(userID);
        if (userInfo == null) {
          userInfo = new UserInfo();
          this.players.put(userID, userInfo);
        }
        userInfo.updateFields(playerUpdates);
      }
    }
  }

  @Override
  public String toString() {
    return "MockRoom{"
        + "roomID='"
        + roomID
        + '\''
        + ", roomName='"
        + roomName
        + '\''
        + ", problemID='"
        + problemID
        + '\''
        + ", duration='"
        + duration
        + '\''
        + ", difficulty='"
        + difficulty
        + '\''
        + ", timeCreated='"
        + timeCreated
        + '\''
        + ", players="
        + players
        + '}';
  }
}
