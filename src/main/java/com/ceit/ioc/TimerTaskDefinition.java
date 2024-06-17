package com.ceit.ioc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 记录{@link RequestMapping}方法
 */
public class TimerTaskDefinition {
    private Method method;
    private String BeanName;
	private int delay;
    private int period;

    public TimerTaskDefinition(Method method, String BeanName, int delay, int period) {
        this.method = method;
        this.BeanName = BeanName;
        this.delay = delay;
        this.period = period;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public String getBeanName() {
		return BeanName;
	}

	public void setBeanName(String beanName) {
		BeanName = beanName;
	}

 
}
