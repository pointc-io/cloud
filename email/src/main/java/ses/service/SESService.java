package ses.service;

import com.sun.istack.internal.NotNull;
import common.service.AbstractEmailService;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s3.service.S3Service;
import ses.data.SESSendRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SES Email services manager.
 *
 * @author
 */
@Singleton
public class SESService extends AbstractEmailService<SESSendRequest> {
    private final static Logger LOG = LoggerFactory.getLogger(SESService.class);

    // ses services
    private final SESSendPrepService sesSendPrepService;
    private final SESAttachmentService SESAttachmentService;
    private final SESSendService sesSendService;

    @Inject
    public SESService(@NotNull EventBus eventBus, @NotNull SqlDatabase db, @NotNull S3Service s3Service) {
        this.SESAttachmentService = new SESAttachmentService(db, s3Service);
        this.sesSendService = new SESSendService(eventBus, db);
        this.sesSendPrepService = new SESSendPrepService(eventBus, db, SESAttachmentService, sesSendService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sesSendService.startAsync();
        this.SESAttachmentService.startAsync();
        this.sesSendPrepService.startAsync();
        LOG.info("SES Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sesSendPrepService.stopAsync();
        this.SESAttachmentService.stopAsync();
        this.sesSendService.stopAsync();
        LOG.info("SES Service Shutdown");
    }

    @Override
    public void send(@NotNull SESSendRequest sendRequest) {
        if(sendRequest.getEmailEntity() == null) {
            sendRequest.getSendHandler().onFailure(new Exception("Null email entity."));
            return;
        }
        if(sendRequest.getEmailEntity().getId() == null || sendRequest.getEmailEntity().getId().isEmpty()) {
            sendRequest.getSendHandler().onFailure(new Exception("Null or empty email entity Id."));
            return;
        }
        this.sesSendPrepService.enqueue(sendRequest);
    }
}