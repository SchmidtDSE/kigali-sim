/**
 * Persistence strategy for assistant chats.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

/**
 * A polymorphic persistence mechanism for chats.
 *
 * <p>Implementations may store chats in memory, on disk, or in browser storage depending on the
 * platform. Missing chats are reported with {@link java.util.NoSuchElementException} while
 * conflicting names are reported with {@link IllegalArgumentException}.</p>
 */
public interface ChatLogStrategy {

  /**
   * Creates a chat.
   *
   * @param name The requested chat name.
   * @return The canonical name of the created chat.
   * @throws IllegalArgumentException If a chat by that name already exists.
   */
  String createChat(String name);

  /**
   * Reads the log for a chat.
   *
   * @param name The chat name.
   * @return The chat log.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  ChatLog readChat(String name);

  /**
   * Appends one message or an array of messages to a chat's wire transcript.
   *
   * @param chatName The chat name.
   * @param wireContents The message or messages to append.
   * @return The updated chat log.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  ChatLog appendChat(String chatName, String wireContents);

  /**
   * Replaces a chat's wire transcript.
   *
   * @param chatName The chat name.
   * @param wireContents The replacement wire transcript.
   * @return The updated chat log.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  ChatLog setChat(String chatName, String wireContents);

  /**
   * Deletes a chat.
   *
   * @param name The chat name.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  void deleteChat(String name);
}
