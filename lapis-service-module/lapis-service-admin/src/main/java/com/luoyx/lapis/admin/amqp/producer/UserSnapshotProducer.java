package com.luoyx.lapis.admin.amqp.producer;

import com.luoyx.lapis.usersnapshot.msg.UserSnapshotMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.luoyx.lapis.admin.api.sys.constant.MQConstant.USER_SNAPSHOT_FANOUT_EXCHANGE;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserSnapshotProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(UserSnapshotMessage userSnapshotMessage) {
        log.debug("生产消息【{}】", userSnapshotMessage);
        this.rabbitTemplate.convertAndSend(USER_SNAPSHOT_FANOUT_EXCHANGE, "", userSnapshotMessage);
    }
}
