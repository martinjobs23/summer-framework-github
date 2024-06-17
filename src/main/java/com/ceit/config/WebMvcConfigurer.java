package com.ceit.config;

import java.util.List;

import com.ceit.interceptor.InterceptorRegistry;

/**
 * web配置接口
 */
public interface WebMvcConfigurer {

    default void addInterceptors(InterceptorRegistry registry) {
    }

    /**
     * 自定义扫描路径
     */
    default void addScanPath(List<String> paths) {
    }
}
