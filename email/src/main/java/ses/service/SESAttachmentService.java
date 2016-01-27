package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s3.service.S3Service;
import ses.data.DownloadRequest;
import ses.handler.AttachmentQueueHandler;

/**
 * SES Attachment download queue manager.
 *
 * @author Brad Behnke
 */
public class SESAttachmentService extends AbstractIdleService {

    private final static Logger LOG = LoggerFactory.getLogger(SESAttachmentService.class);
    private final QueueService<DownloadRequest> queueService;

    public SESAttachmentService(SqlDatabase db, S3Service s3Service) {
        final QueueServiceConfig<DownloadRequest> config = new QueueServiceConfig<>("AttachmentDLQueue", DownloadRequest.class, true, 2, 1);
        config.setHandler(new AttachmentQueueHandler(db, s3Service));

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
    }

    public void enqueue(DownloadRequest downloadRequest) {
        this.queueService.add(downloadRequest);
    }
}