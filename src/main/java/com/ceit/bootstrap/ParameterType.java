package com.ceit.bootstrap;

import com.alibaba.fastjson2.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Parameter;

public //定义参数类型, 按照位操作定义，8位，从低到高
// 0000 0001 Request
// 0000 0010 Response
// 0000 0100 Json
// 0000 1000 Map
// 0001 0000 String

// ParameterType 是各种组合
// 例如：ReqRespJsonMapStr 是5个参数都需要

enum ParameterType
{
    Nop(0),

    //任选1个 C5,1 = 5
    Req(1),
    Resp(2),
    Json(4),
    Map(8),
    Str(16),

    //任选2个 C5,2 = 10
    ReqResp(Req.value | Resp.value),
    ReqJson(Req.value | Json.value),
    ReqMap(Req.value | Map.value),
    ReqStr(Req.value | Str.value),
    RespJson(Resp.value | Json.value),
    RespMap(Resp.value | Map.value),
    RespStr(Resp.value | Str.value),
    JsonMap(Json.value | Map.value),
    JsonStr(Json.value | Str.value),
    MapStr(Map.value | Str.value),

    //任选3个  C5,3 = 10
    ReqRespJson(Req.value | Resp.value | Json.value),
    ReqRespMap(Req.value | Resp.value | Map.value),
    ReqRespStr(Req.value | Resp.value | Str.value),
    ReqJsonMap(Req.value | Json.value | Map.value),
    ReqJsonStr(Req.value | Json.value | Str.value),
    ReqMapStr(Req.value | Map.value | Str.value),
    RespJsonMap(Resp.value | Json.value | Map.value),
    RespJsonStr(Resp.value | Json.value | Str.value),
    RespMapStr(Resp.value | Map.value | Str.value),
    JsonMapStr(Json.value | Map.value | Str.value),

    //任选4个  C5,4 = 5
    ReqRespJsonMap(Req.value | Resp.value | Json.value | Map.value),
    ReqRespJsonStr(Req.value | Resp.value | Json.value | Str.value),
    ReqRespMapStr(Req.value | Resp.value | Map.value | Str.value),
    ReqJsonMapStr(Req.value | Json.value | Map.value | Str.value),
    RespJsonMapStr(Resp.value | Json.value | Map.value | Str.value),

    //任选5个  C5,5 = 1
    ReqRespJsonMapStr(Req.value | Resp.value | Json.value | Map.value | Str.value);

    private int value;

    ParameterType(int value)
    {
        this.value = value;
    }

    public static ParameterType valueOf(int value)
    {
        for (ParameterType type : ParameterType.values())
        {
            if (type.value == value)
            {
                return type;
            }
        }
        return null;
    }

    public static ParameterType valueOf(Parameter[] params)
    {
        int value = Nop.value;
        for (Parameter param : params) {
            Class<?> type = param.getType();

            //JSONObject是Map子类，要放前面，不然会错误
            if (JSONObject.class.isAssignableFrom(type)) {
                value = value | ParameterType.Json.value;
            } else if (java.util.Map.class.isAssignableFrom(type)) {
                value = value | ParameterType.Map.value;
            } else if (HttpServletRequest.class.isAssignableFrom(type)) {
                value = value | ParameterType.Req.value;
            } else if (HttpServletResponse.class.isAssignableFrom(type)) {
                value = value | ParameterType.Resp.value;
            } else  if (String.class.isAssignableFrom(type)) {
                value = value | ParameterType.Str.value;
            }
        }

        return valueOf(value);
    }

    public boolean contains(ParameterType type)
    {
        return (value & type.value) == type.value;
    }

    public int value()
    {
        return value;
    }
}