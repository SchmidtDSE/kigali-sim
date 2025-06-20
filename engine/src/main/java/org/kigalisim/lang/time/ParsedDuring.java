/**
 * Description of QubecTalk time period.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Description of a "during" time period described in QubecTalk.
 *
 * <div>Description of a "during" time period described in QubecTalk where the following are
 * observed:
 *
 * <ul>
 *   <li>Single year ranges should have both start and end with the same TimePoints.</li>
 *   <li>
 *     A range which should apply starting in a year until the end may have start be non-empty and
 *     the end is empty. Alternatively, start may be given and empty be onwards (dynamic cap).
 *   </li>
 *   <li>
 *     A range which should apply until a given year may have start be non-empty an the start is
 *     empty. Alternatively, end may be given and start be beginning (dynamic cap).
 *   </li>
 *   <li>Other ranges may have start and end both specified.</li>
 * </ul>
 * </div>
 */
public class ParsedDuring {

  private final Optional<TimePointFuture> start;
  private final Optional<TimePointFuture> end;

  /**
   * Create a description of a "during" time period.
   *
   * @param start The start of the period, or empty if unbounded.
   * @param end The end of the period, or empty if unbounded.
   */
  public ParsedDuring(Optional<TimePointFuture> start, Optional<TimePointFuture> end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Get the start of the period.
   *
   * @return The start of the period, or empty if unbounded.
   */
  public Optional<TimePointFuture> getStart() {
    return start;
  }

  /**
   * Get the end of the period.
   *
   * @return The end of the period, or empty if unbounded.
   */
  public Optional<TimePointFuture> getEnd() {
    return end;
  }

  /**
   * Build a YearMatcher from this parsed version of a during statement.
   *
   * <p>If a TimePointFuture is a dynamic cap (beginning or onwards), it is interpreted as
   * Optional.empty().</p>
   *
   * @param machine Machine to use for calculations if needed.
   * @return The YearMatcher built from the values parsed and saved to this ParsedDuring.
   */
  public YearMatcher buildYearMatcher(PushDownMachine machine) {
    Optional<Integer> startYear = Optional.empty();
    Optional<Integer> endYear = Optional.empty();

    // Process start time point if present
    if (start.isPresent()) {
      TimePointRealized startRealized = start.get().realize(machine);
      if (!startRealized.isDynamicCap()) {
        EngineNumber startValue = startRealized.getPointValue();
        startYear = Optional.of(startValue.getValue().intValue());
      }
    }

    // Process end time point if present
    if (end.isPresent()) {
      TimePointRealized endRealized = end.get().realize(machine);
      if (!endRealized.isDynamicCap()) {
        EngineNumber endValue = endRealized.getPointValue();
        endYear = Optional.of(endValue.getValue().intValue());
      }
    }

    return new YearMatcher(startYear, endYear);
  }

}
