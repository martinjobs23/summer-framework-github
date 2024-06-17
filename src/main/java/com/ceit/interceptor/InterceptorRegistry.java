package com.ceit.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ceit.ioc.HandlerDefinition;

/**
 * @author: ko
 * @date: 21.07.27 17:14:16
 * @description: {@link InterceptorDefinition}注册中心
 */
public class InterceptorRegistry {

    private final List<InterceptorDefinition> definitions = new ArrayList<>();

    public InterceptorRegistry() {
    }

    public InterceptorRegistry(List<InterceptorDefinition> definitions) {
        definitions.stream().forEach(item -> {
            this.definitions.add(item);
        });
    }

    public InterceptorDefinition addInterceptor(HandlerInterceptor interceptor) {
        InterceptorDefinition definition = new InterceptorDefinition(interceptor);
        this.definitions.add(definition);
        return definition;
    }

    /**
     * 返回所有拦截器,没有路径匹配
     */
    public List<HandlerInterceptor> getInterceptors() {
        return this.definitions.stream()
                .map(InterceptorDefinition::getInterceptor)
                .collect(Collectors.toList());
    }

    /**
     * 返回路径匹配的{@link InterceptorRegistry}
     * 
     * @param url
     *            请求路径
     */
    public InterceptorRegistry getRegistryByUrl(String url) {
        // TODO: 每次都创建新的，能否优化
        return new InterceptorRegistry(this.definitions.stream()
                .map(item -> {
                    return item.getInterceptor(url);
                })
                .filter(item -> {
                    return item != null;
                })
                .collect(Collectors.toList()));
    }

    /**
     * 执行所有拦截器的prehandle函数
     * 
     * @param request
     * @param response
     * @param handler
     * @return 存在一个拦截器为false时，返回false
     */
    public boolean prehandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler) {
        List<HandlerInterceptor> interceptors = getInterceptors();
        for (HandlerInterceptor interceptor : interceptors) {
            boolean flag = interceptor.preHandle(request, response, handler);
            if (!flag) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行所有拦截器的posthandle函数
     * 
     * @param request
     * @param response
     * @param handler
     * @param reqBody
     */
    public void posthandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler,
            Map<String, Object> reqBody) {
        List<HandlerInterceptor> interceptors = getInterceptors();
        interceptors.forEach(item -> {
            item.postHandle(request, response, handler, reqBody);
        });
    }

    /**
     * 执行所有拦截器的afterCompletion函数
     * 
     * @param request
     * @param response
     * @param handler
     * @param reqBody
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler,
            Object result) {
        List<HandlerInterceptor> interceptors = getInterceptors();
        interceptors.forEach(item -> {
            item.afterCompletion(request, response, handler, result);
        });
    }
}
