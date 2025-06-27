/**
 * Simple module entrypoint.
 * 
 * @license BSD, see LICENSE.md
 */

import antlr4 from 'antlr4';
import QubecTalkLexer from './QubecTalkLexer.js';
import QubecTalkParser from './QubecTalkParser.js';
import QubecTalkListener from './QubecTalkListener.js';
import QubecTalkVisitor from './QubecTalkVisitor.js';


/**
 * Get the ANTLR toolkit needed to process plastics language scripts.
 * 
 * @returns Toolkit components.
 */
function getToolkit() {
  return {
    "antlr4": antlr4,
    "QubecTalkLexer": QubecTalkLexer,
    "QubecTalkParser": QubecTalkParser,
    "QubecTalkListener": QubecTalkListener,
    "QubecTalkVisitor": QubecTalkVisitor
  };
}


export {getToolkit};