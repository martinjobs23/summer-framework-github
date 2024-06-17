# 更新日志

[框架逻辑](src/main/java/com/ceit/README.md)

更新时间及更新内容

## 2021.8.2

+ 增加**SqlUtil**，用于简化sql流程

## 2021.7.28

+ 增加**拦截器**以及**配置中心**接口

+ 自动执行所有配置

#### HandlerInterceptor

```java
public interface HandlerInterceptor{
     /**
     * 执行前拦截请求判断是否可执行
     */
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler) {
        return true;
    }

    /**
     * 实际上发生于执行handler之前，用于请求的前置处理
     */
    default void postHandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler,
            Map<String, Object> reqBody) {

    }

    /**
     * 请求完成后的后置处理
     */
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler,
            Object result) {

    }
}
```

#### WebMvcConfigurer

```java
public interface WebMvcConfigurer {

    default void addInterceptors(InterceptorRegistry registry) {
    }
}
```

## 2021.7.25

+ 删除**EntryServlet**，入口改为**ContextListener**

+ **DBPool**增加资源释放功能

+ 增加启动及销毁logo

+ 增加**ContainerInitializer**，用于自动初始化framework相关组件

+ 删除web.xml

+ 新增**ContextListener**，用于监听servlet容器上下文，统一加载和销毁资源

## 2021.7.23

+ 删除**TestController**

+ **JDBCTemplate**不再提供开发接口

#### SimpleJDBC

+ 增加**selectForJsonObject**函数，返回json格式String

```java
public String selectForJsonObject(String sql, Object... obs);
```

+ 增加**selectForJsonArray**函数，返回json数组格式String

```java
public String selectForJsonArray(String sql, Object... obs);
```

+ 修改函数各函数资源释放时机

## 2021.7.22

共有 更新

#### SimpleJDBC

+ **select**函数返回结果改为json格式String

```java
public String select(String sql, Object... obs);
```

+ 增加**selectForMap**函数，将一行数据以Map格式输出

```java
public Map<String, Object> selectForMap(String sql, Object... obs);
```

+ 增加**selectForList**函数，将多行数据以List格式输出

```java
public List<Map<String, Object>> selectForList(String sql, Object... obs);
```

+ 增加**selectForIndexObject**函数，获得第一行的第**index**条数据

```java
public Object selectForIndexObject(int index, String sql, Object... obs);
```

#### JSON

+ **resultSet2JsonObject**函数逻辑修改

除基本数据类型、包装类以及null外，所有数据类型均加"双引号"

+ 增加**map2Json**函数，将Map格式转化为Json字符串

```java
public String map2Json(Map<String, Object> map);
```

+ 增加**isPrimitive**函数，判断是否为基本数据类型及其包装类

```java
public boolean isPrimitive(Object obj);
```

+ **inputstream2Map**函数读取时采用UTF-8格式

#### DBPool
+ 增加JDBC连接时间(毫秒)设置，参数为**jdbc.defaultConnectionTimeout**

```java
Integer defaultConnectionTimeout = Integer.valueOf(System.getProperty("jdbc.defaultConnectionTimeout"));
if (defaultConnectionTimeout != null) {
    conn.setNetworkTimeout(Executors.newFixedThreadPool(3), defaultConnectionTimeout);
}
```

#### Assert

+ 增加**isTrue**函数，判定条件为真

```java
public static void isTrue(boolean expression);
```