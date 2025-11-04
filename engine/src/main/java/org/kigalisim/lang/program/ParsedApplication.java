/**
 * Record of an application parsed from the source of a QubecTalk program.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import java.util.Map;
import java.util.Set;
import org.kigalisim.lang.validation.DuplicateValidator;

/**
 * Record of an application parsed from the source of a QubecTalk program.
 *
 * <p>Contains the substances defined in this application with their associated operations.</p>
 */
public class ParsedApplication {

  private final String name;
  private final Map<String, ParsedSubstance> substances;

  /**
   * Create a new record of an application.
   *
   * @param name The name of the application parsed.
   * @param substances The substances defined in this application.
   */
  public ParsedApplication(String name, Iterable<ParsedSubstance> substances) {
    this.name = name;
    this.substances = DuplicateValidator.validateUniqueNames(
        substances,
        ParsedSubstance::getName,
        "substance",
        "application '" + name + "'"
    );
    DuplicateValidator.validateUniqueSubstanceEquipmentCombinations(substances, name);
  }

  /**
   * Get the name of this application.
   *
   * @return The name of this application.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the names of all substances defined in this application.
   *
   * @return Set of substance names.
   */
  public Set<String> getSubstances() {
    return substances.keySet();
  }

  /**
   * Get a specific substance by name.
   *
   * @param name The name of the substance to retrieve.
   * @return The substance with the specified name.
   * @throws IllegalArgumentException if no substance with the given name exists.
   */
  public ParsedSubstance getSubstance(String name) {
    if (!substances.containsKey(name)) {
      throw new IllegalArgumentException("No substance named " + name);
    }
    return substances.get(name);
  }
}
