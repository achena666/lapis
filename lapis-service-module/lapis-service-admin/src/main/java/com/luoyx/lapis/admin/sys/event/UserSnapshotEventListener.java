package com.luoyx.lapis.admin.sys.event;

import com.luoyx.lapis.admin.amqp.producer.UserSnapshotProducer;
import com.luoyx.lapis.admin.sys.converter.UserSnapshotConverter;
import com.luoyx.lapis.usersnapshot.msg.UserSnapshotMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class UserSnapshotEventListener {

    private final UserSnapshotConverter userSnapshotConverter;
    private final UserSnapshotProducer userSnapshotProducer;

    /**
     * 处理用户快照事件
     * 用户的增删改功能，事务提交后，触发该事件
     *
     * @param event 用户快照事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSnapshotEvent(UserSnapshotEvent event) {
        UserSnapshotMessage msg = new UserSnapshotMessage();
        msg.setEventType(event.getEventType());
        msg.setUserSnapshotList(userSnapshotConverter.toMsgUserSnapshotList(event.getUserSnapshotList()));
        userSnapshotProducer.send(msg);
    }
}
