package org.yeyr2.as12306.convention.exception;

import org.yeyr2.as12306.convention.errorcode.BaseErrorCode;
import org.yeyr2.as12306.convention.errorcode.IErrorCode;

// 客户端服务异常
public class ClientException extends AbstractException{

    public ClientException(IErrorCode errorCode){
        this(null,null,errorCode);
    }

    public ClientException(String message) {
        this(message,null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message,IErrorCode errorCode) {
        this(message,null, errorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode iErrorCode) {
        super(message, throwable, iErrorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
