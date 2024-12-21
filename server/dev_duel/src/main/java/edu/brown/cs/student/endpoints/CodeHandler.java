package edu.brown.cs.student.endpoints;

import edu.brown.cs.student.code_engine.ICodeEngineApi;
import edu.brown.cs.student.util.AdapterRecords.CodeRecord;
import edu.brown.cs.student.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import spark.Request;
import spark.Response;
import spark.Route;

/** Handles code submitted by user by querying piston api checking test cases */
public class CodeHandler implements Route {
  private final ICodeEngineApi datasource;

  public CodeHandler(ICodeEngineApi datasource) {
    this.datasource = datasource;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    Map<String, String> requestInfo = JsonUtil.requestInfoMap(request.url());
    String requestBody = request.body();
    System.out.println("requestBody: " + requestBody);
    Map<String, Object> responseMap = new HashMap<>();
    if (requestBody == null || requestBody.isEmpty()) {
      System.out.println("__EMPTY_CODERUN_REQUEST_BODY_ERR__: no request body provided");
      responseMap = JsonUtil.generateErrorMap("No request body provided");
      responseMap.put("requestInfo", requestInfo);
      return JsonUtil.toMoshiJson(responseMap);
    }

    CodeRecord codeRecord = JsonUtil.toObject(requestBody, CodeRecord.class);

    responseMap = this.datasource.runCode(codeRecord);
    responseMap.put("requestInfo", requestInfo);
    return JsonUtil.toMoshiJson(responseMap);
  }
}
