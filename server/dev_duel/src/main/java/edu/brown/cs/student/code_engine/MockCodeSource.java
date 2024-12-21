package edu.brown.cs.student.code_engine;

import edu.brown.cs.student.util.AdapterRecords.CodeRecord;
import java.util.Map;

/** The MockDatsource object provides a convenient means of mocking data in tests. */
public class MockCodeSource implements ICodeEngineApi {

  @Override
  public Map<String, Object> getRuntimes() {
    return null;
  }

  @Override
  public Map<String, Object> runCode(CodeRecord payload) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'runCode'");
  }
}
