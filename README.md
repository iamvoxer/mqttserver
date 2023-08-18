## 更新：
2023-08-18 添加 SSL 的支持，并修复一个bug，参考链接 https://www.jianshu.com/p/7f123ddb8a85?v=1692352744101

## 一. 功能概述
1. 启动一个MQTT Server实例监控链接上的 MQTT Client
2. 开发者只需要实现对应的接口来处理在线、离线、消息处理等事件。无需考虑内部实现细节。
3. 可以实时获取到所有在线 MQTT Client 的列表和信息
4. 可以利用和特定Client链接的通道来接收和发送数据
5. test目录里是用python模拟2000个mqtt客户端来同时链接

## 二. 使用方式
#### 1. 修改build.gradle
可以通过依赖我们内部的企业的库中心来获取最新的版本。当然你可以下载源码加入到自己的项目里
```
......
repositories {
    maven { url 'https://maven.aliyun.com/repository/public/' }
    mavenCentral()
    maven {
        allowInsecureProtocol=true
        url 'http://118.253.150.123:8081/repository/d1-java/'
    }
}
dependencies {
    implementation('d1.duoxian:mqttserver:2.0.7')
	......
}
......
```

#### 2. 启动 MQTT Server 服务
```
MqttServerService mqttserver = new MqttServerService();
//1)简单启动服务，其中60001是监听的端口，listner是实现 `interface IMqttMessageListener`接口的对象，后面会提到
mqttserver.startup(60001,listener,null);
//2）启动参数还有很多个，大部分都可以用缺省值，完整参数参考MqttServerServiceOption
mqttserver.startup(new MqttServerServiceOption.Builder()   //需要改那个参数就设置那个，大部分有缺省值，不需要设置
                .port(port)
                .messageListener(listener)
                .verifyListener(verifyListener)
                .checkOfflineInterval(checkOfflineInterval)
                .maxBytesInMessage(maxBytesInMessage)
                .build());
//3）以下是参数列表
    /**
     * 缺省的字符集，用于把mqtt发送接收的数据二进制转换成string，缺省是StandardCharsets.UTF_8
     */
    private Charset defaultCharset = StandardCharsets.UTF_8;

    /**
     * 监听的端口
     */
    private Integer port;
    /**
     * 消息监听的接口实现实例
     */
    private IMqttMessageListener messageListener;

    /**
     * 校验客户端身份的接口实现实例，如果不校验用户密码传null即可
     */
    private IMqttVerifyListener verifyListener;
    /**
     * 负责接收accept消息的线程数，通常1个线程(缺省）即可，传0或负数则为1，大于5的数则为5
     */
    private Integer bossThreadNumber = 0;
    /**
     * 负责处理事务的线程数，缺省是cpu的核心*2个数量，传0或负数则为缺省，大于100的数则为100
     */
    private Integer workThreadNumber = 0;
    /**
     * 判断是否离线的时间间隔，缺省是90秒
     */
    private Integer checkOfflineInterval = 90;
    /**
     * 最大一次性接收的报文长度，如果是图片之类的，这个值要设大一点，缺省是102400字节
     */
    private Integer maxBytesInMessage = 102400;
    /**
     * 接收到的消息先存入队列，这个值是缺省的队列大小，如果队列满了，就无法接收新的数据，请确保快速处理队列里的数据
     */
    private Integer maxMessageCount = 100000;

```

#### 3. 实现 IMqttMessageListener 接口
最核心的处理是在 onMessage 方法里处理从客户端发送过来的数据。
```

/**
* 设备离线后触发，请不要在这个函数里添加耗时操作
*/
void offline(String uuid, ClientSession clientSession);

/**
* 接收到客户端返回的消息后触发，请不要在这个函数里添加耗时操作
*
* @param ip        客户端的ip地址
* @param channelId 通道id
* @param topic     接收到的消息TOPIC
* @param message   接收到的消息正文，已转换成字符串
* @param publisher 回调函数，如果需要给客户端发送数据，通过这个函数来处理
*/
void onMessage(String ip, String channelId, String topic, String message, Function<CustomMqttPublishMessage, Boolean> publisher);

/**
* 设备上线后触发，请不要在这个函数里添加耗时操作
*
* @param uuid 链接的设备的uuid
*/
void online(String uuid, ClientSession clientSession);

/**
* 某些特定设备的uuid并不是mqtt的clientId，需要做额外的处理
* 请不要在这个函数里添加耗时操作
*
* @param clientId mqtt client的唯一标识
* @return 根据clientId返回对应的uuid，绝大部分情况是相等
*/
String clientIdToUuid(String clientId);
```
#### 4. 获取当前在线客户端列表和发送数据给客户端
内存里保存当前所有在线客户端对应的对象，ClientSession对象，包含了客户端 uuid、ip、回调函数等重要属性.

```
class ClientSession {
    /**
     * mqtt连接的用户名
     */
    private String username;
    /**
     * mqtt连接的密码
     */
    private byte[] password;
    /**
     * mqtt连接的ip
     */
    private String ip;
    /**
     * 设备唯一标识
     */
    private String uuid;
    /**
     * 设备对应的通信通道id
     */
    private String channelId;

    /**
     * 给设备发送消息的回调函数
     */
    private Function<CustomMqttPublishMessage, Boolean> publisher;

    /**
     * 设备最后刷新时间，也就是最后一次给paas发送数据的时间
     */
    private Calendar refreshTime;
    /**
     * 一些额外的数据，用于扩展
     */
    private Object data;
}
```
通常使用场景有2个：
- 获取所有在线客户端列表
- 根据客户端uuid获取对应对象的回调函数，通过回调函数来发送数据给客户端

```
//获取所有在线客户端列表
Map<String, ClientSession>  clients = mqttserver.getClientSessionManager().getClientsMap();
//根据客户端uuid获取对应的对象
ClientSession client = clients.get("uuid123");
//构建一个要发送给客户端的数据结构
CustomMqttPublishMessage message = new CustomMqttPublishMessage(client.getChannelId(),"mytopic","mycontent");
//发送数据给客户端
client.getPublisher().apply(message);
```

## 三. 项目说明

#### 1. mqttserver 子目录
这个目录是这个库的核心代码，只依赖了netty-all:4.1.86.Final

#### 2. src 子目录
是测试mqttserver库的测试代码 里面依赖了一些其它库，需要用到 jdk 17

#### 3. test 子目录
test目录里是用python模拟2000个mqtt客户端来同时链接这个mqttserver，用于功能测试和压力测试
