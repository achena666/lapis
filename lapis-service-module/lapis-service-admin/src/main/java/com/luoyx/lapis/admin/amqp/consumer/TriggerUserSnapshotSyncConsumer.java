package com.luoyx.lapis.admin.amqp.consumer;

import com.luoyx.lapis.admin.amqp.producer.UserSnapshotProducer;
import com.luoyx.lapis.admin.api.sys.dto.TriggerUserSnapshotSyncDTO;
import com.luoyx.lapis.admin.sys.converter.UserSnapshotConverter;
import com.luoyx.lapis.admin.sys.service.UserSnapshotService;
import com.luoyx.lapis.usersnapshot.enums.EventType;
import com.luoyx.lapis.usersnapshot.msg.UserSnapshotMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 触发用户快照同步消费者
 *
 * @author Alan.Luo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriggerUserSnapshotSyncConsumer {

    private final UserSnapshotProducer userSnapshotProducer;
    private final UserSnapshotService userSnapshotService;
    private final UserSnapshotConverter userSnapshotConverter;

    /**
     * 其他服务启动完成后，会发送MQ消息到该队列，触发用户快照同步
     *
     * @param triggerUserSnapshotSyncDTO 触发用户快照同步DTO
     */
    @RabbitListener(queues = "trigger.user.snapshot.sync.queue")
    public void handleTriggerUserSnapshotSync(TriggerUserSnapshotSyncDTO triggerUserSnapshotSyncDTO) {
        log.info("收到服务【{}】的用户快照同步请求", triggerUserSnapshotSyncDTO.getServiceName());
        UserSnapshotMessage userSnapshotMessage = new UserSnapshotMessage();
        userSnapshotMessage.setEventType(EventType.CONSUMER_SERVER_STARTED);
        userSnapshotMessage.setUserSnapshotList(userSnapshotConverter.toMsgUserSnapshotList(userSnapshotService.list()));
        userSnapshotProducer.send(userSnapshotMessage);
    }
}
