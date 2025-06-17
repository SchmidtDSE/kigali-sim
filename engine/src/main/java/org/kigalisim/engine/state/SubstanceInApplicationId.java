/**
 * Class representing a unique identifier for a substance within a specific application.
 *
 * <p>This class provides a unique identifier combining an application name and
 * substance name to represent a substance used within a specific application context.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

/**
 * Class representing a unique identifier for a substance within a specific application.
 *
 * <p>Encapsulates the combination of application and substance names to create
 * a unique identifier for substance-application pairs.</p>
 */
public class SubstanceInApplicationId {

  private final String application;
  private final String substance;

  /**
   * Create a new substance-in-application identifier.
   *
   * @param application The name of the application (e.g., "domestic refrigeration")
   * @param substance The name of the substance (e.g., "HFC-134a")
   */
  public SubstanceInApplicationId(String application, String substance) {
    this.application = application;
    this.substance = substance;
  }

  /**
   * Get the name of the application.
   *
   * @return The application name associated with this identifier
   */
  public String getApplication() {
    return application;
  }

  /**
   * Get the name of the substance.
   *
   * @return The substance name associated with this identifier
   */
  public String getSubstance() {
    return substance;
  }
}
