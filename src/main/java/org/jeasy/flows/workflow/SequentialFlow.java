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
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jeasy.flows.action.ActionStatus.FAILED;

/**
 * A sequential flow executes a set of action units in sequence.
 *
 * If a unit of action fails, next action units in the pipeline will be skipped.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class SequentialFlow extends AbstractWorkFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialFlow.class.getName());

    private final List<Action> actionUnits = new ArrayList<>();

    SequentialFlow(String name, List<Action> actionUnits) {
        super(name);
        this.actionUnits.addAll(actionUnits);
    }

    /**
     * {@inheritDoc}
     */
    public ActionReport execute(ActionContext actionContext) {
        ActionReport actionReport = null;
        for (Action action : actionUnits) {
            actionReport = action.execute(actionContext);
            if (actionReport != null && FAILED.equals(actionReport.getStatus())) {
                LOGGER.info("Action unit ''{}'' has failed, skipping subsequent action units", action.getName());
                break;
            }
        }
        return actionReport;
    }

    public static class Builder {

        private Builder() {
            // force usage of static method aNewSequentialFlow
        }

        public static NameStep aNewSequentialFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            ThenStep execute(Action initialAction);
            ThenStep execute(List<Action> initialActionUnits);
        }

        public interface ThenStep {
            ThenStep then(Action nextAction);
            ThenStep then(List<Action> nextActionUnits);
            SequentialFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, ThenStep {

            private String name;
            private final List<Action> actions;
            
            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.actions = new ArrayList<>();
            }
            
            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public ThenStep execute(Action initialAction) {
                this.actions.add(initialAction);
                return this;
            }

            @Override
            public ThenStep execute(List<Action> initialActionUnits) {
                this.actions.addAll(initialActionUnits);
                return this;
            }

            @Override
            public ThenStep then(Action nextAction) {
                this.actions.add(nextAction);
                return this;
            }

            @Override
            public ThenStep then(List<Action> nextActionUnits) {
                this.actions.addAll(nextActionUnits);
                return this;
            }

            @Override
            public SequentialFlow build() {
                return new SequentialFlow(this.name, this.actions);
            }
        }
    }
}
