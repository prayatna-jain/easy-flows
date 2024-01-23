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
import org.jeasy.flows.action.ActionReportPredicate;
import org.junit.Test;
import org.mockito.Mockito;

public class ConditionalFlowTest {

    @Test
    public void callOnPredicateSuccess() {
        // given
        Action toExecute = Mockito.mock(Action.class);
        Action nextOnPredicateSuccess = Mockito.mock(Action.class);
        Action nextOnPredicateFailure = Mockito.mock(Action.class);
        ActionContext actionContext = Mockito.mock(ActionContext.class);
        ActionReportPredicate predicate = ActionReportPredicate.ALWAYS_TRUE;
        ConditionalFlow conditionalFlow = ConditionalFlow.Builder.aNewConditionalFlow()
                .named("testFlow")
                .execute(toExecute)
                .when(predicate)
                .then(nextOnPredicateSuccess)
                .otherwise(nextOnPredicateFailure)
                .build();

        // when
        conditionalFlow.execute(actionContext);

        // then
        Mockito.verify(toExecute, Mockito.times(1)).execute(actionContext);
        Mockito.verify(nextOnPredicateSuccess, Mockito.times(1)).execute(actionContext);
        Mockito.verify(nextOnPredicateFailure, Mockito.never()).execute(actionContext);
    }

    @Test
    public void callOnPredicateFailure() {
        // given
        Action toExecute = Mockito.mock(Action.class);
        Action nextOnPredicateSuccess = Mockito.mock(Action.class);
        Action nextOnPredicateFailure = Mockito.mock(Action.class);
        ActionContext actionContext = Mockito.mock(ActionContext.class);
        ActionReportPredicate predicate = ActionReportPredicate.ALWAYS_FALSE;
        ConditionalFlow conditionalFlow = ConditionalFlow.Builder.aNewConditionalFlow()
                .named("anotherTestFlow")
                .execute(toExecute)
                .when(predicate)
                .then(nextOnPredicateSuccess)
                .otherwise(nextOnPredicateFailure)
                .build();

        // when
        conditionalFlow.execute(actionContext);

        // then
        Mockito.verify(toExecute, Mockito.times(1)).execute(actionContext);
        Mockito.verify(nextOnPredicateFailure, Mockito.times(1)).execute(actionContext);
        Mockito.verify(nextOnPredicateSuccess, Mockito.never()).execute(actionContext);
    }

}
