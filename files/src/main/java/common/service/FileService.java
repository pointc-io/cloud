package common.service;

import common.handler.FileGetHandler;
import common.handler.FileStatusHandler;
import entity.FileEntity;
import io.vertx.core.buffer.Buffer;

/**
 * Abstraction for all cloud file services
 *
 * @author Brad Behnke
 */
public abstract class FileService {
    public abstract void getAsync(FileEntity fileEntity, FileGetHandler handler);
    public abstract void putAsync(FileEntity fileEntity, Buffer data, FileStatusHandler handler);
    public abstract void deleteAsync(FileEntity fileEntity, FileStatusHandler handler);
}
