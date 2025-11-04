# Kigali Sim Java

The members at the root of `org.kigalisim` provide the entrypoints into the tool. The primary ways users leverage this simulation toolkit are as follows:

 - **Standalone jar execution**: Running simulations from the command line in which script files are provided and results are written to CSV files.
 - **UI-editor or browser-based execution**: Building and executing simulations using an IDE specific to Kigali Sim. This includes a no-code UI-based editor which can generate and modify QuebecTalk code.

The `KigaliSimCommander` offers the command line interface entrypoint but logic is actually fulfilled by `org.kigalisim.command`. Meanwhile, entry through WASM / TeaVM happens through `KigaliWasmSimFacade`. We use `ProgressReportCallback` to provide access to simluation progress before full results are available. In any case, QubecTalk code is interpreted using `org.kigalisim.lang` and execution goes through `KigaliSimFacade` into `org.kigalisim.engine`.
