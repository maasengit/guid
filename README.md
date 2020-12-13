基于雪花算法的Java版本的全局ID算法
===
guid是用Java语言实现的全局唯一ID算法，基于Twitter的雪花算法snowflake。
尤其适用于分布式/集群环境中生成全局唯一的ID。

# 1. 特点
guid算法具有以下特点：
## 高性能：单节点，100万/秒
## 递增：各节点按时间趋势递增
## 长度短：64位长度的long类型整数，十进制表示时最长19位
## 高可用：各节点独立生成ID
## 可配置：各组成部分的长度可以根据需要在环境变量里配置
## 可解析：ID可以被解析出各个组成部分
## 轻量部署：分布式或集群环境里只需要为每台机器/JVM配置两个环境变量（数据中心ID和机器ID），不依赖数据库

# 2. ID结构
guid生成的ID包括6个部分：
0 - 41位时间戳 - 3位数据中心标识 - 7位机器标识 - 2位保留位 - 10位序列号
1. 1位标识，0表示正数。
2. 41位时间戳，当前时间的毫秒减去开始时间戳（2020-01-01 00:00:00）。可用 (2 ^ 41) / (1000L * 60 * 60 * 24 * 365) = 69年。
3. 3位数据中心标识，可支持(2 ^ 3) = 8个数据中心。
4. 7位机器标识，每个数据中心可支持(2 ^ 7) = 128个机器标识。
5. 2位时钟回拨次数，可支持(2 ^ 2 - 1) = 3次时钟回拨。
6. 10位序列号，每个节点每一毫秒支持(2 ^ 10) = 1024个序列号。

# 3. 参数配置
环境变量中可以配置以下参数，其中UUID_WORKER_ID必须配置，而且必须唯一：

|参数名|参数Key|必需|缺省值|
|----|----|----|----|
|机器ID|UUID_WORKER_ID|是|无|
|数据中心ID|UUID_DATACENTER_ID|否|0|
|时间戳起点|UUID_START_EPOCH|否|2020-01-01 00:00:00|
|时钟回拨阈值|UUID_CLOCK_BACK_THRESHOLD|否|50L|
|时间戳长度|UUID_TIMESTAMP_LEN|否|41L|
|数据中心长度|UUID_DATACENTER_ID_LEN|否|3L|
|机器ID长度|UUID_WORKER_ID_LEN|否|7L|
|时钟回拨次数长度|UUID_REMAIN_LEN|否|2L|
|序列号长度|UUID_SEQUENCE_LEN|否|10L|

各部分的长度可以定制：时间戳长度、数据中心长度、机器长度、时钟回拨次数长度、序列号长度，总长是63

# 4. 用法

* maven依赖

下载代码后本地编译打包上传到本地仓库
```
<dependency>
    <groupId>com.abc</groupId>
    <artifactId>guid</artifactId>
    <version>1.0</version>
</dependency>
```

* 配置机器ID

1. 使用时需要为当前机器/JVM设置环境变量：GUID_WORKER_ID

2. 集群环境下启动时需要执行以下两个脚本文件：

src/main/resources/start_guid.sh：根据IP地址设置集群里所有机器的ID
```
export GUID_DATACENTER_ID=1
export IP_192_168_1_100_GUID_WORKER_ID=1
export IP_192_168_1_101_GUID_WORKER_ID=2
```

src/main/resources/start.sh：集群机器启动脚本，根据本机IP地址从start_guid.sh获得机器ID设置为环境变量
```
BASEDIR=$(cd "$(dirname "$0")"; pwd)

if [ -f "${BASEDIR}/start_guid.sh" ];then
. ${BASEDIR}/start_guid.sh
IP_ADDRESS=`hostname -i`
GUID_WORKER_ID_NAME=IP_${IP_ADDRESS//\./\_}_GUID_WORKER_ID
export GUID_WORKER_ID=$(eval echo '$'"$GUID_WORKER_ID_NAME")
fi
```

* 获得ID
```
long id = GIDGenerator.nextId();
```
结果：113274030270189578
* 解析ID
```
String str = GUIDGenerator.id2Str(113274030270189578L);
```
结果：20201108T21:50:33.346-0-1-0-10
