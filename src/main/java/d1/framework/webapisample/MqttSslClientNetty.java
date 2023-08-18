package d1.framework.webapisample;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.File;

public class MqttSslClientNetty {

    private static final String MQTT_HOST = "127.0.0.1";
    private static final int MQTT_PORT = 8888; // MQTT over SSL port

    private static final String CLIENT_ID = "mqtt-client";
    private static final String TOPIC = "test/topic";

    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        final SslContext sslCtx = SslContextBuilder.forClient()
                //双向验证
                .keyManager(new File("D:\\Work\\framework\\mqttserver-duoxian\\cert\\client.crt"),
                        new File("D:\\Work\\framework\\mqttserver-duoxian\\cert\\pkcs8_client.key"))
                .trustManager(new File("D:\\Work\\framework\\mqttserver-duoxian\\cert\\ca.crt"))// CA证书，验证对方证书

//                 不验证SERVER
//                 .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                            ch.pipeline().addLast(new IdleStateHandler(90, 0, 0));
                            ch.pipeline().addLast("mqttDecoder", new MqttDecoder(102400));
                            ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
                            ch.pipeline().addLast("mqttHandler", new MqttClientHandler());
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect(MQTT_HOST, MQTT_PORT).sync();
            Channel channel = channelFuture.channel();

            // Connect to the MQTT broker
            MqttConnectMessage connectMessage = new MqttConnectMessage(
                    new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnectVariableHeader(MqttVersion.MQTT_3_1_1.protocolName(), MqttVersion.MQTT_3_1_1.protocolLevel(), true, true, false, 0, false, false, 60),
                    new MqttConnectPayload(CLIENT_ID, "aaa", "bbb", "admin", "password"));

            channel.writeAndFlush(connectMessage);

//            // Subscribe to a topic
//            MqttSubscribeMessage subscribeMessage = new MqttSubscribeMessage(
//                    new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0),
//                    MqttMessageIdVariableHeader.from(1),
//                    new MqttSubscribePayload().addSubscription(new MqttTopicSubscription(TOPIC, MqttQoS.AT_LEAST_ONCE)));
//
//            channel.writeAndFlush(subscribeMessage);
            System.out.println("sss");
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }


    private static class MqttClientHandler extends SimpleChannelInboundHandler<MqttMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
            System.out.println("eeee: " + msg);
            // Handle incoming MQTT messages
            if (msg instanceof MqttSubAckMessage) {
                System.out.println("Subscribed to topic: " + TOPIC);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            System.out.println(("MQTT channelActive:{}" + ctx.channel().id().asLongText()));
        }

        /**
         * 从客户端收到新的数据、读取完成时调用
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            System.out.println("MQTT channelReadComplete:{}" + ctx.channel().id().asLongText());
        }

        /**
         * 当出现 Throwable 对象才会被调用，即当 Netty 由于 IO 错误或者处理器在处理事件时抛出的异常时
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // super.exceptionCaught(ctx, cause); 注释是为了解决：远程主机强迫关闭了一个现有的连接 错误，
            System.out.println("MQTT Server IO异常,{}:{}" + ctx.channel().id().asLongText() + cause.getMessage());
        }

        /**
         * 客户端与服务端 断连时执行
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            String channelId = ctx.channel().id().asLongText();
            System.out.println("MQTT channelInactive:{}" + channelId);
        }

        /**
         * 服务端 当读超时时 会调用这个方法
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
            System.out.println("MQTT Server Channel timeout:{}" + ctx.channel().id().asLongText() + evt.toString());
//            ctx.close();
        }


        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            super.channelWritabilityChanged(ctx);
            System.out.println("MQTT channelWritabilityChanged:{}" + ctx.channel().id().asLongText());
        }

    }
}
