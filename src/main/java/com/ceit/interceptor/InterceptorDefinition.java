package com.ceit.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ceit.utils.StringUtil;

/**
 * @author: ko
 * @date: 21.07.26 22:17:09
 * @description: 拦截器及其相关url路径
 */
public class InterceptorDefinition {
    private final HandlerInterceptor interceptor;
    private List<String> includePatterns;
    private List<String> excludePatterns;

    public InterceptorDefinition(HandlerInterceptor interceptor) {

        this.interceptor = interceptor;
    }

    /**
     * 添加拦截路径
     * 
     * @param patterns
     *            路径数组
     * @return {@link InterceptorDefinition}
     */
    public InterceptorDefinition addPathPatterns(String... patterns) {
        return addPathPatterns(Arrays.asList(patterns));
    }

    /**
     * 添加拦截路径
     * 
     * @param patterns
     *            路径队列
     * @return {@link InterceptorDefinition}
     */
    public InterceptorDefinition addPathPatterns(List<String> patterns) {
        this.includePatterns = (this.includePatterns != null ? this.includePatterns : new ArrayList<>(patterns.size()));
        this.includePatterns.addAll(patterns);
        return this;
    }

    /**
     * 排除拦截路径
     * 
     * @param patterns
     *            路径数组
     * @return {@link InterceptorDefinition}
     */
    public InterceptorDefinition excludePathPatterns(String... patterns) {
        return excludePathPatterns(Arrays.asList(patterns));
    }

    /**
     * 排除拦截路径
     * 
     * @param patterns
     *            路径队列
     * @return {@link InterceptorDefinition}
     */
    public InterceptorDefinition excludePathPatterns(List<String> patterns) {
        this.excludePatterns = (this.excludePatterns != null ? this.excludePatterns : new ArrayList<>(patterns.size()));
        this.excludePatterns.addAll(patterns);
        return this;
    }

    public HandlerInterceptor getInterceptor() {
        return interceptor;
    }

    /**
     * 路径匹配
     */
    public InterceptorDefinition getInterceptor(String url) {
        if (includePatterns != null && !patternsUrl(url, includePatterns)) {
            return null;
        }
        if (excludePatterns != null && patternsUrl(url, excludePatterns)) {
            return null;
        }
        return this;
    }

    /**
     * url默认为两段式/../..
     * 
     * @param url
     *            两段式请求路径
     * @param list
     *            匹配的所有路径
     * @return 是或否
     */
    private boolean patternsUrl(String url, List<String> list) {
        for (String str : list) {
            if (StringUtil.urlPatterns(url, str)) {
                return true;
            }
        }
        return false;
    }

}
