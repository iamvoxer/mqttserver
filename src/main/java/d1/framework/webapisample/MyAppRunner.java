package d1.framework.webapisample;

import d1.duoxian.mqttserver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * @author Buter
 * @date 2020/3/20 14:59
 */
@Component
public class MyAppRunner implements ApplicationRunner, IMqttMessageListener {
    private Logger logger = LoggerFactory.getLogger(MyAppRunner.class);


    @Override
    public void run(ApplicationArguments args) throws Exception {
        //在这里添加应用启动的时候一些初始化工作
        String sslFileRoot = "D:\\Work\\framework\\mqttserver-duoxian\\cert\\";
        String caCertFile = sslFileRoot + "ca.crt";
        String serverCertFile = sslFileRoot + "server.crt";
        String keyFile = sslFileRoot + "pkcs8_server.key";
        //不带SSL
        //new MqttServerService().startup(8888, this, null);
        //启动SSL
        new MqttServerService().startup(new MqttServerServiceOption.Builder()
                .port(8888)
                .messageListener(this)
                .ssl(caCertFile, serverCertFile, keyFile)
                .build());
    }

    @Override
    public void offline(String uuid, ClientSession clientSession) {

    }

    @Override
    public void onMessage(String ip, String channelId, String topic, String message, Function<CustomMqttPublishMessage, Boolean> publisher) {
        logger.info(topic + ":" + message);
        publisher.apply(new CustomMqttPublishMessage(channelId, topic + "_reply", message));
    }

    @Override
    public void online(String uuid, ClientSession clientSession) {

    }

    @Override
    public String clientIdToUuid(String clientId) {
        return clientId;
    }
}
