框架主要由容器**生命周期管理、请求分发处理、配置中心**三部分功能构成

# 生命周期
> 发布app时，tomcat自动加载src\main\webapp\META-INF\services\javax.servlet.ServletContainerInitializer文件，根据其中类名，初始化**ContainerInitializer**

## ContainerInitializer
app初始化时自动执行onStartup()函数，实现如下两个功能

+ 注册**DispatcherServlet(请求分发处理中心)、ContextListener(上下文监听器)、WebConfigFilter**

```java
    ServletRegistration.Dynamic servlet = ctx.addServlet("DispatcherServlet", DispatcherServlet.class);
    servlet.addMapping("/*");
    ctx.addListener(ContextListener.class);
    FilterRegistration.Dynamic filter = ctx.addFilter("WebConfigFilter", WebConfigFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
```

+ 执行全部**WebMvcConfigurer(配置中心)**，并将全部拦截器等注册进容器上下文

```java
    List<WebMvcConfigurer> configurers = c.stream().filter(item -> {
            return item.isAnnotationPresent(Configuration.class);
        }).map(item -> {
            try {
                return (WebMvcConfigurer) item.getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return new WebMvcConfigurer() {
                };
            }
        }).collect(Collectors.toList());
    WebMvcConfigurerComposite composite = new WebMvcConfigurerComposite(configurers);
    InterceptorRegistry registry = new InterceptorRegistry();
    composite.addInterceptors(registry);
    ctx.setAttribute("InterceptorRegistry", registry);
```

onStartup()完成后，初始化ContextListener

## ContextListener
监听servlet上下文，并在初始化时执行contextInitialized()用于**加载系统默认配置、初始化BeanFactory**

```java
public void contextInitialized(ServletContextEvent sce) {
    // 加载系统默认配置
    ConfigLoader.loadDefaultConfig();
    try (InputStream inputStream = this.getClass().getResourceAsStream("../defaultResource/hello.txt")) {
        int length = inputStream.available();
        byte[] bytes = new byte[length];
        if (inputStream.read(bytes) != -1) {
            System.out.println("\n" + new String(bytes, "UTF-8"));
        }
    } catch (Exception e) {
    }
    // 初始化BeanFactory
    beanFactory.init(ConfigLoader.class.getResource("/").getPath());
    ServletContextListener.super.contextInitialized(sce);
}
```

## BeanFactory
init()函数扫描指定路径，初始化并注册维护**Component(一般组件)、Controller(请求控制器)、HandlerDefinition(请求处理器相关定义)**组件


## 两个重要Component
### JSON
### SimpleJDBC

## 销毁
销毁时触发**ContextListener**的contextDestroyed()函数，用于**释放资源**
```java
public void contextDestroyed(ServletContextEvent sce) {
    // 释放数据库连接池的资源
    try {
        while (DriverManager.getDrivers().hasMoreElements()) {
            DriverManager.deregisterDriver(DriverManager.getDrivers().nextElement());
        }
        DBPool dbPool = DBPool.getInstance();
        dbPool.closeAll();
    } catch (Exception e) {
        e.printStackTrace();
    }
    try (InputStream inputStream = this.getClass().getResourceAsStream("../defaultResource/bye.txt")) {
        int length = inputStream.available();
        byte[] bytes = new byte[length];
        if (inputStream.read(bytes) != -1) {
            System.out.println("\n" + new String(bytes, "UTF-8") + "\n");
        }
    } catch (Exception e) {
    }
    ServletContextListener.super.contextDestroyed(sce);
}
```


# 请求处理
请求处理以**DispatcherServlet**类为核心，初始化时设置了其映射路径为"/*"，即拦截所有请求，其请求处理过程分三步：

+ 处理请求路径，删除tomcat默认添加的前置路径，判断路径是否合规，删除路径变量

```java
    String url = req.getRequestURI();
    String contextPath = req.getServletContext().getContextPath();
    if (contextPath.equals("/") == false) {
        // 忽略/summer
        url = url.substring(contextPath.length());
    }
    if (!url.startsWith("/")) {
        out.print(new Result(ResultCode.PARAM_ERROR).toString());
        return;
    }
    // 去掉?后面的参数 /user/doxxx?id=123
    int index = url.indexOf("?");
    if (index != -1) {
        url = url.substring(0, index);
    }
```

