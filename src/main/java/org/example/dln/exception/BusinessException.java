package org.example.dln.exception;

/**
 * 包名：org.example.dln.exception
 * 类名：BusinessException
 * 类描述：定义业务异常类型。
 * 创建人：@author Rain_润
 */
public class BusinessException extends RuntimeException {
    private final Integer code;

    /**
    * 创建业务异常对象。
    */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
    * 创建业务异常对象。
    */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
    * 获取业务异常状态码。
    */
    public Integer getCode() {
        return code;
    }
}
