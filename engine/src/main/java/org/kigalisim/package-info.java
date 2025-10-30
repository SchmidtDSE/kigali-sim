/**
 * <strong>Kigali Sim Java</strong>
 *
 * <p>The members at the root of {@code org.kigalisim} provide the entrypoints into the tool. The
 * primary ways users leverage this simulation toolkit are as follows:
 *
 * <ul>
 *   <li><strong>Standalone jar execution</strong>: Running simulations from the command line in
 *       which script files are provided and results are written to CSV files.
 *   <li><strong>UI-editor or browser-based execution</strong>: Building and executing simulations
 *       using an IDE specific to Kigali Sim. This includes a no-code UI-based editor which can
 *       generate and modify QubecTalk code.
 * </ul>
 *
 * <p>The {@code KigaliSimCommander} offers the command line interface entrypoint but logic is
 * actually fulfilled by {@code org.kigalisim.command}. Meanwhile, entry through WASM / TeaVM
 * happens through {@code KigaliWasmSimFacade}. We use {@code ProgressReportCallback} to provide
 * access to simulation progress before full results are available. In any case, QubecTalk code is
 * interpreted using {@code org.kigalisim.lang} and execution goes through {@code
 * KigaliSimFacade} into {@code org.kigalisim.engine}.
 *
 * @license BSD-3-Clause
 */
package org.kigalisim;
