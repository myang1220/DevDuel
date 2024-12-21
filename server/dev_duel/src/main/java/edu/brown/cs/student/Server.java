package edu.brown.cs.student;

import static spark.Spark.after;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.brown.cs.student.code_engine.ICodeEngineApi;
import edu.brown.cs.student.code_engine.PistonCodeEngineApi;
import edu.brown.cs.student.endpoints.CodeHandler;
import edu.brown.cs.student.endpoints.GetProblemsHandler;
import edu.brown.cs.student.endpoints.RoomDel;
import edu.brown.cs.student.endpoints.RoomInfo;
import edu.brown.cs.student.endpoints.RoomList;
import edu.brown.cs.student.endpoints.RoomSet;
import edu.brown.cs.student.endpoints.RuntimesHandler;
import edu.brown.cs.student.endpoints.UserInfo;
import edu.brown.cs.student.endpoints.UserLeaderboard;
import edu.brown.cs.student.endpoints.UserList;
import edu.brown.cs.student.endpoints.UserSet;
import edu.brown.cs.student.endpoints.UserUpdateHist;
import edu.brown.cs.student.storage.CacheStorage;
import edu.brown.cs.student.storage.FirestoreUtil;
import edu.brown.cs.student.storage.IStorage;
import edu.brown.cs.student.storage.MockStorage;
import edu.brown.cs.student.util.AuthMiddleware;
import java.util.Map;
import org.slf4j.LoggerFactory;
import spark.Spark;

/** Hello world! */
public class Server {
  public static void main(String[] args) {
    // takes care of the console logging noise
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.WARN);

    int port = 3232;
    Spark.port(port);

    // apply the authentication middle ware
    AuthMiddleware.apply();
    // Allow full access to the API
    after(
        (request, response) -> {
          response.header("Access-Control-Allow-Origin", "*");
          response.header("Access-Control-Allow-Methods", "*");
        });

    IStorage firestoreUtils;
    IStorage mockStorage;
    ICodeEngineApi pistonApIDatasource;
    CacheStorage<Map<String, String>> problemCache;
    try {
      firestoreUtils = new FirestoreUtil();
      mockStorage = new MockStorage();
      problemCache = new CacheStorage<>(50, 30);
      pistonApIDatasource = new PistonCodeEngineApi(problemCache);

      //
      Spark.get("RoomSet", new RoomSet(firestoreUtils));
      Spark.get("RoomInfo", new RoomInfo(firestoreUtils));
      Spark.get("RoomDel", new RoomDel(firestoreUtils));
      Spark.get("RoomList", new RoomList(firestoreUtils));
      Spark.get("UserSet", new UserSet(firestoreUtils));
      Spark.get("UserUpdateHist", new UserUpdateHist(firestoreUtils));
      Spark.get("UserInfo", new UserInfo(firestoreUtils));
      Spark.get("UserLeaderboard", new UserLeaderboard(firestoreUtils));
      Spark.get("UserList", new UserList(firestoreUtils));

      // piston endpoints
      Spark.get("runtimes", new RuntimesHandler(pistonApIDatasource));
      Spark.post("runcode", new CodeHandler(pistonApIDatasource));

      // problems endpoints
      Spark.get("getproblem", new GetProblemsHandler(firestoreUtils, problemCache));

      Spark.notFound(
          (request, response) -> {
            response.status(404); // Not Found
            System.out.println("error: endpoint doesnt exist");
            return "404 Not Found - The requested endpoint does not exist.";
          });

      Spark.init();
      Spark.awaitInitialization();
      System.out.println("Server started at http://localhost:" + port);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(
          "Error: Could not initialize Firebase. Likely due to firebase_config.json not being found. Exiting.");
      System.exit(1);
    }
  }
}
