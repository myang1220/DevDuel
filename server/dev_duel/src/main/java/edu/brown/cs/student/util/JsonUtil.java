package edu.brown.cs.student.util;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class for common operations. */
public class JsonUtil {

  private static final Moshi moshi = new Moshi.Builder().build();
  private static final JsonAdapter<Object> jsonAdapter = moshi.adapter(Object.class);

  /**
   * Converts an object to a JSON string using Moshi.
   *
   * @param obj The object to convert.
   * @return JSON representation of the object.
   */
  public static String toMoshiJson(Object obj) {
    try {
      return jsonAdapter.toJson(obj);
    } catch (Exception e) {
      e.printStackTrace();
      return "{\"response_type\":\"failure\",\"error\":\"JSON conversion error.\"}";
    }
  }

  /**
   *<p>Converts json to a List of element instances.
   * This method serves as a convenient method of filtering large queries for only required data.
   * <pre>toList returns null by choice because it will mainly serve as an intermediate method call that
   * will first convert api response to list of spefic record and back to json.
   *
   * @param json the json list being converted into java list
   * @param of the object contained in the list
   * @return a list containg instances of 'of' or null on exception or empty json
   */
  public static <T> List<T> toObjectList(String json, Type of) {
    Type type = Types.newParameterizedType(List.class, of);
    JsonAdapter<List<T>> jsonAdapter = moshi.adapter(type);
    try {
      return jsonAdapter.fromJson(json);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Generates an error response map.
   *
   * @param message Error message.
   * @return Map containing the error message.
   */
  public static Map<String, Object> generateErrorMap(String message) {
    Map<String, Object> errorMap = new HashMap<>();
    errorMap.put("response_type", "failure");
    errorMap.put("error", message);
    return errorMap;
  }

  /**
   * Converts json into a map object.
   *
   * @param json json file to be converted.
   * @return
   */
  public static Map<String, Object> toMap(String json) {
    Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
    JsonAdapter<Map<String, Object>> adapter = moshi.adapter(type);
    try {
      return adapter.fromJson(json);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Converts json into a map object.
   *
   * @param json json file to be converted.
   * @return
   */
  public static Map<String, String> toStrMap(String json) {
    Type type = Types.newParameterizedType(Map.class, String.class, String.class);
    JsonAdapter<Map<String, String>> adapter = moshi.adapter(type);
    try {
      return adapter.fromJson(json);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Read json files to map object
   *
   * @param <T>
   * @param json
   * @param type
   * @return
   */
  public static Map<String, Map<String, String>> readJsonToMap(String filepath) {
    // todo: handle error with missing file later...
    Type type =
        Types.newParameterizedType(
            Map.class,
            String.class,
            Types.newParameterizedType(Map.class, String.class, String.class));
    JsonAdapter<Map<String, Map<String, String>>> adapter = moshi.adapter(type);
    try (InputStream is = new FileInputStream(filepath)) {
      String fileStream = new String(is.readAllBytes());
      return adapter.fromJson(fileStream);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null; // will throw error later to detect any failure
  }

  /**
   * Converts a json file into an instance of type object
   *
   * @param <T> generic type of object
   * @param json the json to convert
   * @param type object to be converted to
   * @return return a java object
   * @throws Exception
   */
  public static <T> T toObject(String json, Type type) throws IOException {
    JsonAdapter<T> adapter = moshi.adapter(type);
    return adapter.fromJson(json);
  }

  /**
   * Create a receipt time map for the request.
   *
   * @param url url of the request.
   * @return
   */
  public static Map<String, String> requestInfoMap(String url) {

    // Get the current local date and time
    LocalDateTime currentDateTime = LocalDateTime.now();
    // Format the date and time
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedDateTime = currentDateTime.format(formatter);
    return Map.of("url", url, "timestamp", formattedDateTime);
  }

  /**
   * Converts a java map string representation into json format
   *
   * @param input string format to be converted into json
   * @return a json format of java string representation of map
   */
  public static String convertToJson(String input) {
    // Remove the curly braces
    input = input.substring(1, input.length() - 1);

    // Replace key=value with "key": "value"
    Pattern pattern = Pattern.compile("([^,=]+)=([^,]*)");
    Matcher matcher = pattern.matcher(input);
    StringBuffer result = new StringBuffer("{");

    while (matcher.find()) {
      matcher.appendReplacement(
          result, "\"" + matcher.group(1).trim() + "\":\"" + matcher.group(2).trim() + "\"");
    }

    result.append("}");
    return result.toString();
  }
}
