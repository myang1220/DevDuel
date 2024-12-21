package edu.brown.cs.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.brown.cs.student.code_engine.ICodeEngineApi;
import edu.brown.cs.student.code_engine.PistonCodeEngineApi;
import edu.brown.cs.student.endpoints.CodeHandler;
import edu.brown.cs.student.endpoints.GetProblemsHandler;
import edu.brown.cs.student.endpoints.RuntimesHandler;
import edu.brown.cs.student.storage.CacheStorage;
import edu.brown.cs.student.storage.FirestoreUtil;
import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.AdapterRecords.RuntimeRecord;
import edu.brown.cs.student.util.JsonUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import spark.Spark;

public class ProblemTest {

  private CacheStorage<Map<String, String>> problemCache;

  @BeforeAll
  public static void setup_before_everything() {
    Spark.port(3000);
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.WARN);
  }

  @BeforeEach
  public void setup() throws Exception {

    IStorage firestoreUtils = new FirestoreUtil();
    this.problemCache = new CacheStorage<>(5, 30);
    ICodeEngineApi pistonApIDatasource = new PistonCodeEngineApi(this.problemCache);

    Spark.get("runtimes", new RuntimesHandler(pistonApIDatasource));
    Spark.post("runcode", new CodeHandler(pistonApIDatasource));
    Spark.get("getproblem", new GetProblemsHandler(firestoreUtils, this.problemCache));

    Spark.init();
    Spark.awaitInitialization(); // don't continue until the server is listening
    System.out.println("Server started at http://localhost:" + Spark.port());
  }

  @AfterEach
  public void teardown() {
    // Gracefully stop Spark listening on both endpoints after each test
    Spark.unmap("runcode");
    Spark.unmap("getproblem");
    Spark.unmap("runtimes");

    Spark.awaitStop(); // don't proceed until the server is stopped
  }

  @Test
  public void testGetRuntimes() {
    try {
      // send a request
      HttpURLConnection connection = tryRequest("/runtimes", "GET");
      // Read the response
      assertEquals(200, connection.getResponseCode());
      String responseBody = readHttpResponse(connection);
      assertNotNull(responseBody);
      Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
      assertEquals("success", responseMap.get("response_type"));
      Map<String, String> versions =
          Map.of("python", "3.10.0", "java", "15.0.2", "javascript", "18.15.0");
      List<RuntimeRecord> runtimeRecords =
          JsonUtil.toObjectList(JsonUtil.toMoshiJson(responseMap.get("body")), RuntimeRecord.class);
      assertNotNull(runtimeRecords);
      for (RuntimeRecord runtimeRecord : runtimeRecords) {
        String language = runtimeRecord.language();
        String version = runtimeRecord.version();
        if (versions.containsKey(language) && language.equals("javascript")) {
          assertTrue(version.equals("1.32.3") || version.equals(versions.get(language)));
        } else {
          assertEquals(versions.get(language), version);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetProblemEasyDifficulty() {
    try {
      // try a request
      HttpURLConnection connection = tryRequest("/getproblem?difficulty=easy", "GET");
      String responseBody = readHttpResponse(connection);
      System.out.println(responseBody);
      Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
      assertNotNull(responseMap);
      List<Map<String, String>> problems =
          JsonUtil.toObjectList(JsonUtil.toMoshiJson(responseMap.get("body")), Map.class);
      assertNotNull(problems);
      for (Map<String, String> problem : problems) {
        assertEquals("easy", problem.get("difficulty"));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testGetProblemsMediumDifficulty() {
    try {
      // try a request
      HttpURLConnection connection = tryRequest("/getproblem?difficulty=medium", "GET");
      String responseBody = readHttpResponse(connection);
      System.out.println(responseBody);
      Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
      assertNotNull(responseMap);
      List<Map<String, String>> problems =
          JsonUtil.toObjectList(JsonUtil.toMoshiJson(responseMap.get("body")), Map.class);
      assertNotNull(problems);
      for (Map<String, String> problem : problems) {
        assertEquals("medium", problem.get("difficulty"));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testGetProblemsHardDifficulty() {
    try {
      // try a request
      HttpURLConnection connection = tryRequest("/getproblem?difficulty=hard", "GET");
      String responseBody = readHttpResponse(connection);
      System.out.println(responseBody);
      Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
      assertNotNull(responseMap);
      List<Map<String, String>> problems =
          JsonUtil.toObjectList(JsonUtil.toMoshiJson(responseMap.get("body")), Map.class);
      assertNotNull(problems);
      for (Map<String, String> problem : problems) {
        assertEquals("hard", problem.get("difficulty"));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testGetProblemsByProblemID() {
    List<Map<String, String>> problemsList =
        List.of(Map.of("name", "mergeTwo", "id", "1", "difficulty", "easy"));

    try {
      for (Map<String, String> problemMap : problemsList) {
        HttpURLConnection connection =
            tryRequest("getproblem?problemID=" + problemMap.get("id"), "GET");
        String responseBody = readHttpResponse(connection);
        System.out.println(responseBody);
        Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
        assertNotNull(responseMap);
        List<Map<String, String>> problems =
            JsonUtil.toObjectList(JsonUtil.toMoshiJson(responseMap.get("body")), Map.class);
        assertNotNull(problems);
        assertEquals(problemMap.get("name"), problems.get(0).get("name"));
        assertEquals(problemMap.get("difficulty"), problems.get(0).get("difficulty"));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Send payload to the output stream of the given connection
   *
   * @param conn the HttpURLConnection instance
   * @param payload payload to send to the output stream
   * @throws Exception on any exception
   */
  private static void sendRequest(HttpURLConnection conn, String payload) throws Exception {
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }
  }

  /**
   * Read the response from the given connection
   *
   * @param conn
   * @return the response body
   */
  private static String readHttpResponse(HttpURLConnection conn) throws Exception {
    try (InputStream is = conn.getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Helper method for setting up connections
   *
   * @param apiCall - api endpint
   * @param method - http method used to send request.
   */
  private static HttpURLConnection tryRequest(String apiCall, String method) throws Exception {
    method = method == null ? "GET" : method;
    URI uri = new URI("http://localhost:" + Spark.port() + "/" + apiCall);
    URL requestURL = uri.toURL();
    HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();
    clientConnection.setRequestMethod(method);
    if (method.equalsIgnoreCase("POST")) {
      clientConnection.setDoOutput(true);
    }
    clientConnection.addRequestProperty("Content-Type", "application/json");
    return clientConnection;
  }
}
// todo: Add a couple integration test for getting problems and runcode after we add problems to the
// backend.
