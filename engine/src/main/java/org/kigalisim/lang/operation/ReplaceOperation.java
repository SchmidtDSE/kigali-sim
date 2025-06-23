package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Operation that replaces a substance with another substance.
 *
 * <p>This operation calculates a replacement amount and applies it to the engine.
 * It can optionally be limited to a specific time period using a ParsedDuring object.</p>
 */
public class ReplaceOperation implements Operation {

  private final Operation amountOperation;
  private final String stream;
  private final String destinationSubstance;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new ReplaceOperation that applies to all years.
   *
   * @param amountOperation The operation that calculates the replacement amount.
   * @param stream The stream to replace.
   * @param destinationSubstance The substance to replace with.
   */
  public ReplaceOperation(Operation amountOperation, String stream, String destinationSubstance) {
    this.amountOperation = amountOperation;
    this.stream = stream;
    this.destinationSubstance = destinationSubstance;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new ReplaceOperation that applies to a specific time period.
   *
   * @param amountOperation The operation that calculates the replacement amount.
   * @param stream The stream to replace.
   * @param destinationSubstance The substance to replace with.
   * @param during The time period during which this operation applies.
   */
  public ReplaceOperation(Operation amountOperation, String stream, String destinationSubstance,
      ParsedDuring during) {
    this.amountOperation = amountOperation;
    this.stream = stream;
    this.destinationSubstance = destinationSubstance;
    duringMaybe = Optional.of(during);
  }

  @Override
  public void execute(PushDownMachine machine) {
    amountOperation.execute(machine);
    EngineNumber result = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();
    engine.replace(result, stream, destinationSubstance, yearMatcher);
  }
}
