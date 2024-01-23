/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.jeasy.flows.workflow;

import org.jeasy.flows.action.Action;
import org.jeasy.flows.action.ActionContext;
import org.jeasy.flows.action.ActionReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class ParallelFlowExecutor {

    private final ExecutorService workExecutor;

    ParallelFlowExecutor(ExecutorService workExecutor) {
        this.workExecutor = workExecutor;
    }

    List<ActionReport> executeInParallel(List<Action> actionUnits, ActionContext actionContext) {
        // prepare tasks for parallel submission
        List<Callable<ActionReport>> tasks = new ArrayList<>(actionUnits.size());
        actionUnits.forEach(action -> tasks.add(() -> action.execute(actionContext)));

        // submit action units and wait for results
        List<Future<ActionReport>> futures;
        try {
            futures = this.workExecutor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException("The parallel flow was interrupted while executing action units", e);
        }
        Map<Action, Future<ActionReport>> workToReportFuturesMap = new HashMap<>();
        for (int index = 0; index < actionUnits.size(); index++) {
            workToReportFuturesMap.put(actionUnits.get(index), futures.get(index));
        }

        // gather reports
        List<ActionReport> actionReports = new ArrayList<>();
        for (Map.Entry<Action, Future<ActionReport>> entry : workToReportFuturesMap.entrySet()) {
            try {
                actionReports.add(entry.getValue().get());
            } catch (InterruptedException e) {
                String message = String.format("The parallel flow was interrupted while waiting for the result of action unit '%s'", entry.getKey().getName());
                throw new RuntimeException(message, e);
            } catch (ExecutionException e) {
                String message = String.format("Unable to execute action unit '%s'", entry.getKey().getName());
                throw new RuntimeException(message, e);
            }
        }

        return actionReports;
    }
}
