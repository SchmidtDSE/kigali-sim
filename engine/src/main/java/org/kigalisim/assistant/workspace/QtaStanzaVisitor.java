/**
 * Visitor which collects top-level stanza metadata from a QubecTalk parse tree.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.kigalisim.lang.QubecTalkBaseVisitor;
import org.kigalisim.lang.QubecTalkParser;

/**
 * Walks a parsed QubecTalk program and records the line range of every top-level stanza.
 *
 * <p>This replaces manual line scanning with the canonical parser so the assistant sees exactly the
 * stanzas the interpreter would. The parse tree is required to be free of errors before visiting;
 * callers which may hold partially edited code should fall back to a tolerant scan.</p>
 */
final class QtaStanzaVisitor extends QubecTalkBaseVisitor<Void> {

  private final List<QtaStanza> stanzas = new ArrayList<>();

  /**
   * Gets the stanzas found in source order.
   *
   * @return The discovered stanzas.
   */
  List<QtaStanza> getStanzas() {
    return List.copyOf(stanzas);
  }

  @Override
  public Void visitAboutStanza(QubecTalkParser.AboutStanzaContext ctx) {
    addStanza(ctx, "about", null);
    return null;
  }

  @Override
  public Void visitDefaultStanza(QubecTalkParser.DefaultStanzaContext ctx) {
    addStanza(ctx, "default", null);
    return null;
  }

  @Override
  public Void visitPolicyStanza(QubecTalkParser.PolicyStanzaContext ctx) {
    String name = ctx.name == null ? null : ctx.name.getText();
    addStanza(ctx, "policy", name);
    return null;
  }

  @Override
  public Void visitSimulationsStanza(QubecTalkParser.SimulationsStanzaContext ctx) {
    addStanza(ctx, "simulations", null);
    return null;
  }

  /**
   * Records a stanza from its parse tree context.
   *
   * @param ctx The stanza context.
   * @param keyword The normalized stanza keyword.
   * @param name The policy name when applicable, otherwise null.
   */
  private void addStanza(ParserRuleContext ctx, String keyword, String name) {
    int startLine = ctx.getStart().getLine() - 1;
    int endLine = ctx.getStop().getLine() - 1;
    stanzas.add(new QtaStanza(keyword, name, startLine, endLine));
  }
}
