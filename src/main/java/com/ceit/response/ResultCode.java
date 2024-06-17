package com.ceit.response;

public enum ResultCode {

    PARAM_ERROR("param error", 400), 
    REQUEST_NOT_FOUND("request not found", 404), 
    SUCCESS("success", 200), 
    SUCCESS_TOTREE("success to tree",  201), 
    UNKNOWN_ERROR("unknown error", 401), 
    REFUSE_ACCESS("refuse access", 402);

    private String msg;
    private Integer code;

    private ResultCode(String msg, Integer code) {
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getCode() {
        return code;
    }
}
