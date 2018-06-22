package com.manerfan.snowflake

import org.slf4j.LoggerFactory

class IdWorker(var workerId: Long, val dataCenterId: Long) {
    private val logger = LoggerFactory.getLogger(IdWorker::class.java)

    private val twepoch: Long = 1461956400000L // 起始时间

    private val timestampBits: Int = 41 // 时间戳占用位数
    private val workerIdBits: Int = 5 // workerId占用位数
    private val dataCenterIdBits: Int = 5 // dataCenterId占用位数
    private val sequenceBits: Int = 12 // 序列号占用位数

    private val timestampMask: Long = -1L xor (-1L shl timestampBits) // 时间戳可以使用的最大数值，防溢出
    private val maxWorkerId: Long = -1L xor (-1L shl workerIdBits) // workerId可以使用的最大数值
    private val maxDataCenterId: Long = -1L xor (-1L shl dataCenterIdBits) // dataCenterId可以使用的最大数值
    private val sequenceMask: Long = -1L xor (-1L shl sequenceBits) // 序列号可以使用的最大数值，防溢出

    private var sequence: Long = 0 // 序列号

    private val workerIdShift: Int = sequenceBits // workerId左移位数
    private val dataCenterIdShift: Int = workerIdShift + workerIdBits // dataCenterId左移位数
    private val timestampShift: Int = dataCenterIdShift + dataCenterIdBits // 时间戳左移位数


    private var lastTimestamp: Long = -1L

    init {
        if (workerId > maxWorkerId || workerId < 0) {
            logger.error("workerId should not be greater than $maxWorkerId or less than 0")
            throw IllegalArgumentException("workerId should not be greater than $maxWorkerId or less than 0")
        }

        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            logger.error("dataCenterId should not be greater than $maxDataCenterId or less than 0")
            throw IllegalArgumentException("dataCenterId should not be greater than $maxDataCenterId or less than 0")
        }

        logger.info("Worker Started. Timestamp left shift $twepoch, dataCenterId: $dataCenterId, workerId: $workerId")
    }

    @Synchronized
    fun nextId(): Long {
        var currTimeMillis = System.currentTimeMillis()
        if (currTimeMillis < lastTimestamp) {
            logger.error("Clock is Moved Backwards. Reject requests until $lastTimestamp")
            throw RuntimeException("Clock is Moved backwards. Refuse to generate id for ${lastTimestamp - currTimeMillis} millis")
        }

        // 计算 sequence
        if (lastTimestamp == currTimeMillis) {
            // 同一毫秒内的请求
            sequence = (sequenceBits + 1L) and sequenceMask
            if (sequence <= 0) {
                // 同一毫秒内，已将sequence用完，等待下一毫秒
                currTimeMillis = tilNextMillis(currTimeMillis)
            }
        } else {
            sequence = 0
        }

        lastTimestamp = currTimeMillis

        val long = (((currTimeMillis - twepoch) and timestampMask) shl timestampShift) or
                (dataCenterId shl dataCenterIdShift) or
                (workerId shl workerIdShift) or
                sequence
        println(long)
        println(long.toString(16).padStart(16, '0'))
        return long
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }

    fun nextIdHex() = nextId().toString(16).padStart(16, '0')
    fun nextIdBin() = nextId().toString(2).padStart(64, '0')

    fun parseId(id: Long) = Worker(
            ((id ushr timestampShift) and timestampMask) + twepoch,
            (id ushr workerIdShift) and maxWorkerId,
            (id ushr dataCenterIdShift)  and maxDataCenterId,
            id and sequenceMask
    )

    fun parseId(hex: String) = parseId(hex.toLong(16))

    data class Worker(val timestamp: Long, val workId: Long, val dataCenterId: Long, val sequence: Long)
}