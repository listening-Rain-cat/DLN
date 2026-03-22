package org.example.dln.vo;

import lombok.Data;

/**
 * 包名称： org.example.dln.vo
 * 类名称：Result
 * 类描述：结果返回类，返回给前端
 * 创建人：@author Rain_润
 * 创建时间：2026-03-19 14:38
 */
@Data
public class Result<T> {
    //状态码
    private Integer code;
    //消息
    private String message;
    //返回的数据
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
    //默认错误码500
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
    //用于自定义错误码
    //TODO - 写状态码文档添加自定义错误码
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}