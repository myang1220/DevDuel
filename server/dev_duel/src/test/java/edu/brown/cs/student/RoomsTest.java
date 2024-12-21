package edu.brown.cs.student;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import edu.brown.cs.student.endpoints.RoomDel;
import edu.brown.cs.student.endpoints.RoomInfo;
import edu.brown.cs.student.endpoints.RoomList;
import edu.brown.cs.student.endpoints.RoomSet;
import edu.brown.cs.student.storage.FirestoreUtil;
import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.storage.MockRoom;
import edu.brown.cs.student.storage.MockStorage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import okio.Buffer;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;

public class RoomsTest {

  final Type mapStringObject = Types.newParameterizedType(Map.class, String.class, Object.class);
  JsonAdapter<Map<String, Object>> adapter;
  IStorage mockStorage;
  MockRoom mockRoom;

  @BeforeAll
  public static void setup_before_everything() {
    Spark.port(0);
    Logger.getLogger("").setLevel(Level.WARNING); // empty name = root logger
  }

  @BeforeEach
  public void setup() throws IOException {
    IStorage firestoreUtils;
    firestoreUtils = new FirestoreUtil(); // something here could be the problem as to why
    // firestore doesnt work
    // when I uncomment these and try everything with mock it fails
    this.mockStorage = new MockStorage();
    this.mockRoom = new MockRoom();

    Spark.get("RoomSet", new RoomSet(this.mockStorage));
    Spark.get("RoomInfo", new RoomInfo(this.mockStorage));
    Spark.get("RoomDel", new RoomDel(this.mockStorage));
    Spark.get("RoomList", new RoomList(this.mockStorage));

    Moshi moshi = new Moshi.Builder().build();
    adapter = moshi.adapter(mapStringObject);

    Spark.init();
    Spark.awaitInitialization(); // don't continue until the server is listening
    System.out.println("Server started at http://localhost:" + 0);
  }

  @AfterEach
  public void teardown() {
    // Gracefully stop Spark listening on both endpoints after each test
    Spark.unmap("RoomSet");
    Spark.unmap("RoomInfo");
    Spark.unmap("RoomDel");
    Spark.unmap("RoomList");
    Spark.awaitStop(); // don't proceed until the server is stopped
  }

  private static HttpURLConnection tryRequest(String apiCall) throws IOException {
    URL requestURL = new URL("http://localhost:" + Spark.port() + "/" + apiCall);
    HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();
    clientConnection.setRequestMethod("GET");
    clientConnection.connect();
    return clientConnection;
  }

  public void initialRoom() throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest(
            "RoomSet?roomID=333&roomName=testName&problemID=1&difficulty=easy"
                + "&timeCreated=12:00&duration=360&userName=player1&userID=1&userScore=0/7&timeSubmitted=12:01");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Rooms", "333");
    this.mockRoom.updateFields(doc);

    assertEquals(this.mockRoom.roomID, "333");
    assertEquals(this.mockRoom.roomName, "testName");
    assertEquals(this.mockRoom.problemID, "1");
    assertEquals(this.mockRoom.difficulty, "easy");
    assertEquals(this.mockRoom.timeCreated, "12:00");
    assertEquals(this.mockRoom.duration, "360");
    assertEquals(this.mockRoom.players.get("1").userID, "1");
    assertEquals(this.mockRoom.players.get("1").userScore, "0/7");
    assertEquals(this.mockRoom.players.get("1").displayName, "player1");
    assertEquals(this.mockRoom.players.get("1").timeSubmitted, "12:01");

