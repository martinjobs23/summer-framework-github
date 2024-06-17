package com.ceit.jdbc;
 
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.ceit.bootstrap.ConfigLoader;
import com.ceit.utils.SM4;

/*
Druid连接池的工具类
*/

public class DruidUtil {
    //1.定义成员变量 DataSource
    private static DataSource ds = null;

	private static Map<String, DataSource> otherDataSourceMap = new ConcurrentHashMap<String, DataSource>();
	
	private static String decryptPwd(String encryptMode,String encryptedPwd)
	{
		String decrypt ="";
		
		if(encryptMode.length() > 0)
		{
			String key = "2d984a8b7623812bfb0a0491f77d8d0c";
	        try {
				decrypt = SM4.decrypt(key, encryptedPwd);
				/*
				System.out.println("key: " +key);
				System.out.println("encryptedPwd: " +encryptedPwd);
				System.out.println("decrypt: " +decrypt);
				*/
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return decrypt;
	}
	
    //等待配置文件加载完毕再加载数据库
    public static void LoadDataSource()
    {
    	closeDataSource();
    	
    	String encryptMode="";
    	String encryptedPwd="";
    	
    	//1.加载配置文件
        Properties properties = ConfigLoader.getProperties();
        try {
            
        	//属性 jdbc.xxx =>  xxx
    		//迭代器
            Enumeration<?> enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
            	String key =  (String)enumeration.nextElement();
            	String value =  (String)properties.get(key);

            	if(key.startsWith("jdbc."))
            	{
            		properties.remove(key);
            		
            		if(key.equals("jdbc.pwd") || key.equals("jdbc.password")  )
            		{
            			encryptedPwd = value;
            			System.out.println( key+ "=******* ");
            		}
            		else if(key.equals("jdbc.passencypted"))
            		{
            			encryptMode = value;
            			System.out.println(key+ "=" + value);
            		}
            		else
            		{
                		properties.setProperty(key.substring(5), value);
            			System.out.println(key+ "=" + value);
            		}

            	}
            }
            
            //密码是否需要解密
            if(encryptMode.length()>0)
            {
            	String decryptedPwd = decryptPwd(encryptMode, encryptedPwd);
            	properties.setProperty("password", decryptedPwd);
            	
            	encryptMode="";
            	encryptedPwd="";
            }else {
                properties.setProperty("password", encryptedPwd);
            }
            	
            //获取DataSource
            ds = DruidDataSourceFactory.createDataSource(properties);
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //获取连接
    
    public static Connection getConnection() throws SQLException {
    	if(ds !=null)
    		return ds.getConnection();
    	else
			return null;
    }

    public static Connection getConnection(String jdbcDriver, String jdbcUrl, String jdbcUsername, String jdbcPassword) throws SQLException {
		//是否存在
		DataSource datasoure = getDataSource(jdbcDriver,jdbcUrl,jdbcUsername,jdbcPassword);
    	if(datasoure !=null)
    		return datasoure.getConnection();
    	else
			return null;
    }
    
    public static void close(Statement stmt)
    {
    	Connection conn =null;
    	
        if (stmt!=null) {
            try {
            	conn= stmt.getConnection();
                stmt.close();
            } catch (Exception e) {
            	System.out.println("DruidUtil close stmt erorr: " + e.getMessage());
            }
        }
        
        if (conn!=null){
            try {
                conn.close();//归还连接
            } catch (Exception e) {
                System.out.println("DruidUtil close conn erorr: " + e.getMessage());
            }
        }
    }
    
    //释放资源
    public static void close(Statement stmt,Connection conn){
    	
        if (stmt!=null) {
            try {
                stmt.close();
            } catch (Exception e) {
                System.out.println("DruidUtil close stmt conn erorr: " + e.getMessage());
            }
        }
        
        if (conn!=null){
            try {
                conn.close();//归还连接
            } catch (Exception e) {
                System.out.println("DruidUtil close conn erorr: " + e.getMessage());
            }
        }
    }
    
    public static void close(ResultSet rs, Statement stmt, Connection conn){
        if (rs!=null) {
            try {
                rs.close();
            } catch (Exception e) {
            	System.out.println("DruidUtil close ResultSet erorr: " + e.getMessage());
            }
        }
        if (stmt!=null) {
            try {
                stmt.close();
            } catch (Exception e) {
            	System.out.println("DruidUtil close Statement erorr: " + e.getMessage());
            }
        }
        if (conn!=null){
            try {
                conn.close();//归还连接
            } catch (Exception e) {
            	System.out.println("DruidUtil close Connection erorr: " + e.getMessage());
            }
        }
    }
    //获取连接池
    public static DataSource getDataSource(){
        return ds;
    }
    
    //获取连接池
    public static DataSource getDataSource(String jdbcDriver, String jdbcUrl, String jdbcUsername, String jdbcPassword){
        
		//是否存在
    	String key = jdbcDriver +"\n" + jdbcUrl +"\n" + jdbcUsername;
    	
		DataSource datasoure = otherDataSourceMap.get(key);
		if(datasoure!=null)
			return datasoure;
		
		//新建连接池
		Properties properties =new Properties();

		properties.setProperty("driverClassName", jdbcDriver);
		properties.setProperty("url", jdbcUrl);
		properties.setProperty("username", jdbcUsername);
		properties.setProperty("password", jdbcPassword);

		System.out.println("getDataSource driverClassName="  + jdbcDriver);
		System.out.println("getDataSource url="  + jdbcUrl);
		System.out.println("getDataSource username="  + jdbcUsername);
		System.out.println("getDataSource password=*******");

        //获取DataSource
		try {
			datasoure = DruidDataSourceFactory.createDataSource(properties);
			otherDataSourceMap.put(key, datasoure);		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	return datasoure;
    }
    
    //销毁连接池
    public static void closeDataSource() {
 
        if(ds!=null)
        {
        	DruidDataSource dataSource = (DruidDataSource) ds;
        	
        	// 获取Druid连接池的监控信息
        	 // 判断是否还有活动连接
        	Set<DruidPooledConnection> set = dataSource.getActiveConnections();
        	for(DruidPooledConnection conn : set)
        	{
        		try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	
        	dataSource.close();
        }
 
    }
 
}
