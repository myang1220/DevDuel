package edu.brown.cs.student.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import spark.Spark;

public class AuthMiddleware {
  private static final String CLERK_SECRET_KEY = "";
  private static final String CLERK_API_URL = "https://api.clerk.dev/v1/tokens/verify";

  public static void apply() {
    Spark.options(
        "/*",
        (request, response) -> {
          String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
          String accessControlRequestMethod = request.headers("Access-Control-Request-Method");

          if (accessControlRequestHeaders != null) {
            response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
          }
          if (accessControlRequestMethod != null) {
            response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
          }
          // Allow all origins or specific origin
          response.type("application/json");
          response.status(200);
          return "OK";
        });
    // todo: implement the before later

    //    Spark.before(
    //        (request, response) -> {
    //          if (request.requestMethod().equalsIgnoreCase("OPTIONS")) {
    //            return;
    //          }
    //          String authHeader = request.headers("Authorization");
    //
    //          if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    //            Spark.halt(
    //                401,
    //                JsonUtil.toMoshiJson(JsonUtil.generateErrorMap("Unauthorized: No token
    // provided")));
    //          }
    //          String token = authHeader.substring(7);
    //
    //          try {
    //            boolean isValid = verifyToken(token);
    //            System.out.println("valid token? " + isValid);
    //            if (!isValid) {
    //              Spark.halt(
    //                  401,
    //                  JsonUtil.toMoshiJson(JsonUtil.generateErrorMap("Unauthorized: Invalid
    // token")));
    //            }
    //          } catch (Exception e) {
    //            e.printStackTrace();
    //            Spark.halt(
    //                500,
    //                JsonUtil.toMoshiJson(
    //                    JsonUtil.generateErrorMap("Internal Server Error: Token verification
    // failed")));
    //          }
    //        });
  }

  /**
   * Verifies the token with Clerk's API.
   *
   * @param token Clerk-issued token.
   * @return True if the token is valid, false otherwise.
   * @throws IOException If an I/O error occurs.
   */
  private static boolean verifyToken(String token) throws IOException {
    java.net.URL url = new java.net.URL(CLERK_API_URL);
    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    System.out.println(CLERK_SECRET_KEY);
    conn.setRequestProperty("Authorization", "Bearer " + CLERK_SECRET_KEY);
    conn.setDoOutput(true);

    Map<String, String> payload = new HashMap<>();
    payload.put("token", token);
    String jsonPayload = JsonUtil.toMoshiJson(payload);

    try (java.io.OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int status = conn.getResponseCode();
    if (status != 200) {
      return false;
    }

    try (InputStream is = conn.getInputStream()) {
      String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      Map<String, Object> responseMap = JsonUtil.toMap(responseBody);
      return true; // todo: implement this later
    }
  }
}
