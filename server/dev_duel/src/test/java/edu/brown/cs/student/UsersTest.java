package edu.brown.cs.student;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;
import com.squareup.moshi.Types;
import edu.brown.cs.student.endpoints.UserInfo;
import edu.brown.cs.student.endpoints.UserLeaderboard;
import edu.brown.cs.student.endpoints.UserList;
import edu.brown.cs.student.endpoints.UserSet;
import edu.brown.cs.student.endpoints.UserUpdateHist;
import edu.brown.cs.student.storage.FirestoreUtil;
import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.storage.MockStorage;
import edu.brown.cs.student.storage.MockUser;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;

public class UsersTest {

  final Type mapStringObject = Types.newParameterizedType(Map.class, String.class, Object.class);
  JsonAdapter<Map<String, Object>> adapter;
  IStorage mockStorage;
  MockUser mockUser;

  @BeforeAll
  public static void setup_before_everything() {
    Spark.port(0);
    Logger.getLogger("").setLevel(Level.WARNING); // empty name = root logger
  }

  @BeforeEach
  public void setup() throws IOException {
    IStorage firestoreUtils;
    firestoreUtils = new FirestoreUtil();
    this.mockStorage = new MockStorage();
    this.mockUser = new MockUser();

    Spark.get("UserSet", new UserSet(this.mockStorage));
    Spark.get("UserUpdateHist", new UserUpdateHist(this.mockStorage));
    Spark.get("UserInfo", new UserInfo(this.mockStorage));
    Spark.get("UserList", new UserList(this.mockStorage));
    Spark.get("UserLeaderboard", new UserLeaderboard(this.mockStorage));

    Moshi moshi = new Moshi.Builder().build();
    adapter = moshi.adapter(mapStringObject);

    Spark.init();
    Spark.awaitInitialization(); // don't continue until the server is listening
    System.out.println("Server started at http://localhost:" + 0);
  }

  @AfterEach
  public void teardown() {
    this.mockUser = null;
    this.mockStorage = null;
    // Gracefully stop Spark listening on both endpoints after each test
    Spark.unmap("UserSet");
    Spark.unmap("UserUpdateHist");
    Spark.unmap("UserInfo");
    Spark.awaitStop(); // don't proceed until the server is stopped
  }

  private static HttpURLConnection tryRequest(String apiCall) throws IOException {
    URL requestURL = new URL("http://localhost:" + Spark.port() + "/" + apiCall);
    HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();
    clientConnection.setRequestMethod("GET");
    clientConnection.connect();
    return clientConnection;
  }

