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
package org.jeasy.flows.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.flows.action.ActionReportPredicate.COMPLETED;
import static org.jeasy.flows.engine.WorkFlowEngineBuilder.aNewWorkFlowEngine;
import static org.jeasy.flows.workflow.ConditionalFlow.Builder.aNewConditionalFlow;
import static org.jeasy.flows.workflow.ParallelFlow.Builder.aNewParallelFlow;
import static org.jeasy.flows.workflow.RepeatFlow.Builder.aNewRepeatFlow;
import static org.jeasy.flows.workflow.SequentialFlow.Builder.aNewSequentialFlow;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jeasy.flows.action.*;
import org.jeasy.flows.workflow.*;
import org.junit.Test;
import org.mockito.Mockito;

public class WorkFlowEngineImplTest {

    private final WorkFlowEngine workFlowEngine = new WorkFlowEngineImpl();

    @Test
    public void run() {
        // given
        WorkFlow workFlow = Mockito.mock(WorkFlow.class);
        ActionContext actionContext = Mockito.mock(ActionContext.class);

        // when
        workFlowEngine.run(workFlow,actionContext);

        // then
        Mockito.verify(workFlow).execute(actionContext);
    }

    /**
     * The following tests are not really unit tests, but serve as examples of how to create a workflow and execute it
     */

    @Test
    public void composeWorkFlowFromSeparateFlowsAndExecuteIt() {

        PrintMessageWork work1 = new PrintMessageWork("foo");
        PrintMessageWork work2 = new PrintMessageWork("hello");
        PrintMessageWork work3 = new PrintMessageWork("world");
        PrintMessageWork work4 = new PrintMessageWork("done");

        RepeatFlow repeatFlow = aNewRepeatFlow()
                .named("print foo 3 times")
                .repeat(work1)
                .times(3)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        ParallelFlow parallelFlow = aNewParallelFlow()
                .named("print 'hello' and 'world' in parallel")
                .execute(work2, work3)
                .with(executorService)
                .build();

        ConditionalFlow conditionalFlow = aNewConditionalFlow()
                .execute(parallelFlow)
                .when(COMPLETED)
                .then(work4)
                .build();

        SequentialFlow sequentialFlow = aNewSequentialFlow()
                .execute(repeatFlow)
                .then(conditionalFlow)
                .build();

        WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
        ActionContext actionContext = new ActionContext();
        ActionReport actionReport = workFlowEngine.run(sequentialFlow, actionContext);
        executorService.shutdown();
        assertThat(actionReport.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        System.out.println("workflow report = " + actionReport);
    }

    @Test
    public void defineWorkFlowInlineAndExecuteIt() {

        PrintMessageWork work1 = new PrintMessageWork("foo");
        PrintMessageWork work2 = new PrintMessageWork("hello");
        PrintMessageWork work3 = new PrintMessageWork("world");
        PrintMessageWork work4 = new PrintMessageWork("done");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        WorkFlow workflow = aNewSequentialFlow()
                .execute(aNewRepeatFlow()
                            .named("print foo 3 times")
                            .repeat(work1)
                            .times(3)
                            .build())
                .then(aNewConditionalFlow()
                        .execute(aNewParallelFlow()
                                    .named("print 'hello' and 'world' in parallel")
                                    .execute(work2, work3)
                                    .with(executorService)
                                    .build())
                        .when(COMPLETED)
                        .then(work4)
                        .build())
                .build();

        WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
        ActionContext actionContext = new ActionContext();
        ActionReport actionReport = workFlowEngine.run(workflow, actionContext);
        executorService.shutdown();
        assertThat(actionReport.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        System.out.println("workflow report = " + actionReport);
    }

    @Test
    public void useWorkContextToPassInitialParametersAndShareDataBetweenWorkUnits() {
        WordCountWork work1 = new WordCountWork(1);
        WordCountWork work2 = new WordCountWork(2);
        AggregateWordCountsWork work3 = new AggregateWordCountsWork();
        PrintWordCount work4 = new PrintWordCount();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        WorkFlow workflow = aNewSequentialFlow()
                .execute(aNewParallelFlow()
                            .execute(work1, work2)
                            .with(executorService)
                            .build())
                .then(work3)
                .then(work4)
                .build();

        WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
        ActionContext actionContext = new ActionContext();
        actionContext.put("partition1", "hello foo");
        actionContext.put("partition2", "hello bar");
        ActionReport actionReport = workFlowEngine.run(workflow, actionContext);
        executorService.shutdown();
        assertThat(actionReport.getStatus()).isEqualTo(ActionStatus.COMPLETED);
    }

    static class PrintMessageWork implements Action {

        private final String message;

        public PrintMessageWork(String message) {
            this.message = message;
        }

        public String getName() {
            return "print message action";
        }

        public ActionReport execute(ActionContext actionContext) {
            System.out.println(message);
            return new DefaultActionReport(ActionStatus.COMPLETED, actionContext);
        }

    }

    static class WordCountWork implements Action {

        private final int partition;

        public WordCountWork(int partition) {
            this.partition = partition;
        }

        @Override
        public String getName() {
            return "count words in a given string";
        }

        @Override
        public ActionReport execute(ActionContext actionContext) {
            String input = (String) actionContext.get("partition" + partition);
            actionContext.put("wordCountInPartition" + partition, input.split(" ").length);
            return new DefaultActionReport(ActionStatus.COMPLETED, actionContext);
        }
    }

    static class AggregateWordCountsWork implements Action {

        @Override
        public String getName() {
            return "aggregate word counts from partitions";
        }

        @Override
        public ActionReport execute(ActionContext actionContext) {
            Set<Map.Entry<String, Object>> entrySet = actionContext.getEntrySet();
            int sum = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                if (entry.getKey().contains("InPartition")) {
                    sum += (int) entry.getValue();
                }
            }
            actionContext.put("totalCount", sum);
            return new DefaultActionReport(ActionStatus.COMPLETED, actionContext);
        }
    }

    static class PrintWordCount implements Action {

        @Override
        public String getName() {
            return "print total word count";
        }

        @Override
        public ActionReport execute(ActionContext actionContext) {
            int totalCount = (int) actionContext.get("totalCount");
            System.out.println(totalCount);
            return new DefaultActionReport(ActionStatus.COMPLETED, actionContext);
        }
    }
}
