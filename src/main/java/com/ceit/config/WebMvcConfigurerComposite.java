package com.ceit.config;

import java.util.List;

import com.ceit.interceptor.InterceptorRegistry;

/**
 * @author: ko
 * @date: 21.07.27 17:14:38
 * @description: 委托执行所有{@link WebMvcConfigurer}
 */
public class WebMvcConfigurerComposite implements WebMvcConfigurer {

    private final List<WebMvcConfigurer> delegates;

    public WebMvcConfigurerComposite(List<WebMvcConfigurer> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        for (WebMvcConfigurer delegate : this.delegates) {
            delegate.addInterceptors(registry);
        }
    }

    @Override
    public void addScanPath(List<String> paths) {
        for (WebMvcConfigurer delegate : this.delegates) {
            delegate.addScanPath(paths);
        }
    }

}
