/*
 * Copyright (c) 2011-2015 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package move.google.cloud.pubsub;

import move.google.cloud.pubsub.Message;
import move.google.cloud.pubsub.ReceivedMessage;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReceivedMessageTest {

  @Test
  public void testOfEncoded() throws Exception {
    final ReceivedMessage receivedMessage = ReceivedMessage.ofEncoded("a1", "hello world");
    assertThat(receivedMessage.ackId(), is("a1"));
    assertThat(receivedMessage.message().data(), is(Message.encode("hello world")));
  }
}