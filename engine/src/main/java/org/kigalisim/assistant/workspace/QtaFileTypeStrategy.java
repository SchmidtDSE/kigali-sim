/**
 * File type strategy for QubecTalk simulation files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Outlines and extracts the top-level stanzas of a QubecTalk simulation.
 *
 * <p>A forgiving, line-oriented scanner is used rather than the full ANTLR parser so that the
 * assistant can still outline and read a file which is only partially edited. Nested constructs
 * such as {@code define} and {@code uses} blocks do not affect top-level stanza boundaries.</p>
 */
public class QtaFileTypeStrategy implements TextFileTypeStrategy {

  /**
   * The file extension supported by this strategy.
   */
  public static final String EXTENSION = ".qta";

  private static final Pattern START_PATTERN = Pattern.compile(
      "^\\s*start\\s+(about|default|policy|simulations)\\b",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern END_PATTERN = Pattern.compile(
      "^\\s*end\\s+(about|default|policy|simulations)\\b",
      Pattern.CASE_INSENSITIVE);

  @Override
  public Set<String> getFileExtensions() {
    return Set.of(EXTENSION);
  }

  @Override
  public boolean getIsStringNative() {
    return true;
  }

  @Override
  public String interpret(String contents) {
    return contents;
  }

  @Override
  public String interpret(String contents, String section) {
    List<String> lines = splitLines(contents);
    for (Stanza stanza : parseStanzas(contents)) {
      if (matchesSection(stanza, section)) {
        return String.join("\n", lines.subList(stanza.getStartLine(), stanza.getEndLine() + 1));
      }
    }
    throw new NoSuchElementException("No section named '" + section + "'");
  }

  @Override
  public List<String> outline(String contents) {
    List<String> sections = new ArrayList<>();
    for (Stanza stanza : parseStanzas(contents)) {
      sections.add(sectionName(stanza));
    }
    return sections;
  }

  /**
   * Parses the top-level stanzas of the simulation.
   *
   * @param contents The string contents of the simulation.
   * @return The stanzas in source order.
   */
  private List<Stanza> parseStanzas(String contents) {
    List<String> lines = splitLines(contents);
    List<Stanza> stanzas = new ArrayList<>();
    String activeKeyword = null;
    String activeName = null;
    int startLine = -1;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      Matcher endMatcher = END_PATTERN.matcher(line);
      if (activeKeyword != null
          && endMatcher.find()
          && endMatcher.group(1).equalsIgnoreCase(activeKeyword)) {
        stanzas.add(new Stanza(activeKeyword, activeName, startLine, i));
        activeKeyword = null;
        activeName = null;
        startLine = -1;
        continue;
      }
      Matcher startMatcher = START_PATTERN.matcher(line);
      if (startMatcher.find()) {
        if (activeKeyword != null) {
          stanzas.add(new Stanza(activeKeyword, activeName, startLine, i - 1));
        }
        activeKeyword = startMatcher.group(1).toLowerCase(Locale.ROOT);
        activeName = "policy".equals(activeKeyword)
            ? extractPolicyName(line, startMatcher.end())
            : null;
        startLine = i;
      }
    }
    if (activeKeyword != null) {
      stanzas.add(new Stanza(activeKeyword, activeName, startLine, lines.size() - 1));
    }
    return stanzas;
  }

  /**
   * Extracts the policy name which follows the keyword on a {@code start policy} line.
   *
   * @param line The line containing the start of the stanza.
   * @param keywordEnd The index just past the matched keyword.
   * @return The trimmed policy name, or an empty string when none was provided.
   */
  private String extractPolicyName(String line, int keywordEnd) {
    String remainder = line.substring(keywordEnd);
    int comment = remainder.indexOf('#');
    if (comment >= 0) {
      remainder = remainder.substring(0, comment);
    }
    return remainder.trim();
  }

  /**
   * Builds the section token for a stanza.
   *
   * @param stanza The stanza.
   * @return The section token such as {@code policy "Permit"}.
   */
  private String sectionName(Stanza stanza) {
    if ("policy".equals(stanza.getKeyword())
        && stanza.getName() != null
        && !stanza.getName().isBlank()) {
      return "policy " + stanza.getName();
    }
    return stanza.getKeyword();
  }

  /**
   * Determines whether the requested section refers to the given stanza.
   *
   * @param stanza The stanza.
   * @param requested The requested section.
   * @return True when the request matches.
   */
  private boolean matchesSection(Stanza stanza, String requested) {
    String requestedNormalized = normalize(requested);
    if (!"policy".equals(stanza.getKeyword())) {
      return stanza.getKeyword().equalsIgnoreCase(requestedNormalized);
    }
    String nameNormalized = normalize(stanza.getName());
    if (requestedNormalized.equalsIgnoreCase(sectionName(stanza))) {
      return true;
    }
    if (!nameNormalized.isBlank() && requestedNormalized.equalsIgnoreCase(nameNormalized)) {
      return true;
    }
    return requestedNormalized.equalsIgnoreCase("policy " + nameNormalized);
  }

  /**
   * Normalizes a section token for comparison.
   *
   * @param value The value to normalize.
   * @return The trimmed, unquoted, lower-case value.
   */
  private String normalize(String value) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    return trimmed.toLowerCase(Locale.ROOT);
  }

  /**
   * Splits contents into lines after normalizing line endings.
   *
   * @param contents The string contents.
   * @return The lines, including any trailing empty line.
   */
  private List<String> splitLines(String contents) {
    String normalized = contents.replace("\r\n", "\n").replace('\r', '\n');
    return List.of(normalized.split("\n", -1));
  }

  /**
   * A top-level stanza found in a simulation.
   */
  private static final class Stanza {

    private final String keyword;
    private final String name;
    private final int startLine;
    private final int endLine;

    /**
     * Constructs a stanza.
     *
     * @param keyword The stanza keyword such as {@code default}.
     * @param name The policy name when applicable, otherwise null.
     * @param startLine The zero-based index of the opening line.
     * @param endLine The zero-based index of the closing line, inclusive.
     */
    Stanza(String keyword, String name, int startLine, int endLine) {
      this.keyword = keyword;
      this.name = name;
      this.startLine = startLine;
      this.endLine = endLine;
    }

    String getKeyword() {
      return keyword;
    }

    String getName() {
      return name;
    }

    int getStartLine() {
      return startLine;
    }

    int getEndLine() {
      return endLine;
    }
  }
}
