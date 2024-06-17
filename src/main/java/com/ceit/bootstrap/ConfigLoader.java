package com.ceit.bootstrap;

import com.ceit.jdbc.DebugShowSql;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: ko
 * @date: 21.06.21 16:00:34
 * @description: 通用{@link Properties}文件加载器
 */
public class ConfigLoader {

	private static AtomicInteger loadCount= new AtomicInteger();

	private static Properties props = new Properties();
	
	public static Properties getProperties() {
		return props;
	}
	
	public static String getConfig(String name) {
		return (String)props.get(name);
	}

	public static void setConfig(String name, String value) {

		System.out.println("ConfigLoader setConfig: " + name + "=" + value);
		props.put(name, value);

		//重新加载忽略处理请求URL
		DispatcherServlet.loadIgnoreUrlSetings(name);

		//重新加载 writeLog URL
		DispatcherServlet.loadWritelogUrlSetings(name);

		//重新加载SQL日志
		DebugShowSql.loadDebugShowSql(name);
	}

	public static void removeConfig(String name) {

		System.out.println("ConfigLoader removeConfig: " + name);
		props.remove(name);

		//重新加载忽略处理请求URL
		DispatcherServlet.loadIgnoreUrlSetings(name);

		//重新加载 writeLog URL
		DispatcherServlet.loadWritelogUrlSetings(name);

		//重新加载SQL日志
		DebugShowSql.loadDebugShowSql(name);
	}
	
    /**
     * 根据指定路径加载{@link Properties}文件
     */
    public static void load(String path) {
        try {
            InputStream ips = new FileInputStream(path);
            props.clear();
            props.load(ips);
            ips.close();
        } catch (Exception e) {
            e.printStackTrace();
            // 初始化失败直接关闭程序
            System.exit(0);
        }
    }

    //获取war包之外的配置文件
    private static File getWebRootConfigFile(String classesPath)
    {
    	// /ywaqsj/web/summer/WEB-INF/classes/
    	// /D:/apache-tomcat-9.0.74/webapps/bastion-summer/WEB-INF/classes/

    	int index = classesPath.lastIndexOf("/WEB-INF/classes");
    	if(index <1)
    		return null;
    	
    	//查找
    	index = classesPath.lastIndexOf("/", index -1 );
    	if(index <1)
    		return null;
    	
    	//如果存在
    	String webrootPath =classesPath.substring(0, index);
    	File file = findConfigFileInDir(webrootPath);
    	if(file!=null)
    		return file;
    	
    	System.out.println("loadDefaultConfig: 目录" + webrootPath +"中不存在.properties: ");

    	//上一级目录
    	index = classesPath.lastIndexOf("/", index -1 );
    	if(index <0)
    		return null;
 
    	webrootPath =classesPath.substring(0, index);
    	file = findConfigFileInDir(webrootPath);
    	if(file!=null)
    		return file;
 
    	System.out.println("loadDefaultConfig: 目录" + webrootPath +"中不存在.properties: ");
    	
    	return file;
    }
    
    private static File findConfigFileInDir(String dir)
    {
    	if(dir==null|| dir.trim().length()==0)
    		return null;
    	
    	File webrootDir = new File(dir);
    	if(webrootDir.exists())
    	{
    		System.out.println("loadDefaultConfig: check dir " +webrootDir.getAbsolutePath() );
    		
    		//优先查找 application.properties 文件
    		File configFile = new File(webrootDir.getAbsolutePath(),"application.properties" );
    		if(configFile.exists() && configFile.isFile())
    		{
    			return configFile;
    		}
    		
    		//查找其他.properties 文件
            String[] list = webrootDir.list();
            for (String filename : list){
            	
            	if(filename.endsWith(".properties"))
            	{
            		File file = new File(dir,filename);
                    if(file.isFile()) {
                  		return file;
                    }
            	}
            }
    	}
    	
    	return null;
    }
    /**
     * 加载系统默认配置
     */
    public static void loadDefaultConfig() {

		//第一次是Web容器加载，第二次是Context初始化不需要重复，以后Context重置加载
		int count = loadCount.incrementAndGet();
		if(count==1) {
			return;
		}

        String path = ConfigLoader.class.getResource("/").getPath();;
        if(path==null)
        	return;
        
        System.out.println("loadDefaultConfig: path=" +path);
        
        //先查找外部配置
        File configFile = getWebRootConfigFile(path);
        if(configFile !=null)
        {
        	System.out.println("loadDefaultConfig: 加载配置文件" + configFile.getAbsolutePath());
        	load(configFile.getAbsolutePath());
        	return;
        }
 
        //查找当前war解压后的路径
        configFile = findConfigFileInDir(path);
        if(configFile !=null)
        {
        	System.out.println("loadDefaultConfig: 加载配置文件" + configFile.getAbsolutePath());
        	load(configFile.getAbsolutePath());
        	return;
        }
        else
        {
        	System.out.println("loadDefaultConfig: clasess目录" + path +"中不存在.properties: ");
        }
 
    }
}
