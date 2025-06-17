/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import org.teavm.jso.JSExport;


/**
 * Facade which offers access to JS clients.
 *
 * <p>Entry point for the KigaliSim command line application which can run simulations from within
 * the browser.</p>
 */
public class KigaliWasmSimFacade {

  /**
   * Returns the version of KigaliSim.
   *
   * @return The version string "0.0.1".
   */
  @JSExport
  public static String getVersion() {
    return "0.0.1";
  }

  /**
   * Required entrypoint for wasm.
   *
   * @param args ignored arguments
   */
  public static void main(String[] args) {}
}
