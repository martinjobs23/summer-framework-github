package com.ceit.listener;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// @WebListener
public class HttpSessionManager implements HttpSessionListener,  HttpSessionAttributeListener {  

    private static Logger logger = LoggerFactory.getLogger(HttpSessionManager.class);

	private static boolean defaultConfigFromParameterTable = true;
	private static int defaultTimeout =0;
	
	//配置信息
	private static int timeout = 0;
	private static int maxCount = 0;

	private static String loginAttributeName =null;
	private static int maxLoginCount = 0;
	private static int maxUserLogin = 0;

	//统计信息
	private static AtomicInteger sessionCount = new AtomicInteger();;
	private static AtomicInteger loginCount= new AtomicInteger();
	
	private static final Map<String, HttpSession> sessionMap = new ConcurrentHashMap<String, HttpSession>();
	private static final Map<String, String> userMap = new ConcurrentHashMap<String, String>();
	
	private static final Timer checkTimeoutTimer = new Timer();
	
    // 参数  
    ServletContext sc;
    ArrayList list = new ArrayList(); 
    
    static {
    	StartCheckTimeoutTimer();
    }
    
    // 新建一个session时触发此操作  
	@Override
    public void sessionCreated(HttpSessionEvent se) {  
		
		HttpSession session = se.getSession();
		String id = session.getId();
		int count =0;
		
		if(sessionMap.containsKey(id))
		{
			count = sessionCount.get();
		}
		else
		{
			count = sessionCount.incrementAndGet();
		}
 
		sessionMap.put(id, session);
        logger.debug("New Session: " + id + " sessionCount="+count );

		if(timeout>0)
			session.setMaxInactiveInterval(timeout);
		else
			defaultTimeout = session.getMaxInactiveInterval();

		//判断是否超出maxcount
		CheckMaxCount(session);
    }

    // 销毁一个session时触发此操作 HttpSession#invalidate()时立即触发
	// timeout的会话不确定什么时候会调用该方法
	@Override
    public void sessionDestroyed(HttpSessionEvent se) {

		HttpSession session = se.getSession();
		String sessionId = session.getId();
		int count =0;

		logger.debug("sessionDestroyed: " + sessionId);

		if(loginAttributeName!=null)
		{
			Object loginValue = session.getAttribute(loginAttributeName);
			if(loginValue!=null)
			{
				RemoveUserLogin(session, loginValue.toString());
			}
		}

		if(sessionMap.containsKey(sessionId))
		{
			sessionMap.remove(sessionId);
			count = sessionCount.decrementAndGet();

			logger.debug("Delete Session: " + sessionId + " sessionCount="+count );
		}
		else
		{
			count = sessionCount.get();
			logger.debug("Delete Unexisted Session: " + sessionId + " sessionCount="+count );
		}



    }

    // 在session中添加对象时触发此操作，在list中添加一个对象
	@Override
    public void attributeAdded(HttpSessionBindingEvent sbe) {

		String name = sbe.getName();
		Object value = sbe.getValue();
		HttpSession session = sbe.getSession();

        System.out.println("AttributeAdded: "+ name);

		if( value ==null)
			return;

		//登录
		if(name.equals(loginAttributeName))
		{
			NewUserLogin(session, value.toString());

			//检查是否有多个会话
			CheckMaxLoginCount(session);
		}

    }

    // 修改、删除session中添加对象时触发此操作
	@Override
    public void attributeRemoved(HttpSessionBindingEvent sbe) {

		String name = sbe.getName();
		Object value = sbe.getValue();

        //System.out.println("MyListener attributeRemoved: "+ sbe.getName());

		if(value ==null)
			return;

		//登录
		if(name.equals(loginAttributeName))
		{
			RemoveUserLogin(sbe.getSession(), value.toString());
		}
    }

	@Override
    public void attributeReplaced(HttpSessionBindingEvent sbe) {
        System.out.println("MyListener attributeReplaced: "+ sbe.getName());
    }

