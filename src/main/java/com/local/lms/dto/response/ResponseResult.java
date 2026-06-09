package com.local.lms.dto.response;

import com.local.lms.exceptions.ResponseCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ResponseResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 8174582198511424310L;

    private LocalDateTime timestamp;
    private String code;
    private String msg;
    private T data;

    public static <T> ResponseResult<T> success(){
        return response(
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMessage(),
                null
        );
    }

    public static <T> ResponseResult<T> success(T data){
        return response(
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMessage(),
                data
        );
    }

    public static <T> ResponseResult<T> success(String msg, T data){
        return response(ResponseCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> ResponseResult<T> success(String msg){
        return response(ResponseCode.SUCCESS.getCode(), msg, null);
    }

    public static <T> ResponseResult<T> failed(){
        return response(
                ResponseCode.BAD_REQUEST.getCode(),
                ResponseCode.BAD_REQUEST.getMessage(),
                null
        );
    }

    public static <T> ResponseResult<T> failed(String msg){
        return response(ResponseCode.BAD_REQUEST.getCode(), msg, null);
    }

    public static <T> ResponseResult<T> response(String code, String msg, T data) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setTimestamp(LocalDateTime.now());
        responseResult.setCode(code);
        responseResult.setMsg(msg);
        responseResult.setData(data);
        return responseResult;
    }
}
