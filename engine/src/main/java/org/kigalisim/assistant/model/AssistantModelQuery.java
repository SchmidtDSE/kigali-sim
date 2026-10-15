/**
 * Data transfer object describing a query sent to a language model.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

/**
 * The message and assembled request associated with a model query.
 *
 * <p>This object lets a user interface render a user's message optimistically. The same object is
 * delivered on results through {@code AssistantResult.getMessage()}.</p>
 */
public final class AssistantModelQuery {

  private final String chatName;
  private final String context;
  private final String message;
  private final AssistantModel model;

  /**
   * Constructs a query.
   *
   * @param chatName The name of the chat to which the query belongs.
   * @param context The fully assembled request body.
   * @param message The new user message only.
   * @param model The model which will service the query.
   */
  public AssistantModelQuery(
      String chatName, String context, String message, AssistantModel model) {
    this.chatName = chatName;
    this.context = context;
    this.message = message;
    this.model = model;
  }

  /**
   * Gets the name of the chat to which the query belongs.
   *
   * @return The chat name.
   */
  public String getChatName() {
    return chatName;
  }

  /**
   * Gets the assembled request body.
   *
   * @return The full request sent to the provider.
   */
  public String getContext() {
    return context;
  }

  /**
   * Gets the new user message only.
   *
   * @return The user message which prompted this query.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the model which will service the query.
   *
   * @return The model.
   */
  public AssistantModel getModel() {
    return model;
  }
}