+ 根据请求路径获取处理器信息(HandlerDefinition)、拦截器注册中心(InterceptorRegistry)

```java
    HandlerDefinition definition = beanFactory.getHandlerDefinition(url);
    if (definition == null) {
        Result result = new Result("Method " + url + " Not Found", 404, null);
        out.print(result.toString());
        return;
    }
    InterceptorRegistry registry = ((InterceptorRegistry) req.getServletContext()
            .getAttribute("InterceptorRegistry")).getRegistryByUrl(url);

```

+ 获取请求参数，执行请求处理逻辑(包括**拦截器和处理器**)

```java
    if (!registry.prehandle(req, resp, definition)) {
        out.print(new Result(ResultCode.REFUSE_ACCESS));
        return;
    }
    Map<String, Object> reqBody = body2Map(req);
    registry.posthandle(req, resp, definition, reqBody);
    Result result = execute(req, resp, url, reqBody, definition);
    registry.afterCompletion(req, resp, definition, result);
```
## 核心类说明

### BeanFacory
+ 初始化、注册并维护controller、component、HandlerDefinition

```java
    private Map<String, Object> controllerContainer = new HashMap<>();
    private Map<String, Object> componentContainer = new HashMap<>();
    private Map<String, HandlerDefinition> handlerMap = new HashMap<>();
```

### HandlerDefinition
+ 与Controller一起注册，用于记录@RequestMapping方法(包括方法本身、对应的controller名、方法的参数、对应的controller.class、处理的url)

```java
    private Method method;
    private String beanName;
    private Parameter[] params;
    private Class<?> beanType;
    private String url;
```

### InterceptorRegistry
+ 注册并维护所有拦截器信息(InterceptorDefinition)

```java
private final List<InterceptorDefinition> definitions = new ArrayList<>();
```

+ 其getRegistryByUrl(String url)函数返回所有成功匹配的InterceptorDefinition

```java
public InterceptorRegistry getRegistryByUrl(String url) {
    return new InterceptorRegistry(this.definitions.stream()
            .map(item -> {
                // 调用InterceptorDefinition.getInterceptor(String url)
                return item.getInterceptor(url);
            })
            .filter(item -> {
                // 为null则表示拦截器不匹配
                return item != null;
            })
            .collect(Collectors.toList()));
}
```

### InterceptorDefinition
保存拦截器(HandlerInterceptor)相关信息(拦截器本身、匹配路径、排除路径)
```java
    private final HandlerInterceptor interceptor;
    private List<String> includePatterns;
    private List<String> excludePatterns;
```

+ getInterceptor(String url)函数，如果路径匹配成功则返回InterceptorDefinition，不成功返回null
```java
public InterceptorDefinition getInterceptor(String url) {
    if (includePatterns != null && !patternsUrl(url, includePatterns)) {
        return null;
    }
    if (excludePatterns != null && patternsUrl(url, excludePatterns)) {
        return null;
    }
    return this;
}
```

# 配置中心
**ContainerInitializer**类通过@HandlesTypes注解关注所有WebMvcConfigurer及其实现类

+ 将关注的类中包含@Configuration注解的WebMvcConfigurer全部实例化，并放入WebMvcConfigurerComposite中，全部执行
> 当前只涉及InterceptorRegistry，以后可添加更多配置
```java
    List<WebMvcConfigurer> configurers = c.stream().filter(item -> {
            return item.isAnnotationPresent(Configuration.class);
        }).map(item -> {
            try {
                return (WebMvcConfigurer) item.getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return new WebMvcConfigurer() {
                };
            }
        }).collect(Collectors.toList());
    WebMvcConfigurerComposite composite = new WebMvcConfigurerComposite(configurers);
    InterceptorRegistry registry = new InterceptorRegistry();
    composite.addInterceptors(registry);
    ctx.setAttribute("InterceptorRegistry", registry);
```
