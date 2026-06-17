package com.ocean.mq;

import com.ocean.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.consumer.vote.enabled", havingValue = "true", matchIfMissing = false)
@RocketMQMessageListener(topic = "VOTE_TOPIC", consumerGroup = "ocean-consumer")
public class VoteMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("收到点赞通知消息: {}", message);
        WebSocketServer.broadcastMessage(message);
    }
}
