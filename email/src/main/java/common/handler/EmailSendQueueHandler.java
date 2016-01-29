package common.handler;

import common.data.SendRequest;
import data.schema.Tables;
import entity.EmailEntity;
import entity.EmailRecipientEntity;
import entity.RecipientStatus;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.SqlExecutor;
import io.clickhandler.sql.db.SqlResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Date;

/**
 * @author Brad Behnke
 */
public abstract class EmailSendQueueHandler<T extends SendRequest> implements QueueHandler<T>, Tables {

    private final static Logger LOG = LoggerFactory.getLogger(EmailSendQueueHandler.class);
    private SqlExecutor db;

    public EmailSendQueueHandler(SqlExecutor db) {
        this.db = db;
    }

    protected void failure(SendRequest sendRequest, Throwable throwable) {
        if(sendRequest.getCompletionHandler() != null) {
            sendRequest.getCompletionHandler().handle(Future.failedFuture(throwable));
        }
    }

    protected void success(SendRequest sendRequest, EmailEntity emailEntity) {
        if(sendRequest.getCompletionHandler() != null) {
            sendRequest.getCompletionHandler().handle(Future.succeededFuture(emailEntity));
        }
    }

    protected void updateRecords(EmailEntity emailEntity) {
        final boolean success = emailEntity.getMessageId() != null && !emailEntity.getMessageId().isEmpty();
        updateEmailEntityObservable(emailEntity)
                .doOnError(throwable -> LOG.error(throwable.getMessage()))
                .doOnNext(emailEntity1 -> updateEmailRecipientEntitiesObservable(emailEntity1.getId(), success)
                        .doOnError(throwable -> LOG.error(throwable.getMessage())));
    }

    private Observable<EmailEntity> updateEmailEntityObservable(EmailEntity emailEntity) {
        ObservableFuture<EmailEntity> observableFuture = RxHelper.observableFuture();
        updateEmailEntity(emailEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void updateEmailEntity(EmailEntity emailEntity, Handler<AsyncResult<EmailEntity>> completionHandler) {
        db.writeObservable(session -> new SqlResult<>((session.update(emailEntity) == 1), emailEntity))
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(emailEntitySqlResult -> {
                    if(completionHandler != null) {
                        if(emailEntitySqlResult.isSuccess()) {
                            completionHandler.handle(Future.succeededFuture(emailEntitySqlResult.get()));
                        } else {
                            completionHandler.handle(Future.failedFuture(new Exception("Email Entity Update Failed.")));
                        }
                    }
                });
    }

    private Observable<Integer> updateEmailRecipientEntitiesObservable(String emailId, boolean success) {
        ObservableFuture<Integer> observableFuture = RxHelper.observableFuture();
        updateEmailRecipientEntities(emailId, success, observableFuture.toHandler());
        return observableFuture;
    }

    private void updateEmailRecipientEntities(String emailId, boolean success, Handler<AsyncResult<Integer>> completionHandler) {
        db.readObservable(session ->
                session.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                        .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailId))
                        .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class))
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(emailRecipientEntities -> {
                    for(EmailRecipientEntity recipientEntity:emailRecipientEntities) {
                        if(success) {
                            recipientEntity.setStatus(RecipientStatus.SENT);
                            recipientEntity.setSent(new Date());
                        } else {
                            recipientEntity.setStatus(RecipientStatus.FAILED);
                            recipientEntity.setFailed(new Date());
                        }
                        db.writeObservable(session -> {
                            Integer result = session.update(recipientEntity);
                            return new SqlResult<>(result == 1, result);
                        })
                                .doOnError(throwable -> {
                                    if(completionHandler != null) {
                                        completionHandler.handle(Future.failedFuture(throwable));
                                    }
                                })
                                .doOnNext(integerSqlResult -> {
                                    if(completionHandler != null) {
                                        if (integerSqlResult.isSuccess()) {
                                            completionHandler.handle(Future.succeededFuture(integerSqlResult.get()));
                                        } else {
                                            completionHandler.handle(Future.failedFuture(new Exception("Email Recipient Entity Update Failed.")));
                                        }
                                    }
                                });
                    }
                });
    }

}
