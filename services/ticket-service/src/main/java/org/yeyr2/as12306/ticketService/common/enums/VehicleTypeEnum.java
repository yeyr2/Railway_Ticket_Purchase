package org.yeyr2.as12306.ticketService.common.enums;

import cn.hutool.core.collection.ListUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 交通工具类型
 */
@RequiredArgsConstructor
@Getter
public enum VehicleTypeEnum {

    /**
     * 高铁
     */
    HIGH_SPEED_RAIN(0, "HIGH_SPEED_RAIN", "高铁", ListUtil.of(VehicleSeatTypeEnum.BUSINESS_CLASS.getCode(), VehicleSeatTypeEnum.FIRST_CLASS.getCode(), VehicleSeatTypeEnum.SECOND_CLASS.getCode())),

    /**
     * 动车
     */
    BULLET(1, "BULLET", "动车", ListUtil.of(VehicleSeatTypeEnum.SECOND_CLASS_CABIN_SEAT.getCode(), VehicleSeatTypeEnum.FIRST_SLEEPER.getCode(), VehicleSeatTypeEnum.SECOND_SLEEPER.getCode(), VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getCode())),

    /**
     * 普通车
     */
    REGULAR_TRAIN(2, "REGULAR_TRAIN", "普通车", ListUtil.of(VehicleSeatTypeEnum.SOFT_SLEEPER.getCode(), VehicleSeatTypeEnum.HARD_SLEEPER.getCode(), VehicleSeatTypeEnum.HARD_SEAT.getCode(), VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getCode())),

    /**
     * 汽车
     */
    CAR(3, "CAR", "汽车", null),

    /**
     * 飞机
     */
    AIRPLANE(4, "AIRPLANE", "飞机", null);

    private final Integer code;
    private final String name;
    private final String value;
    private final List<Integer> seatTypes;

    /**
     * 根据编码查找名称
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(VehicleTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleTypeEnum::getName)
                .orElse(null);
    }

    /**
     * 根据编码查找座位类型集合
     */
    public static List<Integer> findSeatTypesByCode(Integer code) {
        return Arrays.stream(VehicleTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleTypeEnum::getSeatTypes)
                .orElse(null);
    }
}
