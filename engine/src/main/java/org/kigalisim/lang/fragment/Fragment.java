/**
 * Interface for a piece of interpreted QubecTalk code.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.program.ParsedApplication;
import org.kigalisim.lang.program.ParsedPolicy;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.program.ParsedScenario;
import org.kigalisim.lang.program.ParsedScenarios;
import org.kigalisim.lang.program.ParsedSubstance;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Description of a part of a parsed QubecTalk program.
 *
 * <p>Description of a part of a parsed QubecTalk program that provides a type which can be used to
 * facilitate the ANTLR visitor.</p>
 */
public abstract class Fragment {

  /**
   * Get the string parsed from the source of this fragment.
   *
   * @return The string found on this fragment.
   */
  public String getString() {
    throw new RuntimeException("This fragment does not have a string.");
  }

  /**
   * Get the calculation parsed from the source of this fragment.
   *
   * @return Operation which can be resolved to an EngineNumber at runtime.
   */
  public Operation getOperation() {
    throw new RuntimeException("This fragment does not have an operation.");
  }

  /**
   * Get the units that should be applied to the result of a calculation or to an EngineNumber.
   *
   * @return Parsed units as a string.
   */
  public String getUnit() {
    throw new RuntimeException("This fragment does not have units.");
  }

  /**
   * Get the during that should be applied in filtering if an operation should be applied.
   *
   * @return Parsed during.
   */
  public ParsedDuring getDuring() {
    throw new RuntimeException("This fragment does not have a during.");
  }

  /**
   * Get the program parsed from the source of this fragment.
   *
   * @return Parsed program.
   */
  public ParsedProgram getProgram() {
    throw new RuntimeException("This fragment does not have a program.");
  }

  /**
   * Get the policy parsed from the source of this fragment.
   *
   * @return Parsed policy.
   */
  public ParsedPolicy getPolicy() {
    throw new RuntimeException("This fragment does not have a policy.");
  }

  /**
   * Get the scenario parsed from the source of this fragment.
   *
   * @return Parsed scenario.
   */
  public ParsedScenario getScenario() {
    throw new RuntimeException("This fragment does not have a scenario.");
  }

  /**
   * Get the application parsed from the source of this fragment.
   *
   * @return Parsed application.
   */
  public ParsedApplication getApplication() {
    throw new RuntimeException("This fragment does not have an application.");
  }

  /**
   * Get the substance parsed from the source of this fragment.
   *
   * @return Parsed substance.
   */
  public ParsedSubstance getSubstance() {
    throw new RuntimeException("This fragment does not have a substance.");
  }

  /**
   * Get the scenarios parsed from the source of this fragment.
   *
   * @return The parsed scenarios.
   */
  public ParsedScenarios getScenarios() {
    throw new RuntimeException("This fragment does not have scenarios.");
  }

  /**
   * Check if this fragment is a scenarios stanza.
   *
   * @return True if this fragment is a scenarios stanza, false if another stanza.
   * @throws RuntimeException Thrown if requested on a non-stanza.
   */
  public boolean getIsStanzaScenarios() {
    throw new RuntimeException("This fragment is not a stanza.");
  }

  /**
   * Check if this fragment is a policy stanza (or default).
   *
   * @return True if policy stanza, false if another stanza.
   */
  public boolean getIsStanzaPolicyOrDefault() {
    throw new RuntimeException("This fragment is not a stanza.");
  }
}
