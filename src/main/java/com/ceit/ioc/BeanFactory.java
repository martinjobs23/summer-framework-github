package com.ceit.ioc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.ceit.ioc.annotations.*;
import com.ceit.utils.ClassUtil;

/**
 * @author: ko
 * @date: 21.06.25 10:40:29
 * @description: bean管理容器，并非工厂模式
 */
public class BeanFactory {

    private final Map<Class<?>, Consumer<Class<?>>> FUNC_MAP = new HashMap<>(8);
    private final List<Class<? extends Annotation>> FUNC_LIST = new ArrayList<>(8);
    private Map<String, Object> singletonBeans = new HashMap<>(32);
    private Map<String, HandlerDefinition> handlerMap = new HashMap<>(64);
    private Map<String, Class<?>> nonSingletonBeans = new HashMap<>(8);

    //执行定时任务
    private static final List<TimerTaskDefinition> TimerTaskList = new ArrayList<>(8);
    private static ScheduledExecutorService scheduledExecutorService = null;
    
    // 策略模式提高扩展性
    private BeanFactory() {
        Consumer<Class<?>> beanStrategy = (clazz) -> {
            try {
                if (!singletonBeans.containsKey(clazz.getName()) && !nonSingletonBeans.containsKey(clazz.getName())) {
                    System.out.println("Set Singleton Bean : " + clazz.getName());
                    setSingletonBean(clazz);
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(PostConstruct.class)) {
                        Object obj = getInstance(clazz);
                        method.invoke(obj);
                    }
                }
            } catch (Exception e) {
            }
        };
        FUNC_LIST.add(Component.class);
        FUNC_MAP.put(Component.class, beanStrategy);
        FUNC_LIST.add(Controller.class);
        FUNC_MAP.put(Controller.class, clazz -> {
            registryHandler(clazz);
            beanStrategy.accept(clazz);
        });
    }


    /**
     * 装载路径下的组件
     */
    public void registryBeans(String path) {
    	
        System.out.println("Scanning in path: " + path);
        
        TimerTaskList.clear();
        
        List<Class<?>> classes = ClassUtil.getAllClassByPath(path);

        // 策略模式
        classes.stream().filter(clazz -> {
            // 此处不false是为了非单例的组件 registryHandler(clazz); 正常执行
            if (nonSingleton(clazz))
                nonSingletonBeans.put(clazz.getName(), clazz);
            return true;
        }).forEach(clazz -> {
            Class<?> clz = getStrategy(clazz);
            if (clz != null) {
                FUNC_MAP.get(clz).accept(clazz);
            }
        });
    }

    /**
     * 根据注解获取策略
     */
    private Class<?> getStrategy(Class<?> clazz) {
        for (Class<? extends Annotation> class1 : FUNC_LIST) {
            if (clazz.isAnnotationPresent(class1)) {
                return class1;
            }
        }
        return null;
    }

    /**
     * 实例化并注册bean
     */
    private void setSingletonBean(Class<?> clazz) throws Exception {
        Object obj = getInstance(clazz);
        // getInstance中可能直接注册当前clazz，故再次判断containsKey
        if (obj != null && !singletonBeans.containsKey(clazz.getName())) {
            singletonBeans.put(clazz.getName(), obj);
        }
    }

    private boolean nonSingleton(Class<?> clazz) {
        return clazz.isAnnotationPresent(Scope.class)
                && !clazz.getAnnotation(Scope.class).value().equals("Singleton");
    }

    /**
     * 创建类实例
     */
    private Object getInstance(Class<?> clazz) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        Object obj = null;
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        obj = ctor.newInstance();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                String key = field.getType().getName();
                // 递归时一定满足第一个条件
                synchronized (singletonBeans) {
                    if (singletonBeans.containsKey(key)) {
                        Object value = singletonBeans.get(key);
                        field.set(obj, value);
                    } else {
                        // 先将当前组件简单初始化，防止循环依赖,后续组件注入当前组件的引用
                        // 临时存放
                        singletonBeans.put(clazz.getName(), obj);
                        Object value = getInstance(field.getType());
                        // 该组件重新注入后其他依赖组件的依赖属性亦会改变
                        field.set(obj, value);
                        if (!nonSingletonBeans.containsKey(key)) {
                            System.out.println("Set Singleton Bean : " + key);
                            singletonBeans.put(key, value);
                        }
                    }
                }
            }
        }
        if (nonSingleton(clazz)) {
            singletonBeans.remove(clazz.getName());
        }
        return obj;
    }

    /**
     * 根据name获取组件
     */
    public Object getBean(String name) {
        // 非单例模式
        if (nonSingletonBeans.containsKey(name)) {
            try {
                return getInstance(nonSingletonBeans.get(name));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return singletonBeans.get(name);
    }

    /**
     * 注册处理器
     */
    private void registryHandler(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                Parameter[] parameters = method.getParameters();
                StringBuilder url = new StringBuilder();
                String firstUrl = clazz.getAnnotation(Controller.class).value();
                if (!firstUrl.startsWith("/")) {
                    url.append('/');
                }
                url.append(firstUrl);
                String lastUrl = method.getAnnotation(RequestMapping.class).value();
                if (!lastUrl.startsWith("/")) {
                    url.append('/');
                }
                url.append(lastUrl);
                HandlerDefinition definition = new HandlerDefinition(method, clazz.getName(), parameters, clazz);
                definition.setUrl(url.toString());
                handlerMap.put(url.toString(), definition);
            }
            else if (method.isAnnotationPresent(TimerTask.class)) {
            	//定时任务
            	Annotation[] annotations = method.getAnnotations();
            	for(Annotation annotation: annotations)
            	{
            		if(annotation instanceof TimerTask)
            		{
            			TimerTask taskAnno = (TimerTask)annotation;
                    	System.out.println("TimerTask method "+ method.getName() +" delay=" +taskAnno.delay()+" period="+ taskAnno.value()	);
                    	 
                    	TimerTaskDefinition task = new TimerTaskDefinition(method, clazz.getName(),taskAnno.delay(), taskAnno.value());
                    	TimerTaskList.add(task);
            		}
            	}
            }
        }
    }

    public int StartTaskTimers() {
 
    	int count = TimerTaskList.size();
    	
		System.out.println("StartTaskTimers, ScheduledThreadPool TimeTasks count=" + count);
		
		if(count==0)
			return 0;
		
		scheduledExecutorService = Executors.newScheduledThreadPool(count); // 10 为线程数量
    	for(TimerTaskDefinition task: TimerTaskList)
    	{
    		scheduledExecutorService.scheduleAtFixedRate(
    				() -> { try {
    					Object object = getBean(task.getBeanName());
						task.getMethod().invoke(object);
					} catch (Exception e) {
						e.printStackTrace();
					} 
    				}, 
    				task.getDelay(), 
    				task.getPeriod(),
    				TimeUnit.SECONDS); // 0s 后开始执行，每 2s 执行一次
    	}
 
    	return count;
    }
    
    public static void StopTaskTimers() {
    	
		if(scheduledExecutorService!=null)
		{
			System.out.println("scheduledExecutorService.shutdownNow");
			//scheduledExecutorService.shutdown();
			List<Runnable> list =scheduledExecutorService.shutdownNow();
			for(Runnable r:list)
			{
				try {
					r.wait(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
    }
    
    /**
     * 根据路径获取处理器
     */
    public HandlerDefinition getHandlerDefinition(String url) {
        return handlerMap.get(url);
    }

    private static class SingletonBeanFactory {
        private static final BeanFactory INSTANCE = new BeanFactory();
    }

    public static BeanFactory getInstance() {
        return SingletonBeanFactory.INSTANCE;
    }
}
