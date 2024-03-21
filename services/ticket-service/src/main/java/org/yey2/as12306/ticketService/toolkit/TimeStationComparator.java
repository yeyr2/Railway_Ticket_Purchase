package org.yey2.as12306.ticketService.toolkit;

import org.yey2.as12306.ticketService.dto.domain.TicketListDTO;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * 时间比较器
 */
public class TimeStationComparator implements Comparator<TicketListDTO> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 比较出发时间
     */
    @Override
    public int compare(TicketListDTO ticketList1, TicketListDTO ticketList2) {
        LocalTime localTime1 = LocalTime.parse(ticketList1.getDepartureTime(), FORMATTER);
        LocalTime localTime2 = LocalTime.parse(ticketList2.getDepartureTime(), FORMATTER);
        return localTime1.compareTo(localTime2);
    }

}
