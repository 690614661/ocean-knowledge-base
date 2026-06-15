package com.ocean.mq;

import com.ocean.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
// 开发环境如未启动 RocketMQ，可临时注释 @Component 注解
//@Component
@ConditionalOnProperty(name = "spring.rocketmq.name-server", havingValue = "localhost:9876", matchIfMissing = false)
@RocketMQMessageListener(topic = "VOTE_TOPIC", consumerGroup = "ocean-consumer")
public class VoteMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("收到点赞通知消息: {}", message);
        WebSocketServer.broadcastMessage(message);
    }
}
