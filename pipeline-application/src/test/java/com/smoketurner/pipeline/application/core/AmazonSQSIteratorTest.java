/**
 * Copyright 2018 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.pipeline.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.codahale.metrics.SharedMetricRegistries;

public class AmazonSQSIteratorTest {

    private static final String QUEUE_URL = "http://sqs/test";
    private final AmazonSQSClient mockSQS = mock(AmazonSQSClient.class);
    private final AmazonSQSIterator iterator = new AmazonSQSIterator(mockSQS,
            QUEUE_URL);

    @Before
    public void setUp() {
        SharedMetricRegistries.clear();
    }

    @Test
    public void testHasNext() throws Exception {
        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    public void testNext() throws Exception {
        final ReceiveMessageResult expected = new ReceiveMessageResult();
        when(mockSQS.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(expected);
        final List<Message> actual = iterator.next();
        assertThat(actual).isEqualTo(expected.getMessages());
    }

    @Test
    public void testDeleteMessage() throws Exception {
        final Message message = new Message();
        message.setReceiptHandle("myReceipt");
        final boolean actual = iterator.deleteMessage(message);
        verify(mockSQS).deleteMessage(QUEUE_URL, "myReceipt");
        assertThat(actual).isTrue();
    }

    @Test
    public void testDeleteMessageException() throws Exception {
        doThrow(new AmazonServiceException("error")).when(mockSQS)
                .deleteMessage(QUEUE_URL, "myReceipt");

        final Message message = new Message();
        message.setReceiptHandle("myReceipt");

        final boolean actual = iterator.deleteMessage(message);
        verify(mockSQS).deleteMessage(QUEUE_URL, "myReceipt");
        assertThat(actual).isFalse();
    }

    @Test
    public void testDeleteMessageNull() throws Exception {
        final boolean actual = iterator.deleteMessage(null);
        assertThat(actual).isFalse();
    }
}
