package edu.brown.cs.student.util;

import java.util.List;
import java.util.Map;

/**
 * AdapterRecords class contain utility record for converting between json and java object using
 * Moshi.
 */
public class AdapterRecords {
  /** ProblemRecord is used as a moshi adapter to convert json to java object */
  public record CodeRecord(String name, String language, String version, String code) {}

  /** TestRecord is used as a moshi adapter to convert json to java object */
  public record TestRecord(String params, String expected, String jparams) {}

  /** RuntimeRecord is used to serialize and deserialize respons e json */
  public record RuntimeRecord(String language, String version) {}

  public record ApiResponseRunFieldRecord(String stderr, String output) {}

  public record ApiResponseRecord(String message, ApiResponseRunFieldRecord run) {}

  public record ProblemRecord(
      String problemID,
      String name,
      String description,
      String difficulty,
      boolean expectExact,
      String returnType,
      Map<String, String> signature,
      List<Map<String, String>> tests) {}

  public record CodeRunResponseRecord(
      String score, List<String> output, List<Map<String, String>> tests, String response_type) {}
}
