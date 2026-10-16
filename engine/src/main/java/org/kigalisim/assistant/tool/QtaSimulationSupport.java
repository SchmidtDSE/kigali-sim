/**
 * Shared bridge from the assistant workspace to the Kigali Sim engine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.assistant.workspace.QtaFileTypeStrategy;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Reads and loads QubecTalk simulations held in an assistant workspace.
 *
 * <p>Centralizes the {@code .qta} extension check, workspace read, and the in-memory
 * {@link KigaliSimFacade} parse/interpret bridge shared by the simulation tools. The helper lives
 * inside the assistant package so that no assistant type leaks into the rest of the codebase.</p>
 */
final class QtaSimulationSupport {

  private static final String UNSUPPORTED_TEMPLATE =
      "Only QubecTalk simulation files are supported; received '%s'";
  private static final String PARSE_FAILED_TEMPLATE = "Failed to parse QubecTalk code:\n%s";
  private static final String INTERPRET_FAILED_TEMPLATE =
      "Failed to interpret QubecTalk code:\n%s";

  private QtaSimulationSupport() {
    // Utility class - prevent instantiation
  }

  /**
   * Determines whether a filename names a QubecTalk simulation file.
   *
   * @param filename The filename to test.
   * @return True when the filename ends with the QubecTalk extension, ignoring case.
   */
  static boolean isQta(String filename) {
    if (filename == null) {
      return false;
    }
    return filename.toLowerCase(Locale.ROOT).endsWith(QtaFileTypeStrategy.EXTENSION);
  }

  /**
   * Validates the extension, reads the file, then parses and interprets it.
   *
   * <p>On any failure the first problem is reported through the callback as a failed tool call and
   * an empty result is returned. Nothing thrown by the workspace or the engine escapes this
   * method.</p>
   *
   * @param workspace The workspace holding the simulation.
   * @param filename The name of the simulation file.
   * @param callback The callback through which failures are reported.
   * @return The interpreted program, or empty when loading failed.
   */
  static Optional<ParsedProgram> load(AssistantWorkspace workspace, String filename,
      AssistantActionCallback callback) {
    Objects.requireNonNull(workspace, "workspace");
    Objects.requireNonNull(filename, "filename");
    Objects.requireNonNull(callback, "callback");

    if (!isQta(filename)) {
      String message = String.format(UNSUPPORTED_TEMPLATE, filename);
      reportError(callback, message);
      return Optional.empty();
    }

    final String code;
    try {
      code = workspace.readFile(filename);
    } catch (NoSuchElementException | IllegalArgumentException
        | UnsupportedOperationException e) {
      String message = getMessageOf(e);
      reportError(callback, message);
      return Optional.empty();
    }

    final ParseResult parseResult;
    try {
      parseResult = KigaliSimFacade.parse(code);
    } catch (RuntimeException e) {
      String message = String.format(PARSE_FAILED_TEMPLATE, getMessageOf(e));
      reportError(callback, message);
      return Optional.empty();
    }

    if (parseResult.hasErrors()) {
      String message = String.format(PARSE_FAILED_TEMPLATE,
          KigaliSimFacade.getDetailedErrorMessage(parseResult));
      reportError(callback, message);
      return Optional.empty();
    }

    try {
      return Optional.of(KigaliSimFacade.interpret(parseResult));
    } catch (RuntimeException e) {
      String message = String.format(INTERPRET_FAILED_TEMPLATE, getMessageOf(e));
      reportError(callback, message);
      return Optional.empty();
    }
  }

  /**
   * Reports a failed tool call through the callback.
   *
   * @param callback The callback to report through.
   * @param message The error message to report.
   */
  private static void reportError(AssistantActionCallback callback, String message) {
    callback.onActionComplete(AssistantResultBuilder.forToolCallError(message).build());
  }

  /**
   * Gets a non-null message for a throwable.
   *
   * @param e The throwable to describe.
   * @return The throwable message, or its class name when the message is null.
   */
  static String getMessageOf(RuntimeException e) {
    return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
  }
}
