package com.manerfan.snowflake

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.framework.state.ConnectionStateListener
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.CreateMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * 使用zookeeper配置SnowFlake集群的Machine ID
 *
 * 设置 zookeeper.enable = true
 */
@ConditionalOnProperty("zookeeper.enable")
@Configuration
class ZKConfiguration {
    private val logger = LoggerFactory.getLogger(ZKConfiguration::class.java)

    @Value("\${zookeeper.url}")
    private lateinit var url: String

    @Value("\${machineId.datacenterId:16}")
    private var dataCenterId: Long = 16

    @Bean
    @Primary
    fun idWorker(): IdWorker {
        logger.info("Zookeeper Detected! Create IdWorker using ZKConfiguration!")
        val client = CuratorFrameworkFactory.builder()
                .connectString(url)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(5000)
                .retryPolicy(ExponentialBackoffRetry(1000, 3))
                .build()

        client.start()

        val parent = "/snowflake/$dataCenterId"
        val worker = "$parent/worker"
        client.checkExists().forPath("/snowflake/$dataCenterId")
                ?: client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(parent)

        // 利用临时节点序列设置workerId
        val name = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(worker)
        val workerId = name.substring(worker.length).toLong()
        var idWorker = IdWorker(workerId, dataCenterId)

        // 重连监听
        client.connectionStateListenable.addListener(ConnectionStateListener { _client: CuratorFramework, state: ConnectionState ->
            when (state) {
                ConnectionState.RECONNECTED -> {
                    val name = _client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(worker)
                    val workerId = name.substring(worker.length).toLong()
                    idWorker.workerId = workerId
                    logger.info("ZK ReConnected. workerId changed: $workerId")
                }
                ConnectionState.LOST, ConnectionState.SUSPENDED -> {
                    logger.warn("ZK is Abnormal. State is $state")
                }
                else -> {
                    logger.info("ZK State Changed: $state")
                }
            }
        })

        return idWorker
    }
}

/**
 * 单机配置SnowFlake的Machine ID
 *
 * 设置 zookeeper.enable = false
 */
@ConditionalOnProperty("zookeeper.enable", matchIfMissing = true, havingValue = "false")
@Configuration
class SingletonConfiguration {
    private val logger = LoggerFactory.getLogger(SingletonConfiguration::class.java)

    @Value("\${machineId.dataCenterId:16}")
    private var dataCenterId: Long = 16

    @Value("\${machineId.workerId:0}")
    private var workerId: Long = 0

    @Bean
    fun idWorker(): IdWorker {
        logger.info("Singleton Detected! Create IdWorker using SingletonConfiguration!")
        return IdWorker(workerId, dataCenterId)
    }
}