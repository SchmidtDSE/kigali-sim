/**
 * Parallel executor for running multiple simulations concurrently.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Executes simulations in parallel using a producer/consumer pattern.
 *
 * <p>This executor runs simulations across multiple CPU cores, with producer threads
 * running scenarios and a consumer collecting results with progress reporting.</p>
 */
public class ParallelSimulationExecutor {
  private final ParsedProgram program;
  private final List<SimulationTask> tasks;
  private final ProgressReporter progressReporter;

  /**
   * Create a new parallel simulation executor.
   *
   * @param program The parsed program containing scenarios to run
   * @param tasks The list of simulation tasks to execute
   * @param progressReporter Reporter for tracking progress
   */
  public ParallelSimulationExecutor(ParsedProgram program, List<SimulationTask> tasks,
                                    ProgressReporter progressReporter) {
    this.program = program;
    this.tasks = tasks;
    this.progressReporter = progressReporter;
  }

  /**
   * Execute all simulation tasks in parallel.
   *
   * @return List of all engine results from all simulations
   * @throws Exception If execution fails
   */
  public List<EngineResult> execute() throws Exception {
    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService producerPool = Executors.newFixedThreadPool(numThreads);
    BlockingQueue<List<EngineResult>> resultQueue = new ArrayBlockingQueue<>(tasks.size());

    AtomicInteger completedTasks = new AtomicInteger(0);

    // Submit all simulation tasks to producer pool
    List<Future<Void>> futures = new ArrayList<>();
    for (SimulationTask task : tasks) {
      Future<Void> future = producerPool.submit(() -> {
        // Run the scenario
        Stream<EngineResult> results = KigaliSimFacade.runScenario(
            program, task.getScenarioName(), null);
        List<EngineResult> resultList = new ArrayList<>();
        results.forEach(resultList::add);

        // Put results in queue for consumer
        resultQueue.put(resultList);

        // Update progress
        int completed = completedTasks.incrementAndGet();
        progressReporter.reportProgress(completed, tasks.size());

        return null;
      });
      futures.add(future);
    }

    // Shutdown producer pool (no more tasks will be submitted)
    producerPool.shutdown();

    // Consumer: collect all results from queue
    List<EngineResult> allResults = new ArrayList<>();
    try {
      // Wait for all producer tasks to complete
      for (Future<Void> future : futures) {
        future.get(); // Will throw if any task failed
      }

      // Wait for producer pool to fully terminate
      producerPool.awaitTermination(1, TimeUnit.HOURS);

      // Collect all results from queue
      while (!resultQueue.isEmpty()) {
        allResults.addAll(resultQueue.poll());
      }

    } catch (Exception e) {
      producerPool.shutdownNow();
      throw e;
    }

    return allResults;
  }
}
