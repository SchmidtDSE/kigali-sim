/**
 * Validates a parsed program for cross-operation conflicts.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.kigalisim.lang.operation.ChangeOperation;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.operation.RetireWeibullOperation;
import org.kigalisim.lang.operation.SetOperation;

/**
 * Static validator for cross-operation constraints that are only visible after a
 * program has been parsed into its operation model.
 *
 * <p>Runs at interpret time (see {@code QubecTalkInterpreter.interpret}) so that both
 * {@code validate} and {@code run} report the same errors before a simulation executes.</p>
 */
public final class ProgramValidator {

  private static final String DEFAULT_STANZA = "default";

  private static final String PRIOR_MESSAGE =
      "Weibull retirement requires equipment ages, which are derived from simulated sales. "
      + "priorEquipment is set directly for this substance, so ages are unknown. You can begin the "
      + "simulation before this substance entered service, use a constant rate such as "
      + "`retire 5 % / year`, or assume the existing equipment is of typical age by adding the "
      + "`assuming new` keyword in the Advanced Editor.";

  private ProgramValidator() {
    // Utility class - prevent instantiation
  }

  /**
   * Validate a parsed program, raising a {@link RuntimeException} on the first conflict found.
   *
   * <p>Checks each policy on its own so that policies no scenario references are still
   * validated, then checks each scenario's stacked view (the default stanza plus the
   * scenario's policies) so that conflicts split across stanzas are caught too.</p>
   *
   * @param program the parsed program to validate.
   */
  public static void validate(ParsedProgram program) {
    for (String policyName : program.getPolicies()) {
      ParsedPolicy policy = program.getPolicy(policyName);
      for (String applicationName : policy.getApplications()) {
        ParsedApplication application = policy.getApplication(applicationName);
        for (String substanceName : application.getSubstances()) {
          ParsedSubstance substance = application.getSubstance(substanceName);
          validateOperations(substance.getOperations());
        }
      }
    }

    for (String scenarioName : program.getScenarios()) {
      validateScenario(program, program.getScenario(scenarioName));
    }
  }

  /**
   * Validate the stacked operations a scenario applies to each application and substance.
   *
   * <p>Mirrors the layering in {@code KigaliSimFacade.runTrial}: the default stanza runs
   * first and then each of the scenario's policies in order. Without this, a
   * {@code set priorEquipment} in one stanza and a Weibull retire in another would slip
   * past the per-policy check even though they meet at run time.</p>
   *
   * @param program the parsed program providing the policies.
   * @param scenario the scenario whose stacked operations to validate.
   */
  private static void validateScenario(ParsedProgram program, ParsedScenario scenario) {
    Map<String, List<Operation>> operationsByKey = new LinkedHashMap<>();
    collectPolicy(program, DEFAULT_STANZA, operationsByKey);
    for (String policyName : scenario.getPolicies()) {
      collectPolicy(program, policyName, operationsByKey);
    }

    for (List<Operation> operations : operationsByKey.values()) {
      validateOperations(operations);
    }
  }

  /**
   * Append a policy's operations to the per-application and per-substance accumulator.
   *
   * <p>Unknown policy names are ignored here; they are reported when the scenario runs.</p>
   *
   * @param program the parsed program providing the policy.
   * @param policyName the name of the policy whose operations to collect.
   * @param operationsByKey accumulator keyed by application and substance name.
   */
  private static void collectPolicy(ParsedProgram program, String policyName,
      Map<String, List<Operation>> operationsByKey) {
    if (!program.getPolicies().contains(policyName)) {
      return;
    }

    ParsedPolicy policy = program.getPolicy(policyName);
    for (String applicationName : policy.getApplications()) {
      ParsedApplication application = policy.getApplication(applicationName);
      for (String substanceName : application.getSubstances()) {
        String key = applicationName + "\t" + substanceName;
        operationsByKey.computeIfAbsent(key, unused -> new ArrayList<>())
            .addAll(application.getSubstance(substanceName).getOperations());
      }
    }
  }

  /**
   * Validate a set of operations for the prior-equipment / Weibull conflict.
   *
   * @param operations the operations to validate together.
   */
  private static void validateOperations(Iterable<Operation> operations) {
    boolean hasWeibullWithoutAssumingNew = false;
    boolean setsPriorEquipment = false;

    for (Operation operation : operations) {
      if (operation instanceof RetireWeibullOperation weibull && !weibull.getAssumingNew()) {
        hasWeibullWithoutAssumingNew = true;
      }
      if (operation instanceof SetOperation set && "priorEquipment".equals(set.getStream())) {
        setsPriorEquipment = true;
      }
      if (operation instanceof ChangeOperation change && "priorEquipment".equals(change.getStream())) {
        setsPriorEquipment = true;
      }
    }

    if (hasWeibullWithoutAssumingNew && setsPriorEquipment) {
      throw new RuntimeException(PRIOR_MESSAGE);
    }
  }
}
