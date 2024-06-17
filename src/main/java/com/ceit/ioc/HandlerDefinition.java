package com.ceit.ioc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.ceit.bootstrap.ParameterType;
import com.ceit.ioc.annotations.RequestMapping;

/**
 * 记录{@link RequestMapping}方法
 */
public class HandlerDefinition {
    private Method method;
    private String beanName;
    private Parameter[] params;
    private ParameterType paramType;
    private Class<?> beanType;
    private String url;

    public HandlerDefinition(Method method, String beanName, Parameter[] params, Class<?> beanType) {
        this.method = method;
        this.beanName = beanName;
        this.params = params;
        this.beanType = beanType;

        //根据参数计算参数类型
        paramType = ParameterType.valueOf(params);
    }

    public ParameterType getParameterType(){ return paramType; };

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Parameter[] getParams() {
        return params;
    }

    public void setParams(Parameter[] params) {
        this.params = params;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public void setBeanType(Class<?> beanType) {
        this.beanType = beanType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