	public static void NewUserLogin(HttpSession session, String username)
	{
		String oldSessionId = userMap.get(username);

		//如果只允许一个用户登录，踢掉之前登录session
		if(maxUserLogin >0)
		{
			if(oldSessionId !=null)
			{
				//FIXME: 允许多个会话


				//原来如果是多个
				String[] ids = oldSessionId.split(",");
				for(String id: ids)
				{
					logger.debug("Found Old Login Session: "+ id +" User: " + username + " loginCount="+ loginCount.get() );

					//删除原来的会话
					HttpSession oldSession = sessionMap.get(id);
					if(oldSession!=null)
					{
						oldSession.invalidate();
					}

				}

				userMap.remove(username);
				oldSessionId =null;
			}

		}

		String newSessionId = session.getId();

		//原来会话如果已经全部清除掉
		if(oldSessionId ==null) {
			userMap.put(username, newSessionId);
			loginCount.incrementAndGet();
		}
		else
		{
			//多个逗号隔开
			userMap.put(username, oldSessionId+","+newSessionId);
			loginCount.incrementAndGet();
		}

		logger.debug("Add Login Session: "+ newSessionId +" User: " + username + " loginCount="+ loginCount.get() );
	}

	public static void RemoveUserLogin(HttpSession session, String username)
	{
		String existedSessionId = userMap.get(username);

		//已经删除了，或者过期被替换了
		if(existedSessionId ==null || existedSessionId.length()==0)
		{
			// NewUserLogin 中先userMap.remove(username) 再 session.invalidate()
			// 会进入此处
			return;
		}

		//只有一个id,就是当前session
		if(existedSessionId.equals(session.getId()))
		{
			userMap.remove(username);
			loginCount.decrementAndGet();

			logger.debug("Remove Login Session: "+ existedSessionId +" User: " + username + " loginCount="+ loginCount.get() );

			return;
		}

		//多个id
		String toDeleteId = session.getId();
		boolean found =false;

		StringBuilder sb =new StringBuilder();
		String[] ids = existedSessionId.split(",");
		for(String id:ids)
		{
			if(id.equals(toDeleteId))
			{
				//存在要删除的Id
				found = true;
			}
			else
			{
				sb.append(id);
				sb.append(',');
			}
		}

		if(found ==false)
		{
			//没有找到当前session
			logger.debug("Remove Login Not Found Session: "+ toDeleteId +" User: " + username + " loginCount="+ loginCount.get() );
			return;
		}

		if(sb.length() > 1)
		{
			//去掉逗号
			sb.deleteCharAt(sb.length()-1);

			//删掉一个id
			userMap.put(username, sb.toString());
		}
		else
		{
			userMap.remove(username);
		}

		loginCount.decrementAndGet();
		logger.debug("Remove Login Session: "+ toDeleteId +" User: " + username + " loginCount="+ loginCount.get() );

	}

	public static void CheckMaxCount(HttpSession newSession)
	{
		if(maxCount>0)
		{
			int count =sessionCount.get();
			if(count > maxCount )
			{
				//删除最新的还是最久的？
				if(newSession!=null) {
					logger.debug("CheckMaxCount sessionCount="+sessionCount
							+" > maxCount="+maxCount
							+" invalidate Session "+newSession.getId());
					newSession.invalidate();
				}
				else
				{
					//FIXME: 随机删除一个？
				}
			}
		}
	}

	public static void CheckMaxLoginCount(HttpSession newSession)
	{
		if(maxLoginCount>0)
		{
			int count = loginCount.get();
			if(count > maxLoginCount )
			{
				//删除最新的还是最久的？
				if(newSession!=null) {
					logger.debug("CheckMaxLoginCount loginCount="+loginCount
							+" > maxLoginCount="+maxLoginCount
							+" invalidate Session "+newSession.getId());
					newSession.invalidate();
				}
				else
				{
					//FIXME: 随机删除一个？
				}
			}
		}
	}

	public static void StopCheckTimeoutTimer()
	{
		checkTimeoutTimer.cancel();
	}

	public static void StartCheckTimeoutTimer()
	{
		//每10s钟检测一次
		System.out.println("MyListener StartCheckTimeoutTimer" );

		checkTimeoutTimer.schedule(new TimerTask() {

			@Override
			public void run() {

				if(timeout==0)
					return;

				long lastAlive = System.currentTimeMillis() - timeout*1000;

				Iterator<Entry<String, HttpSession>> iter =sessionMap.entrySet().iterator();
				while(iter.hasNext()) {
					Entry<String, HttpSession> entry = iter.next();
					HttpSession session = entry.getValue();

			        try {

						if(session.getLastAccessedTime() < lastAlive) {

							/*
							System.out.println("currentTimeMillis="+ System.currentTimeMillis()
							+" timeout*1000="+ timeout*1000
							+" lastAlive="+ lastAlive
							+" getLastAccessedTime="+ session.getLastAccessedTime() );
							*/

							logger.debug("Session Timeout id "+ session.getId());
							session.invalidate();
						}
			        }
			        catch(Exception error)
			        {
			        	//会话已失效,直接删除 不用处理
			        	sessionMap.remove(entry.getKey());
			        }

				}

			}

		}, 1000, 10000);
	}

