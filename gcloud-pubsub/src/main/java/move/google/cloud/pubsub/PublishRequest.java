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

import static java.util.Arrays.asList;

import java.util.List;

interface PublishRequest {

  List<Message> messages();

  static Builder builder() {
    return new Builder();
  }

  static PublishRequest of(List<Message> messages) {
    return builder().messages(messages).build();
  }

  static PublishRequest of(Message... messages) {
    return of(asList(messages));
  }

  class Builder implements PublishRequest {

    public List<Message> messages;

    public PublishRequest build() {
      return this;
    }

    public List<Message> messages() {
      return this.messages;
    }

    public Builder messages(
        final List<Message> messages) {
      this.messages = messages;
      return this;
    }
  }
}
