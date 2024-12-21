package edu.brown.cs.student.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// do we need this?
public class CacheStorage<V> {
  private final Cache<String, V> cache;

  /**
   * CacheStorage is wrapper class around Google's Guava cache api. This cache cache provides
   * constructor for setting maximum cache size and also setting the minutes to evict item after
   * acess. All methods in the class are simple wrappers around the corresponding guava api.
   */
  public CacheStorage(int maxSize, int evictAfterGetMin) {
    this.cache =
        CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(evictAfterGetMin, TimeUnit.MINUTES)
            .build();
  }

  /**
   * Wrapper around guava cache's getIfPresent
   *
   * @param key key of item to retrieve
   * @return item associated with key or null if no item found.
   */
  public V get(String key) {
    return this.cache.getIfPresent(key);
  }

  /**
   * Wrapper around guava cache's getIfPresent
   *
   * @param key
   * @return
   */
  public void put(String key, V item) {
    this.cache.put(key, item);
  }

  /**
   * @return the items in cache as unmodifiable map
   */
  public Map<String, V> asMap() {
    return this.cache.asMap();
  }
}
