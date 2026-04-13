package org.example.dln.vo;

import lombok.Data;

/**
 * 包名：org.example.dln.vo
 * 类名：Result
 * 类描述：封装通用接口响应结果。
 * 创建人：@author Rain_润
 */
@Data
public class Result<T> {
    // 状态码
    private Integer code;
    // 消息
    private String message;
    // 返回的数据
    private T data;

    /**
    * 构建成功响应结果。
    */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
    * 构建成功响应结果。
    */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
    // 默认错误码 500
    /**
     * 构建失败响应结果。
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
    // 用于自定义错误码
    // TODO - 编写状态码文档并补充自定义错误码
    /**
     * 构建失败响应结果。
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
