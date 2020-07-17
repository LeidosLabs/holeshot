package com.leidoslabs.holeshot.tileserver.service;

import com.leidoslabs.holeshot.tileserver.cache.ByteBufferCodec;
import com.leidoslabs.holeshot.tileserver.cache.RedisCache;
import com.leidoslabs.holeshot.tileserver.service.pool.TransferBufferPool;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Abstract class intended to be extended by actual implementations of TileStore. Contains
 * common constants and functions for handling the Redis cache
 */
public abstract class AbstractTileStoreBase implements ITileStore{

    private static final XLogger logger = XLoggerFactory.getXLogger(AbstractTileStoreBase.class);

    protected final static int TRANSFER_POOL_BUFFER_SIZE_IN_BYTES = 1024 * 8;
    protected final static int MAX_CACHED_TILE_SIZE_IN_BYTES = 1000000;

    protected TransferBufferPool cacheTransferBufferPool;

    /**
     * Write to outputstream from cache using a transfer buffer,
     * @param wholeKey
     * @param out
     * @return
     * @throws Exception
     */
    protected boolean writeFromCache(String wholeKey, OutputStream out) throws Exception {
        logger.entry();
        boolean success = false;
        ByteBuffer buffer = cacheGet(wholeKey);
        if (buffer != null) {
            if (buffer.hasArray()) {
                out.write(buffer.array(), buffer.position(), buffer.remaining());
            } else {
                byte[] transferBuffer = null;
                try {
                    transferBuffer = cacheTransferBufferPool.borrowObject();
                    int imageSize = buffer.remaining();
                    buffer.get(transferBuffer, 0, imageSize);
                    out.write(transferBuffer, 0, imageSize);
                } finally {
                    cacheTransferBufferPool.returnObject(transferBuffer);
                }
            }
            success = true;
        }
        logger.exit();
        return success;
    }


    /**
     * Add file to RedisCache under key
     * @param wholeKey
     * @param file
     */
    protected void cacheAdd(String wholeKey, ByteBuffer file) {
        logger.entry();
        if (RedisCache.getInstance().isAvailable()) {
            RedisClient client = RedisCache.getInstance().getRedis();
            try (StatefulRedisConnection<String, ByteBuffer> connection = client.connect(new ByteBufferCodec())) {
                RedisCommands<String, ByteBuffer> commands = connection.sync();
                commands.set(wholeKey,  file);
            }
        }
        logger.exit();
    }

    /**
     * Retrieve object from RedisCache
     * @param wholeKey
     * @return
     */
    protected ByteBuffer cacheGet(String wholeKey) {
        logger.entry();
        ByteBuffer value = null;
        if (RedisCache.getInstance().isAvailable()) {
            RedisClient client = RedisCache.getInstance().getRedis();
            try (StatefulRedisConnection<String, ByteBuffer> connection = client.connect(new ByteBufferCodec())) {
                RedisCommands<String, ByteBuffer> commands = connection.sync();
                value = commands.get(wholeKey);
            }
        }
        logger.exit();
        return value;
    }

}
