package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.data.DownloadRequest;
import ses.handler.AttachmentQueueHandler;

/**
 * @author Brad Behnke
 */
public class AttachmentService extends AbstractIdleService {

    private final static Logger LOG = LoggerFactory.getLogger(AttachmentService.class);
    private QueueService<DownloadRequest> queueService;

    public AttachmentService(Database db) {
        final QueueServiceConfig<DownloadRequest> config = new QueueServiceConfig<>("AttachmentDLQueue", DownloadRequest.class, true, 2, 1);
        config.setHandler(new AttachmentQueueHandler(db));

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(config);
    }

    @Override
    protected void startUp() throws Exception {
        this.queueService.startAsync();
        LOG.info("S3 Download Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.queueService.stopAsync();
        LOG.info("S3 Download Service Shutdown");
    }

    public void enqueue(DownloadRequest downloadRequest) {
        this.queueService.add(downloadRequest);
    }
}