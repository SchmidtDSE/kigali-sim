/**
 * Record of a policy parsed from the source of a QubecTalk program.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Record of a policy parsed from the source of a QubecTalk program.
 *
 * <p>Contains the applications defined in this policy with their associated substances.</p>
 */
public class ParsedPolicy {

  private final String name;
  private final Map<String, ParsedApplication> applications;

  /**
   * Create a new record of a policy.
   *
   * @param name The name of the policy parsed.
   * @param applications The applications defined in this policy.
   */
  public ParsedPolicy(String name, Iterable<ParsedApplication> applications) {
    this.name = name;
    this.applications = StreamSupport.stream(applications.spliterator(), false)
        .collect(Collectors.toMap(ParsedApplication::getName, Function.identity()));
  }

  /**
   * Get the name of this policy.
   *
   * @return The name of this policy.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the names of all applications defined in this policy.
   *
   * @return Set of application names.
   */
  public Set<String> getApplications() {
    return applications.keySet();
  }

  /**
   * Get a specific application by name.
   *
   * @param name The name of the application to retrieve.
   * @return The application with the specified name.
   * @throws IllegalArgumentException if no application with the given name exists.
   */
  public ParsedApplication getApplication(String name) {
    if (!applications.containsKey(name)) {
      throw new IllegalArgumentException("No application named " + name);
    }
    return applications.get(name);
  }
}
