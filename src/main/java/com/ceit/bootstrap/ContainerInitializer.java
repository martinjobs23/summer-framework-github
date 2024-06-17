package com.ceit.bootstrap;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
 
import com.ceit.config.WebMvcConfigurer;
import com.ceit.config.WebMvcConfigurerComposite;
import com.ceit.interceptor.InterceptorRegistry;
import com.ceit.filter.WebConfigFilter;
import com.ceit.ioc.annotations.Configuration;
import com.ceit.listener.ContextListener;
import com.ceit.listener.HttpSessionManager;

/**
 * @author: ko
 * @date: 21.07.27 17:03:01
 * @description: 容器初始化入口
 */
@HandlesTypes(value = { WebMvcConfigurer.class })
public class ContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {

        System.out.println("ContainerInitializer onStartup:  version " + Version.version);
        System.out.println("summer-framework git_version:    " + Version.git_version);
        System.out.println("summer-framework git_logcount:   " + Version.git_logcount);
        System.out.println("summer-framework git_datetime:   " + Version.git_version);
        System.out.println("summer-framework build_datetime: " + Version.build_datetime);

        if (Version.git_version.startsWith("$") || Version.git_datetime.startsWith("$") ) {
            System.out.println("未知的git_version和git_datetime版本信息，不允许运行，请重新配置编译");
            throw new ServletException("未知的git_version和git_datetime版本信息，不允许运行，请重新配置编译");
        }

        //加载系统配置
        ConfigLoader.loadDefaultConfig();

        // 注册三个组件
        ServletRegistration.Dynamic servlet = ctx.addServlet("DispatcherServlet", DispatcherServlet.class);
        servlet.addMapping("/");

        ctx.addListener(ContextListener.class);

        //是否使用自定义的HttpSessionManager
		String enableHttpSessionManager =ConfigLoader.getConfig("HttpSessionManager.enabled");
		if("1".equals(enableHttpSessionManager) || "true".equals(enableHttpSessionManager)) {
            System.out.println("ContainerInitializer onStartup: Add SessionListener");
            ctx.addListener(HttpSessionManager.class);
		}

        FilterRegistration.Dynamic filter = ctx.addFilter("WebConfigFilter", WebConfigFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        // 自动执行全部WebMvcConfigurer，并将全部拦截器等注册进容器上下文
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
        // 注册拦截器
        WebMvcConfigurerComposite composite = new WebMvcConfigurerComposite(configurers);
        InterceptorRegistry registry = new InterceptorRegistry();
        composite.addInterceptors(registry);
        ctx.setAttribute("InterceptorRegistry", registry);
        // 扫描路径
        List<String> paths = new ArrayList<>(10);
        paths.add("com.ceit.json");
        paths.add("com.ceit.jdbc");
        composite.addScanPath(paths);
        paths.add(ConfigLoader.class.getResource("/").getPath());
        ctx.setAttribute("ScanPath", paths);
    }

}
