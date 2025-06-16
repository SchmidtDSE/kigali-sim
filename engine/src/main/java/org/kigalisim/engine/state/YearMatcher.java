/**
 * Class representing a range of years where inclusion can be tested.
 *
 * <p>This class provides functionality to test whether a given year falls
 * within a specified range, supporting both bounded and unbounded ranges.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

/**
 * Class representing a range of years where inclusion can be tested.
 *
 * <p>Supports testing year inclusion within a range where null values
 * represent positive or negative infinity bounds.</p>
 */
public class YearMatcher {

  private final Integer start;
  private final Integer end;

  /**
   * Create a new year range.
   *
   * <p>Create a new year range between start and end where null in either means
   * positive or negative infinity.</p>
   *
   * @param start The starting year (inclusive) in this range or null if no min year
   * @param end The ending year (inclusive) in this range or null if no max year
   */
  public YearMatcher(Integer start, Integer end) {
    boolean hasNull = start == null || end == null;
    boolean startHasSpecial = "beginning".equals(start) || "onwards".equals(start);
    boolean endHasSpecial = "beginning".equals(end) || "onwards".equals(end);

    if (hasNull || startHasSpecial || endHasSpecial) {
      this.start = start;
      this.end = end;
    } else {
      int startRearrange = Math.min(start, end);
      int endRearrange = Math.max(start, end);

      this.start = startRearrange;
      this.end = endRearrange;
    }
  }

  /**
   * Determine if a year is included in this range.
   *
   * @param year The year to test for inclusion
   * @return true if this value is between getStart and getEnd
   */
  public boolean getInRange(int year) {
    boolean meetsMin = start == null || start <= year;
    boolean meetsMax = end == null || end >= year;
    return meetsMin && meetsMax;
  }

  /**
   * Get the start of the year range.
   *
   * @return The minimum included year in this range or null if negative infinity
   */
  public Integer getStart() {
    return start;
  }

  /**
   * Get the end of the year range.
   *
   * @return The maximum included year in this range or null if positive infinity
   */
  public Integer getEnd() {
    return end;
  }
}
