package com.ceit.response;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

public class Result {
    private String msg;
    private Integer code;
    private Object data;
    boolean rawData;

    public Result() {
        rawData =false;
    }

    public Result(boolean isRaw, Object data) {

        rawData =isRaw;

        if(!rawData){
            this.msg = "";
            this.code = 200;
        }

        this.data = data;
    }
    public Result(Integer code, String msg, Object data) {
        this.msg = msg;
        this.code = code;
        this.data = data;
    }
    
    public Result(String msg, Integer code, Object data) {
        this.msg = msg;
        this.code = code;
        this.data = data;
    }

    public Result(Integer code,  String msg) {
        this.msg = msg;
        this.code = code;
    }
    
    public Result(String msg, Integer code) {
        this.msg = msg;
        this.code = code;
    }

    public Result(ResultCode resultCode) {
        this.msg = resultCode.getMsg();
        this.code = resultCode.getCode();
    }

    public Result(ResultCode resultCode, Object data) {
        this.msg = resultCode.getMsg();
        this.code = resultCode.getCode();
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * {@link Result}转化成json格式字符串
     */
    @SuppressWarnings("unchecked")
    @Override
    public String toString() {

        //rawData
        if(rawData) {
            return JSON.toJSONString(this.data,"yyyy-MM-dd HH:mm:ss");
        }

        JSONObject jsonObj=new JSONObject();
        jsonObj.put("code", code);
        jsonObj.put("msg", msg);

        //兼容返回值是String的情况
        if(data instanceof String) {
            String retStr = (String)data;
            if(retStr.startsWith("[") || retStr.startsWith("{")) {
                //转成JSON，如果转换失败，还用原值
                try{
                    data = com.alibaba.fastjson2.JSON.parse(retStr);
                } catch (Exception jsonerr){
                    data = retStr;
                }
            }
        }

        jsonObj.put("data", data);

        // 不是基本类型，也不是json
        return JSON.toJSONString(jsonObj,"yyyy-MM-dd HH:mm:ss");
    }
}
