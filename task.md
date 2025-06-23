# Add WASM layer

Make a `WasmLayer` and `WasmBackend` similar to Josh.

## Background

We are in the process of integrating the Java version of the QubecTalk interpreter and KigaliSim engine. This is in the `engine` directory. We need to formalize a web worker similar to the Josh project to actually run simulations.

We support having different "backends" like seen in Josh's `WasmEngineBackend`. However, we only have one backend for now: `LegacyJsBackend`. We had a queue similar to `WasmLayer` and a `wasm.worker.js` but called `LegacyJsLayer` and `legacy.worker.js`. That said, unlike Josh, we do not need to specify the trial count, simulation name, data, or if we prefer BigDecimals. Instead, we can just post code to the worker and have the worker return a string where we have a status message ("OK" or description of the error) followed by a blank line and then the CSV contents. We have already added a ReportDataParser in LegacyJsLayer that checks for an error in the status code and returns that through existing error handling logic. If there is no error, it parses the CSV into ReportDataWrapper by iterating through to get the JS version of EngineResult objects with EngineNumbers.

Note that we have also already added `wasm.worker.js` for the Java implementation of QubecTalk similar to Josh's `wasm.worker.js`. This should also have updated the GitHub actions similar to Josh's GitHub actions except that the double build is not needed as the jar does not have a server.

## Objective

We should make `WasmBackend` as an alternative to `LegacyJsBackend` and `WasmLayer` similar to `LegacyJsLayer` but that uses `wasm.worker.js`. We should then change QubecTalk / KigaliSim to use the `WasmBackend` and `WasmLayer` now instead of `LegacyJsBackend` and `LegacyJsLayer`.

## Limitations

We will only use the backend for actual execution. Validation should remain the same in using the existing JS logic. The code to UI configuration visitor will also remain the same and can be used for validation.

The web worker should take in as tasks only string and return back to the main thread with only strings as the result.

## Notes

See `execute` in `_onBuild` in `main.js`. We should be using the new backend now. This may have to become async with promises. See Josh for example.

Due to module loading issues, the legacy version isn't working from a web worker. It is just showing the architecture. The goal is to make the web worker minimal such that imports are not needed in the worker itself similar to the Josh version.
