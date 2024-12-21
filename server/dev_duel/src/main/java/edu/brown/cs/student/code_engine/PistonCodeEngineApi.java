package edu.brown.cs.student.code_engine;

import com.squareup.moshi.Types;
import edu.brown.cs.student.storage.CacheStorage;
import edu.brown.cs.student.util.AdapterRecords.ApiResponseRecord;
import edu.brown.cs.student.util.AdapterRecords.CodeRecord;
import edu.brown.cs.student.util.AdapterRecords.RuntimeRecord;
import edu.brown.cs.student.util.AdapterRecords.TestRecord;
import edu.brown.cs.student.util.JsonUtil;
import edu.brown.cs.student.util.TypeResolverUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The PistonApiDatasource object models the response object returned from querying the pistonApi.
 */
// Runners should have access to the cache so that they can get test and code stuff from it..
public class PistonCodeEngineApi implements ICodeEngineApi {
  private final CacheStorage<Map<String, String>> cache;
  private final String PISTON_API_URL = "https://emkc.org/api/v2/piston/";
  private final String helperCodesPath = "data/codemap.json";
  private final Map<String, Map<String, String>>
      helperCodeMap; // contains some predefined language specific code string

  public PistonCodeEngineApi(CacheStorage<Map<String, String>> cache) {
    this.cache = cache;
    this.helperCodeMap = JsonUtil.readJsonToMap(this.helperCodesPath);
  }

  @Override
  public Map<String, Object> getRuntimes() {
    try {
      URI uri = new URI(PISTON_API_URL + "runtimes");
      URL url = uri.toURL();
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestProperty("Content-Type", "application/json");

      // implicitly open connection and get the response code.
      int status = conn.getResponseCode();
      if (status == 200) {
        try (InputStream is = conn.getInputStream()) {
          // Read and filter api response
          String apiResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          return this.filterResponse(apiResponse);
        }
      }

    } catch (URISyntaxException | MalformedURLException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println(e.getMessage()); // update this to return a server error object
    }
    return null;
  }

  /**
   * Filter api response for the required runtimes.
   *
   * @param apiResponse api response to filter for runtime
   * @return a list of api recored
   */
  private Map<String, Object> filterResponse(String apiResponse) {
    // For proof of concept: use 5 languages for now;
    Set<String> requiredRuntimes = Set.of("python", "javascript", "java");
    List<RuntimeRecord> responseList = JsonUtil.toObjectList(apiResponse, RuntimeRecord.class);

    if (responseList == null) {
      return JsonUtil.generateErrorMap("code engine error");
    }

    List<RuntimeRecord> outputList = new ArrayList<>();
    for (RuntimeRecord runtime : responseList) {
      if (requiredRuntimes.contains(runtime.language())) {
        outputList.add(runtime);
      }
    }

    Map<String, Object> clientResponse = new LinkedHashMap<>();
    clientResponse.put("response_type", "success");
    clientResponse.put("body", outputList);
    return clientResponse;
  }

