package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.data.SESSendRequest;
import ses.handler.SESSendQueueHandler;

import javax.mail.internet.MimeMessage;

/**
 * SES email sending queue manager.
 *
 * @author Brad Behnke
 */
public class SESSendService extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(SESSendService.class);

    private final QueueService<SESSendRequest> queueService;
    private final SESSendQueueHandler queueHandler;

    public SESSendService(EventBus eventBus, SqlDatabase db) {
        final QueueServiceConfig<SESSendRequest> config = new QueueServiceConfig<>("SESSendQueue", SESSendRequest.class, true, 2, 10);
        this.queueHandler = new SESSendQueueHandler(eventBus, db);
        config.setHandler(this.queueHandler);

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(config);
    }

    @Override
    protected void startUp() throws Exception {
        this.queueService.startAsync();
    }

    @Override
    protected void shutDown() throws Exception {
        this.queueService.stopAsync();
        this.queueHandler.shutdown();
    }

    public void enqueue(SESSendRequest sendRequest) {
        queueService.add(sendRequest);
    }
}