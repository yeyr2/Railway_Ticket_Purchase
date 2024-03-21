package org.yeyr2.as12306.ticketService.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.ticketService.common.constant.TicketRocketMQConstant;
import org.yeyr2.as12306.ticketService.common.enums.CanalExecuteStrategyMarkEnum;
import org.yeyr2.as12306.ticketService.mq.event.CanalBinlogEvent;
import org.yeyr2.as12306.designpattern.strategy.AbstractStrategyChoose;

import java.util.Objects;

/**
 * 列车车票余量缓存更新消费端
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.CANAL_COMMON_SYNC_TOPIC_KEY,
        consumerGroup = TicketRocketMQConstant.CANAL_COMMON_SYNC_CG_KEY
)
public class CanalCommonSyncBinlogConsumer implements RocketMQListener<CanalBinlogEvent> {

    private final AbstractStrategyChoose abstractStrategyChoose;

    @Value("${ticket.availability.cache-update.type}")
    private String ticketAvailabilityCacheUpdateType;

    @Override
    public void onMessage(CanalBinlogEvent canalBinlogEvent) {
        // binlog监听
        if(canalBinlogEvent.getIsDdl()
                || CollUtil.isEmpty(canalBinlogEvent.getOld())
                || !Objects.equals("UPDATE",canalBinlogEvent.getType())
                || !StrUtil.equals(ticketAvailabilityCacheUpdateType,"binlog")){
            return;
        }
        abstractStrategyChoose.chooseAndExecute(
                canalBinlogEvent.getTable(),
                canalBinlogEvent,
                CanalExecuteStrategyMarkEnum.isPatternMatch(canalBinlogEvent.getTable())
        );
    }
}
