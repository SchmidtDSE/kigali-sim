/**
 * <strong>Kigali Sim Lang</strong>
 *
 * <p>This package contains the logic for interpreting QubecTalk code. The entrypoint is {@code
 * QubecTalkEngineVisitor} which is an ANTLR visitor that parses simulations. It does this through
 * "fragments" found in {@code org.kigalisim.lang.fragment} where we recurse down to the bottom of
 * the parse tree and interpret code into Kigali Sim constructs. These fragments are joined
 * together on the way back up in recursion until they have enough information to construct
 * commands or "operations" found in {@code org.kigalisim.lang.operation} which can be performed
 * against the engine (see {@code org.kigalisim.engine}). Note that the grammar file used by the
 * visitor can be found in {@code QubecTalk.g4}.
 *
 * @license BSD-3-Clause
 */
package org.kigalisim.lang;
