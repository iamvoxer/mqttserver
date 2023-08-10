package d1.duoxian.mqttserver;

import io.netty.channel.nio.NioEventLoopGroup;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * server启动有多个参数，所以增加一个option类
 *
 * @author liuyi
 */
public class MqttServerServiceOption {
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
     * 校验客户端身份的接口实现实例
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

    private MqttServerServiceOption(Builder builder) {
        this.defaultCharset = builder.defaultCharset;
        this.port = builder.port;
        this.messageListener = builder.messageListener;
        this.verifyListener = builder.verifyListener;
        this.bossThreadNumber = builder.bossThreadNumber;
        this.workThreadNumber = builder.workThreadNumber;
        this.checkOfflineInterval = builder.checkOfflineInterval;
        this.maxBytesInMessage = builder.maxBytesInMessage;
        this.maxMessageCount = builder.maxMessageCount;
    }

    public static class Builder {
        private Charset defaultCharset;
        private Integer port;
        private IMqttMessageListener messageListener;
        private IMqttVerifyListener verifyListener;
        private Integer bossThreadNumber;
        private Integer workThreadNumber;
        private Integer checkOfflineInterval;
        private Integer maxBytesInMessage;
        private Integer maxMessageCount = 100000;

        public MqttServerServiceOption build() {
            return new MqttServerServiceOption(this);
        }

        public Builder maxMessageCount(Integer maxMessageCount) {
            this.maxMessageCount = maxMessageCount;
            return this;
        }

        public Builder defaultCharset(Charset defaultCharset) {
            this.defaultCharset = defaultCharset;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder messageListener(IMqttMessageListener messageListener) {
            this.messageListener = messageListener;
            return this;
        }

        public Builder verifyListener(IMqttVerifyListener verifyListener) {
            this.verifyListener = verifyListener;
            return this;
        }

        public Builder bossThreadNumber(Integer bossThreadNumber) {
            this.bossThreadNumber = bossThreadNumber;
            return this;
        }

        public Builder workThreadNumber(Integer workThreadNumber) {
            this.workThreadNumber = workThreadNumber;
            return this;
        }

        public Builder checkOfflineInterval(Integer checkOfflineInterval) {
            this.checkOfflineInterval = checkOfflineInterval;
            return this;
        }

        public Builder maxBytesInMessage(Integer maxBytesInMessage) {
            this.maxBytesInMessage = maxBytesInMessage;
            return this;
        }
    }

    public Integer getMaxMessageCount() {
        if (maxMessageCount <= 0) {
            return 100000;
        }
        return maxMessageCount;
    }

    public Charset getDefaultCharset() {
        if (defaultCharset == null) {
            return StandardCharsets.UTF_8;
        }
        return defaultCharset;
    }

    public Integer getPort() {
        return port;
    }

    public IMqttMessageListener getMessageListener() {
        return messageListener;
    }

    public IMqttVerifyListener getVerifyListener() {
        return verifyListener;
    }

    public Integer getBossThreadNumber() {
        if (bossThreadNumber == null) {
            return 1;
        }
        //负责接收accept消息的线程组,1个线程足以
        if (bossThreadNumber <= 0) {
            return 1;
        }
        if (bossThreadNumber > 5) {
            return 5;
        }
        return bossThreadNumber;
    }

    public NioEventLoopGroup getWorkThreadGroup() {
        //负责接收读写消息的线程组,缺省是cpu的核心*2个数量
        if (workThreadNumber == null || workThreadNumber <= 0) {
            return new NioEventLoopGroup();
        } else if (workThreadNumber > 100) {
            //最大100
            return new NioEventLoopGroup(100);
        } else {
            return new NioEventLoopGroup(workThreadNumber);
        }
    }

    public Integer getCheckOfflineInterval() {
        if (checkOfflineInterval == null || checkOfflineInterval <= 0) {
            return 90;
        }
        return checkOfflineInterval;
    }

    public Integer getMaxBytesInMessage() {
        if (maxBytesInMessage == null || maxBytesInMessage <= 0) {
            maxBytesInMessage = 102400;
        }
        return maxBytesInMessage;
    }
}
