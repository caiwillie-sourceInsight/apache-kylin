/*
 *
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *
 *  contributor license agreements. See the NOTICE file distributed with
 *
 *  this work for additional information regarding copyright ownership.
 *
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *
 *  (the "License"); you may not use this file except in compliance with
 *
 *  the License. You may obtain a copy of the License at
 *
 *
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 *  Unless required by applicable law or agreed to in writing, software
 *
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *
 *  limitations under the License.
 *
 * /
 */

package org.apache.kylin.streaming;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

/**
 * Created by qianzhou on 2/16/15.
 */
public class KafkaConsumerTest extends KafkaBaseTest {

    private TestProducer producer;

    private static final int TOTAL_SEND_COUNT = 100;

    @Before
    public void before() throws IOException {
        producer = new TestProducer(TOTAL_SEND_COUNT);
        producer.start();
    }

    @After
    public void after() {
        producer.stop();
    }

    private void waitForProducerToStop(TestProducer producer) {
        while (!producer.isStopped()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test() throws InterruptedException {
        final TopicMeta kafkaTopicMeta = KafkaRequester.getKafkaTopicMeta(kafkaConfig);
        final ExecutorService executorService = Executors.newFixedThreadPool(kafkaTopicMeta.getPartitionIds().size());
        List<BlockingQueue<Stream>> queues = Lists.newArrayList();
        for (Integer partitionId : kafkaTopicMeta.getPartitionIds()) {
            KafkaConsumer consumer = new KafkaConsumer(kafkaTopicMeta.getName(), partitionId, kafkaConfig.getBrokers(), kafkaConfig);
            queues.add(consumer.getStreamQueue());
            executorService.execute(consumer);
        }
        waitForProducerToStop(producer);
        int count = 0;
        for (BlockingQueue<Stream> queue : queues) {
            count += queue.size();
        }

        logger.info("count of messages are " + count);
        //since there will be historical data
        assertTrue(count >= TOTAL_SEND_COUNT);
    }
}
