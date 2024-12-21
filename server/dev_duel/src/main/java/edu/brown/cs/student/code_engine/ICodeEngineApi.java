package edu.brown.cs.student.code_engine;

import edu.brown.cs.student.util.AdapterRecords.CodeRecord;
import java.util.Map;

/**
 * Implementing this interface enables an object to be targeted as a datasource. A datasource is any
 * object providing any structured form of data (json,csv,etc.)
 */
public interface ICodeEngineApi {
  /**
   * Queries pistonApi for available runtimes
   *
   * @return a json list of programming languages and versions.
   */
  public Map<String, Object> getRuntimes();

  public Map<String, Object> runCode(CodeRecord payload);
}
