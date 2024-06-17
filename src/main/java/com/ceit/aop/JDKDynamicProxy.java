package com.ceit.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * jdk动态代理，被代理对象必须实现接口
 */
public class JDKDynamicProxy implements InvocationHandler {

    private Object target;

    public JDKDynamicProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object[] objects = before(method, args);
        Object value = method.invoke(target, objects);
        Object result = after(value);
        return result;
    }

    public Object[] before(Method method, Object... objects) {
        return objects;
    }

    public Object after(Object value) {
        return value;
    }
}
