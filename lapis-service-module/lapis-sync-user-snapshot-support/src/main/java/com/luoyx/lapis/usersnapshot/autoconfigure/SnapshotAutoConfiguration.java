package com.luoyx.lapis.usersnapshot.autoconfigure;

import com.luoyx.lapis.usersnapshot.amqp.QueueConfig;
import com.luoyx.lapis.usersnapshot.amqp.UserSnapshotAmqpConsumer;
import com.luoyx.lapis.usersnapshot.converter.UserSnapshotConverter;
import com.luoyx.lapis.usersnapshot.mapper.UserSnapshotProperties;
import com.luoyx.lapis.usersnapshot.service.impl.SyncUserSnapshotServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 用户快照模块的自动配置类
 * 该类负责自动配置用户快照模块的相关组件，包括 AMQP 消费者、转换器和队列配置。
 *
 * @author Alan.Luo
 */
@Configuration
@EnableConfigurationProperties(UserSnapshotProperties.class)
@Import({UserSnapshotAmqpConsumer.class, SyncUserSnapshotServiceImpl.class, UserSnapshotConverter.class, QueueConfig.class})
public class SnapshotAutoConfiguration {
}