  /**
   * Queries piston api to run code
   *
   * @return
   */
  public Map<String, Object> runCode(CodeRecord problem) {
    String key = "Problems/" + problem.name();

    try {
      List<TestRecord> testsRecordList =
          JsonUtil.toObjectList(this.cache.get(key).get("test"), TestRecord.class);
      if (testsRecordList == null) {
        System.out.println("__TESTS_NOT_FOUND_CACHE_ERR__: code engine timeout");
        return JsonUtil.generateErrorMap(
            "Code engine timeout: Make sure you aren't running your own code...");
      }
      Map<String, Object> responseMap = new HashMap<>();

      //      // TestRecord prefliRecordTest = testsRecordList.get(0);
      //      ApiResponseRecord preflightResponse = this.preFlightCode(problem,
      // testsRecordList.get(0));
      //      // check if there's error due to malformed code
      //      if (preflightResponse.message() != null) {
      //        System.err.println("__PREFLIGHT_RUN_ERR__: " + preflightResponse.message());
      //        return JsonUtil.generateErrorMap(preflightResponse.message());
      //      }
      //      // check if code run with error
      //      String stderr = preflightResponse.run().stderr();
      //      System.out.println("STDERR: " + stderr);
      //      if (codeRunWithError(stderr)) {
      //        String output = preflightResponse.run().stderr();
      //        responseMap.put("response_type", "bug");
      //        responseMap.put("output", toStdIOList(output));
      //        return responseMap;
      //      }
      // Time to run code with

      ApiResponseRecord testRunResponse = this.runCodeWithTest(problem);
      // check for any malformed code json error
      if (testRunResponse.message() != null) {
        String message = testRunResponse.message();
        System.err.println(message);
        return JsonUtil.generateErrorMap("Internal server error.");
      }

      // check for any execution errors
      String stderr = testRunResponse.run().stderr();
      Map<String, List<String>> stioMap = this.toStdIOMap(testRunResponse.run().output());
      List<String> outputList = stioMap.get("output");
      if (codeRunWithError(stderr)) {
        System.err.println("__TEST_RUN_HAS_ERROR__:" + outputList);
        responseMap.put("response_type", "bug");
        responseMap.put("output", outputList);
        return responseMap;
      }
      // remove and return the storage string output;
      String resultStorageString = stioMap.get("storage").get(0);
      if (!resultStorageString.contains("STORAGE=")) {
        System.out.println("__MISSING_STORAGE=_STRING_ERR__: no storage string in test result");
        return JsonUtil.generateErrorMap("Internal server error.");
      }
      Map<String, Object> testValidationMap =
          this.getTestResults(resultStorageString, key, problem.language());
      responseMap.put("tests", testValidationMap.get("tests"));
      responseMap.put("score", testValidationMap.get("score"));
      responseMap.put("output", outputList);
      responseMap.put("response_type", "success");
      return responseMap;
    } catch (Exception e) {
      System.out.println("__RUNCODE_ERR__: " + e.getMessage());
      e.printStackTrace();
      return JsonUtil.generateErrorMap("Internal server error.");
    }
  }

