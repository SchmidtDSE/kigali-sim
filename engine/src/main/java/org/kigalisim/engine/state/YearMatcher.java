/**
 * Class representing a range of years where inclusion can be tested.
 *
 * <p>This class provides functionality to test whether a given year falls
 * within a specified range, supporting both bounded and unbounded ranges.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.Optional;

/**
 * Class representing a range of years where inclusion can be tested.
 */
public class YearMatcher {

  private final Optional<Integer> start;
  private final Optional<Integer> end;

  /**
   * Create a new year range.
   *
   * <p>Create a new year range between start and end where empty Optional in either means
   * positive or negative infinity.</p>
   *
   * @param start The starting year (inclusive) in this range or empty if no min year
   * @param end The ending year (inclusive) in this range or empty if no max year
   */
  public YearMatcher(Optional<Integer> start, Optional<Integer> end) {
    boolean hasEmpty = start.isEmpty() || end.isEmpty();

    if (hasEmpty) {
      this.start = start;
      this.end = end;
    } else {
      int startRearrange = Math.min(start.get(), end.get());
      int endRearrange = Math.max(start.get(), end.get());

      this.start = Optional.of(startRearrange);
      this.end = Optional.of(endRearrange);
    }
  }

  /**
   * Create a new year range from Integer values.
   *
   * <p>Create a new year range between start and end where null in either means
   * positive or negative infinity.</p>
   *
   * @param start The starting year (inclusive) in this range or null if no min year
   * @param end The ending year (inclusive) in this range or null if no max year
   */
  public YearMatcher(Integer start, Integer end) {
    this(Optional.ofNullable(start), Optional.ofNullable(end));
  }

  /**
   * Create a new unbounded year range.
   *
   * <p>Create a new year range with no bounds (matches any year).</p>
   *
   * @return A YearMatcher with no bounds
   */
  public static YearMatcher unbounded() {
    return new YearMatcher(Optional.empty(), Optional.empty());
  }

  /**
   * Determine if a year is included in this range.
   *
   * @param year The year to test for inclusion
   * @return true if this value is between getStart and getEnd
   */
  public boolean getInRange(int year) {
    boolean meetsMin = start.isEmpty() || start.get() <= year;
    boolean meetsMax = end.isEmpty() || end.get() >= year;
    return meetsMin && meetsMax;
  }

  /**
   * Get the start of the year range.
   *
   * @return The minimum included year in this range or empty if negative infinity
   */
  public Optional<Integer> getStart() {
    return start;
  }

  /**
   * Get the end of the year range.
   *
   * @return The maximum included year in this range or empty if positive infinity
   */
  public Optional<Integer> getEnd() {
    return end;
  }
}
