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

import org.jeasy.flows.action.*;

import java.util.UUID;

/**
 * A conditional flow is defined by 4 artifacts:
 *
 * <ul>
 *     <li>The action to execute first</li>
 *     <li>A predicate for the conditional logic</li>
 *     <li>The action to execute if the predicate is satisfied</li>
 *     <li>The action to execute if the predicate is not satisfied (optional)</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 * @see ConditionalFlow.Builder
 */
public class ConditionalFlow extends AbstractWorkFlow {

    private final Action initialActionUnit, nextOnPredicateSuccess, nextOnPredicateFailure;
    private final ActionReportPredicate predicate;

    ConditionalFlow(String name, Action initialActionUnit, Action nextOnPredicateSuccess, Action nextOnPredicateFailure, ActionReportPredicate predicate) {
        super(name);
        this.initialActionUnit = initialActionUnit;
        this.nextOnPredicateSuccess = nextOnPredicateSuccess;
        this.nextOnPredicateFailure = nextOnPredicateFailure;
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     */
    public ActionReport execute(ActionContext actionContext) {
        ActionReport jobReport = initialActionUnit.execute(actionContext);
        if (predicate.apply(jobReport)) {
            jobReport = nextOnPredicateSuccess.execute(actionContext);
        } else {
            if (nextOnPredicateFailure != null && !(nextOnPredicateFailure instanceof NoOpAction)) { // else is optional
                jobReport = nextOnPredicateFailure.execute(actionContext);
            }
        }
        return jobReport;
    }

    public static class Builder {

        private Builder() {
            // force usage of static method aNewConditionalFlow
        }

        public static NameStep aNewConditionalFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            WhenStep execute(Action initialActionUnit);
        }

        public interface WhenStep {
            ThenStep when(ActionReportPredicate predicate);
        }

        public interface ThenStep {
            OtherwiseStep then(Action action);
        }

        public interface OtherwiseStep extends BuildStep {
            BuildStep otherwise(Action action);
        }

        public interface BuildStep {
            ConditionalFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, WhenStep, ThenStep, OtherwiseStep, BuildStep {

            private String name;
            private Action initialActionUnit, nextOnPredicateSuccess, nextOnPredicateFailure;
            private ActionReportPredicate predicate;

            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.initialActionUnit = new NoOpAction();
                this.nextOnPredicateSuccess = new NoOpAction();
                this.nextOnPredicateFailure = new NoOpAction();
                this.predicate = ActionReportPredicate.ALWAYS_FALSE;
            }

            @Override
            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public WhenStep execute(Action initialActionUnit) {
                this.initialActionUnit = initialActionUnit;
                return this;
            }

            @Override
            public ThenStep when(ActionReportPredicate predicate) {
                this.predicate = predicate;
                return this;
            }

            @Override
            public OtherwiseStep then(Action action) {
                this.nextOnPredicateSuccess = action;
                return this;
            }

            @Override
            public BuildStep otherwise(Action action) {
                this.nextOnPredicateFailure = action;
                return this;
            }

            @Override
            public ConditionalFlow build() {
                return new ConditionalFlow(this.name, this.initialActionUnit, this.nextOnPredicateSuccess, this.nextOnPredicateFailure, this.predicate);
            }
        }
    }
}
