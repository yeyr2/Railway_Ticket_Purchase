package org.yey2.as12306.ticketService.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 座位类型和座位数量实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTypeCountDTO {

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 该座位类型对应数量
     */
    private Integer seatCount;
}
