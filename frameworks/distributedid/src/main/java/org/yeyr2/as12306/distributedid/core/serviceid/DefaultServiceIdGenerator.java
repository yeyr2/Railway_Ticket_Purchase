package org.yeyr2.as12306.distributedid.core.serviceid;

import org.yeyr2.as12306.distributedid.core.IdGenerator;
import org.yeyr2.as12306.distributedid.toolkit.SnowflakeIdUtil;

// 默认业务 ID 生成器
public final class DefaultServiceIdGenerator implements IdGenerator {
    /**
     * 工作 ID 5 bit
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心 ID 5 bit
     */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /**
     * 序列号 12 位，表示只允许 workerId 的范围为 0-4095
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 真实序列号 bit
     */
    private static final long SEQUENCE_ACTUAL_BITS = 8L;

    /**
     * 基因 bit
     */
    private static final long SEQUENCE_BIZ_BITS = 4L;

    /**
     * 机器节点左移12位
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 默认开始时间
     */
    private static long DEFAULT_TWEPOCH = 1288834974657L;

    /**
     * 数据中心节点左移 17 位
     */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间毫秒数左移 22 位
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    private final IdGenerator idGenerator;

    private long maxBizIdBitsLen;

    public DefaultServiceIdGenerator(){
        this(SEQUENCE_BIZ_BITS);
    }

    public DefaultServiceIdGenerator(long serviceIDBitLen) {
        this.maxBizIdBitsLen = (long) Math.pow(2,serviceIDBitLen);
        idGenerator = SnowflakeIdUtil.getInstance();
    }

}
