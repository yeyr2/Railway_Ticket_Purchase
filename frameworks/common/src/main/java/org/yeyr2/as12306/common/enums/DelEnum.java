package org.yeyr2.as12306.common.enums;

// 删除标记枚举
public enum DelEnum {
    NORMAL(0),
    DELETE(1);

    private final Integer statusCode;
    DelEnum(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer code(){
        return this.statusCode;
    }

    public String strCode(){
        return String.valueOf(this.statusCode);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
