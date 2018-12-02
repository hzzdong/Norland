# Norland

#### 项目介绍
快速开发基于netty&springboot的TCP/UDP协议网关
定义：1、设备发送消息到平台称为上行消息，容纳上行消息的协议称为上行协议
     2、平台收到上行消息对设备回复称为下行消息，容纳下行消息的协议称为下行协议
     3、平台收到其他业务系统要求修改或查询设备参数或信息的请求后对设备下发的设置参数消息
     或查询设备属性的消息称为指令，容纳指令信息的协议称为指令协议
主要有三个功能
一：如同SpringMVC一样注解处理上行消息
二：对自定义协议的快速查找(ProtoCreator.java)
三：对指令的支持(@ICmdProto)


#### 安装教程

1. 利用maven安装到本地库
下载本项目在IntelliJ IDEA 的右上角Maven Projects目录中的
Lifecycle目录点击install按钮即可

2. 在你的项目的pom.xml添加一下配置引入Norland
<dependency>
    <groupId>io.norland</groupId>
    <artifactId>Norland</artifactId>
    <version>0.0.2-SNAPSHOT</version>
</dependency>

#### 使用说明

1. 自定义指令，上行协议继承IReqProto，下行协议继承WritableProto，指令协议继承ICmdProto
   并使用@Proro注解协议指出唯一的协议身份ID，比如@Proto("0200")则声明了一个身份ID为0200的协议
   该注解的值将会在Dispatcher分发上行协议时被用到，即@ReqMapping("0200")将会匹配@Proto("0200")
   的协议。还将会在协议查找时被用到比如ProtoCreator.create("0200")将会返回@Proto("0200")注解的协议

2. 创建一个协议的包装类，该包装类需要继承自AbstractWrapper，假设包装类为Wrapper，
Wrapper需要实现两个方法requestProtocol与getTerminalSerialNo
requestProtocol需返回请求协议的身份ID（即通过上行信息的到@Proto注解的值）
getTerminalSerialNo需返回设备的唯一标识，比如设备SimNo，物联卡号等等

3. 定义Codec以及Handler并把ProtoChannelInitializer添加到spring容器

4. 配置详解
norland:
    dispatcher-enabled: true #是否开启请求分发
    listen-port: 2376 #netty监听的端口
    reader-idle-time: 10 #读操作过期时限
    writer-idle-time: 0 #写操作过期时限
    all-idle-time: 0 #读与写过期时限
    leak-detector-level: PARANOID #netty内存溢出监听，可能值DISABLED SIMPLE ADVANCED PARANOID
    server-type: TCP #服务器类别,可能值TCP UDP
    long-time-executor-enabled: true #是否开启长时处理队列
    core-thread-num: 3 #长时处理核心线程数
    current-thread-num: 5 #长时处理线程数
    max-thread-num: 6 #长时处理最大线程数

5. 如何在包装类中解析子消息?
   答：先获取ProtoCreator实例（该类采用单例模式）通过ProtoCreator.create方法即可获取到@Proto注解的子协议
   例：String protoName = requestProtocol();
      IReqProto proto = (IReqProto) ProtoCreator.getInstance(getClass()).create(protoName);

6. 如何向指定设备发送信息?
   答：ChannelHolder.containsKey(simNo)可判断设备是否在线
      ChannelHolder.send(simNo, message)可向特定设备发送消息
      ChannelHolder类保存设备登陆信息

7. 设备发送上行消息，平台收到消息后要求在相关信息存入数据库之前应答设备怎么办?
   答：开启长时处理队列将消息push到队列尾部
   例: queueService.push(() -> {
                  longTimeHandle();
              });
   更多具体细节请查看QueueService.java

8. 平台如何下发指令？
   答：一般指令是从数据库或者接口等获取的字符串信息，所以先用ProtoCreator找到对应的指令协议
   再通过readFromFormatString方法填充指令属性
   例：ICmdProto cmdProto = (ICmdProto) ProtoFactory.getProto(protoName);
             cmdProto.readFromFormatString(formatString);
             if (isOnline(terminalSerialNo)) {
                 sendCommand(messageId, simNo, cmdProto);
             }
   
9. 更多使用细节请查看JT808网关实现
