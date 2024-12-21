package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.storage.CacheStorage;
import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.util.AdapterRecords.TestRecord;
import edu.brown.cs.student.util.JsonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import spark.Request;
import spark.Response;
import spark.Route;

public class GetProblemsHandler implements Route {

  private final IStorage db;
  private final CacheStorage<Map<String, String>> cache;

  public GetProblemsHandler(IStorage db, CacheStorage<Map<String, String>> cache) {
    this.db = db;
    this.cache = cache;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, String> requestInfoMap = JsonUtil.requestInfoMap(request.url());
    System.out.println("GET:" + request.url() + "?" + request.queryString());
    String difficulty = request.queryParams("difficulty");
    String problemID = request.queryParams("problemID");

    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> responseBody = List.of();

    if (problemID != null) {
      responseBody = this.handleResponseBody(null, 0, problemID);

    } else if (difficulty == null) {
      System.err.println("_EMPTY_RESPONSE_ERR: no difficulty level or problemID provided");
      responseMap =
          Map.of(
              "response_type",
              "failure",
              "error",
              "Must provide either difficulty or problemID parameter.",
              "requestInfo",
              requestInfoMap);
    } else {
      String numParams = request.queryParams("number");
      int number = numParams != null ? Integer.parseInt(numParams) : 1;

      try {
        responseBody = this.handleResponseBody(difficulty, number, null);

      } catch (Exception e) {
        System.out.println("__RESPONSE_BODY_FORMATION_ERR__: " + e.getMessage());
        e.printStackTrace();
        responseMap =
            Map.of(
                "response_type",
                "failure",
                "error",
                "Internal server error.",
                "requestInfo",
                requestInfoMap);
      }
    }
    // return response Map.
    responseMap =
        Map.of("response_type", "success", "requestInfo", requestInfoMap, "body", responseBody);
    return JsonUtil.toMoshiJson(responseMap);
  }

  /**
   * Helper method formats the response body for get problems. It fetches problems from database,
   * cache them, handles the structure of the response body.
   *
   * @param difficulty
   * @param number
   * @param problemID
   * @return
   * @throws Exception on execution failed , interruption, or io exception.
   */
  private List<Map<String, Object>> handleResponseBody(
      String difficulty, int number, String problemID) throws Exception {
    List<Map<String, Object>> problems =
        (problemID != null)
            ? List.of(this.db.getProblem(problemID))
            : this.db.getProblems(difficulty, number);
    List<Map<String, Object>> response = new ArrayList<>();

    for (Map<String, Object> problem : problems) {
      String key = "Problems/" + problem.get("name").toString();

      // cache tests for retrieved problem
      Object tesObject = problem.get("tests");
      String testString = JsonUtil.toMoshiJson(tesObject);
      List<TestRecord> testsList = JsonUtil.toObjectList(testString, TestRecord.class);
      Map<String, String> toCache =
          Map.of(
              "test",
              testString,
              "expectExact",
              problem.get("expectExact").toString(),
              "returnType",
              problem.get("returnType").toString());
      this.cache.put(key, toCache);

      // send at most three sample tests to the frontend.
      List<TestRecord> sampleTests = new ArrayList<>();
      int sampleTestSize = Math.min(testsList.size(), 3);
      for (int i = 0; i < sampleTestSize; i++) {
        sampleTests.add(testsList.get(i));
      }
      // append this problem to response body.
      response.add(
          Map.of(
              "problemID",
              problem.get("problemID"),
              "name",
              problem.get("name"),
              "signature",
              problem.get("signature"),
              "description",
              problem.get("description"),
              "tests",
              sampleTests,
              "difficulty",
              problem.get("difficulty"),
              "params",
              problem.get("params")));
    }

    return response;
  }
}
