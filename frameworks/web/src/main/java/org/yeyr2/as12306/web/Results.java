package org.yeyr2.as12306.web;

import org.yeyr2.as12306.convention.errorcode.BaseErrorCode;
import org.yeyr2.as12306.convention.exception.AbstractException;
import org.yeyr2.as12306.convention.result.Result;

import java.util.Optional;

// 全局返回对象构造器
public class Results {
    // 构造成功响应
    public static Result<Void> success(){
        return new Result<Void>().setCode(Result.SUCCESS_CODE);
    }

    // 构造带返回值的成功响应
    public static <T> Result<T> success(T data){
        return new Result<T>().setCode(Result.SUCCESS_CODE).setData(data);
    }

    // 构建服务端失败的响应
    protected static Result<Void> failure(){
        return new Result<Void>()
                .setCode(BaseErrorCode.SERVICE_ERROR.code())
                .setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }

    // 通过AbstractException
    protected  static Result<Void> failure(AbstractException abstractException){
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(BaseErrorCode.SERVICE_ERROR.message());
        return new Result<Void>().setCode(errorCode).setMessage(errorMessage);
    }

    //通过errorCode,errorMessage构建失败响应
    protected static Result<Void> failure(String errorCode,String errorMessage){
        return new Result<Void>().setCode(errorCode).setMessage(errorMessage);
    }
}
