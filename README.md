# DataCenterKafkaToDB

项目名称：cBSS数据增量同步接入 项目场景：数据流向，cbss oracle--》cbss kafka --》本程序 --》 省分oracle 项目架构：maven构建的java程序，采用多进程+多线程+消息队列 多进程：kafka读取多进程，kafka通道和进程一一对应，可以启动n个进程。 多线程：单个进程启动多个线程 1个kafka线程， n个oracle线程（现在1）c3p0jdbc引擎，连接池，采用批量提交。 2个监控线程1个10秒的输出处理统计，一个1小时频率输出处理结果 消息队列：kafka读取和oracle执行之间用消息队列弹性缓冲数据，队列采用java线程安全的原生队列。 项目特点：技术简单，全配置 技术简单：简单通用的java编码，队列也采用java原生，部署简单没有其他依赖，代码易维护。 全配置：整个程序都是参数配置化的包括oracle，kafka，消息模板。简单配置20分钟即可上线应用 项目效率：单进程单线程，kafka读取并解析 3万/秒。oracle处理 1000/秒。10个进程oracle单线程吞吐量 crm每天20g 账务 每天80g 项目目标：通用工具，kafka consumer ，oracle 动态sql
