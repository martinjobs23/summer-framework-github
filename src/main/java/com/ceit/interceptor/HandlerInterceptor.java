package com.ceit.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ceit.ioc.HandlerDefinition;

/**
 * 通用拦截器
 */
public interface HandlerInterceptor {

    /**
     * 执行前拦截请求判断是否可执行
     * 
     * @param request
     *            当前的HTTP请求
     * @param response
     *            当前的HTTP响应
     * @param handler
     *            请求分发的处理器函数{@link HandlerDefinition}
     * @return
     */
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler) {
        return true;
    }

    /**
     * 实际上发生于执行handler之前，用于请求的前置处理
     * 
     * @param request
     *            当前的HTTP请求
     * @param response
     *            当前的HTTP响应
     * @param handler
     *            请求分发的处理器函数{@link HandlerDefinition}
     * @param reqBody
     *            参数
     */
    default void postHandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler,
            Map<String, Object> reqBody) {

    }

    /**
     * 请求完成后的后置处理
     * 
     * @param request
     *            当前的HTTP请求
     * @param response
     *            当前的HTTP响应
     * @param handler
     *            请求分发的处理器函数{@link HandlerDefinition}
     * @param result
     *            处理结果
     */
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler,
            Object result) {

    }
}
