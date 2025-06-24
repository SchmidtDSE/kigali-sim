package org.kigalisim.engine.state;

import java.util.Optional;

/**
 * Simple implementation of UseKey that generates keys based on application and substance.
 *
 * <p>This class provides a lightweight way to generate keys for substance streams
 * without requiring a full Scope object. The key generation follows the same pattern
 * as Scope, using lazy initialization and caching for performance.</p>
 */
public class SimpleUseKey implements UseKey {

  private final Optional<String> application;
  private final Optional<String> substance;
  private volatile Optional<String> key;

  /**
   * Creates a new SimpleUseKey with the specified application and substance.
   *
   * @param application The application name, or null if not specified
   * @param substance The substance name, or null if not specified
   */
  public SimpleUseKey(String application, String substance) {
    this.application = Optional.ofNullable(application);
    this.substance = Optional.ofNullable(substance);
    this.key = Optional.empty();
  }

  /**
   * Gets the key string used to identify substance streams.
   *
   * <p>The key is lazily initialized and cached. It consists of the application
   * and substance names separated by a tab character, with "-" used for null values.</p>
   *
   * @return The unique key for this application/substance combination
   */
  @Override
  public String getKey() {
    Optional<String> localKey = key;
    if (localKey.isEmpty()) {
      synchronized (this) {
        localKey = key;
        if (localKey.isEmpty()) {
          StringBuilder keyBuilder = new StringBuilder();
          keyBuilder.append(application.orElse("-"));
          keyBuilder.append("\t");
          keyBuilder.append(substance.orElse("-"));
          String computedKey = keyBuilder.toString();
          key = localKey = Optional.of(computedKey);
        }
      }
    }
    return localKey.get();
  }

  @Override
  public String getApplication() {
    return application.orElse(null);
  }

  @Override
  public String getSubstance() {
    return substance.orElse(null);
  }
}