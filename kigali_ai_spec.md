# Kigali Sim AI Implementation Spec
This system is to be implemented as a prototype in a series of components where one component is to be completed before moving to the next.

<br>

## Objective
[This harness intends to offer ergenomic access to AI assistance for the purposes of constructing Kigali Sim simulations](https://hackmd.io/@Sampottinger/SkidfrHSGl). This may include reading from PDF / Office documents in order to pull information into these projects. Primarily, we wish to provide access to this AI assistance in the browser without install. However, we also should allow for use of a minimal harness via the jar. Despite this secondary objective, we do not intend to provide a full harness like OpenCode or similar.

There are a number of advantages to providing a custom harness for Kigali Sim in WASM. First, the model is inherently sandboxed first by the DSL and then by limited tools. Second, it is constrained by the WASM environment itself which is also subject to browser standboxing. Although some safeguards like CORS may usable non-DSE services, for example.

All that said, there are [limitations in some parts of the JDK available in TeaVM / WASM](https://www.teavm.org/jcl-report/recent/jcl.html), specifically around reflection and network. These presently prevent use of [LangChain4j](https://docs.langchain4j.dev/intro/) and [Jackson](https://github.com/fasterxml/jackson). Therefore, we are building a custom small JVM harness within these constraints.

The code from this effort will likely also benefit Josh which has a similar DSL and project architecture. Even so, this is first intended as a Kigali Sim exploration to determine the viability and desirability of this design prior to consideration of reuse. It is likely that some of these decisions will change given practical experience with a working prototype. Should this be successful in this smaller DSL, these components can be lifted to a jar shared across both projects.

<br>

## Architecture
We are rolling our own lightweight harness due to portability demands across the JVM and WASM. Towards that end, we will create the `org.kigalisim.assistant` package for this built-in assistant capability.

### Entry point

**Package**: `org.kigalisim.assistant`

**Summary**: It is important that we try to encapsulate the complexity of the harness from the rest of the codebase where possible. This is an optional feature that may users of the platform will not leverage. To help facilitate decoupling, `KigaliAssistant` is a facade providing an entry point into this functionality.

It should have methods:

 - `void createChat(String name)` which creates a chat, throwing if a chat by that name already exists.
 - `ChatLog readChat(String name)` which returns the log for a chat and throws a documented exception if the chat does not exist.
 - `AssistantModelQuery sendMessage(String chatName, String message, AssistantTurnCallback callback)` which sends a message and starts a turn.
 - `void deleteChat(String name)` which deletes a chat, throwing if a chat by that name does not exist.

`KigaliAssistant` is a thin facade. It delegates:

 - Storage to the `ChatLogStrategy`.
 - Turn loop to the `AssistantTurnStrategy`.

It appends to the chat log internally as the turn progresses. The returned `AssistantModelQuery` lets the UI render the user's message optimistically; the same object is also delivered on results via `AssistantResult.getMessage()`.

To construct this object, we will create a `KigaliAssistantBuilder`. This registers the built-in tools first. Then, it exposes `addTool(AssistantToolStrategy)` for additional tools (created through the implementation components). In the process, it rejects duplicate tool names at construction. The builder also accepts the `AssistantModel`, `AssistantTurnStrategy`, `ChatLogStrategy`, and `WorkspaceStrategy`.

### Callbacks

**Package**: `org.kigalisim.assistant.callback`

**Summary**: This interface and its extension allow the interface (either CLI or UI-based editor) to subscribe to both progress and completion of an LLM's chat and actions. Due to the JVM / WASM split, a callback approach used instead of `Future`.

Callbacks are split so that only the turn strategy can end a turn:

 - `AssistantActionCallback` interface with `onActionComplete(AssistantResult result)`. This is what strategies and tools receive.
 - `AssistantTurnCallback extends AssistantActionCallback` which adds `onTurnComplete(AssistantResult result)`. This is what callers of `KigaliAssistant.sendMessage` provide.

Note that this follows this calling pattern:

 - `onActionComplete` fires once for every action taken during a turn (each LLM query and each tool call) so the UI can show progress.
 - `onTurnComplete` fires exactly once per turn after the final `onActionComplete` and carries the overall (terminal) result

The type system acts as a correctness constraint here. The turn strategy passes a narrowed `AssistantActionCallback` view down to strategies and tools, so those layers cannot end a turn even by accident.

### Models

**Package**: `org.kigalisim.assistant.model`

**Summary**: `AssistantModel` provides access to the LLM and tackles two challenges in portability. First, this manages the complexities of the OpenAI API and affords a mechanism to support other kinds of providers later. Second, the JVM and WASM platforms have different approaches to network and this abstracts over those differences. This has some supporting classes.

This acts as a template method and uses two strategies within it:

 - `AssistantModelWireStrategy` which handles transport.
 - `AssistantModelProtocolStrategy` which handles request formation and response parsing.

It also has:

 - `String getModelName()` which provides the name of the model in use.
 - `String getModelService()` which provides information about where the model is running.
 - `AssistantRequestHandle sendMessage(String body, AssistantActionCallback callback)` which sends the message to invoke the given callback with resutls.

`AssistantModelWireStrategy` handles transport and may support portability across paltforms. It is an interface with `AssistantRequestHandle sendMessage(String body, AssistantActionCallback callback)` which sends a request where body is the full context (including history plus new message) and reports the outcome. The returned handle allows the in-flight request to be cancelled which may also be a platform-specific operation. Even so, we should be careful to limit the scope of this object. First, `AssistantModelWireStrategy` does not build requests, does not know about tools, and does not parse responses. Second, streaming responses are out of scope for this prototype: the wire strategy delivers the complete body in a single callback.

In order to handle these limitations, `AssistantModelProtocolStrategy` is an interface with:

 - `String buildRequest(List<JsonObject> history, Iterable<AssistantToolStrategy> tools, String systemContext, String userMessage)` which forms the provider request, setting `tool_choice` to `"required"` and serializing the tool definitions.
 - `AssistantModelResponse parseResponse(String rawBody)` which parses a provider response into content, tool calls, and finish reason.
 - `String renderHumanTranscript(String wireTranscript)` which derives the human readable transcript from the stored wire transcript.

Note that, in support, `AssistantModelQuery` can have:

 - `String getChatName()`
 - `String getContext()` which is the assembled request body.
 - `String getMessage()` which is the new user message only.
 - `AssistantModel getModel()`

As a result, `AssistantModelResponse` has:

 - `Optional<String> getContent()`
 - `List<AssistantToolInvocation> getToolCalls()`
 - `Optional<String> getFinishReason()`
 - `JsonObject getUninterpreted()` which returns the assistant message exactly as received. This is required for verbatim replay into history.
 - `String getRaw()` which returns the raw String payload provided.

Here `AssistantModelResponse` is intended just as a data transfer object.

### Tools

**Package**: `org.kigalisim.assistant.tool`

**Summary**: The primary mechanism for the LLM to take actions is through tools offered by the harness. This requires a set of strategies which are registered into LLM context. This is primarily run through a `AssistantToolStrategy` interface.

The `AssistantToolStrategy` represents a tool which can be used by the LLM. It should be an interface with:

 - `String getName()`
 - `String getDescription()`
 - `JsonObject getParameterSchema()`
 - `void execute(JsonObject call, AssistantActionCallback callback)`

`getParameterSchema` returns a JSON Schema object which the protocol strategy serializes into the request's tool definitions so the model knows how to call the tool. As with the wire strategy, the tool reports its outcome through the callback.

Related to this, we should add an `AssistantToolbelt` interface. This is intended as a thin wrapper around `Map<String, AssistantToolStrategy>`. It should support lookup by name and iteration over the tools.  This object should be limited in scope. It is populated with the built-in tools via the `KigaliAssistantBuilder`. However, it should not know about these built-ins on its own. To support project objectives around mutability, `AssistantToolbelt` should only have methods for reading but a concrete `MutableAssistantToolbelt` should have methods for adding. Duplicate names should be rejected by exception.

Finally, `AssistantToolInvocation` will have:

 - `String getToolName()` which gives the name of the tool invoked.
 - `String getCallId()` which gives the provider-assigned identifier used to match the result back to the request.
 - `JsonObject getCall()` which gives the arguments the model provided.

### Results

**Package**: `org.kigalisim.assistant.result`

**Summary**: The results of actions and chats can be complex. For intermediate results in which a turn is not yet complete, status may need to be updated to the user and actions taken from outside the LLM within tools before resuming model inference. For completed turns, information has to be compiled to support user situational awareness. This is largely handled through a data record class callsed `AssistantResult`.

The `AssistantResult` will have:

 - `AssistantActionType getActionType()` which returns the type of action taken.
 - `Optional<AssistantModelQuery> getMessage()` which returns the query sent to the LLM or empty if not an LLM query.
 - `Optional<AssistantToolInvocation> getToolInvocation()` which returns the invocation or empty if not a tool call.
 - `Optional<AssistantModelResponse> getRawResponse()` which gives the parsed response body for an LLM query or the tool output text for a tool call, empty if there was an error.
 - `Optional<String> getError()` which gives details on the error if error encountered (empty otherwise).
 - `boolean getWasSuccessful()` which indicates whether the action succeeded.

Each layer fills only the fields it owns. The wire and tool strategies populate the transport outcome (`getResponse` / `getError`) while the turn strategy attaches `getMessage()` and `getToolInvocation()` as it forwards a result, since it holds the query and knows which tool it dispatched.

Here `AssistantActionType` is an enum with values:

 - `LLM_QUERY` used for when we tried communicating with the LLM.
 - `TOOL_CALL` used when reporting on a tool call outcome.
 - `TURN_ACTIONS_EXCEEDED` used when a turn is ended by the maximum action guard rather than by `end_turn`. This is the only turn level member of the enum; its result has `getError()` populated and therefore `getWasSuccessful()` false.

### Log

**Package**: `org.kigalisim.assistant.log`

**Summary**: The chat log itself presents two complexities. First, in addition to a human readable log, a machine interpretable version needs to be maintained with replay capability for OpenAI API compatible endpoints. Second, depending on platform, persistence of the log (if any) may be specific to environment. Therefore, `ChatLogStrategy` provides a polymorphic persistence mechanism. Additionally, `ChatLog` offers an opportunity to support non-OpenAI APIs.

`ChatLogStrategy` is an interface for a persistence mechanism with the following:

 - `String createChat(String name)` which creates a chat and returns its canonical name. It throws an exception on conflicting name.
 - `ChatLog readChat(String name)` which returns the log for a chat. It throws if chat not found.
 - `ChatLog appendChat(String chatName, String wireContents)` which appends to a chat's wire transcript. It throws if chat not found.
 - `ChatLog setChat(String chatName, String wireContents)` which replaces a chat's wire transcript. It throws if chat not found.
 - `void deleteChat(String name)` which throws if chat not found.

Both human readable and machine readable versions of this log are required. Therefore, `ChatLog` is an interface and has:

 - `String getWireTranscript()` which returns the stored (for example OpenAI-compatible) message array.
 - `List<HumanTranscriptElement> getHumanTranscript()` which derives the human readable transcript through the `AssistantModelProtocolStrategy`.

We will have `OpenAiCompatibleChatLog` as the only implementer for now. Note that, in this design, the wire transcript is the single source of truth and the human transcript is derived from it. What the human transcript shows (user turns and assistant messages only, or tool activity as well) is decided at Component 9 once live model behavior can be observed. We will provide `InMemoryLogStrategy` which keeps chats in memory.

The details of `HumanTranscriptElement` are left open but there should be a unique id for each element and some string representation in markdown.

### Workspace

**Package**: `org.kigalisim.assistant.workspace`

**Summary**: An abstraction over a collection of files which will be simulation files (`.qta`) and supporting documents usable by the assistant like PDFs and spreadsheets. We do not support directories and paths.

A `AssistantWorkspace` interface defines persistence and storage. It is expected to have:

 - `String createFile(String filename)` which allows creation of new files within the workspace. Error thrown if file already exists. This form creates an empty file.
 - `String createFile(String filename, String contents)` which allows creation of new files within the workspace. Error thrown if file already exists. This form creates the file with contents.
 - `String createFile(String filename, byte[] contents)` which allows creation of new files within the workspace. Error thrown if file already exists. This form creates the file with contents.
 - `String readFile(String filename)` which reads a file as a string in its entirety. Error thrown if file not found.
 - `String readFile(String filename, String section)` which reads a part of a file as described by section (format specific). Error thrown if section or file not found.
 - `String writeFile(String filename, String contents)` which writes a file in its entirety (overwrite prior contents). Error thrown if file does not exist.
 - `void deleteFile(String filename)` which removes a file. Error thrown if file does not exist.

`AssistantWorkspace` should be given strategies for working with different file extensions. This is important because some file types may be binary but have to present as text to the LLM (these types are read only). This `AssistantFileTypeStrategy` interface should have

 - `Set<String> getFileExtensions()` gets the list of file extensions like `.pdf` which are supported by this file type strategy.
 - `boolean getIsStringNative()` returns a flag where, if true, the underlying file type is string / text based and false if binary.

Then, subtype `BinaryFileTypeStrategy` interface should have:

 - `String interpret(byte[] contents)` returns the LLM-friendly string contents of the file contents provided.
 - `String interpret(byte[] contents, String section)` returns the LLM-friendly string contents of the file contents provided limited to just the part described by section (specific to type of file).
 - `List<String> outline(byte[] contents)` returns a list of sections that can be requested.

Finally, subtype `TextFileTypeStrategy` interface should have:

 - `String interpret(String contents)` returns the LLM-friendly string contents of the file contents provided.
 - `String interpret(String contents, String section)` returns the LLM-friendly string contents of the file contents provided limited to just the part described by section (specific to type of file).
 - `List<String> outline(String contents)` returns a list of sections that can be requested.

We should make an `AssistantWorkspaceBuilder` which helps create `AssistantWorkspace` objects.

<br>

## Implementation recommendations
This specification leaves a number of implementation details to the components. However, a few recommendations are provided in advance.

### The turn loop
The primary mechanism of the harness is to run "turns" in which an LLM may take actions and send messages before returning control to the user. `AssistantTurnStrategy` encapsulates this turn loop and Component 3 provides `UntimedAssistantTurnStrategy`. It drives a single turn:

 - Append the user message to history and assemble the request with `tool_choice` set to `"required"`.
 - Send the request through the model and report the result via `onActionComplete`.
 - Parse any tool calls from the response. Append the assistant message verbatim, then for each call resolve the tool, execute it, and append a tool result message carrying the matching call id.
 - If `EndTurnTool` was among the calls, finish the batch and call `onTurnComplete`. Otherwise repeat from step 2.
 - Stop after a configurable maximum number of actions per turn (default 25) and complete the turn with `TURN_ACTIONS_EXCEEDED`.

The following rules apply:

 - Every tool call in a batch must be executed and every result appended, including when the batch contains `EndTurnTool`. The provider requires a result message for each call id before the history can be reused, so a partially answered batch makes the conversation unusable. `EndTurnTool` therefore appends its own result before the turn completes.
 - The model may return several tool calls in a single message, so the strategy must handle zero, one, or many, and `EndTurnTool` may arrive alongside other calls such as `MessageUserTool`. Calls are executed in serial, in the order returned, and their results are appended in that same order.
 - `tool_choice` of `"required"` instructs the model that it must call at least one tool, which is how the tool-only protocol is enforced. Prompt text alone cannot guarantee this and support varies by provider. The strategy must therefore also handle a plain text reply: if a response contains no tool calls, treat the text as an implicit call to `MessageUserTool` followed by an end of turn, so it reaches the UI through the same path as any other message.
 - History is appended from the raw provider JSON and requests replay those stored objects. An assistant message carrying tool calls is never rebuilt from a parsed model, as that can silently drop provider fields and produce a request the provider rejects. Verbatim here means structurally identical, not byte identical.
 - If the model names a tool that does not exist, a tool result saying so is appended so the batch stays complete, and a failed `TOOL_CALL` result is reported.
 - If the model returns malformed arguments, the call becomes a failed `TOOL_CALL` rather than a crash. This is reachable when a response is truncated.
 - An unparseable response body becomes a failed `LLM_QUERY`.
 - Every strategy call is wrapped so a thrown exception becomes a failed result.
 - `onTurnComplete` fires exactly once per turn, including when the turn fails.
 - A turn already in flight for a chat rejects a further `sendMessage` for that same chat by returning a failed result. In this case, other chats are unaffected.

This presents a some cross-platform complexity. On the JVM, the callback may arrive on a different thread. However, within WASM / TeaVM, it arrives on the event loop.

Note here that the LLM must use a tool to speak to the user and can only reply with tool invocations. This will be specified in model context (the model is talking to an intermediary for the user and needs to end turn to ask questions / clarifications).

### Built-in tools
Two tools are required by the harness itself and should be constructed first:

 - `EndTurnTool` named `end_turn` with no parameters. It signals the turn strategy to conclude the turn. The tool itself does not call `onTurnComplete`; the turn strategy does, once the batch is complete.
 - `MessageUserTool` named `message_user` taking a message string. This is how the assistant speaks to the user: its text is delivered through `onActionComplete` so the UI can render it and is also returned to the model as the tool result.

These should also be added first in the builder to ensure they are available correctly.

### Other tools
In addition to the built-in tools, required for the harness to function in any capacity, the following are also expected:

 - **ValidateSimluationTool** (`validate_simulation`): Run static checks against a simluation file given a single argument: the fiename of that simulation.
 - **RunSimulationTool** (`run_simulation`): Given name of simulation file, execute a simulation using the current model code and return back CSV output. 
 - **ListFilesTool** (`list_files`): List the files in the workspace.
 - **OutlineFileTool** (`outline_file`): Read metadata of a document where a PDF table of conents and length can be provided, Excel documents describe sheets, and Kigali Sim simulations list top level stanzas of a simulation. Should be given the filename to outline.
 - **ReadFileTool** (`read_file`): Read a specific file where PDFs can be specified in page numbers or section name, Excel documents can be specified in sheets, and Kigali Sim code can be specified by stanza name. Two arguments are expected: filename and section where, if section is empty, the entire file is returned. Note that this returns files as text so PDF / office file text is extracted and returned. Paths / directories not supported.
 - **EditFileTool** (`edit_simulation`): Execute a patch-based edit against a file where parameter of filename and patch (both string) are expected. At this time, only Kigali Sim simultion code files can be edited. Others will give an error. Paths / directories not supported.
 - **MessageUser** (`message_user`): Send markdown message to the user. HTML will be escaped prior to converting Markdown to HTML.
 - **CalculateTool** (`calculate`): Evaluate a mathematical expression through an embedded [EvalEx](https://ezylang.github.io/EvalEx/) instance.

### Open source libaries
A number of open source libraries are recommended in support of this work:

 - A JSON parser is needed for LLM communciation and [Clifton Labs' json-simple](https://cliftonlabs.github.io/json-simple/) is recommended from `com.github.cliftonlabs:json-simple`. This provides the `JsonObject` class. Note that some tools like Jackson which require reflection may have compatibility issues with TeaVM / WASM.
 - A tool for an LLM to perform calculations is likely helpful and [EvalEx](https://ezylang.github.io/EvalEx/) is recommended.
 - A Markdown library is needed for rendering to the user and [commonmark-java](https://github.com/commonmark/commonmark-java) is recommended. 
 - A library for PDF reading is needed and [Apache PDFBox](https://pdfbox.apache.org) is recommended.
 - A library for office files is needed and [Apache POI](https://poi.apache.org/) is recommnded.
 - A library for working wtih text diffs is needed and [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) is recommended.
 - From the JVM, network requests may be aided by [unirest](https://kong.github.io/unirest-java/).

### Testing approach
Completion of this component will include working `ChatLogStrategy` and `KigaliAssistant` as well as mock implementers of the model and tool strategies, the built-in tools, the turn strategy, and fixture-based tests over request assembly and response parsing. Because the protocol strategy is real even though the wire strategy is mocked, those tests can feed captured provider payloads through the loop and validate the wire format at this component rather than at the TeaVM work.

## Construction
This prototype is to be built through a multi-agent flow.

### Proceedure
An agent (coordinating agent) will be assigned a component and it will do the following:

 - **Design**: Use a sub-agent to look at existing code and design a full solution. This agent should not yet modify live code but further clarify the implementation plan, highlighting questions for the user as needed. That plan should be written into this file in the subsection corresponding to the component assigned. It should only work on the component assigned. Please also have this sub-agent check if some of the work that needs to be done is better in or mentioned as assigned to later components and indicate that work as out of scope. Where a design question hinges on documented behavior of a library, API, or platform (for example TeaVM / WASM constraints or a provider request format), have this sub-agent use the `kagi` command for web search to learn more about the problem and confirm assumptions rather than guess (see Notes at the bottom of this file). Check git diff before concluding.
 - **Implement**: Use a sub-agent to implement the component, completing only the component assigned. A description of what was done should be written into this file. This sub-agent should work to implement successfully and pass relevant checks given the sope of the change (ex: `./gradlew test`) after adding relevant unit tests but not worry about stylechecks like `./gradlew checkstyleMain checkstyleTest`. If anything unexpected emerged that could not be resolved, it should let the coordinating agent know so they can raise it to the user. It should only work on the component assigned. Check git diff before concluding.
 - **Clean**: Use a sub-agent to check the implementation of the component and improve it where appropriate before ensuring style issues are resolved and checking that `./gradlew checkstyleMain checkstyleTest` (or similar depending on scope) passes. If anything unexpected emerged that could not be resolved, it should let the coordinating agent know so they can raise it to the user. This sub-agent should look for similar pre-existing code in the project and ensure new code follows similar conventions. It should only work on the component assigned. Check git diff before starting and before concluding.
 - **Finalize**: If there are no open questions for the user, use a sub-agent to double check all automated checks relevant to the change pass including `./gradlew test checkstyleMain checkstyleTest`. It should confirm these first. Next, if and only if the component was implemented successfully, this sub-agent should summarize the whole component just completed in no more than 4-5 sentences and replace that component's design plan and implementation notes in this file with that summary, so that this specification does not grow too long; if the component was not completed successfully, it should leave the plan and details in place so any open work remains visible. Afterwards, it should check if the system context file needs any updates given the changes made in this component (remembering that the LLM will not be able to access the internet). Finally, it should commit. In that commit, `A Samuel Pottinger <sam.pottinger@berkeley.edu>` should be author but the following co-author tag is expected: `Co-authored-by: DeepSeek V4.1 Flash (via OpenRouter) <noreply@deepseek.com>`. It should only work on the component assigned. Check git diff before starting and before concluding.

Before starting work, all agents and sub-agents should read:

 - This file (`kigali_ai_spec.md`) 
 - `kigali_ai_background.md`
 - `llms-full.txt`
 - `DEVELOPING.md`
 - `CONTRIBUTING.md`
 - `README.md`

If instructed to automatically continue to the next component, the coordinating agent may continue to the next component if no unresolved issues raised. Otherwise, it should check in with the user before progressing to the next component.

### Components
This will be implemented in a series of individual components.

#### Component 1: System context
We will need a text file which is committed to the git respoistory which will be provided to the LLM before the user chat. This should include llms-full.txt as well as information about where the LLM is running (description of the tools and that it cannot talk directly to the user without using tools and that it cannot access the CLI or internet).

**Summary:** Component 1 adds the assistant's system context as a committed harness preamble (`engine/src/main/resources/org/kigalisim/assistant/system_context_preamble.md`) covering the sandbox constraints (no network, CLI, or filesystem), the tool-only `message_user` / `end_turn` protocol, the flat workspace, and provisional tool capabilities, with the canonical `llms-full.txt` appended. To keep the repo-root `llms-full.txt` as the single source of truth, `engine/build.gradle` bundles it into the assistant package at build time instead of committing a duplicate. `org.kigalisim.assistant.SystemContextProvider` reads both classpath resources as UTF-8, joins and caches them, and fails fast when either is missing. `SystemContextProviderTest` covers the preamble, bundled documentation, ordering, caching, and failure, and `./gradlew test checkstyleMain checkstyleTest` passes.

#### Component 2: UI Reorganization

**Summary:** Component 2 consolidates the scattered AI-assistant help into a single "AI Assistant" tab in `#editor-tabs`, replacing the two modal dialogs and their two launch buttons with one static `#ai-assistant-pane`. The pane merges the example prompt, documentation URLs, tutorial links, and the optional/privacy notes, with no login or chat inputs at this stage. The two dialogs (`#ai-assistant-dialog`, `#ai-designer-dialog`), two buttons (`#ai-designer-button` and the broken `#ai-assistant-button`), and the now-dead `AIAssistantPresenter` / `AIDesignerPresenter` classes with their `main.js` wiring were removed. The `prompt-code` CSS rule was retargeted to `#ai-assistant-pane .prompt-code`, and QUnit plus eslint pass.

#### Component 3: Harness foundation

**Summary:** Component 3 implements the transport-agnostic harness skeleton: the `KigaliAssistant`/`KigaliAssistantBuilder` facade, a split `AssistantActionCallback`/`AssistantTurnCallback` callback API, and the `AssistantResult` DTO, plus the built-in `message_user` and `end_turn` tools with a read-only `AssistantToolbelt` (and mutable concrete implementation). The model layer separates transport from protocol with a mockable `AssistantModelWireStrategy` and a real `OpenAiModelProtocolStrategy` that assembles `tool_choice:"required"` requests and parses content, ordered tool calls, finish reason, and verbatim replayable assistant messages. A callback-driven `UntimedAssistantTurnStrategy` implements the specified turn loop, including multi-call batches, implicit plain-text `message_user` replies, unknown-tool and malformed-argument failures, and the `TURN_ACTIONS_EXCEEDED` guard. Chat persistence is provided by `OpenAiCompatibleChatLog` and `InMemoryLogStrategy`, while `AssistantWorkspace` and `AssistantFileTypeStrategy` remain interfaces only. The `com.github.cliftonlabs:json-simple:4.0.1` dependency was added (and credited in the README), and JUnit 5 tests exercise the real protocol strategy through a mocked wire strategy with captured provider fixtures, with `./gradlew test checkstyleMain checkstyleTest` and Spotless passing.

#### Component 4: Workspace abstraction

**Summary:** Component 4 adds the in-memory workspace layer and its four model-facing tools: `TextFileTypeStrategy` plus the forgiving line-scanner `QtaFileTypeStrategy`, `InMemoryWorkspace` (byte-array `LinkedHashMap` storage with extension dispatch and `IllegalArgumentException` / `NoSuchElementException` error semantics), and `AssistantWorkspaceBuilder` (QubecTalk registered by default), extending `AssistantWorkspace` with `listFiles()` and `outlineFile(String)`. `ListFilesTool`, `OutlineFileTool`, `ReadFileTool`, and `EditFileTool` validate JSON arguments and convert workspace and patch errors into `forToolCallError` results, with `EditFileTool` applying standard unified diffs through `io.github.java-diff-utils:java-diff-utils:4.17` (credited in the README) on the logging-free `UnifiedDiffUtils.parseUnifiedDiff` + `Patch.applyTo` path to stay TeaVM-safe. All new types remain under `org.kigalisim.assistant`, with eight new test classes (39 tests) covering the strategies, workspace, builder, each tool, and an end-to-end workspace-tools scenario. No `BinaryFileTypeStrategy`, disk/OPFS workspace, simulation validation, or WASM/UI wiring is included here, and `./gradlew test checkstyleMain checkstyleTest spotlessCheck` and `./gradlew war` pass (`war` is a build smoke check because the assistant is not yet reachable from `KigaliWasmSimFacade`).

#### Component 5: Math in harness

**Decision (approved):** Use EvalEx `com.ezylang:EvalEx:3.7.0` to unblock this component, accepting that it is **not TeaVM/WASM-compatible**. Evaluating even a single expression fails a TeaVM 0.15.0 WasmGC build with `Method java.util.Date.toInstant()Ljava/time/Instant; was not found` / `Method java.util.Calendar.toInstant()Ljava/time/Instant; was not found`, raised from `DateTimeConverter.convert` via `DefaultEvaluationValueConverter.convertObject` -> `EvaluationValue.of` -> `Expression.convertValue` (reproduced in `/tmp/evalex-teavm`). This is unavoidable through the public API because `ExpressionConfiguration` field-initializes a `DefaultEvaluationValueConverter` and `EvaluationValue.of` always routes through it. It is acceptable for now because no assistant class is reachable from `KigaliWasmSimFacade`, so `./gradlew war` still passes and the failure will not surface until Component 15. **This dependency is temporary and will be replaced with a TeaVM-safe evaluator before WASM integration.** `com.ezylang:EvalEx:3.7.0` is 98 days old as of 2026-10-16 (Maven Central `lastUpdated=20260710152930`; no newer release) and declares no runtime dependencies (provided Lombok only).

**Problem statement:** The model needs a `calculate` tool so it can evaluate a single mathematical expression (for example unit conversions or back-of-envelope arithmetic while assembling a simulation) and receive the numeric result as tool output. This must follow the existing tool pattern in `org.kigalisim.assistant.tool` (`AssistantToolStrategy`, `JsonObject` JSON Schema, `AssistantResultBuilder.forToolCall` / `forToolCallError`). The EvalEx dependency makes the reachable WASM build fail at Component 15; that breakage is knowingly deferred, and `war` passing now is only because the assistant is not yet reachable from the WASM facade.

**Implementation steps:**

 - **`engine/build.gradle`** — add `implementation("com.ezylang:EvalEx:3.7.0")` beside the json-simple dependency, with a comment noting it is a temporary dependency that is not TeaVM/WASM-safe and must be replaced before Component 15.
 - **`README.md`** — add an Apache-2.0 credit in the alphabetical "Open Source" list, after the ESLint entry and before JSDoc: `- [EvalEx](https://ezylang.github.io/EvalEx/) under [Apache-2.0](https://github.com/ezylang/EvalEx/blob/main/LICENSE).`
 - **`engine/src/main/java/org/kigalisim/assistant/tool/CalculateTool.java`** (new; all types stay under `org.kigalisim.assistant`):
   - Implements `AssistantToolStrategy`; `public static final String NAME = "calculate";`.
   - Holds one embedded, shared `private final ExpressionConfiguration configuration = ExpressionConfiguration.defaultConfiguration();` (immutable in EvalEx and safe to reuse; do not cache `Expression` instances).
   - `getName()` returns `NAME`; `getDescription()` returns e.g. `"Evaluate a single mathematical expression and return the numeric result"`.
   - `getParameterSchema()` returns `{"type":"object","properties":{"expression":{"type":"string","description":"The mathematical expression to evaluate, e.g. (1 + 2) * 3"}},"required":["expression"]}`.
   - `execute(JsonObject call, AssistantActionCallback callback)`:
     - Read `call.get("expression")`; if it is not a `String` or is blank, report `AssistantResultBuilder.forToolCallError("Missing required parameter: expression").build()` and return.
     - Evaluate with `EvaluationValue value = new Expression(expression, configuration).evaluate();`.
     - If `!value.isNumberValue()`, report `forToolCallError("The expression did not evaluate to a number: " + expression)`.
     - Otherwise report `AssistantResultBuilder.forToolCall(value.getNumberValue().stripTrailingZeros().toPlainString()).build()`.
     - Catch `EvaluationException | ParseException` (the latter checked), plus `ArithmeticException | IllegalArgumentException`, and report `forToolCallError(e.getMessage())`; the tool must not throw. (Division by zero arrives as `EvaluationException("Division by zero")`; malformed input as `ParseException`.)
   - Do **not** modify `KigaliAssistantBuilder`: `calculate` is an "other tool" registered by callers via `addTool(new CalculateTool())`, exactly like the Component 4 workspace tools.
 - **`engine/src/test/java/org/kigalisim/assistant/tool/CalculateToolTest.java`** (new):
   - `testNameAndSchema`: `NAME == "calculate"`, `getName()` matches, schema `type == "object"`, `properties.expression.type == "string"`, `required` contains `expression`.
   - `testSimpleExpression`: `2 + 3 * 4` -> `"14"`.
   - `testDecimalAndParentheses`: `(1 + 2) / 4` -> `"0.75"`.
   - `testPower`: `2 ^ 10` -> `"1024"`.
   - `testMissingExpression`: empty `JsonObject` -> one failed result mentioning `expression`.
   - `testBlankExpression`: `"   "` -> failure.
   - `testNonStringExpression`: `5` (Integer) -> failure.
   - `testMalformedExpression`: `"2 +"` -> failure (`getWasSuccessful()` false).
   - `testDivisionByZero`: `"1 / 0"` -> failure whose error contains `Division by zero`.
   - `testNonNumericResult`: `"1 == 1"` -> failure mentioning non-numeric.
   - Follow `MessageUserToolTest` / `ListFilesToolTest` style: collect into `ArrayList<AssistantResult>` via `tool.execute(call, results::add)`, assert `getWasSuccessful()` and `getRawResponse().orElseThrow().getContent().orElse(null)` on success, `getError()` on failure.

**Out of scope:** registering `calculate` in `KigaliAssistantBuilder` or the system context preamble; symbolic variables, custom functions, and multi-expression programs; any change to existing tools, workspace, model, or turn-loop code; and no `org.kigalisim` core types are touched or imported. Replacing EvalEx with a TeaVM-safe evaluator is explicitly deferred to a later component (before WASM integration), not done here.

**Verification:** run `./gradlew test checkstyleMain checkstyleTest spotlessCheck` and `./gradlew war`. Record that `war` passes only because the assistant is not yet reachable from `KigaliWasmSimFacade`; EvalEx will break WasmGC at Component 15 unless replaced.


#### Component 6: Simulation execution
We will add support for `ValidateSimulationTool` and `RunSimulationTool`. This requires designing a bridge into the rest of the Kigali Sim codebase and the results should be described in CSV text so that they can be transported to the LLM. In this process, we should be careful that types from the assistant package do not leak into the rest of the codebase. See `KigaliSimFacade`.

#### Component 7: PDF files
An example PDF file will be provided for use in tests. A `FileTypeStrategy` should be added for PDF files using Apache PDFBox.

#### Component 8: Office files
An example PDF file will be provided for use in tests. A `FileTypeStrategy` should be added for PDF files using Apache POI.

#### Component 9: Human transcript content
At this stage, human readable versions of the chat transcript are not yet available. 

#### Component 10: Context document update
The context document created in component 1 should be updated now that all of the available tools are completed. We should remember that the LLM will not have access to the public web so this document needs to be quite complete, including with how to use the calculator, for example.

#### Component 11: JVM strategies
We should build out remaining classes which allow for running on the JVM. Specifically, `LocalDirectoryWorkspace` which should use a directory from file system as the workspace (again paths not supported for security reasons) as well as a new `LocalChatLogStrategy` that has one file per chat saved to a local directory.

#### Component 12: Live model
We should add support for `UnirestModelWireStrategy` and `OpenAiModelProtocolStrategy` that are prepared to interact with an OpenAI API compliant endpoint.

#### Component 13: CLI with files backing
In this step, we will add new command line access to an interactive version of this harness (see `KigaliSimCommander`) via a new `ai` command. This is intended as somewhat minimal. It should be given the path to a directory to use as the workspace (`--workspace`) and a path to a directory to use for persisting chats (`--chats`). To allow those directories to be the same, we should filter for `.chat` files and assume those are chat only. In this process, we should create `DiskWorkspace` and `DiskLogStrategy`. Additionally, it should read the OpenAI API compatible endpoint information from flags (`--model-endpoint`, `--model-name`, `--model-api-key`) but, if not given, it should read them from environment variables `KIGLAI_AI_MODEL_ENDPOINT`, `KIGALI_AI_MODEL_NAME`, and `KIGALI_AI_API_KEY`.

#### Component 14: TeaVM strategies
Next, we need to offer alternatives to the JVM objects we implemented for WASM. See the [Josh project](https://github.com/SchmidtDSE/josh) and [its CompatibilityLayerKeeper](https://github.com/SchmidtDSE/josh/blob/main/src/main/java/org/joshsim/JoshJsSimFacade.java#L271) for a similar kind of system and how that codebase manages compatibility across the JVM and WASM. For this, we may wish to make an JsAiFacade in the JS side of this project (see `editor`). That can then be made available to Java code per [TeaVM documentation](https://teavm.org/docs/runtime/jso.html). This should allow us to create `OpfsWorkspace` and `OpfsLogStrategy` which can split on `.chat` to determine if chat or workspace file. For an example of working with multiple files in OPFS, please see [data.js from Josh](https://github.com/SchmidtDSE/josh/blob/main/editor/js/data.js) but, again, these may need to be exposed to WASM code per [TeaVM documentation](https://teavm.org/docs/runtime/jso.html).

#### Component 15: WASM / JS layer
Next, we should make the new AI functionality from WASM available to JS. There is a queue system run via [wasm_backend.js](https://github.com/SchmidtDSE/kigali-sim/blob/main/editor/js/wasm_backend.js) which should be extended. We should have a request type to describe current requests (`evaluateSimluation`) and then we can add request types for AI as well. One of these should be (`initAssistant`) and should include information about the OpenAI API compatible endpoint to access and that those will eventually be read from the UI. We should still have [the worker](https://github.com/SchmidtDSE/kigali-sim/blob/main/editor/js/wasm.worker.js) actually execute these requests by way of new methods on [KigaliWasmSimFacade](https://github.com/SchmidtDSE/kigali-sim/blob/main/engine/src/main/java/org/kigalisim/KigaliWasmSimFacade.java).

#### Component 16: UI integration (no files)
This component will not yet address supporting file management and will continue to assume only a single chat, only a single `.qta` file (current functionality), and no supporting office or PDF files. Nevertheless, the new AI tab should have three links:

 - Use Kigali Sim’s AI Assistant. This should have description text underneath: Use a privacy-respecting AI assistant provided by Kigali Sim to create, edit, and interpret simulations. Clicking should show an input box to let the user enter their key or instructions to email the `hello@` address to get access.
 - Give Kigali Sim to Your AI Assistant. This should have description text underneath: Hook up an AI assistant you already use such that it can run Kigali Sim for you. You can chat like you normally would. Clicking should show current instructions.
 - Put Your AI Assisstant Inside Kigali Sim. This should have description text underneath: Chat with your existing AI assistant directly from Kigali Sim and let it take actions for you here. Clicking should show login inputs for model endpoint, model name, and model key.

This should create a simple chat interface (can only send messages and review a chat log). This should be supported through new Presenters (see existing editor code) in a new file `assistant.js`. See current import structure.

#### Component 17: UI integration (Multiple chats)
This should extend Component 16. Instead of seeing a chat immediately, a list of chats (list of links) should be shown with a link to start a new chat as the final item in that list. One should also have delete links for chats. When clicking delete, prompt to confirm the chat should be deleted. When clicking new chat, prompt for name (open new chat automatically). Clicking into a chat will show the existing chat interface but there will now be a link to go back to chat list at the top of the chat tab. List sorted alphabetically.

#### Component 18: UI integration (support files)
In the chat interface, there should be a link to manage supporting files. This should open a `dialog` in which the user can add / remove supporting files like PDF files and Office files. See [data.js](https://github.com/SchmidtDSE/josh/blob/main/editor/js/data.js) from Josh for an example.

#### Component 19: Multiple qta files
Up to this point, only one script file can be selected / edited in the Kigali Sim editor. Next to the simulation header there should be a link for the name of the simulation which starts as Untitled (reads as: `Simulation: <a href=“#”>Untitled</a>`). Clicking that link should open a `dialog` box where the user can select from different (`.qta`) files saved in OPFS (manage this through a `sketchbook.js` with new Presenters following existing pattern). Each simulation in the list displayed should have a link for open, rename, delete. Finally, at the bottom of the list, there should be a link for new simulation. The webpage should open to the last selected simluation if found or, if not, a new untitled simluation. This can be tracked by cookie. If `Untitled.qta` is already present, use `Untitled_1.qta` and so on. Spaces in names should be converted to `_`. Clicking new will prompt for name (error if name collides with existing). Clicking open will replace the editor contents, re-render designer, and re-run simulation. Clicking rename will prompt for new name (error if name collides with existing). Clicking delete will remove after confirm.

#### Component 20: Timeout handling
JVM and WASM need different ways to time out if waiting too long to hear from the LLM. Please use the `kagi` command to web search for options on how best to do this.

#### Component 21: Markdown files
We should add support for `.md` files in the workspaces for both JVM and WASM where outline will show headers up to and including `###`. That will allow the LLM to ask for specific sections.

#### Component 22: Privacy policy
The privacy policy should be updated to indicate that cookies are used but essential only to keep track of which simluation is open. This is not transmitted back for analytics. We should also add that supporting data files and chats are also held locally. Finally, in the chat list, we should mention that chats are saved locally on the user’s machine only. Then, we should do something similar in the supporting files and simulation files list.

<br>

## Notes
To web search, please use the Kagi command like so: `kagi search --format pretty "test"`.

