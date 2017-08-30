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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static move.google.cloud.pubsub.Message.isEncoded;
import static move.google.cloud.pubsub.Subscription.canonicalSubscription;
import static move.google.cloud.pubsub.Subscription.validateCanonicalSubscription;
import static move.google.cloud.pubsub.Topic.canonicalTopic;
import static move.google.cloud.pubsub.Topic.validateCanonicalTopic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An async low-level Google Cloud Pub/Sub client.
 */
public class Pubsub implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(Pubsub.class);

  private static final String VERSION = "1.0.0";
  private static final String USER_AGENT =
      "Move-Google-Pubsub-Java-Client/" + VERSION + " (gzip)";

  private static final Object NO_PAYLOAD = new Object();

  private static final String CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform";
  private static final String PUBSUB = "https://www.googleapis.com/auth/pubsub";
  private static final List<String> SCOPES = ImmutableList.of(CLOUD_PLATFORM, PUBSUB);
  private static final String APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8";

  private static final int DEFAULT_PULL_MAX_MESSAGES = 1000;
  private static final boolean DEFAULT_PULL_RETURN_IMMEDIATELY = true;

  private final HttpClient client;
  private final String baseUri;
  private final Credential credential;
  private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

  //  private final ExecutorService executor = getExitingExecutorService(
//      (ThreadPoolExecutor) Executors.newCachedThreadPool());
  private final int compressionLevel;
  private final Vertx vertx;
  private final boolean internalVertx;
  private final long timerID;

  private volatile String accessToken;
