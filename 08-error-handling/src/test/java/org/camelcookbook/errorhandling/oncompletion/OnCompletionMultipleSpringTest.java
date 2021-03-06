/*
 * Copyright (C) Scott Cranton, Jakub Korab, and Christian Posta
 * https://github.com/CamelCookbook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camelcookbook.errorhandling.oncompletion;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Demonstrates that multiple onCompletion blocks defined in the same scope do not behave as expected.
 * The one defined last within the route will be triggered.
 * The one defined first in a global scope will be triggered.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // timing issue with mocks
public class OnCompletionMultipleSpringTest extends CamelSpringTestSupport {

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/onCompletionMultiple-context.xml");
    }

    @Test
    public void testOnCompletionDefinedAtRouteLevel() throws InterruptedException {
        // this block is defined first, the definition of the failure only will be considered
        MockEndpoint mockCompleted = getMockEndpoint("mock:completed");
        mockCompleted.setExpectedMessageCount(0);

        MockEndpoint mockFailed = getMockEndpoint("mock:failed");
        mockFailed.setExpectedMessageCount(0);

        // neither of the global completion blocks should be triggered
        MockEndpoint mockGlobalCompleted = getMockEndpoint("mock:globalCompleted");
        mockGlobalCompleted.setExpectedMessageCount(0);

        MockEndpoint mockGlobalFailed = getMockEndpoint("mock:globalFailed");
        mockGlobalFailed.setExpectedMessageCount(0);

        template.asyncSendBody("direct:in", "this message should be fine");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testOnCompletionFailureAtRouteLevel() throws InterruptedException {
        MockEndpoint mockFailed = getMockEndpoint("mock:failed");
        mockFailed.setExpectedMessageCount(1);
        mockFailed.message(0).body().isEqualTo("this message should explode");

        MockEndpoint mockCompleted = getMockEndpoint("mock:completed");
        mockCompleted.setExpectedMessageCount(0);

        // neither of the global completion blocks should be triggered
        MockEndpoint mockGlobalCompleted = getMockEndpoint("mock:globalCompleted");
        mockGlobalCompleted.setExpectedMessageCount(0);

        MockEndpoint mockGlobalFailed = getMockEndpoint("mock:globalFailed");
        mockGlobalFailed.setExpectedMessageCount(0);

        template.asyncSendBody("direct:in", "this message should explode");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testOnCompletionDefinedAtGlobalLevel() throws InterruptedException {
        MockEndpoint mockGlobalCompleted = getMockEndpoint("mock:globalCompleted");
        mockGlobalCompleted.setExpectedMessageCount(1);
        mockGlobalCompleted.message(0).body().isEqualTo("this message should be fine");

        MockEndpoint mockGlobalFailed = getMockEndpoint("mock:globalFailed");
        mockGlobalFailed.setExpectedMessageCount(0);

        template.asyncSendBody("direct:global", "this message should be fine");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testOnCompletionFailureAtGlobalLevel() throws InterruptedException {
        // this block is defined last, the definition of the completion only will be considered
        MockEndpoint mockGlobalFailed = getMockEndpoint("mock:globalFailed");
        mockGlobalFailed.setExpectedMessageCount(0);

        MockEndpoint mockGlobalCompleted = getMockEndpoint("mock:globalCompleted");
        mockGlobalCompleted.setExpectedMessageCount(0);

        template.asyncSendBody("direct:in", "this message should explode");

        assertMockEndpointsSatisfied();
    }

}