  // helper to avoid repeating the initial user setup
  public void setUpUser() throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest(
            "UserSet?displayName=testTwo&userID=user_2&email=test.example@gmail.com&"
                + "wins=0&date=12/2/24");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 0);
    assertEquals(this.mockUser.startDate, "12/2/24");

    clientConnection.disconnect();
  }

  @Test
  public void UserSet() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
  }

  @Test
  public void UserSetOverride() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    HttpURLConnection clientConnection =
        tryRequest(
            "UserSet?displayName=testOne&userID=user_2&email=name.example@gmail.com&"
                + "wins=1&date=12/3/24");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testOne");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "name.example@gmail.com");
    assertEquals(this.mockUser.wins, 1.0);
    assertEquals(this.mockUser.startDate, "12/3/24");

    clientConnection.disconnect();
  }

  @Test
  public void UserSetInvalidParams() throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest("UserSet?userID=user_2&email=test.example@gmail.com&" + "wins=0");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("failure", response.get("response_type"));
    assertEquals(
        "java.lang.IllegalArgumentException: "
            + "One or more required parameters are missing or null.",
        response.get("error"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    Assert.assertNull(doc);
    clientConnection.disconnect();
  }

  @Test
  public void UserSetOverrideInvalidParams()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    HttpURLConnection clientConnection =
        tryRequest("UserSet?userID=user_2&email=test.example@gmail.com&" + "wins=0");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("failure", response.get("response_type"));
    assertEquals(
        "java.lang.IllegalArgumentException: "
            + "One or more required parameters are missing or null.",
        response.get("error"));

    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 0);
    assertEquals(this.mockUser.startDate, "12/2/24");

    clientConnection.disconnect();
  }

  @Test
  public void UserSetTwice() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    HttpURLConnection clientConnection =
        tryRequest(
            "UserSet?displayName=testThree&userID=user_3&email=test3.example@gmail.com&"
                + "wins=0&date=12/9/24");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc2 = this.mockStorage.getDocument("Users", "user_3");
    MockUser user2 = new MockUser();
    user2.updateFields(doc2);

    assertEquals(user2.displayName, "testThree");
    assertEquals(user2.userID, "user_3");
    assertEquals(user2.email, "test3.example@gmail.com");
    assertEquals(user2.wins, 0);
    assertEquals(user2.startDate, "12/9/24");

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 0);
    assertEquals(this.mockUser.startDate, "12/2/24");

    clientConnection.disconnect();
  }

  public void addHistUser_2() throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?userID=user_2&problemID=3&date=2024-12-03T12:00:01Z&"
                + "score=2/4&code=System.out.println(%22Goodbye%20cruel%20World!%22);&win=true");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 1.0);
    assertEquals(this.mockUser.startDate, "12/2/24");
    assertEquals(
        this.mockUser.code.get("problem3").code, "System.out.println(\"Goodbye cruel World!\");");
    assertEquals(this.mockUser.code.get("problem3").date, "2024-12-03T12:00:01Z");
    assertEquals(this.mockUser.code.get("problem3").score, "2/4");

    clientConnection.disconnect();
  }

  @Test
  public void UserUpdateHistNormal() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
  }

  @Test
  public void UserUpdateHistNoID() throws IOException {
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?problemID=3&date=2024-12-03T12:00:01Z&"
                + "score=2/4&code=System.out.println(%22Goodbye%20cruel%20World!%22);&win=true");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("failure", response.get("response_type"));
    assertEquals(
        "java.lang.IllegalArgumentException: One or more required parameters "
            + "are missing or null.",
        response.get("error"));

    clientConnection.disconnect();
  }

  @Test
  public void UserUpdateHistDifferentProb()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?userID=user_2&problemID=4&date=2024-12-03T11:00:01Z&"
                + "score=2/8&code=System.out.println(%22Hello%20cruel%20World!%22);&win=true");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 2);
    assertEquals(this.mockUser.startDate, "12/2/24");
    assertEquals(
        this.mockUser.code.get("problem3").code, "System.out.println(\"Goodbye cruel World!\");");
    assertEquals(this.mockUser.code.get("problem3").date, "2024-12-03T12:00:01Z");
    assertEquals(this.mockUser.code.get("problem3").score, "2/4");
    assertEquals(
        this.mockUser.code.get("problem4").code, "System.out.println(\"Hello cruel World!\");");
    assertEquals(this.mockUser.code.get("problem4").date, "2024-12-03T11:00:01Z");
    assertEquals(this.mockUser.code.get("problem4").score, "2/8");

    clientConnection.disconnect();
  }

  public void updateHistAgainBetterScore()
      throws IOException, ExecutionException, InterruptedException {
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?userID=user_2&problemID=3&date=2024-12-03T11:30:01Z&"
                + "score=3/4&code=System.out.println(%22Bello%20cruel%20World!%22);&win=true");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 2);
    assertEquals(this.mockUser.startDate, "12/2/24");
    assertEquals(
        this.mockUser.code.get("problem3").code, "System.out.println(\"Bello cruel World!\");");
    assertEquals(this.mockUser.code.get("problem3").date, "2024-12-03T11:30:01Z");
    assertEquals(this.mockUser.code.get("problem3").score, "3/4");

    clientConnection.disconnect();
  }

  @Test
  public void UserUpdateHistBetterScoreWin()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    this.updateHistAgainBetterScore();
  }

  @Test
  public void UserUpdateHistBetterScoreLose()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?userID=user_2&problemID=3&date=2024-12-03T11:30:01Z&"
                + "score=3/4&code=System.out.println(%22Bello%20cruel%20World!%22);&win=false");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 1.0);
    assertEquals(this.mockUser.startDate, "12/2/24");
    assertEquals(
        this.mockUser.code.get("problem3").code, "System.out.println(\"Bello cruel World!\");");
    assertEquals(this.mockUser.code.get("problem3").date, "2024-12-03T11:30:01Z");
    assertEquals(this.mockUser.code.get("problem3").score, "3/4");

    clientConnection.disconnect();
  }

  @Test
  public void UserUpdateHistWorseScoreWin()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?userID=user_2&problemID=3&date=2024-12-03T11:30:01Z&"
                + "score=1/4&code=System.out.println(%22Bello%20cruel%20World!%22);&win=true");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 2);
    assertEquals(this.mockUser.startDate, "12/2/24");
    assertEquals(
        this.mockUser.code.get("problem3").code, "System.out.println(\"Goodbye cruel World!\");");
    assertEquals(this.mockUser.code.get("problem3").date, "2024-12-03T12:00:01Z");
    assertEquals(this.mockUser.code.get("problem3").score, "2/4");

    clientConnection.disconnect();
  }

  @Test
  public void UserUpdateHistWorseScoreLost()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    HttpURLConnection clientConnection =
        tryRequest(
            "UserUpdateHist?userID=user_2&problemID=3&date=2024-12-03T11:30:01Z&"
                + "score=1/4&code=System.out.println(%22Bello%20cruel%20World!%22);&win=false");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, "testTwo");
    assertEquals(this.mockUser.userID, "user_2");
    assertEquals(this.mockUser.email, "test.example@gmail.com");
    assertEquals(this.mockUser.wins, 1.0);
    assertEquals(this.mockUser.startDate, "12/2/24");
    assertEquals(
        this.mockUser.code.get("problem3").code, "System.out.println(\"Goodbye cruel World!\");");
    assertEquals(this.mockUser.code.get("problem3").date, "2024-12-03T12:00:01Z");
    assertEquals(this.mockUser.code.get("problem3").score, "2/4");

    clientConnection.disconnect();
  }

  @Test
  public void UserInfoNormal() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    HttpURLConnection clientConnection = tryRequest("UserInfo?userID=user_2");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, response.get("data").get("displayName"));
    assertEquals(this.mockUser.userID, response.get("data").get("userID"));
    assertEquals(this.mockUser.email, response.get("data").get("email"));
    assertEquals(this.mockUser.wins, response.get("data").get("wins"));
    assertEquals(this.mockUser.startDate, response.get("data").get("startDate"));
    assertEquals(
        this.mockUser.code.get("problem3").toString(),
        "code{code="
            + "'System.out.println(\"Goodbye cruel World!\");', date='2024-12-03T12:00:01Z', score='2/4'}");

    clientConnection.disconnect();
  }

  @Test
  public void UserUpdateHistTwiceThenInfo()
      throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    this.addHistUser_2();
    this.updateHistAgainBetterScore();
    HttpURLConnection clientConnection = tryRequest("UserInfo?userID=user_2");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    Map<String, Object> doc = this.mockStorage.getDocument("Users", "user_2");
    this.mockUser.updateFields(doc);

    assertEquals(this.mockUser.displayName, response.get("data").get("displayName"));
    assertEquals(this.mockUser.userID, response.get("data").get("userID"));
    assertEquals(this.mockUser.email, response.get("data").get("email"));
    assertEquals(this.mockUser.wins, response.get("data").get("wins"));
    assertEquals(this.mockUser.startDate, response.get("data").get("startDate"));
    assertEquals(
        this.mockUser.code.get("problem3").toString(),
        "code{code="
            + "'System.out.println(\"Bello cruel World!\");', date='2024-12-03T11:30:01Z', score='3/4'}");

    clientConnection.disconnect();
  }

  @Test
  public void UserInfoNoID() throws IOException {
    HttpURLConnection clientConnection = tryRequest("UserInfo");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("failure", response.get("response_type"));
    assertEquals("java.lang.IllegalArgumentException: Must specify userID", response.get("error"));

    clientConnection.disconnect();
  }

  @Test
  public void invalidEndpoint() throws IOException {
    HttpURLConnection clientConnection = tryRequest("LoserInfo");
    assertEquals(404, clientConnection.getResponseCode());
    assertEquals("Not Found", clientConnection.getResponseMessage());

    clientConnection.disconnect();
  }

  @Test
  public void noEndpoint() throws IOException {
    HttpURLConnection clientConnection = tryRequest("");
    assertEquals(404, clientConnection.getResponseCode());
    assertEquals("Not Found", clientConnection.getResponseMessage());

    clientConnection.disconnect();
  }

  @Test
  public void UsersList() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();
    HttpURLConnection clientConnection = tryRequest("UserList");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));
    List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("data");

    Assert.assertTrue(userList.toString().contains("userID=user_2"));

    clientConnection.disconnect();
  }

  @Test
  public void UserLeaderboard() throws IOException, ExecutionException, InterruptedException {
    this.setUpUser();

    HttpURLConnection clientConnection =
        tryRequest(
            "UserSet?displayName=testThree&userID=user_3&email=test3.example@gmail.com&"
                + "wins=10&date=12/9/24");
    assertEquals(200, clientConnection.getResponseCode());

    Moshi moshi = new Builder().build();
    Map<String, Map<String, Object>> response =
        moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

    assertEquals("success", response.get("response_type"));

    //    Map<String, Object> doc2 = this.mockStorage.getDocument("Users", "user_3");
    //    MockUser user2 = new MockUser();
    //    user2.updateFields(doc2);
    clientConnection.disconnect();

    HttpURLConnection clientConnection2 = tryRequest("UserLeaderboard");
    assertEquals(200, clientConnection2.getResponseCode());

    Moshi moshi2 = new Builder().build();
    Map<String, Map<String, Object>> response2 =
        moshi2
            .adapter(Map.class)
            .fromJson(new Buffer().readFrom(clientConnection2.getInputStream()));

    System.out.println(response2);
    assertEquals("success", response2.get("response_type"));
    List<Map<String, Object>> userList = (List<Map<String, Object>>) response2.get("data");
    System.out.println(userList.get(0).toString());
    Assert.assertTrue(userList.get(0).toString().contains("userID=user_3"));

    clientConnection2.disconnect();
  }
}
