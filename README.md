# SnowFlakeWithZK

SnowFlake with Zookeeper in Kotlin

使用Zookeeper管理SnowFlake集群的workId

## 安装

下载并解压 [SnowFlakeWithZK-1.0.0.zip](https://github.com/manerfan/SnowFlakeWithZK/releases/download/v1.0.0/SnowFlakeWithZK-1.0.0.zip)

进入解压目录并执行
`./SnowFlakeWithZK.jar start`

## API

GET http(s)://[host]:[port]/api/next/long   以长整型返回

GET http(s)://[host]:[port]/api/next/hex    以十六进制返回

GET http(s)://[host]:[port]/api/next/bin    以二进制返回

GET http(s)://[host]:[port]/api/parse/long/{id}    解析长整型id

GET http(s)://[host]:[port]/api/parse/hex/{id}     解析十六进制id

## 单机使用

修改SnowFlakeWithZK.conf中RUN_ARGS参数，新增`--zookeeper.enable=false`

## 集群使用

### 使用zookeeper

修改SnowFlakeWithZK.conf中RUN_ARGS参数，新增`--zookeeper.enable=true --zookeeper.url=[zookeeper-host]:[zookeeper-port]`

### 不使用zookeeper

修改SnowFlakeWithZK.conf中RUN_ARGS参数，新增`--zookeeper.enable=false --machineId.workId=[You workId]`

> 注：集群中每个SnowFlake实例的workId需要保证各不相同

## RUN_ARGS参数

--server.port               服务端口

--machineId.dataCenterId    数据中心id，0~31，默认16

--machineId.workerId        实例id，0~31，默认5，--zookeeper.enable=false时生效，同一数据中心的不同实例，需要保证各不相同

--zookeeper.enable          是否使用zookeeper管理workerId，默认true

--zookeeper.url             zookeeper连接地址，默认localhost:2181，--zookeeper.enable=true时生效
