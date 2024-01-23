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

import org.jeasy.flows.action.ActionContext;
import org.jeasy.flows.action.ActionReport;
import org.jeasy.flows.action.ActionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aggregate report of the partial reports of action units executed in a parallel flow.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class ParallelFlowReport implements ActionReport {

    private final List<ActionReport> reports;

    /**
     * Create a new {@link ParallelFlowReport}.
     */
    public ParallelFlowReport() {
        this(new ArrayList<>());
    }

    /**
     * Create a new {@link ParallelFlowReport}.
     * 
     * @param reports of works executed in parallel
     */
    public ParallelFlowReport(List<ActionReport> reports) {
        this.reports = reports;
    }

    /**
     * Get partial reports.
     *
     * @return partial reports
     */
    public List<ActionReport> getReports() {
        return reports;
    }

    void add(ActionReport actionReport) {
        reports.add(actionReport);
    }

    void addAll(List<ActionReport> actionReports) {
        reports.addAll(actionReports);
    }

    /**
     * Return the status of the parallel flow.
     *
     * The status of a parallel flow is defined as follows:
     *
     * <ul>
     *     <li>{@link ActionStatus#COMPLETED}: If all action units have successfully completed</li>
     *     <li>{@link ActionStatus#FAILED}: If one of the action units has failed</li>
     * </ul>
     * @return workflow status
     */
    @Override
    public ActionStatus getStatus() {
        for (ActionReport report : reports) {
            if (report.getStatus().equals(ActionStatus.FAILED)) {
                return ActionStatus.FAILED;
            }
        }
        return ActionStatus.COMPLETED;
    }

    /**
     * Return the first error of partial reports.
     *
     * @return the first error of partial reports.
     */
    @Override
    public Throwable getError() {
        for (ActionReport report : reports) {
            Throwable error = report.getError();
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * The parallel flow context is the union of all partial contexts. In a parallel
     * flow, each action unit should have its own unique keys to avoid key overriding
     * when merging partial contexts.
     * 
     * @return the union of all partial contexts
     */
    @Override
    public ActionContext getActionContext() {
        ActionContext actionContext = new ActionContext();
        for (ActionReport report : reports) {
            ActionContext partialContext = report.getActionContext();
            for (Map.Entry<String, Object> entry : partialContext.getEntrySet()) {
                actionContext.put(entry.getKey(), entry.getValue());
            }
        }
        return actionContext;
    }
}
