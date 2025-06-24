package org.kigalisim.engine.state;

/**
 * Interface for objects that can generate a string key for finding substance streams
 * based on application and substance usage.
 */
public interface UseKey {

  /**
   * Gets the key string used to identify substance streams.
   *
   * @return The key string for this application/substance combination
   */
  String getKey();

  /**
   * Get the application name.
   *
   * @return The application name in which the substance is used.
   */
  String getApplication();

  /**
   * Get the substance name.
   *
   * @return The name of the substance consumed in the application.
   */
  String getSubstance();
}
