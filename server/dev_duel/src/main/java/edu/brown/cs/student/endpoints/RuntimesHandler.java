package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.code_engine.ICodeEngineApi;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import spark.Request;
import spark.Response;
import spark.Route;

public class RuntimesHandler implements Route {
  private final ICodeEngineApi datasource;

  // private final IStorage storageManager; had to comment this for now. Having problems with
  // firestore.

  public RuntimesHandler(ICodeEngineApi datasource) {
    this.datasource = datasource;
  }

  @Override
  /**
   * Handlers the GET request to obtain programming languages and versions from the provided
   * datasource.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @return json response of the request of api query
   */
  public Object handle(Request request, Response response) throws Exception {
    System.out.println("GET: " + request.url());
    // Provide information about request
    Map<String, Object> requestInfoMap = new HashMap<>();
    requestInfoMap.put("url", request.url());
    requestInfoMap.put("timestamp", System.currentTimeMillis());

    // handle responseBody
    Map<String, Object> responseMap = this.datasource.getRuntimes();
    responseMap.put("timestamp", System.currentTimeMillis());
    responseMap.put("requestInfo", requestInfoMap);
    return JsonUtil.toMoshiJson(responseMap);
  }
}
