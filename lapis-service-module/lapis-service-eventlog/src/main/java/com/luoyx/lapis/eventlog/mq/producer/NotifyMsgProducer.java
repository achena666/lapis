package com.luoyx.lapis.eventlog.mq.producer;

import com.luoyx.lapis.eventlog.base.ExchangeName;
import com.luoyx.lapis.eventlog.base.RoutingKeyName;
import com.luoyx.lapis.eventlog.vo.NotifyMsgSendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Slf4j
@Component
public class NotifyMsgProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void send(NotifyMsgSendVO notifyMsgSendVO){
        log.debug("生产消息【{}】", notifyMsgSendVO);
        this.rabbitTemplate.convertAndSend(ExchangeName.NOTIFY_MSG.name(), RoutingKeyName.NOTIFY_MSG.name(), notifyMsgSendVO);
    }
}
