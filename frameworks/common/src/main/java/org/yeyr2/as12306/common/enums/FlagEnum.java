package org.yeyr2.as12306.common.enums;

// 标志枚举
public enum FlagEnum {
    // false
    FALSE(0),
    // true
    TRUE(1);

    private final Integer flag;
    FlagEnum(Integer flag) {
        this.flag = flag;
    }

    public Integer code(){
        return flag;
    }

    public String strCode(){
        return String.valueOf(this.flag);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
