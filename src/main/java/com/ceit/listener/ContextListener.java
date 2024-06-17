package com.ceit.listener;

import com.ceit.bootstrap.ConfigLoader;
import com.ceit.bootstrap.DispatcherServlet;
import com.ceit.filter.WebConfigFilter;
import com.ceit.ioc.BeanFactory;
import com.ceit.jdbc.DruidUtil;
import com.ceit.utils.SearchOptionPageUtil;
import com.ceit.utils.SqlUtil;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.InputStream;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;

/**
 * @author: ko
 * @date: 21.07.26 09:36:13
 * @description: 监听servlet上下文
 */
public class ContextListener implements ServletContextListener {

	/**
	 * 容器初始化时加载系统配置，输出logo，初始化beanFactory
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {

		//加载系统配置已经在ContainerInitializer中做过了，Servlet容器支持热部署时可能多次进入这个函数，重新加载配置
		ConfigLoader.loadDefaultConfig();
		
		//其他参数
		SqlUtil.loadDefaultConfig();
		SearchOptionPageUtil.loadDefaultConfig();
		WebConfigFilter.LoadConfig();
		
		//显示Logo
		try (InputStream inputStream = this.getClass().getResourceAsStream("../defaultResource/hello.txt")) {
			int length = inputStream.available();
			byte[] bytes = new byte[length];
			if (inputStream.read(bytes) != -1) {
				System.out.println("\n" + new String(bytes, "UTF-8"));
			}
		} catch (Exception e) {
		}
		
		//加载数据库连接
		DruidUtil.LoadDataSource();

		//加载DispatcherServlet的默认不处理的url列表
		DispatcherServlet.loadIgnoreUrlSetings(null);
		DispatcherServlet.loadWritelogUrlSetings(null);

		System.out.println(" ----------------------------------- ");
		//加载各种Bean
		BeanFactory beanFactory = BeanFactory.getInstance();
		@SuppressWarnings("unchecked")
		List<String> paths = (List<String>) sce.getServletContext().getAttribute("ScanPath");
		for (String path : paths) {
			beanFactory.registryBeans(path);
		}
		sce.getServletContext().removeAttribute("ScanPath");

		// 执行一次性任务和定时任务
		beanFactory.StartTaskTimers();
		
		//是否使用自定义的HttpSessionManager
		String enableHttpSessionManager =ConfigLoader.getConfig("HttpSessionManager.enabled");
		if("1".equals(enableHttpSessionManager) || "true".equals(enableHttpSessionManager)) {
			System.out.println("启用自定义HttpSessionManager");
			//从数据库中加载默认的HttpSession配置
			HttpSessionManager.LoadDefaultSetting();
		}

	}

	/**
	 * 销毁时，释放所有JDBC资源，输出bye
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {

		System.out.println("\ncontextDestroyed\n");

		try {

			// 停止定时任务,销毁自定义的线程池对象
			BeanFactory beanFactory = BeanFactory.getInstance();
			beanFactory.StopTaskTimers();

			// com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
			// AbandonedConnectionCleanupThread.uncheckedShutdown();

			// 关闭数据库连接池
			DruidUtil.closeDataSource();

			// 清理MYSQL驱动
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			final Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
				final Driver driver = drivers.nextElement();
				// We deregister only the classes loaded by this application's classloader
				if (driver.getClass().getClassLoader() == cl) {
					try {
						DriverManager.deregisterDriver(driver);
					} catch (SQLException e) {
						event.getServletContext().log("JDBC Driver deregistration problem.", e);
					}
				}
			}

			// 关闭logback后台日志线程
			// LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			// loggerContext.stop();

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
	}
}
