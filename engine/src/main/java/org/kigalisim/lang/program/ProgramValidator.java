/**
 * Validates a parsed program for cross-operation conflicts.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

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

  private static final String PRIOR_MESSAGE =
      "Weibull retirement requires equipment ages, which are derived from simulated sales. "
      + "This substance sets priorEquipment directly, so ages are unknown. You can begin the "
      + "simulation before this substance entered service, use a constant rate such as "
      + "`retire 5 % / year`, or assume the existing equipment is of typical age by adding the "
      + "`assuming new` keyword in the Advanced Editor.";

  private ProgramValidator() {
    // Utility class - prevent instantiation
  }

  /**
   * Validate a parsed program, raising a {@link RuntimeException} on the first conflict found.
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
          validateSubstance(substance);
        }
      }
    }
  }

  /**
   * Validate a single substance's operations for the prior-equipment / Weibull conflict.
   *
   * @param substance the parsed substance whose operations to validate.
   */
  private static void validateSubstance(ParsedSubstance substance) {
    boolean hasWeibullWithoutAssumingNew = false;
    boolean setsPriorEquipment = false;

    for (Operation operation : substance.getOperations()) {
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
