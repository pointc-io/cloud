package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.handler.SESPrepQueueHandler;

/**
 * Initializes/Stops queue and handler for email send requests
 *
 * @author Brad Behnke
 */
public class SESSendPrepService extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(SESSendPrepService.class);
    private QueueService<String> queueService;

    public SESSendPrepService(Database db, AttachmentService attachmentService, SESSendService sesSendService) {
        final QueueServiceConfig<String> config = new QueueServiceConfig<>("SESPrepQueue", String.class, true, SESConfig.getPrepParallelism(), SESConfig.getPrepBatchSize());
        config.setHandler(new SESPrepQueueHandler(db, attachmentService, sesSendService));

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(config);
    }

    @Override
    protected void startUp() throws Exception {
        this.queueService.startAsync();
        LOG.info("SES Prep Send Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.queueService.stopAsync();
        LOG.info("SES Prep Send Service Shutdown");
    }

    public void enqueue(String emailId) {
        this.queueService.add(emailId);
    }
}
