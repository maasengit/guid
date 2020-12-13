package com.abc.guid;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 基于雪花算法(最长19)<br>
 * 在java中用long类型标识，共64位（每部分用-分开）：<br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 000 - 0000000 - 00 - 0000000000<br>
 * 1位标识，0表示正数。<br>
 * 41位时间戳，当前时间的毫秒减去开始时间戳（2020-01-01 00:00:00）。可用 (2 ^ 41) / (1000L * 60 * 60 * 24 * 365) = 69年。<br>
 * 3位数据中心标识，可支持(2 ^ 3) = 8个数据中心。<br>
 * 7位机器标识，每个数据中心可支持(2 ^ 7) = 128个机器标识。<br>
 * 2位保留位，可支持(2 ^ 2 - 1) = 3次时钟回拨。<br>
 * 10位序列号，每个节点每一毫秒支持(2 ^ 10) = 1024个序列号。<br>
 * <p>
 * 时钟回拨：超过阈值多次则抛异常
 * <p>
 * 每个毫秒的初值为0~9随机数
 * <p>
 * 环境变量可以配置以下参数：
 * 参数名          参数Key                       必需  缺省值
 * 机器ID          GUID_WORKER_ID                Yes   无
 * <p>
 * 数据中心ID      GUID_DATACENTER_ID            No   0
 * 时间戳起点      GUID_START_EPOCH              No    2020-01-01 00:00:00
 * 时钟回拨阈值    GUID_CLOCK_BACK_THRESHOLD     No    50L
 * 时间戳长度      GUID_TIMESTAMP_LEN            No    41L
 * 数据中心长度    GUID_DATACENTER_ID_LEN        No    3L
 * 机器ID长度      GUID_WORKER_ID_LEN            No    7L
 * 保留长度        GUID_REMAIN_LEN               No    2L
 * 序列号长度      GUID_SEQUENCE_LEN             No    10L
 * <p>
 * <p>
 * 各部分的长度可以定制：时间戳长度、数据中心长度、机器长度、保留长度、序列号长度，总长是63
 */