//  private NetHttpTransport transport;

  private Pubsub(final Builder builder) {
//    final AsyncHttpClientConfig config = builder.clientConfig.build();
//
//    log.debug("creating new pubsub client with config:");
//    log.debug("uri: {}", builder.uri);
//    log.debug("connect timeout: {}", config.getConnectTimeout());
//    log.debug("read timeout: {}", config.getReadTimeout());
//    log.debug("request timeout: {}", config.getRequestTimeout());
//    log.debug("max connections: {}", config.getMaxConnections());
//    log.debug("max connections per host: {}", config.getMaxConnectionsPerHost());
//    log.debug("enabled cipher suites: {}", Arrays.toString(config.getEnabledCipherSuites()));
//    log.debug("response compression enforced: {}", config.isCompressionEnforced());
//    log.debug("request compression level: {}", builder.compressionLevel);
//    log.debug("accept any certificate: {}", config.isAcceptAnyCertificate());
//    log.debug("follows redirect: {}", config.isFollowRedirect());
//    log.debug("pooled connection TTL: {}", config.getConnectionTTL());
//    log.debug("pooled connection idle timeout: {}", config.getPooledConnectionIdleTimeout());
//    log.debug("pooling connections: {}", config.isAllowPoolingConnections());
//    log.debug("pooling SSL connections: {}", config.isAllowPoolingSslConnections());
//    log.debug("user agent: {}", config.getUserAgent());
//    log.debug("max request retry: {}", config.getMaxRequestRetry());

//    final SSLSocketFactory sslSocketFactory =
//        new ConfigurableSSLSocketFactory(config.getEnabledCipherSuites(),
//                                         (SSLSocketFactory) SSLSocketFactory.getDefault());

//    this.transport = new NetHttpTransport.Builder()
//        .setSslSocketFactory(sslSocketFactory)
//        .build();

    if (builder.vertx == null) {
      vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(0));
      internalVertx = true;
    } else {
      vertx = builder.vertx;
      internalVertx = false;
    }

    this.client = vertx.createHttpClient(builder.options);
    this.compressionLevel = builder.compressionLevel;

    if (builder.credential == null) {
      this.credential = scoped(defaultCredential());
    } else {
      this.credential = scoped(builder.credential);
    }

    this.baseUri = builder.uri.getPath();

    // Get initial access token
    refreshAccessToken();
    if (accessToken == null) {
      throw new RuntimeException("Failed to get access token");
    }

    // Wake up every 10 seconds to check if access token has expired
    timerID = vertx.setTimer(10 * 1000, event -> refreshAccessToken());
  }

  private static Credential defaultCredential() {
    try {
      return GoogleCredential.getApplicationDefault(
          Utils.getDefaultTransport(), Utils.getDefaultJsonFactory());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Create a new {@link Pubsub} client with default configuration.
   */
  public static Pubsub create() {
    return builder().build();
  }

  /**
   * Create a new {@link Builder} that can be used to build a new {@link Pubsub} client.
   */
  public static Builder builder() {
    return new Builder();
  }

  private Credential scoped(final Credential credential) {
    if (credential instanceof GoogleCredential) {
      return scoped((GoogleCredential) credential);
    }
    return credential;
  }

  private Credential scoped(final GoogleCredential credential) {
    if (credential.createScopedRequired()) {
      return credential.createScoped(SCOPES);
    }
    return credential;
  }

  /**
   * Close this {@link Pubsub} client.
   */
  @Override
  public void close() {
    vertx.cancelTimer(timerID);
    client.close();
    closeFuture.complete(null);

    if (internalVertx) {
      vertx.close();
    }
  }

  /**
   * Get a future that is completed when this {@link Pubsub} client is closed.
   */
  public CompletableFuture<Void> closeFuture() {
    return closeFuture;
  }

  /**
   * Refresh the Google Cloud API access token, if necessary.
   */
  private void refreshAccessToken() {
    final Long expiresIn = credential.getExpiresInSeconds();

    // trigger refresh if token is about to expire
    String accessToken = credential.getAccessToken();
    if (accessToken == null || expiresIn != null && expiresIn <= 60) {
      try {
        credential.refreshToken();
        accessToken = credential.getAccessToken();
      } catch (final IOException e) {
        log.error("Failed to fetch access token", e);
      }
    }
    if (accessToken != null) {
      this.accessToken = accessToken;
    }
  }

  /**
   * List the Pub/Sub topics in a project. This will get the first page of topics. To enumerate all
   * topics you might have to make further calls to {@link #listTopics(String, String)} with the
   * page token in order to get further pages.
   *
   * @param project The Google Cloud project.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<TopicList> listTopics(final String project) {
    final String path = "projects/" + project + "/topics";
    return get("list topics", path, TopicList.class);
  }

  /**
   * Get a page of Pub/Sub topics in a project using a specified page token.
   *
   * @param project The Google Cloud project.
   * @param pageToken A token for the page of topics to get.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<TopicList> listTopics(final String project,
      final String pageToken) {
    final String query = (pageToken == null) ? "" : "?pageToken=" + pageToken;
    final String path = "projects/" + project + "/topics" + query;
    return get("list topics", path, TopicList.class);
  }

  /**
   * Create a Pub/Sub topic.
   *
   * @param project The Google Cloud project.
   * @param topic The name of the topic to create.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Topic> createTopic(final String project,
      final String topic) {
    return createTopic(canonicalTopic(project, topic));
  }

  /**
   * Create a Pub/Sub topic.
   *
   * @param canonicalTopic The canonical (including project) name of the topic to create.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Topic> createTopic(final String canonicalTopic) {
    return createTopic(canonicalTopic, Topic.of(canonicalTopic));
  }

  /**
   * Create a Pub/Sub topic.
   *
   * @param canonicalTopic The canonical (including project) name of the topic to create.
   * @param req The payload of the create request. This seems to be ignore in the current API
   * version.
   * @return A future that is completed when this request is completed.
   */
  private PubsubFuture<Topic> createTopic(final String canonicalTopic,
      final Topic req) {
    validateCanonicalTopic(canonicalTopic);
    return put("create topic", canonicalTopic, NO_PAYLOAD, Topic.class);
  }

  /**
   * Get a Pub/Sub topic.
   *
   * @param project The Google Cloud project.
   * @param topic The name of the topic to get.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Topic> getTopic(final String project, final String topic) {
    return getTopic(canonicalTopic(project, topic));
  }

  /**
   * Get a Pub/Sub topic.
   *
   * @param canonicalTopic The canonical (including project) name of the topic to get.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Topic> getTopic(final String canonicalTopic) {
    validateCanonicalTopic(canonicalTopic);
    return get("get topic", canonicalTopic, Topic.class);
  }

  /**
   * Delete a Pub/Sub topic.
   *
   * @param project The Google Cloud project.
   * @param topic The name of the topic to delete.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Void> deleteTopic(final String project,
      final String topic) {
    return deleteTopic(canonicalTopic(project, topic));
  }

  /**
   * Delete a Pub/Sub topic.
   *
   * @param canonicalTopic The canonical (including project) name of the topic to delete.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Void> deleteTopic(final String canonicalTopic) {
    validateCanonicalTopic(canonicalTopic);
    return delete("delete topic", canonicalTopic, Void.class);
  }

  /**
   * Create a Pub/Sub subscription.
   *
   * @param project The Google Cloud project.
   * @param subscriptionName The name of the subscription to create.
   * @param topic The name of the topic to subscribe to.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Subscription> createSubscription(final String project,
      final String subscriptionName,
      final String topic) {
    return createSubscription(canonicalSubscription(project, subscriptionName),
        canonicalTopic(project, topic));
  }

  /**
   * List the Pub/Sub subscriptions in a project. This will get the first page of subscriptions. To
   * enumerate all subscriptions you might have to make further calls to {@link #listTopics(String,
   * String)} with the page token in order to get further pages.
   *
   * @param project The Google Cloud project.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<SubscriptionList> listSubscriptions(final String project) {
    final String path = "projects/" + project + "/subscriptions";
    return get("list subscriptions", path, SubscriptionList.class);
  }

  /**
   * Get a page of Pub/Sub subscriptions in a project using a specified page token.
   *
   * @param project The Google Cloud project.
   * @param pageToken A token for the page of subscriptions to get.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<SubscriptionList> listSubscriptions(final String project,
      final String pageToken) {
    final String query = (pageToken == null) ? "" : "?pageToken=" + pageToken;
    final String path = "projects/" + project + "/subscriptions" + query;
    return get("list subscriptions", path, SubscriptionList.class);
  }

  /**
   * Create a Pub/Sub subscription.
   *
   * @param canonicalSubscriptionName The canonical (including project) name of the scubscription to
   * create.
   * @param canonicalTopic The canonical (including project) name of the topic to subscribe to.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Subscription> createSubscription(final String canonicalSubscriptionName,
      final String canonicalTopic) {
    return createSubscription(Subscription.of(canonicalSubscriptionName, canonicalTopic));
  }

  /**
   * Create a Pub/Sub subscription.
   *
   * @param subscription The subscription to create.
   * @return A future that is completed when this request is completed.
   */
  private PubsubFuture<Subscription> createSubscription(final Subscription subscription) {
    return createSubscription(subscription.name(), subscription);
  }

  /**
   * Create a Pub/Sub subscription.
   *
   * @param canonicalSubscriptionName The canonical (including project) name of the subscription to
   * create.
   * @param subscription The subscription to create.
   * @return A future that is completed when this request is completed.
   */
  private PubsubFuture<Subscription> createSubscription(final String canonicalSubscriptionName,
      final Subscription subscription) {
    validateCanonicalSubscription(canonicalSubscriptionName);
    return put("create subscription", canonicalSubscriptionName, subscription.forCreate(),
        Subscription.class);
  }

  /**
   * Get a Pub/Sub subscription.
   *
   * @param project The Google Cloud project.
   * @param subscription The name of the subscription to get.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Subscription> getSubscription(final String project,
      final String subscription) {
    return getSubscription(canonicalSubscription(project, subscription));
  }

  /**
   * Get a Pub/Sub subscription.
   *
   * @param canonicalSubscriptionName The canonical (including project) name of the subscription to
   * get.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Subscription> getSubscription(final String canonicalSubscriptionName) {
    validateCanonicalSubscription(canonicalSubscriptionName);
    return get("get subscription", canonicalSubscriptionName, Subscription.class);
  }

  /**
   * Delete a Pub/Sub subscription.
   *
   * @param project The Google Cloud project.
   * @param subscription The name of the subscription to delete.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Void> deleteSubscription(final String project,
      final String subscription) {
    return deleteSubscription(canonicalSubscription(project, subscription));
  }

  /**
   * Delete a Pub/Sub subscription.
   *
   * @param canonicalSubscriptionName The canonical (including project) name of the subscription to
   * delete.
   * @return A future that is completed when this request is completed. The future will be completed
   * with {@code null} if the response is 404.
   */
  public PubsubFuture<Void> deleteSubscription(final String canonicalSubscriptionName) {
    validateCanonicalSubscription(canonicalSubscriptionName);
    return delete("delete subscription", canonicalSubscriptionName, Void.class);
  }

  /**
   * Publish a batch of messages.
   *
   * @param project The Google Cloud project.
   * @param topic The topic to publish on.
   * @param messages The batch of messages.
   * @return a future that is completed with a list of message ID's for the published messages.
   */
  public PubsubFuture<List<String>> publish(final String project, final String topic,
      final Message... messages) {
    return publish(project, topic, asList(messages));
  }

  /**
   * Publish a batch of messages.
   *
   * @param project The Google Cloud project.
   * @param topic The topic to publish on.
   * @param messages The batch of messages.
   * @return a future that is completed with a list of message ID's for the published messages.
   */
  public PubsubFuture<List<String>> publish(final String project, final String topic,
      final List<Message> messages) {
    return publish0(messages, Topic.canonicalTopic(project, topic));
  }

  /**
   * Publish a batch of messages.
   *
   * @param messages The batch of messages.
   * @param canonicalTopic The canonical topic to publish on.
   * @return a future that is completed with a list of message ID's for the published messages.
   */
  public PubsubFuture<List<String>> publish(final List<Message> messages,
      final String canonicalTopic) {
    Topic.validateCanonicalTopic(canonicalTopic);
    return publish0(messages, canonicalTopic);
  }

  /**
   * Publish a batch of messages.
   *
   * @param messages The batch of messages.
   * @param canonicalTopic The canonical topic to publish on.
   * @return a future that is completed with a list of message ID's for the published messages.
   */
  private PubsubFuture<List<String>> publish0(final List<Message> messages,
      final String canonicalTopic) {
    final String path = canonicalTopic + ":publish";
    for (final Message message : messages) {
      if (!isEncoded(message)) {
        throw new IllegalArgumentException("Message data must be Base64 encoded: " + message);
      }
    }
    return post("publish", path, PublishRequest.of(messages), PublishResponse.class)
        .thenApply(PublishResponse::messageIds);
  }

  /**
   * Pull a batch of messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription to pull from.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(final String project, final String subscription) {
    return pull(project, subscription, DEFAULT_PULL_RETURN_IMMEDIATELY, DEFAULT_PULL_MAX_MESSAGES);
  }

  /**
   * Pull a batch of messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription to pull from.
   * @param returnImmediately {@code true} to return immediately if the queue is empty. {@code
   * false} to wait for at least one message before returning.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(final String project, final String subscription,
      final boolean returnImmediately) {
    return pull(project, subscription, returnImmediately, DEFAULT_PULL_MAX_MESSAGES);
  }

  /**
   * Pull a batch of messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription to pull from.
   * @param returnImmediately {@code true} to return immediately if the queue is empty. {@code
   * false} to wait for at least one message before returning.
   * @param maxMessages Maximum number of messages to return in batch.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(
      final String project,
      final String subscription,
      final boolean returnImmediately,
      final int maxMessages) {
    return pull(
        Subscription.canonicalSubscription(project, subscription),
        returnImmediately,
        maxMessages
    );
  }

  /**
   * Pull a batch of messages.
   *
   * @param canonicalSubscriptionName The canonical (including project name) subscription to pull
   * from.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(final String canonicalSubscriptionName) {
    return pull(canonicalSubscriptionName, DEFAULT_PULL_RETURN_IMMEDIATELY);
  }

  /**
   * Pull a batch of messages.
   *
   * @param canonicalSubscriptionName The canonical (including project name) subscription to pull
   * from.
   * @param returnImmediately {@code true} to return immediately if the queue is empty. {@code
   * false} to wait for at least one message before returning.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(final String canonicalSubscriptionName,
      final boolean returnImmediately) {
    return pull(canonicalSubscriptionName, returnImmediately, DEFAULT_PULL_MAX_MESSAGES);
  }

  /**
   * Pull a batch of messages.
   *
   * @param canonicalSubscriptionName The canonical (including project name) subscription to pull
   * from.
   * @param returnImmediately {@code true} to return immediately if the queue is empty. {@code
   * false} to wait for at least one message before returning.
   * @param maxMessages Maximum number of messages to return in batch.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(final String canonicalSubscriptionName,
      final boolean returnImmediately, final int maxMessages) {
    final String path = canonicalSubscriptionName + ":pull";
    final PullRequest req = PullRequest.builder()
        .returnImmediately(returnImmediately)
        .maxMessages(maxMessages)
        .build();
    return pull(path, req);
  }

  /**
   * Pull a batch of messages.
   *
   * @param pullRequest The pull request.
   * @return a future that is completed with a list of received messages.
   */
  public PubsubFuture<List<ReceivedMessage>> pull(final String path,
      final PullRequest pullRequest) {
    // TODO (dano): use async client when chunked encoding is fixed
    return post("pull", path, pullRequest, PullResponse.class)
        .thenApply(PullResponse::receivedMessages);
//    return requestJavaNet("pull", POST, path, PullResponse.class, pullRequest)
//        .thenApply(PullResponse::receivedMessages);
  }

  /**
   * Acknowledge a batch of received messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription to acknowledge messages on.
   * @param ackIds List of message ID's to acknowledge.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Void> acknowledge(final String project, final String subscription,
      final String... ackIds) {
    return acknowledge(project, subscription, asList(ackIds));
  }

  /**
   * Acknowledge a batch of received messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription to acknowledge messages on.
   * @param ackIds List of message ID's to acknowledge.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Void> acknowledge(final String project, final String subscription,
      final List<String> ackIds) {
    return acknowledge(Subscription.canonicalSubscription(project, subscription), ackIds);
  }

  /**
   * Acknowledge a batch of received messages.
   *
   * @param canonicalSubscriptionName The canonical (including project name) subscription to
   * acknowledge messages on.
   * @param ackIds List of message ID's to acknowledge.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Void> acknowledge(final String canonicalSubscriptionName,
      final List<String> ackIds) {
    final String path = canonicalSubscriptionName + ":acknowledge";
    final AcknowledgeRequest req = AcknowledgeRequest.builder()
        .ackIds(ackIds)
        .build();
    return post("acknowledge", path, req, Void.class);
  }

  /**
   * Modify the ack deadline for a list of received messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription of the received message to modify the ack deadline on.
   * @param ackDeadlineSeconds The new ack deadline.
   * @param ackIds List of message ID's to modify the ack deadline on.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Void> modifyAckDeadline(final String project, final String subscription,
      final int ackDeadlineSeconds, final String... ackIds) {
    return modifyAckDeadline(project, subscription, ackDeadlineSeconds, asList(ackIds));
  }

  /**
   * Modify the ack deadline for a list of received messages.
   *
   * @param project The Google Cloud project.
   * @param subscription The subscription of the received message to modify the ack deadline on.
   * @param ackDeadlineSeconds The new ack deadline.
   * @param ackIds List of message ID's to modify the ack deadline on.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Void> modifyAckDeadline(final String project, final String subscription,
      final int ackDeadlineSeconds, final List<String> ackIds) {
    return modifyAckDeadline(Subscription.canonicalSubscription(project, subscription),
        ackDeadlineSeconds, ackIds);
  }

  /**
   * Modify the ack deadline for a list of received messages.
   *
   * @param canonicalSubscriptionName The canonical (including project name) subscription of the
   * received message to modify the ack deadline on.
   * @param ackDeadlineSeconds The new ack deadline.
   * @param ackIds List of message ID's to modify the ack deadline on.
   * @return A future that is completed when this request is completed.
   */
  public PubsubFuture<Void> modifyAckDeadline(final String canonicalSubscriptionName,
      final int ackDeadlineSeconds,
      final List<String> ackIds) {
    final String path = canonicalSubscriptionName + ":modifyAckDeadline";
    final ModifyAckDeadlineRequest req = ModifyAckDeadlineRequest.builder()
        .ackDeadlineSeconds(ackDeadlineSeconds)
        .ackIds(ackIds)
        .build();
    return post("modify ack deadline", path, req, Void.class);
  }

  /**
   * Make a GET request.
   */
  private <T> PubsubFuture<T> get(final String operation, final String path,
      final Class<T> responseClass) {
    return request(operation, HttpMethod.GET, path, responseClass);
  }

  /**
   * Make a POST request.
   */
  private <T> PubsubFuture<T> post(final String operation, final String path, final Object payload,
      final Class<T> responseClass) {
    return request(operation, HttpMethod.POST, path, responseClass, payload);
  }

  /**
   * Make a PUT request.
   */
  private <T> PubsubFuture<T> put(final String operation, final String path, final Object payload,
      final Class<T> responseClass) {
    return request(operation, HttpMethod.PUT, path, responseClass, payload);
  }

  /**
   * Make a DELETE request.
   */
  private <T> PubsubFuture<T> delete(final String operation, final String path,
      final Class<T> responseClass) {
    return request(operation, HttpMethod.DELETE, path, responseClass);
  }

  /**
   * Make an HTTP request.
   */
  private <T> PubsubFuture<T> request(final String operation, final HttpMethod method,
      final String path,
      final Class<T> responseClass) {
    return request(operation, method, path, responseClass, NO_PAYLOAD);
  }

  /**
   * Make an HTTP request.
   */
  private <T> PubsubFuture<T> request(final String operation, final HttpMethod method,
      final String path,
      final Class<T> responseClass, final Object payload) {

    final String uri = baseUri + path;
    final HttpClientRequest builder = client.request(method, uri)
        .putHeader(HttpHeaderNames.AUTHORIZATION, "Bearer " + accessToken)
        .putHeader(HttpHeaderNames.USER_AGENT, "Move");

    final long payloadSize;
    final byte[] json;
    if (payload != NO_PAYLOAD) {
      byte[] _json = Json.write(payload);

      if (_json.length > 384) {
        json = gzipJson(_json);
        builder.putHeader(HttpHeaderNames.CONTENT_ENCODING, "gzip");
      } else {
        json = _json;
      }

      payloadSize = json.length;
      builder.putHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(json.length));
      builder.putHeader(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON_UTF8);
    } else {
      payloadSize = 0;
      json = null;
    }

    final RequestInfo requestInfo = RequestInfo.builder()
        .operation(operation)
        .method(method.toString())
        .uri(uri)
        .payloadSize(payloadSize)
        .build();

    final PubsubFuture<T> future = new PubsubFuture<>(requestInfo);

    builder.exceptionHandler(e -> {
      if (future.isDone()) {
        return;
      }
      future.fail(e);
    });
    builder.handler(response -> {
      response.exceptionHandler(e -> {
        if (future.isDone()) {
          return;
        }
        future.fail(e);
      });

      response.bodyHandler(buffer -> {
        if (future.isDone()) {
          return;
        }

        // Return null for 404'd GET & DELETE requests
        if (response.statusCode() == 404 && method == HttpMethod.GET
            || method == HttpMethod.DELETE) {
          future.succeed(null);
          return;
        }

        // Fail on non-2xx responses
        final int statusCode = response.statusCode();
        if (!(statusCode >= 200 && statusCode < 300)) {
          future.fail(new RequestFailedException(response.statusCode(), response.statusMessage()));
          return;
        }

        try {
          future.succeed(Json.read(buffer.getBytes(), responseClass));
        } catch (IOException e) {
          future.fail(e);
        }
      });
    });

    if (json != null) {
      builder.end(Buffer.buffer(json));
    } else {
      builder.end();
    }

    return future;
  }

  /**
   * Make an HTTP request using {@link java.net.HttpURLConnection}.
   */
//  private <T> PubsubFuture<T> requestJavaNet(final String operation, final HttpMethod method,
//      final String path,
//      final Class<T> responseClass, final Object payload) {
//
//    final HttpRequestFactory requestFactory = transport.createRequestFactory();
//
//    final String uri = baseUri + path;
//
//    final HttpHeaders headers = new HttpHeaders();
//    final HttpRequest request;
//    try {
//      request = requestFactory.buildRequest(method.name(), new GenericUrl(URI.create(uri)), null);
//    } catch (IOException e) {
//      throw Throwables.propagate(e);
//    }
//
//    headers.setAuthorization("Bearer " + accessToken);
//    headers.setUserAgent("Move");
//
//    final long payloadSize;
//    if (payload != NO_PAYLOAD) {
//      final byte[] json = gzipJson(payload);
//      payloadSize = json.length;
//      headers.setContentEncoding("gzip");
//      headers.setContentLength((long) json.length);
//      headers.setContentType(APPLICATION_JSON_UTF8);
//      request.setContent(new ByteArrayContent(APPLICATION_JSON_UTF8, json));
//    } else {
//      payloadSize = 0;
//    }
//
//    request.setHeaders(headers);
//
//    final RequestInfo requestInfo = RequestInfo.builder()
//        .operation(operation)
//        .method(method.toString())
//        .uri(uri)
//        .payloadSize(payloadSize)
//        .build();
//
//    final PubsubFuture<T> future = new PubsubFuture<>(requestInfo);
//
//    executor.execute(() -> {
//      final HttpResponse response;
//      try {
//        response = request.execute();
//      } catch (IOException e) {
//        future.fail(e);
//        return;
//      }
//
//      // Return null for 404'd GET & DELETE requests
//      if (response.getStatusCode() == 404 && method == HttpMethod.GET
//          || method == HttpMethod.DELETE) {
//        future.succeed(null);
//        return;
//      }
//
//      // Fail on non-2xx responses
//      final int statusCode = response.getStatusCode();
//      if (!(statusCode >= 200 && statusCode < 300)) {
//        future.fail(
//            new RequestFailedException(response.getStatusCode(), response.getStatusMessage()));
//        return;
//      }
//
//      if (responseClass == Void.class) {
//        future.succeed(null);
//        return;
//      }
//
//      try {
//        future.succeed(Json.read(response.getContent(), responseClass));
//      } catch (IOException e) {
//        future.fail(e);
//      }
//    });
//
//    return future;
//  }
  private byte[] gzipJson(final Object payload) {
    // TODO (dano): cache and reuse deflater
    try (
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final GZIPOutputStream gzip = new GZIPOutputStream(bytes) {
          {
            this.def.setLevel(compressionLevel);
          }
        }
    ) {
      Json.write(gzip, payload);
      return bytes.toByteArray();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * A {@link Builder} that can be used to build a new {@link Pubsub} client.
   */
  public static class Builder {

    //    private static final URI DEFAULT_URI = URI.create("https://pubsub.googleapis.com/v1/");
    private static final URI DEFAULT_URI = URI.create("http://localhost:8085/v1/");

    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 30000;

//    private final AsyncHttpClientConfig.Builder clientConfig = new AsyncHttpClientConfig.Builder()
//        .setCompressionEnforced(true)
//        .setUseProxySelector(true)
//        .setRequestTimeout(DEFAULT_REQUEST_TIMEOUT_MS)
//        .setReadTimeout(DEFAULT_REQUEST_TIMEOUT_MS);

    private final HttpClientOptions options = new HttpClientOptions()
        .setDefaultHost(DEFAULT_URI.getHost())
        .setSsl(true)
        .setTryUseCompression(true);

    private Vertx vertx;
    private Credential credential;
    private URI uri = DEFAULT_URI;
    private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

    private Builder() {
      uri(DEFAULT_URI);
    }

    /**
     * Creates a new {@code Pubsub}.
     */
    public Pubsub build() {
      return new Pubsub(this);
    }

    public Builder vertx(final Vertx vertx) {
      this.vertx = vertx;
      return this;
    }

    public Builder uri(URI uri) {
      checkNotNull(uri, "uri");
      checkArgument(uri.getRawQuery() == null, "illegal service uri: %s", uri);
      checkArgument(uri.getRawFragment() == null, "illegal service uri: %s", uri);
      this.uri = uri;

      if (uri.getScheme().contains("https")) {
        options.setSsl(true);
      } else {
        options.setSsl(false);
      }

      options.setDefaultHost(uri.getHost());
      if (uri.getPort() > 0) {
        options.setDefaultPort(uri.getPort());
      } else {
        if (options.isSsl()) {
          options.setDefaultPort(443);
        } else {
          options.setDefaultPort(80);
        }
      }
      return this;
    }

    /**
     * Set the maximum time in milliseconds the client will can wait when connecting to a remote
     * host.
     *
     * @param connectTimeout the connect timeout in milliseconds.
     * @return this config builder.
     */
    public Builder connectTimeout(final int connectTimeout) {
      options.setConnectTimeout(connectTimeout);
//      clientConfig.setConnectTimeout(connectTimeout);
      return this;
    }

    /**
     * Set the Gzip compression level to use, 0-9 or -1 for default.
     *
     * @param compressionLevel The compression level to use.
     * @return this config builder.
     * @see Deflater#setLevel(int)
     * @see Deflater#DEFAULT_COMPRESSION
     * @see Deflater#BEST_COMPRESSION
     * @see Deflater#BEST_SPEED
     */
    public Builder compressionLevel(final int compressionLevel) {
      checkArgument(compressionLevel > -1 && compressionLevel <= 9,
          "compressionLevel must be -1 or 0-9.");
      this.compressionLevel = compressionLevel;
      return this;
    }

    /**
     * Set the connection read timeout in milliseconds.
     *
     * @param readTimeout the read timeout in milliseconds.
     * @return this config builder.
     */
    public Builder readTimeout(final int readTimeout) {
//      options.
//      clientConfig.setReadTimeout(readTimeout);
      return this;
    }

    /**
     * Set the request timeout in milliseconds.
     *
     * @param requestTimeout the maximum time in milliseconds.
     * @return this config builder.
     */
    public Builder requestTimeout(final int requestTimeout) {
//      clientConfig.setRequestTimeout(requestTimeout);
      return this;
    }

    /**
     * Set the maximum number of connections client will open. Default is unlimited (-1).
     *
     * @param maxConnections the maximum number of connections.
     * @return this config builder.
     */
    public Builder maxConnections(final int maxConnections) {
//      clientConfig.setMaxConnections(maxConnections);
      if (maxConnections >= 0) {
        options.setMaxPoolSize(maxConnections);
        options.setHttp2MaxPoolSize(maxConnections);
      }
      return this;
    }

    /**
     * Set the maximum number of milliseconds a pooled connection will be reused. -1 for no limit.
     *
     * @param pooledConnectionTTL the maximum time in milliseconds.
     * @return this config builder.
     */
    public Builder pooledConnectionTTL(final int pooledConnectionTTL) {
//      clientConfig.setConnectionTTL(pooledConnectionTTL);
      return this;
    }

    /**
     * Set the maximum number of milliseconds an idle pooled connection will be be kept.
     *
     * @param pooledConnectionIdleTimeout the timeout in milliseconds.
     * @return this config builder.
     */
    public Builder pooledConnectionIdleTimeout(final int pooledConnectionIdleTimeout) {
//      clientConfig.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
      return this;
    }

    /**
     * Set whether to allow connection pooling or not. Default is true.
     *
     * @param allowPoolingConnections the maximum number of connections.
     * @return this config builder.
     */
    public Builder allowPoolingConnections(final boolean allowPoolingConnections) {
//      clientConfig.setAllowPoolingConnections(allowPoolingConnections);
//      clientConfig.setAllowPoolingSslConnections(allowPoolingConnections);
      return this;
    }

    /**
     * Set Google Cloud API credentials to use. Set to null to use application default credentials.
     *
     * @param credential the credentials used to authenticate.
     */
    public Builder credential(final Credential credential) {
      this.credential = credential;
      return this;
    }

    /**
     * Set cipher suites to enable for SSL/TLS.
     *
     * @param enabledCipherSuites The cipher suites to enable.
     */
    public Builder enabledCipherSuites(final String... enabledCipherSuites) {
      options.setSsl(true);
//      clientConfig.setEnabledCipherSuites(enabledCipherSuites);
      return this;
    }

    /**
     * Set cipher suites to enable for SSL/TLS.
     *
     * @param enabledCipherSuites The cipher suites to enable.
     */
    public Builder enabledCipherSuites(final List<String> enabledCipherSuites) {
      options.setSsl(true);
//      clientConfig.setEnabledCipherSuites(enabledCipherSuites.toArray(new String[enabledCipherSuites.size()]));
      return this;
    }
  }
}