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
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.lang.parse.ParseResult;

/**
 * Outlines and extracts the top-level stanzas of a QubecTalk simulation.
 *
 * <p>Valid source is parsed with the canonical ANTLR grammar and the stanza metadata is read from
 * the parse tree by {@link QtaStanzaVisitor}. Because the assistant may outline a file while it is
 * only partially edited, an invalid source falls back to a tolerant, line-oriented scan so that
 * top-level stanza boundaries can still be found. Nested constructs such as {@code define} and
 * {@code uses} blocks do not affect top-level stanza boundaries.</p>
 */
public class QtaFileTypeStrategy implements TextFileTypeStrategy {

  /**
   * The file extension supported by this strategy.
   */
  public static final String EXTENSION = ".qta";

  private static final Pattern START_PATTERN = Pattern.compile(
      "^\\s*start\\s+(about|default|policy|simulations)\\b",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern END_PATTERN = Pattern.compile(
      "^\\s*end\\s+(about|default|policy|simulations)\\b",
      Pattern.CASE_INSENSITIVE
  );

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
    for (QtaStanza stanza : parseStanzas(contents)) {
      if (matchesSection(stanza, section)) {
        return String.join("\n", lines.subList(stanza.getStartLine(), stanza.getEndLine() + 1));
      }
    }
    throw new NoSuchElementException("No section named '" + section + "'");
  }

  @Override
  public List<String> outline(String contents) {
    List<String> sections = new ArrayList<>();
    for (QtaStanza stanza : parseStanzas(contents)) {
      sections.add(sectionName(stanza));
    }
    return sections;
  }

  /**
   * Parses the top-level stanzas of the simulation.
   *
   * <p>The canonical parser and {@link QtaStanzaVisitor} are used when the source is valid. Invalid
   * or partially edited source falls back to a tolerant line scan.</p>
   *
   * @param contents The string contents of the simulation.
   * @return The stanzas in source order.
   */
  private List<QtaStanza> parseStanzas(String contents) {
    ParseResult parseResult = KigaliSimFacade.parse(contents);
    if (!parseResult.hasErrors() && parseResult.getProgram().isPresent()) {
      QtaStanzaVisitor visitor = new QtaStanzaVisitor();
      visitor.visit(parseResult.getProgram().get());
      return visitor.getStanzas();
    }
    return scanStanzas(contents);
  }

  /**
   * Tolerantly scans the top-level stanzas of partially edited source.
   *
   * @param contents The string contents of the simulation.
   * @return The stanzas in source order.
   */
  private List<QtaStanza> scanStanzas(String contents) {
    List<String> lines = splitLines(contents);
    List<QtaStanza> stanzas = new ArrayList<>();
    String activeKeyword = null;
    String activeName = null;
    int startLine = -1;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      Matcher endMatcher = END_PATTERN.matcher(line);
      if (activeKeyword != null
          && endMatcher.find()
          && endMatcher.group(1).equalsIgnoreCase(activeKeyword)) {
        stanzas.add(new QtaStanza(activeKeyword, activeName, startLine, i));
        activeKeyword = null;
        activeName = null;
        startLine = -1;
        continue;
      }
      Matcher startMatcher = START_PATTERN.matcher(line);
      if (startMatcher.find()) {
        if (activeKeyword != null) {
          stanzas.add(new QtaStanza(activeKeyword, activeName, startLine, i - 1));
        }
        activeKeyword = startMatcher.group(1).toLowerCase(Locale.ROOT);
        activeName = "policy".equals(activeKeyword)
            ? extractPolicyName(line, startMatcher.end())
            : null;
        startLine = i;
      }
    }
    if (activeKeyword != null) {
      stanzas.add(new QtaStanza(activeKeyword, activeName, startLine, lines.size() - 1));
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
  private String sectionName(QtaStanza stanza) {
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
  private boolean matchesSection(QtaStanza stanza, String requested) {
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
}
