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

import org.jeasy.flows.action.NoOpAction;
import org.jeasy.flows.action.Action;
import org.jeasy.flows.action.ActionContext;
import org.jeasy.flows.action.ActionReportPredicate;
import org.jeasy.flows.action.ActionReport;

import java.util.UUID;

/**
 * A repeat flow executes an action repeatedly until its report satisfies a given predicate.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class RepeatFlow extends AbstractWorkFlow {

    private final Action action;
    private final ActionReportPredicate predicate;

    RepeatFlow(String name, Action action, ActionReportPredicate predicate) {
        super(name);
        this.action = action;
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     */
    public ActionReport execute(ActionContext actionContext) {
        ActionReport actionReport;
        do {
            actionReport = action.execute(actionContext);
        } while (predicate.apply(actionReport));
        return actionReport;
    }

    public static class Builder {

        private Builder() {
            // force usage of static method aNewRepeatFlow
        }

        public static NameStep aNewRepeatFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends RepeatStep {
            RepeatStep named(String name);
        }

        public interface RepeatStep {
            UntilStep repeat(Action action);
        }

        public interface UntilStep {
            BuildStep until(ActionReportPredicate predicate);
            BuildStep times(int times);
        }

        public interface BuildStep {
            RepeatFlow build();
        }

        private static class BuildSteps implements NameStep, RepeatStep, UntilStep, BuildStep {

            private String name;
            private Action action;
            private ActionReportPredicate predicate;

            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.action = new NoOpAction();
                this.predicate = ActionReportPredicate.ALWAYS_FALSE;
            }
            
            @Override
            public RepeatStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public UntilStep repeat(Action action) {
                this.action = action;
                return this;
            }

            @Override
            public BuildStep until(ActionReportPredicate predicate) {
                this.predicate = predicate;
                return this;
            }

            @Override
            public BuildStep times(int times) {
                until(ActionReportPredicate.TimesPredicate.times(times));
                return this;
            }

            @Override
            public RepeatFlow build() {
                return new RepeatFlow(name, action, predicate);
            }
        }

    }
}
