package org.yeyr2.as12306.ticketService.canal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.ticketService.mq.event.CanalBinlogEvent;
import org.yeyr2.as12306.designpattern.strategy.AbstractExecuteStrategy;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent,Void> {
}
