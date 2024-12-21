package edu.brown.cs.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.brown.cs.student.code_engine.ICodeEngineApi;
import edu.brown.cs.student.code_engine.PistonCodeEngineApi;
import edu.brown.cs.student.endpoints.CodeHandler;
import edu.brown.cs.student.endpoints.RuntimesHandler;
import edu.brown.cs.student.storage.CacheStorage;
import edu.brown.cs.student.storage.FirestoreUtil;
import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.AdapterRecords.CodeRecord;
import edu.brown.cs.student.util.AdapterRecords.CodeRunResponseRecord;
import edu.brown.cs.student.util.AdapterRecords.TestRecord;
import edu.brown.cs.student.util.JsonUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import spark.Spark;

public class RunCodeTest {

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

    Spark.init();
    Spark.awaitInitialization(); // don't continue until the server is listening
    System.out.println("Server started at http://localhost:" + Spark.port());
  }

  @AfterEach
  public void teardown() {
    // Gracefully stop Spark listening on both endpoints after each test
    Spark.unmap("runcode");
    Spark.unmap("runtimes");

    Spark.awaitStop(); // don't proceed until the server is stopped
  }

  @Test
  public void testRunSimpleCode() {

    // update the cache
    List<TestRecord> testRecs =
        List.of(
            new TestRecord("\"Henry\"", "Hello Henry", "\"Henry\""),
            new TestRecord("\"Mary\"", "Hello Mary", "\"Mary\""),
            new TestRecord("\"Harry\"", "Hello Harry", "\"Harry\""));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "true", "returnType", "String");
    this.problemCache.put("Problems/greet", toCache);

    // mock typed code for each language;
    List<Map<String, String>> programList = new ArrayList<>();
    // java code
    programList.add(
        Map.of(
            "language",
            "java",
            "version",
            "15.0.2",
            "code",
            "public class Solution { public static String greet(String name) { return \"Hello \" + name; } }"));
    // python code
    programList.add(
        Map.of(
            "language",
            "python",
            "version",
            "3.10.0",
            "code",
            "def greet(name: str) -> str:\n\treturn \"Hello \" + name"));
    // javascript code
    programList.add(
        Map.of(
            "language",
            "javascript",
            "version",
            "18.15.0",
            "code",
            "function greet(name) {return \"Hello \" + name;}"));

    for (Map<String, String> program : programList) {
      CodeRecord codeRecord =
          new CodeRecord(
              "greet", program.get("language"), program.get("version"), program.get("code"));
      String payload = JsonUtil.toMoshiJson(codeRecord);
      // throttle request
      int retries = 5;
      int backoff = 1000; // Initial backoff interval in milliseconds

      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        for (int i = 0; i < retries; i++) {

          // write to connection
          sendRequest(conn, payload);
          // read response

          int responseCode = conn.getResponseCode();
          if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Request successful.");
            break;
          } else if (responseCode == 429) {
            System.out.println("Rate limit exceeded. Retrying after " + backoff + " milliseconds.");
            Thread.sleep(backoff);
            backoff *= 2; // Exponential backoff
          } else {
            System.out.println("Error: " + responseCode);
            fail("Unexpected Http status code returned.");
          }
        }

        String responseBody = readHttpResponse(conn);
        System.out.println("response: " + responseBody);

        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("success", responseRecord.response_type());
        assertEquals("3/3", responseRecord.score());
        assertTrue(responseRecord.output().isEmpty());
        // check the tests
        for (Map<String, String> test : responseRecord.tests()) {
          assertEquals(test.get("expected"), test.get("actual"));
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testRunCodeReturnListOrderIrrelevant() {
    // update the cache
    List<TestRecord> testRecs =
        List.of(
            new TestRecord(
                "[1,2,2,3,4, 5],[2, 3, 5]", "[2, 3,5]", "List.of(1,2,2,3,4, 5), List.of( 2, 3, 5)"),
            new TestRecord(
                "[1,2,2,3,4, 5],[2, 3, 5]",
                "[5, 3, 2]",
                "List.of(1,2,2,3,4, 5), List.of( 2, 3, 5)"),
            new TestRecord(
                "[1,2,2,3,4, 5],[2, 3, 5]",
                "[3, 2,5]",
                "List.of(1,2,2,3,4, 5), List.of( 2, 3, 5)"));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "false", "returnType", "List<Integer>");
    this.problemCache.put("Problems/intersectLists", toCache);

    // mock typed code for each language;
    List<Map<String, String>> programList = new ArrayList<>();
    // python code

    programList.add(
        Map.of(
            "language",
            "python",
            "version",
            "3.10.0",
            "code",
            "def intersectLists(list1, list2):\n"
                + "    # Convert the lists to sets to find the intersection\n"
                + "    set1 = set(list1)\n"
                + "    set2 = set(list2)\n"
                + "    \n"
                + "    # Find the intersection of the sets\n"
                + "    intersection = set1.intersection(set2)\n"
                + "    \n"
                + "    # Convert the intersection set back to a list\n"
                + "    return list(intersection)\n"
                + "\n"));
    // java code
    programList.add(
        Map.of(
            "language",
            "java",
            "version",
            "15.0.2",
            "code",
            """
                 import java.util.*;
                 public class Solution{
                  public static List<Integer> intersectLists(List<Integer> list1, List<Integer> list2) {
                    // Convert the lists to sets to find the intersection
                    Set<Integer> set1 = new HashSet<>(list1);
                    Set<Integer> set2 = new HashSet<>(list2);
                   \s
                    // Find the intersection of the sets
                     set1.retainAll(set2);
                   \s
                    // Return the intersection as a list
                    return List.copyOf(set1);
                }
                }
                """));
    // javascript code
    programList.add(
        Map.of(
            "language",
            "javascript",
            "version",
            "18.15.0",
            "code",
            "function intersectLists(list1, list2) {\n"
                + "    // Convert the lists to sets to find the intersection\n"
                + "    const set1 = new Set(list1);\n"
                + "    const set2 = new Set(list2);\n"
                + "    \n"
                + "    // Find the intersection of the sets\n"
                + "    const intersection = [...set1].filter(x => set2.has(x));\n"
                + "    \n"
                + "    // Return the intersection as a list\n"
                + "    return intersection;\n"
                + "}\n"));

    for (Map<String, String> program : programList) {
      CodeRecord codeRecord =
          new CodeRecord(
              "intersectLists",
              program.get("language"),
              program.get("version"),
              program.get("code"));
      String payload = JsonUtil.toMoshiJson(codeRecord);
      // throttle request
      int retries = 5;
      int backoff = 1000; // Initial backoff interval in milliseconds

      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        for (int i = 0; i < retries; i++) {

          // write to connection
          sendRequest(conn, payload);

          // read response
          int responseCode = conn.getResponseCode();
          if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Request successful.");
            break;
          } else if (responseCode == 429) {
            System.out.println("Rate limit exceeded. Retrying after " + backoff + " milliseconds.");
            Thread.sleep(backoff);
            backoff *= 2; // Exponential backoff
          } else {
            System.out.println("Error: " + responseCode);
            fail("Unexpected Http status code returned");
          }
        }

        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("success", responseRecord.response_type());
        assertEquals("3/3", responseRecord.score());
        assertTrue(responseRecord.output().isEmpty());
        // check the tests
        for (Map<String, String> test : responseRecord.tests()) {
          assertEquals(test.get("expected"), test.get("actual"));
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testRunCodeReturnsSet() {
    // update the cache
    List<TestRecord> testRecs =
        List.of(
            new TestRecord("[1,2,2,3,4, 5]", "{1, 2, 3, 4, 5}", "List.of(1,2,2,3,4, 5)"),
            new TestRecord("[1,2,2,3,4, 5]", "{5, 3, 2, 1, 4}", "List.of(1,2,2,3,4, 5)"),
            new TestRecord("[1,2,2,3,4, 5]", "{3, 2, 5, 2, 1}", "List.of(1,2,2,3,4, 5)"));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "false", "returnType", "Set<Integer>");
    this.problemCache.put("Problems/listToSet", toCache);
    // mock typed code for each language;
    List<Map<String, String>> programList = new ArrayList<>();
    // python code

    programList.add(
        Map.of(
            "language",
            "python",
            "version",
            "3.10.0",
            "code",
            "def listToSet(input_list):\n" + "    return set(input_list)\n" + "\n"));
    // javascript code
    programList.add(
        Map.of(
            "language",
            "javascript",
            "version",
            "18.15.0",
            "code",
            "function listToSet(inputList) {\n" + " return new Set(inputList);\n" + "}\n" + "\n"));

    for (Map<String, String> program : programList) {
      CodeRecord codeRecord =
          new CodeRecord(
              "listToSet", program.get("language"), program.get("version"), program.get("code"));
      String payload = JsonUtil.toMoshiJson(codeRecord);
      // throttle request
      int retries = 5;
      int backoff = 1000; // Initial backoff interval in milliseconds

      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        // write to connection
        sendRequest(conn, payload);
        // read request
        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("success", responseRecord.response_type());
        assertEquals("3/3", responseRecord.score());
        assertTrue(responseRecord.output().isEmpty());
        // check the tests
        for (Map<String, String> test : responseRecord.tests()) {
          assertEquals(test.get("expected"), test.get("actual"));
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testRuncodeReturnsMap() {
    List<TestRecord> testRecs =
        List.of(new TestRecord("", JsonUtil.toMoshiJson(Map.of("a", "1", "b", "2", "c", "3")), ""));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "false", "returnType", "Map<String, String>");
    this.problemCache.put("Problems/createMap", toCache);
    // mock typed code for each language;
    List<Map<String, String>> programList = new ArrayList<>();
    // python code

    programList.add(
        Map.of(
            "language",
            "python",
            "version",
            "3.10.0",
            "code",
            "def createMap():\n"
                + "    my_dict = {}\n"
                + "    \n"
                + "    # Adding key-value pairs to the dictionary\n"
                + "    my_dict['a'] = '1'\n"
                + "    my_dict['b'] = '2'\n"
                + "    my_dict['c'] = '3'\n"
                + "    \n"
                + "    return my_dict\n"));
    // javascript code
    programList.add(
        Map.of(
            "language",
            "javascript",
            "version",
            "18.15.0",
            "code",
            "function createMap() {\n"
                + "    let myMap = new Map();\n"
                + "    \n"
                + "    // Adding key-value pairs to the map\n"
                + "    myMap.set('a', '1');\n"
                + "    myMap.set('b', '2');\n"
                + "    myMap.set('c', '3');\n"
                + "    \n"
                + "    return myMap;\n"
                + "}\n"));
    // java code
    programList.add(
        Map.of(
            "language",
            "java",
            "version",
            "15.0.2",
            "code",
            """
                import java.util.*;
                public class Solution {
                  public static Map<String, String> createMap() {
                    return Map.of("a","1","b","2","c","3");
                    }
                  }
                """));

    for (Map<String, String> program : programList) {
      CodeRecord codeRecord =
          new CodeRecord(
              "createMap", program.get("language"), program.get("version"), program.get("code"));
      String payload = JsonUtil.toMoshiJson(codeRecord);

      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        // write to connection
        sendRequest(conn, payload);
        // read request
        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("success", responseRecord.response_type());
        assertEquals("1/1", responseRecord.score());
        assertTrue(responseRecord.output().isEmpty());
        // check the tests
        for (Map<String, String> test : responseRecord.tests()) {
          assertEquals(JsonUtil.toMap(test.get("expected")), JsonUtil.toMap(test.get("actual")));
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testRunCodeReturnsDouble() {
    // update the cache
    List<TestRecord> testRecs =
        List.of(
            new TestRecord("-40", "-40.0", "-40.0"),
            new TestRecord("37", "98.6", "37.0"),
            new TestRecord("23.98765", "75.17777", "23.98765"));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "true", "returnType", "Double");
    this.problemCache.put("Problems/toFahrenheit", toCache);
    // mock typed code for each language;
    List<Map<String, String>> programList = new ArrayList<>();
    // python code

    programList.add(
        Map.of(
            "language",
            "python",
            "version",
            "3.10.0",
            "code",
            "def toFahrenheit(temp):\n" + "    return 9/5 * temp + 32\n" + "\n"));
    // java code
    programList.add(
        Map.of(
            "language",
            "java",
            "version",
            "15.0.2",
            "code",
            """
                public class Solution {
                  public static double toFahrenheit(double temp) {
                   return (9 * temp) / 5 + 32.0;
                  }
                }
                """));
    // javascript code
    programList.add(
        Map.of(
            "language",
            "javascript",
            "version",
            "18.15.0",
            "code",
            "function toFahrenheit(temp) {\n" + " return 9/5 * temp + 32;\n" + "}\n" + "\n"));

    for (Map<String, String> program : programList) {
      CodeRecord codeRecord =
          new CodeRecord(
              "toFahrenheit", program.get("language"), program.get("version"), program.get("code"));
      String payload = JsonUtil.toMoshiJson(codeRecord);
      // throttle request
      int retries = 5;
      int backoff = 1000; // Initial backoff interval in milliseconds

      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        // write to connection
        sendRequest(conn, payload);
        // read request
        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("success", responseRecord.response_type());
        assertEquals("3/3", responseRecord.score());
        assertTrue(responseRecord.output().isEmpty());
        // check the tests
        for (Map<String, String> test : responseRecord.tests()) {
          assertEquals(test.get("expected"), test.get("actual"));
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  // Tests for common python syntax error
  @Test
  public void testRunCodeIndentationError() {
    List<TestRecord> testRecs = List.of(new TestRecord("", "", ""));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "true", "returnType", "String");
    this.problemCache.put("Problems/errorCode", toCache);

    // create problem
    List<String> errorCodes =
        List.of(
            """
                def errorCode():
                return "this is an error code"
                """,
            """
                  def errorCode():
                  print("Hello world")
                return "error code"
                """,
            """
                def errorCode():
                  print("Hello world")

                  var = 3
                return "Hello world"

                """);
    for (int i = 0; i < errorCodes.size(); i++) {
      System.out.println("run" + i);
      CodeRecord codeRecord = new CodeRecord("errorCode", "python", "3.10.0", errorCodes.get(i));
      String payload = JsonUtil.toMoshiJson(codeRecord);
      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        // write to connection
        sendRequest(conn, payload);
        // read request
        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("bug", responseRecord.response_type());
        assertFalse(responseRecord.output().isEmpty());
        // check that output contains indentation error
        String indentErrorMsg = responseRecord.output().get(responseRecord.output().size() - 1);
        System.out.println(indentErrorMsg);
        assertTrue(
            indentErrorMsg.contains("IndentationError")
                || indentErrorMsg.contains("SyntaxError: 'return' outside function"));

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testRunCodeSyntaxError() {
    List<TestRecord> testRecs = List.of(new TestRecord("", "", ""));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "true", "returnType", "String");
    this.problemCache.put("Problems/errorCode", toCache);

    // create problem
    List<String> errorCodes =
        List.of( // misspelled keyword
            """
                def errorCode():
                  whille(True):
                    print("still running")
                """, // unmatched parenthesis
            """
                def errorCode():
                  x = 3
                  y = (x + 3 * 2
                """, // missing colon
            """
                def errorCode()
                  print("Hello world");
                """, // using reserved keyword as variable
            """
                def errorCode():
                  class = "this is a big class"
                """, // incorrect use of operators
            """
                def errorCode():
                 radius = 3
                 area = radius * 2pi
                 """);
    for (int i = 0; i < errorCodes.size(); i++) {
      System.out.println("run" + i);
      CodeRecord codeRecord = new CodeRecord("errorCode", "python", "3.10.0", errorCodes.get(i));
      String payload = JsonUtil.toMoshiJson(codeRecord);
      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        // write to connection
        sendRequest(conn, payload);
        // read request
        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        CodeRunResponseRecord responseRecord =
            JsonUtil.toObject(responseBody, CodeRunResponseRecord.class);
        assertEquals("bug", responseRecord.response_type());
        assertFalse(responseRecord.output().isEmpty());
        // check that output contains indentation error
        String indentErrorMsg = responseRecord.output().get(responseRecord.output().size() - 1);
        System.out.println(indentErrorMsg);
        assertTrue(indentErrorMsg.contains("SyntaxError:"));

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testRuncodePaylodMissingAField() {
    List<TestRecord> testRecs = List.of(new TestRecord("", "", ""));
    String testString = JsonUtil.toMoshiJson(testRecs);
    Map<String, String> toCache =
        Map.of("test", testString, "expectExact", "true", "returnType", "String");
    this.problemCache.put("Problems/errorCode", toCache);
    List<CodeRecord> payloadList =
        List.of(
            new CodeRecord("errorCode", "", "3.10.0", "def errorCode(): return \"hi\""),
            new CodeRecord("errorCode", "python", "", "def errorCode(): return \"hi\""));

    for (CodeRecord codeRecord : payloadList) {
      String payload = JsonUtil.toMoshiJson(codeRecord);
      try {
        // open connection
        HttpURLConnection conn = tryRequest("runcode", "POST");
        // write to connection
        sendRequest(conn, payload);
        // read request
        String responseBody = readHttpResponse(conn);
        System.out.println("response " + responseBody);
        Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
        assertNotNull(responseMap);
        assertEquals("failure", responseMap.get("response_type"));
        // check that output contains indentation error
        assertEquals(responseMap.get("error"), "Internal server error.");

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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
