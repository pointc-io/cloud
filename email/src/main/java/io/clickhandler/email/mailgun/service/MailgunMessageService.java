package io.clickhandler.email.mailgun.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.email.common.data.Message;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.SqlExecutor;
import io.clickhandler.email.mailgun.config.MailgunConfig;
import io.clickhandler.email.mailgun.handler.MailgunMessageQueueHandler;
import io.vertx.rxjava.core.eventbus.EventBus;

/**
 * @author Brad Behnke
 */
public class MailgunMessageService extends AbstractIdleService {
    private QueueService<Message> queueService;

    public MailgunMessageService(MailgunConfig config, EventBus eventBus, SqlExecutor db) {
        QueueFactory factory = new LocalQueueServiceFactory();
        final QueueServiceConfig<Message> mainConfig = new QueueServiceConfig<>("MailgunMessageQueue", Message.class, true, config.getMessageParallelism(), config.getMessageBatchSize());
        mainConfig.setHandler(new MailgunMessageQueueHandler(eventBus, db));
        this.queueService = factory.build(mainConfig);
    }

    @Override
    protected void startUp() throws Exception {
        // start queueing and handling
        this.queueService.startAsync();
    }

    @Override
    protected void shutDown() throws Exception {
        // stop queueing and handling
        this.queueService.stopAsync();
    }

    public void enqueueMessage(Message message) {
        if(message == null) {
            return;
        }
        this.queueService.add(message);
    }
}