    clientConnection.disconnect();
  }

  public void addSecondPlayerToRoom() throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest(
            "RoomSet?roomID=333&userName=player2&userID=22&userScore=0/7&timeSubmitted=12:00");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Rooms", "333");
    this.mockRoom.updateFields(doc);

    assertEquals(this.mockRoom.roomID, "333");
    assertEquals(this.mockRoom.roomName, "testName");
    assertEquals(this.mockRoom.problemID, "1");
    assertEquals(this.mockRoom.difficulty, "easy");
    assertEquals(this.mockRoom.timeCreated, "12:00");
    assertEquals(this.mockRoom.duration, "360");
    assertEquals(this.mockRoom.players.get("22").userID, "22");
    assertEquals(this.mockRoom.players.get("22").userScore, "0/7");
    assertEquals(this.mockRoom.players.get("22").displayName, "player2");
    assertEquals(this.mockRoom.players.get("22").timeSubmitted, "12:00");

    clientConnection.disconnect();
  }

  public void updatePlayerScoreInRoom()
      throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest(
            "RoomSet?roomID=333&userName=player2&userID=22&userScore=3/7&timeSubmitted=12:05");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Rooms", "333");
    this.mockRoom.updateFields(doc);

    assertEquals(this.mockRoom.roomID, "333");
    assertEquals(this.mockRoom.roomName, "testName");
    assertEquals(this.mockRoom.problemID, "1");
    assertEquals(this.mockRoom.difficulty, "easy");
    assertEquals(this.mockRoom.timeCreated, "12:00");
    assertEquals(this.mockRoom.duration, "360");
    assertEquals(this.mockRoom.players.get("22").userID, "22");
    assertEquals(this.mockRoom.players.get("22").userScore, "3/7");
    assertEquals(this.mockRoom.players.get("22").displayName, "player2");
    assertEquals(this.mockRoom.players.get("22").timeSubmitted, "12:05");
    assertEquals(this.mockRoom.players.get("1").userID, "1");
    assertEquals(this.mockRoom.players.get("1").userScore, "0/7");
    assertEquals(this.mockRoom.players.get("1").displayName, "player1");
    assertEquals(this.mockRoom.players.get("1").timeSubmitted, "12:01");

    clientConnection.disconnect();
  }

  @Test
  public void RoomSetNoRoomID() throws IOException {
    HttpURLConnection clientConnection =
        tryRequest(
            "RoomSet?&roomName=testName&problemID=1&difficulty=easy"
                + "&timeCreated=12:00&duration=360&userName=player1&userID=1&userScore=0/7&timeSubmitted=12:01");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("failure", response.get("response_type"));
    assertEquals("java.lang.IllegalArgumentException: Must specify roomID", response.get("error"));

    clientConnection.disconnect();
  }

  @Test
  public void testRoomSet() throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
  }

  @Test
  public void testRoomSetPlayerUpdate()
      throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    this.addSecondPlayerToRoom();
  }

  @Test
  public void testRoomSetUpdatingScores()
      throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    this.addSecondPlayerToRoom();
    this.updatePlayerScoreInRoom();
  }

  @Test
  public void testRoomInfo() throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    this.addSecondPlayerToRoom();

    HttpURLConnection clientConnection = tryRequest("RoomInfo?roomID=333");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    System.out.println(response);

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Rooms", "333");
    this.mockRoom.updateFields(doc);

    assertEquals(this.mockRoom.roomID, response.get("data").get("roomID"));
    assertEquals(this.mockRoom.roomName, response.get("data").get("roomName"));
    assertEquals(this.mockRoom.problemID, response.get("data").get("problemID"));
    assertEquals(this.mockRoom.difficulty, response.get("data").get("difficulty"));
    assertEquals(this.mockRoom.timeCreated, response.get("data").get("timeCreated"));
    assertEquals(this.mockRoom.duration, response.get("data").get("duration"));
    assertEquals(
        this.mockRoom.players.toString(),
        "{22=UserInfo{displayName='player2', timeSubmitted='12:00', userID='22', userScore='0/7'},"
            + " 1=UserInfo{displayName='player1', timeSubmitted='12:01', userID='1', userScore='0/7'}}");
    // ^had to add UserInfo to the string because when I convert the real data into a mockRoom its
    // in a subclass
    // test is still legit it just needed to be this way cuz I added MockRoom to make testing easier

    clientConnection.disconnect();
  }

  @Test
  public void RoomInfoNoID() throws IOException {
    HttpURLConnection clientConnection = tryRequest("RoomInfo");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("failure", response.get("response_type"));
    assertEquals("java.lang.IllegalArgumentException: Must specify roomID", response.get("error"));

    clientConnection.disconnect();
  }

  @Test
  public void RoomList() throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    this.updatePlayerScoreInRoom();
    this.addSecondPlayerToRoom();
    HttpURLConnection clientConnection2 =
        tryRequest(
            "RoomSet?roomID=443&roomName=testName2&problemID=10&difficulty=medium"
                + "&timeCreated=1:00&duration=1200&userName=player5&userID=5&userScore=0/7&timeSubmitted=1:00");
    assertEquals(200, clientConnection2.getResponseCode());
    clientConnection2.disconnect();

    HttpURLConnection clientConnection = tryRequest("RoomList");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Object> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    List<Map<String, Object>> roomList = (List<Map<String, Object>>) response.get("data");

    assertEquals("success", response.get("response_type"));
    assertEquals(2, roomList.size());
    Assert.assertTrue(roomList.toString().contains("roomID=333"));
    Assert.assertTrue(roomList.toString().contains("roomID=443"));
    clientConnection.disconnect();
  }

  @Test
  public void RoomListNoRooms() throws IOException {
    HttpURLConnection clientConnection = tryRequest("RoomList");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Object> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    List<Map<String, Object>> roomList = (List<Map<String, Object>>) response.get("data");

    assertEquals("success", response.get("response_type"));
    assertEquals(0, roomList.size());
    Assert.assertTrue(roomList.isEmpty());
    clientConnection.disconnect();
  }

  @Test
  public void RoomDelNormal() throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    HttpURLConnection clientConnection = tryRequest("RoomDel?roomID=333");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    System.out.println(response);

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Rooms", "333");
    Assertions.assertNull(doc);

    clientConnection.disconnect();
  }

  @Test
  public void RoomDelNoRoom() throws IOException {
    HttpURLConnection clientConnection = tryRequest("RoomDel?roomID=333");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    System.out.println(response);

    assertEquals("failure", response.get("response_type"));
    assertEquals(
        "java.lang.IllegalArgumentException: "
            + "User document for [roomID: 333] not found. "
            + "Check spelling or if the room exists",
        response.get("error"));

    clientConnection.disconnect();
  }

  @Test
  public void RoomDelTwice() throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    HttpURLConnection clientConnection1 = tryRequest("RoomDel?roomID=333");
    assertEquals(200, clientConnection1.getResponseCode());

    Moshi moshi1 = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response1 =
        moshi1
            .adapter(Map.class)
            .fromJson(new Buffer().readFrom(clientConnection1.getInputStream()));

    System.out.println(response1);

    assertEquals("success", response1.get("response_type"));

    Map<String, Object> doc1 = this.mockStorage.getDocument("Rooms", "333");
    Assertions.assertNull(doc1);

    clientConnection1.disconnect();

    HttpURLConnection clientConnection = tryRequest("RoomDel?roomID=333");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    System.out.println(response);

    assertEquals("failure", response.get("response_type"));
    assertEquals(
        "java.lang.IllegalArgumentException: "
            + "User document for [roomID: 333] not found. "
            + "Check spelling or if the room exists",
        response.get("error"));

    clientConnection.disconnect();
  }

  @Test
  public void RoomDelNoID() throws IOException, ExecutionException, InterruptedException {
    this.initialRoom();
    HttpURLConnection clientConnection = tryRequest("RoomDel");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Moshi.Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    System.out.println(response);

    assertEquals("failure", response.get("response_type"));
    assertEquals("java.lang.IllegalArgumentException: Must specify roomID", response.get("error"));

    clientConnection.disconnect();
  }
}