	private static boolean LoadDefaultSettingFromParameterTable() {

		//从数据库中读取默认配置
		SimpleJDBC jdbc = SimpleJDBC.getInstance();
		if(jdbc ==null)
			return false;

		int value;

		//测试一下有没有表
		value = jdbc.selectForOneInt(-1, "select count(*) from parameter", null);
		if(value == -1)
			return false;

		String sql ="select value from parameter where MYID=? and STATE=1";

		//默认超时时间
		value = jdbc.selectForOneInt(-1,sql, "httpsession.timeout");
		if(value > 0)
			timeout = value;

		//最大会话数
		value = jdbc.selectForOneInt(-1,sql,"httpsession.maxcount");
		if(value > 0)
			maxCount = value;

		//最大登录会话数
		value = jdbc.selectForOneInt(-1,sql,"httpsession.maxlogincount");
		if(value > 0)
			maxLoginCount = value;

		//最大登录会话数
		value = jdbc.selectForOneInt(-1,sql,"httpsession.maxuserlogin");
		if(value > 0)
			maxUserLogin = value;

		//登录属性名
		String str = jdbc.selectForOneString(sql,"httpsession.loginname");
		if(str!=null)
			loginAttributeName = str;

		return true;
	}

	private static boolean LoadDefaultSettingFromSysconfigTable() {

		//从数据库中读取默认配置
		SimpleJDBC jdbc = SimpleJDBC.getInstance();
		if(jdbc ==null)
			return false;

		int value;

		//测试一下有没有表
		value = jdbc.selectForOneInt(-1, "select count(*) from sys_config", null);
		if(value == -1)
			return false;

		String sql ="select config_value from sys_config where config_item =?";

		//默认超时时间
		value = jdbc.selectForOneInt(-1,sql, "httpsession.timeout");
		if(value > 0)
			timeout = value;

		//最大会话数
		value = jdbc.selectForOneInt(-1,sql,"httpsession.maxcount");
		if(value > 0)
			maxCount = value;

		//最大登录会话数
		value = jdbc.selectForOneInt(-1,sql,"httpsession.maxlogincount");
		if(value > 0)
			maxLoginCount = value;

		//最大登录会话数
		value = jdbc.selectForOneInt(-1,sql,"httpsession.maxuserlogin");
		if(value > 0)
			maxUserLogin = value;

		//登录属性名
		String str = jdbc.selectForOneString(sql,"httpsession.loginname");
		if(str!=null)
			loginAttributeName = str;

		return true;
	}

	public static void LoadDefaultSetting() {

		boolean result = false;
		//尝试读取配置表
		if(defaultConfigFromParameterTable) {
			result = LoadDefaultSettingFromParameterTable();
			if(!result) {
				result = LoadDefaultSettingFromSysconfigTable();
				defaultConfigFromParameterTable = !result;
			}
		} else {
			result = LoadDefaultSettingFromSysconfigTable();
			if(!result) {
				result = LoadDefaultSettingFromParameterTable();
				defaultConfigFromParameterTable = result;
			}
		}

		//打印默认配置
		if(result) {
			logger.info("加载 HttpSessionManager 默认配置成功：");
			logger.info("httpsession.timeout="+timeout);
			logger.info("httpsession.maxCount="+maxCount);
			logger.info("httpsession.maxLoginCount="+maxLoginCount);
			logger.info("httpsession.maxUserLogin="+maxUserLogin);
			logger.info("httpsession.loginAttributeName="+loginAttributeName);
		}
		else {
			logger.error("加载 HttpSessionManager 默认配置失败");
		}
	}
	
	public static int SetSessionTimeout(int newSessionTimeout) {
		int oldvalue = timeout;
		if(timeout ==0 )
			oldvalue = defaultTimeout;
		
		timeout =newSessionTimeout;
		
		return oldvalue;
	}
}
