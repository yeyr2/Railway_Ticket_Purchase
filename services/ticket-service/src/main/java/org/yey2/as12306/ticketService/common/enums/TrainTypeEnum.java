package org.yey2.as12306.ticketService.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum TrainTypeEnum {
    High_Speed_Rail(0,"高铁"),
    Bullet_Train(1,"动车"),
    Regular_Train(2,"普通车");

    private final Integer code;
    private final String type;

    public static TrainTypeEnum transform(Object code){
        int result;
        if(code instanceof Integer){
            result = (int) code;
        } else if (code instanceof  String) {
            try{
                result = Integer.parseInt((String) code);
            }catch (Exception ex){
                return null;
            }
        }else{
            return null;
        }
        if(result > 2 || result < 0){
            return null;
        }
        return TrainTypeEnum.values()[result];
    }
}