public class GUIDGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GUIDGenerator.class);

    /**
     * 环境变量参数名
     */
    private static final String ENV_KEY_DATACENTER_ID = "GUID_DATACENTER_ID";
    private static final String ENV_KEY_WORKER_ID = "GUID_WORKER_ID";
    private static final String ENV_KEY_START_EPOCH = "GUID_START_EPOCH";
    private static final String ENV_KEY_TIMESTAMP_LEN = "GUID_TIMESTAMP_LEN";
    private static final String ENV_KEY_WORKER_ID_LEN = "GUID_WORKER_ID_LEN";
    private static final String ENV_KEY_DATACENTER_ID_LEN = "GUID_DATACENTER_ID_LEN";
    private static final String ENV_KEY_REMAIN_LEN = "GUID_REMAIN_LEN";
    private static final String ENV_KEY_SEQUENCE_LEN = "GUID_SEQUENCE_LEN";
    private static final String ENV_KEY_CLOCK_BACK_THRESHOLD = "GUID_CLOCK_BACK_THRESHOLD";


    // 开始时间戳：2020-01-01 00:00:00.000
    private static final long DEFAULT_START_EPOCH = 1577836800000L;
    private static final long DEFAULT_TIMESTAMP_LEN = 41L;
    private static final long DEFAULT_DATACENTER_ID_LEN = 3L;
    private static final long DEFAULT_WORKER_ID_LEN = 7L;
    private static final long DEFAULT_REMAIN_LEN = 2L;
    private static final long DEFAULT_SEQUENCE_LEN = 10L;
    // 默认回拨最大支持50毫秒
    private static final long DEFAULT_CLOCK_BACK_THRESHOLD = 50L;

    private static final String DEFAULT_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * 时间戳位数
     */
    private static long timestampBits = DEFAULT_TIMESTAMP_LEN;

    /**
     * 数据中心标识
     */
    private static long dataCenterId;

    /**
     * 机器标识
     */
    private static long workerId = -1L;

    /**
     * 保留标识
     */
    private static long remainId = 0L;

    /**
     * 序列号 , 默认从1开始
     */
    private static long sequence = 1L;

    /**
     * 数据中心标识
     */
    private static long dataCenterIdBits = DEFAULT_DATACENTER_ID_LEN;

    /**
     * 机器标识
     */
    private static long workerIdBits = DEFAULT_WORKER_ID_LEN;

    /**
     * 保留
     */
    private static long remainBits = DEFAULT_REMAIN_LEN;

    /**
     * 序列号
     */
    private static long sequenceBits = DEFAULT_SEQUENCE_LEN;

    /**
     * 时钟回拨阈值
     */
    private static long clockBackThreshold = DEFAULT_CLOCK_BACK_THRESHOLD;

    /**
     * 10位序列号支持的最大正整数
     * 2^10-1 = 1023
     */
    private static long sequenceMask;

    /**
     * 机器ID偏移量
     */
    private static long workerIdShift;

    /**
     * 数据中心ID偏移量
     */
    private static long dataCenterIdShift;
    /**
     * 时间戳偏移量
     */
    private static long timestampShift;

    /**
     * 开始时间戳毫秒:2020-01-01 00:00:00.000
     */
    private static long startEpoch = DEFAULT_START_EPOCH;
    /**
     * 最后的时间戳
     */
    private static long lastTimestamp = -1L;

    static {
        String workerIdStr = System.getenv(ENV_KEY_WORKER_ID);
        String dataCenterIdStr = System.getenv(ENV_KEY_DATACENTER_ID);
        String startEpochStr = System.getenv(ENV_KEY_START_EPOCH);
        String timestampLen = System.getenv(ENV_KEY_TIMESTAMP_LEN);

        String workerIdLen = System.getenv(ENV_KEY_WORKER_ID_LEN);
        String dataCenterIdLen = System.getenv(ENV_KEY_DATACENTER_ID_LEN);
        String remainLen = System.getenv(ENV_KEY_REMAIN_LEN);
        String sequenceLen = System.getenv(ENV_KEY_SEQUENCE_LEN);

        String clockBackThresholdStr = System.getenv(ENV_KEY_CLOCK_BACK_THRESHOLD);

        try {
            if (timestampLen != null) {
                timestampBits = Long.parseLong(timestampLen);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format("时间戳位数[%s]必须是整数", timestampLen));
        }
        if (timestampBits < 0) {
            throw new IllegalArgumentException(String.format("时间戳位数[%d]不能小于0", timestampBits));
        }

        try {
            if (workerIdLen != null) {
                workerIdBits = Long.parseLong(workerIdLen);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("机器ID长度[%s]必须是整数。", workerIdLen));
        }
        if (workerIdBits < 0) {
            throw new IllegalArgumentException(String.format("机器ID长度[%d]不能小于0", workerIdBits));
        }

        try {
            if (dataCenterIdLen != null) {
                dataCenterIdBits = Long.parseLong(dataCenterIdLen);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("数据中心ID长度[%s]必须是整数。", dataCenterIdLen));
        }
        if (dataCenterIdBits < 0) {
            throw new IllegalArgumentException(String.format("数据中心ID长度[%d]不能小于0", dataCenterIdBits));
        }

        try {
            if (remainLen != null) {
                remainBits = Long.parseLong(remainLen);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("保留长度[%s]必须是整数。", remainLen));
        }
        if (remainBits < 0) {
            throw new IllegalArgumentException(String.format("保留长度[%d]不能小于0", remainBits));
        }

        try {
            if (sequenceLen != null) {
                sequenceBits = Long.parseLong(sequenceLen);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("序列化长度[%s]必须是整数。", sequenceLen));
        }
        if (sequenceBits < 0) {
            throw new IllegalArgumentException(String.format("序列化长度[%d]不能小于0", sequenceBits));
        }

        long totalBits = timestampBits + dataCenterIdBits + workerIdBits + remainBits + sequenceBits;
        if (totalBits != 63) {
            throw new IllegalArgumentException(String.format("总长度[%d]必须等于63", totalBits));
        }

        // 检查workerId是否正常
        try {
            workerId = Long.parseLong(workerIdStr);
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format("机器Id[%s]必须是整数", workerIdStr));
        }
        checkWorkId();

        // 数据中心最多支持的最大正整数31
        long maxDataCenterId = ~(-1L << dataCenterIdBits);
        try {
            if (dataCenterIdStr == null) {
                dataCenterId = 0;
            } else {
                dataCenterId = Long.parseLong(dataCenterIdStr);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format("数据中心Id[%s]必须是整数", workerIdStr));
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("数据中心Id[%d]不能大于%d或小于0", dataCenterId, maxDataCenterId));
        }

        // 设置开始时间戳
        try {
            if (startEpochStr != null) {
                startEpoch = Long.parseLong(startEpochStr);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("开始时间戳[%s]必须是整数。", startEpochStr));
        }
        if (startEpoch < 0) {
            throw new IllegalArgumentException(String.format("开始时间戳[%d]不能小于0", startEpoch));
        }

        try {
            if (clockBackThresholdStr != null) {
                clockBackThreshold = Long.parseLong(clockBackThresholdStr);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("时钟回拨阈值[%s]必须是整数。", clockBackThresholdStr));
        }
        if (clockBackThreshold < 0) {
            throw new IllegalArgumentException(String.format("时钟回拨阈值[%d]不能小于0", clockBackThreshold));
        }

        sequenceMask = ~(-1L << sequenceBits);
        workerIdShift = remainBits + sequenceBits;
        dataCenterIdShift = remainBits + sequenceBits + workerIdBits;
        timestampShift = remainBits + sequenceBits + workerIdBits + dataCenterIdBits;
    }

    private static void checkWorkId() {
        long maxWorkerId = ~(-1L << workerIdBits);
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("机器Id[%d]不能大于%d或小于0", workerId, maxWorkerId));
        }
    }

    /**
     * 获取下一个GUID，所有参数可以通过环境变量修改默认值
     *
     * @return
     */
    public static synchronized long nextId() {
        return nextId(dataCenterId);
    }

    /**
     * 获取下一个GUID，所有参数除了数据中心ID可以通过环境变量修改默认值
     *
     * @param dcId 数据中心ID或业务线ID
     * @return
     */
    public static synchronized long nextId(long dcId) {
        checkWorkId();

        // 获取当前时间毫秒数
        long timestamp = System.currentTimeMillis();

        // 如果当前时间毫秒数小于上一次的时间戳，一直等待除非回拨大于阈值
        if (timestamp < lastTimestamp) {
            timestamp = tillNextMillis(timestamp);
        } else if (timestamp == lastTimestamp) {
            // 当前时间毫秒数与上次时间戳相同，增加序列号
            sequence = (sequence + 1) & sequenceMask;
            //如果发生序列号为0，即当前毫秒数的序列号已经溢出，则借用下一毫秒的时间戳
            if (sequence == 0) {
                timestamp = tillNextMillis(timestamp);
            }
        } else {
            // 当前毫秒数大于上次的时间戳，序列号为随机数(0~9)
            sequence = new Random().nextInt(10);
            remainId = 0L;
        }

        // 更新
        lastTimestamp = timestamp;

        // 生成ID算法：各参数左移归位，按位或
        // 1、时间戳偏移量左移：3+7+2+10=22
        // 2、dataCenterId左移：7+12=19
        // 3、workerId左移：12
        // 4、remainId左移：10
        // 5、最后的所有结果按位`或`
        return ((timestamp - startEpoch) << timestampShift) |
                (dataCenterId << dataCenterIdShift) |
                (workerId << workerIdShift) |
                (remainId << sequenceBits) |
                sequence;
    }

    /**
     * 获取下一毫秒
     *
     * @param timestamp 当前时间
     * @return the long
     */
    private static long tillNextMillis(long timestamp) {
        // 如果当前的毫秒数小于等于上次，一直重试
        while (timestamp <= lastTimestamp) {
            // 如果时钟回拨太多且多次则抛异常
            long offset = lastTimestamp - timestamp;
            if (offset > clockBackThreshold) {
                if (remainId < ~(-1L << remainBits)) {
                    remainId = remainId + 1;
                    return lastTimestamp;
                } else {
                    remainId = 0L;
                    DateFormat df = new SimpleDateFormat(DEFAULT_TIMESTAMP_PATTERN);
                    String d = df.format(new Date(lastTimestamp + startEpoch));
                    throw new RuntimeException(String.format("时钟发生回拨，拒绝生成ID，直到： %s.", d));
                }
            } else {
                // 获取当前毫秒数
                timestamp = System.currentTimeMillis();
            }
        }
        remainId = 0L;
        return timestamp;
    }

    public static long getSequence(long id) {
        return id & sequenceMask;
    }

    public static long getDataCenterId() {
        return dataCenterId;
    }

    public static void setDataCenterId(long dataCenterId) {
        GUIDGenerator.dataCenterId = dataCenterId;
    }

    public static long getWorkerId() {
        return workerId;
    }

    public static void setWorkerId(long workerId) {
        GUIDGenerator.workerId = workerId;
    }

    public static long getWorkerIdBits() {
        return workerIdBits;
    }

    public static long getDataCenterIdBits() {
        return dataCenterIdBits;
    }

    public static long getSequenceBits() {
        return sequenceBits;
    }

    /**
     * 解析ID的各部分数值并转换为字符串
     *
     * @param id
     * @return 例如：20201025T10:20:19.673-1-1-0-14
     */
    public static String id2Str(long id) {
        long timestamp = (id >> timestampShift) + startEpoch;
        return DateFormatUtils.format(timestamp, "yyyyMMdd'T'HH:mm:ss.SSS") + "-" + dataCenterId + "-"
                + workerId + "-" + ((id >> sequenceBits) & ~(-1L << remainBits)) + "-" + getSequence(id);
    }
}