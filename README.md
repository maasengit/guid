# guid
A global unique ID generator inspired by Twitter's Snowflake

guid是Java语言实现的全局唯一ID算法，基于Twitter的雪花算法snowflake。

guid算法用来生成64位的ID，可以用long类型来存储和表示，用于分布式/集群环境中生成唯一的ID，并且生成的ID有趋势递增。

64位ID包括5个部分：

0 - 41位时间戳 - 3位数据中心标识 - 7位机器标识 - 2位保留位 - 10位序列号