  /**
   * Helper method for dispatching code to api and handling response
   *
   * @param payload request payload
   * @return record of the api response
   */
  private ApiResponseRecord dispatchCode(String payload) {
    System.out.println("payload " + payload);
    try {
      URI uri = new URI(PISTON_API_URL + "execute");
      URL url = uri.toURL();

      // throttle request for 5 times
      int retries = 5;
      int backoff = 1000; // Initial backoff time in milliseconds
      for (int i = 0; i < retries; i++) {
        HttpURLConnection conn = getHttpURLConnection(payload, url);

        // implicitly open connection and read the response
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
          System.out.println("Request successful.");
          // read Http response
          return this.readHttpResponsebody(conn);
        } else if (responseCode == 429) {
          System.out.println("Rate limit exceeded. Retrying after " + backoff + " milliseconds.");
          Thread.sleep(backoff);
          backoff *= 2; // Exponential backoff
        } else {
          System.out.println("Error: " + responseCode);
          break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return new ApiResponseRecord("code engine error.", null);
  }

  @NotNull
  private HttpURLConnection getHttpURLConnection(String payload, URL url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // set connection properties
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Accept", "*");
    // send payload with request
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }
    return conn;
  }

  /**
   * Split typed code on the last closing brace. This creates room to insert the last
   *
   * @param code
   * @return
   */
  private List<String> splitCodeOnClosingBrace(String code) {
    String strippedCode = code.strip();
    return List.of(strippedCode.substring(0, strippedCode.length() - 1), "}");
  }

  /**
   * appends the series of test cases to the written code
   *
   * @param tests a list of test records
   * @param problemName name of problem
   * @return
   */
  private List<String> formUnitTestAndExpectedValsFromTestMap(
      List<TestRecord> tests, String problemName, String language) {
    StringBuilder testCode = new StringBuilder();
    StringBuilder expectedValues = new StringBuilder();

    // initialize the string
    testCode.append(this.helperCodeMap.get("initString").get(language)).append('\n');
    String appendString = this.helperCodeMap.get("appendString").get(language);
    for (TestRecord test : tests) {
      String params = (language.equalsIgnoreCase("java")) ? test.jparams() : test.params();
      String functionCall = problemName + "(" + params + ")";
      String toAppend = appendString.replace("<VAL>", functionCall);
      testCode.append(toAppend).append("\n");
      expectedValues.append(test.expected()).append("\n");
    }
    // Print out the STORED TEST results;
    testCode.append(this.helperCodeMap.get("printString").get(language)).append("\n");
    return List.of(testCode.toString(), expectedValues.toString());
  }

  /**
   * Process raw request payload into a form accepatable by piston api
   *
   * @param problem
   * @return
   */
  private ApiResponseRecord runCodeWithTest(CodeRecord problem) {
    Set<String> classLangs = Set.of("java", "c++");
    // handle error later
    // make api request payload map
    Map<String, Object> apiPayloadMap = new HashMap<>();
    apiPayloadMap.put("language", problem.language());
    apiPayloadMap.put("version", problem.version());

    String key = "Problems/" + problem.name();
    List<TestRecord> testsRecordList =
        JsonUtil.toObjectList(this.cache.get(key).get("test"), TestRecord.class);

    // handle errors later: probably none of this may be missing

    List<String> unitTestsAndexpectedValsList =
        this.formUnitTestAndExpectedValsFromTestMap(
            testsRecordList, problem.name(), problem.language());
    String unitTestsToAddToUserCode = unitTestsAndexpectedValsList.get(0);
    String codeToRun = problem.code() + "\n";
    // if test has brace, remove the last on end append the tests
    if (classLangs.contains(problem.language())) {
      String mainMethod = this.helperCodeMap.get("mainMethod").get(problem.language());
      List<String> codeSplitOnLastBraceList = this.splitCodeOnClosingBrace(codeToRun);
      codeToRun =
          codeSplitOnLastBraceList.get(0)
              + mainMethod.replace("<TESTCODE>", unitTestsToAddToUserCode)
              + codeSplitOnLastBraceList.get(1);
    } else {
      codeToRun += "\n" + unitTestsToAddToUserCode;
    }
    String payload =
        this.toPayload(new CodeRecord("", problem.language(), problem.version(), codeToRun));
    return this.dispatchCode(payload);
  }

  /*
   * Think about preflighting Later
   * @param problem
   * @return
   */
  private ApiResponseRecord preFlightCode(CodeRecord problem, TestRecord test) {
    Set<String> braceLangs = Set.of("java", "javascript", "c++");
    String problemName = "greet";
    String functionCall = problemName + "(" + test.params() + ");";
    String mainMethod = this.helperCodeMap.get("mainMethod").get(problem.language());
    String codeToRun = problem.code();
    if (braceLangs.contains(problem.language())) {
      List<String> codeSplitOnLastBrace = this.splitCodeOnClosingBrace(codeToRun);
      codeToRun =
          codeSplitOnLastBrace.get(0)
              + mainMethod.replace("<TESTCODE>", functionCall)
              + codeSplitOnLastBrace.get(1);
    } else {
      codeToRun += "\n" + functionCall;
    }
    String payload =
        this.toPayload(
            new CodeRecord(problem.name(), problem.language(), problem.version(), codeToRun));
    return this.dispatchCode(payload);
  }

  /**
   * Read read the inputstream of the given http connection as a map
   *
   * @param conn http connection instance being read;
   * @return a map the read json response as a java map instance
   */
  private ApiResponseRecord readHttpResponsebody(HttpURLConnection conn) throws Exception {
    try (InputStream is = conn.getInputStream()) {
      // Read and convert api response into map
      String response = new String(is.readAllBytes(), "utf-8");
      System.out.println(response);
      return JsonUtil.toObject(response, ApiResponseRecord.class);
    }
  }

  /**
   * Converts problem record to payload.
   *
   * @param problem
   * @return a json format of the final payload map
   */
  private String toPayload(CodeRecord problem) {
    Map<String, Object> payloadMap = new HashMap<>();
    payloadMap.put("language", problem.language());
    payloadMap.put("version", problem.version());

    // make a fileList and get file name
    List<Map<String, Object>> codeFile = new ArrayList<>();
    String fileName = "devduel" + this.helperCodeMap.get("extension").get(problem.language());
    // add code to run to file and append to api payload
    codeFile.add(Map.of("content", problem.code(), "name", fileName));
    payloadMap.put("files", codeFile);
    return JsonUtil.toMoshiJson(payloadMap);
  }

  /**
   * Check whether user code runs without any error. This checks whether the stderr field of the
   * response's run free is empty.
   *
   * @param stderr
   * @return
   */
  private boolean codeRunWithError(String stderr) {
    return stderr != null && !stderr.isEmpty();
  }

  /**
   * Converts the string of standard input/output of executed code to list. The api response contain
   * a json run field with an output field. This field contains the ordered list of console logs
   * separated by newline character.
   *
   * @param apiOutputField run output field list
   * @return
   */
  private Map<String, List<String>> toStdIOMap(String apiOutputField) {
    List<String> outputList = new ArrayList<>();
    Map<String, List<String>> toReturn = new HashMap<>();
    String stripped = apiOutputField.strip();
    String[] splittedStrings = stripped.split("\\r?\\n|\\r");
    for (int i = 0; i < splittedStrings.length; i++) {
      // Don't include our appended code to the output
      if (splittedStrings[i].contains("STORAGE=")) {
        toReturn.put("storage", List.of(splittedStrings[i]));
        break;
      }
      outputList.add(splittedStrings[i]);
    }
    toReturn.put("output", outputList);
    return toReturn;
  }

  /**
   * Converts the api run result string into tests and score format returned in response. The
   * results of running test cases with api call is concatenated in a storage string and printed to
   * stdout. This print result is obtained and reformatted in manner that is returned in the client.
   *
   * @param apiTestResultString concatenated return values from function calls
   * @param key key associated with this problem test record in cache.
   * @return a map of tests and scores or null for malformed string.
   */
  private Map<String, Object> getTestResults(
      String apiTestResultString, String key, String language) throws Exception {
    Map<String, String> cachedProblemInfo = this.cache.get(key);
    List<TestRecord> testRecordListForProblem =
        JsonUtil.toObjectList(cachedProblemInfo.get("test"), TestRecord.class);

    // remove the "STORAGE=" string from result
    String resultString = apiTestResultString.substring(8);
    String[] splittedResults = resultString.split("==SEP==");
    int len = splittedResults.length;
    int scores = 0;
    if (testRecordListForProblem != null && len != testRecordListForProblem.size()) {
      throw new Exception("Too much test cases than returned from running method calls");
    }

    List<TestRecord> testRecordList =
        JsonUtil.toObjectList(cachedProblemInfo.get("test"), TestRecord.class);
    boolean expectExact = Boolean.parseBoolean(cachedProblemInfo.get("expectExact").toLowerCase());
    String returnType = cachedProblemInfo.get("returnType");
    return getTestResultHelper(testRecordList, splittedResults, returnType, expectExact, language);
  }

  /**
   * Helper method called by getResults to compare expect test results with actual results.
   *
   * @param testRecords a list of test record contain inputs and expected values
   * @param actualResults a list containing actual results returned by method calls
   * @param returnType the string of the expected type of method call
   * @param expectExact asserts whether returned value must exactly same as expected
   * @return a map of score list of test results
   */
  private Map<String, Object> getTestResultHelper(
      List<TestRecord> testRecords,
      String[] actualResults,
      String returnType,
      boolean expectExact,
      String language)
      throws Exception {
    // Special case for doubles if necessary...
    int limit = testRecords.size();
    int score = 0;
    Type resolvedType = TypeResolverUtil.resolveType(returnType);
    List<Map<String, Object>> toReturn = new ArrayList<>();
    for (int i = 0; i < limit; i++) {
      String expected = testRecords.get(i).expected();
      String actual = actualResults[i];

      if (expectExact) {
        if (!returnType.equalsIgnoreCase("STRING")) {
          expected = expected.replaceAll(" ", "");
          actual = actual.replaceAll(" ", "");
        }
        System.out.println(actual);

        actual = (returnType.equalsIgnoreCase("DOUBLE")) ? format(actual, 6).toString() : actual;
        score += (expected.equals(actual)) ? 1 : 0;
      } else {
        if (TypeResolverUtil.isListType(resolvedType)) {
          expected = JsonUtil.toMoshiJson(JsonUtil.toObject(expected, resolvedType));
          actual = JsonUtil.toMoshiJson(JsonUtil.toObject(actual, resolvedType));

          List<Object> list1 = JsonUtil.toObject(expected, resolvedType);
          List<Object> list2 = JsonUtil.toObject(actual, resolvedType);

          if (list1.size() == list2.size() && Set.copyOf(list1).equals(Set.copyOf(list2))) {
            expected = actual;
            score++;
          }
        } else if (TypeResolverUtil.isSetType(resolvedType)) {

          String actualTemp = "[" + actual.substring(1, actual.length() - 1) + "]";
          String expectedTemp = "[" + expected.substring(1, expected.length() - 1) + "]";
          ParameterizedType pType = (ParameterizedType) resolvedType;
          Type type = Types.newParameterizedType(List.class, pType.getActualTypeArguments()[0]);
          List<Object> list1 = JsonUtil.toObject(expectedTemp, type);
          List<Object> list2 = JsonUtil.toObject(actualTemp, type);

          String actualReformatted = JsonUtil.toMoshiJson(list2);
          String expectedReformatted = JsonUtil.toMoshiJson(list1);
          actual = "{" + actualReformatted.substring(1, actualReformatted.length() - 1) + "}";
          expected = "{" + expectedReformatted.substring(1, expectedReformatted.length() - 1) + "}";

          if (list1.size() == list2.size()) {
            expected = actual;
            score++;
          }
        } else {
          actual =
              (language.equalsIgnoreCase("JAVA"))
                  ? JsonUtil.convertToJson(actual)
                  : actual.replaceAll("'", "\"");

          Object obj1 = JsonUtil.toObject(expected, resolvedType);
          Object obj2 = JsonUtil.toObject(actual, resolvedType);
          expected = JsonUtil.toMoshiJson(obj1);
          actual = JsonUtil.toMoshiJson(obj2);
          if (obj1.equals(obj2)) {
            score++;
          }
        }
      }
      System.out.println("expected: " + expected);
      System.out.println("actual: " + actual);
      toReturn.add(Map.of("actual", actual, "expected", expected));
    }
    String finalScore = score + "/" + testRecords.size();
    return Map.of("score", finalScore, "tests", toReturn);
  }

  /**
   * Rounds a String number to a specified decimal places
   *
   * @param value number to be rounded
   * @param places number of decimal places to round number to
   * @return value rounded to specified decimal places
   */
  public static String format(String value, int places) {
    if (places < 0) throw new IllegalArgumentException("Decimal places must be non-negative.");

    double number = Double.parseDouble(value);
    // Check if the number is a whole number with trailing zeros
    if (number == Math.floor(number)) {
      return new DecimalFormat("#.0").format(number);
    }
    StringBuilder pattern = new StringBuilder("#.");
    for (int i = 0; i < places; i++) {
      boolean flag = false;
      pattern.append("#");
    }

    DecimalFormat df = new DecimalFormat(pattern.toString());
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(number);
  }
}

/**
 * todo: Handle returns values that are sets separate the returns into a separate method to avoid
 * repeated checking of type Try returns that are doubles Add tests for syntax errors for each
 * programming language Add the authentication middleware. Address the cors problem as well in the
 * option
 */
