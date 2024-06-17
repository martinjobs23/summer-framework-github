package com.ceit.jdbc;

import java.lang.reflect.Array;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceit.ioc.annotations.Component;
import com.ceit.json.JSON;

/**
 * 提供更清晰的JDBC执行方法
 * <p>
 * 可直接代替{@link SimpleJDBC}
 */
@Component
public class SimpleJDBC {

    private static Logger logger = LoggerFactory.getLogger(SimpleJDBC.class);
    private JSON json = new JSON();

    private SimpleJDBC() {
    }

    // 如果参数obj 是 String, String[], List 展开
    private Object[] expandArrayParams(Object... obs)
    {
        if(obs==null || obs.length==0)
            return null;

    	boolean needExpand =false;
    	for(Object obj: obs)
    	{
			if(obj==null)
				continue;

    		if(obj.getClass().isArray())
    		{
    			//数组
    			if(Array.getLength(obj)==0)
    				return null;

    			needExpand =true;
    			break;
    		}
    		else if (obj instanceof Collection) {

    			//List,ArrayList,LinkedList
    			if(obj==null || ((Collection)obj).size()==0)
    				return null;

    			needExpand =true;
    			break;
            }
    	}

    	if(needExpand==false)
    		return obs;

    	//扩展
    	List list =new java.util.ArrayList();
    	for(Object obj: obs)
    	{
			if(obj==null)
			{
	    		//添加null参数
				list.add(obj);
				continue;
			}
			
    		if(obj.getClass().isArray())
    		{
    			//数组
    	        int length = Array.getLength(obj);
    	        for (int idx = 0; idx < length; ++idx) {
    	            Object item = Array.get(obj, idx);
    	            list.add(item);
    	        }
    		}
    		else if (obj instanceof Collection) {
    			for(Object item: (Collection)obj)
    			{
    				list.add(item);
    			}
            }
    		else
    			list.add(obj);
    	}

    	return list.toArray();
    }

    /**
     * 获取操作数据库的对象
     *
     * @param sql
     *            sql语句
     * @param ob
     *            参数 可变
     * @return PreparedStatement
     */
    private PreparedStatement getStatement(String sql, Boolean returnGeneratedKey, Object... ob) {

    	Connection connection =null;
    	PreparedStatement preparedStatement = null;

    	// 加载驱动
        try {
            // 创建连接对象
            //Connection connection = getConnection();
        	if(jdbcDriver==null)
        		connection = DruidUtil.getConnection();
        	else
        		connection = DruidUtil.getConnection(jdbcDriver,jdbcUrl,jdbcUsername,jdbcPassword);

            // 创建执行对象
            if(returnGeneratedKey)
                preparedStatement= connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            else
                preparedStatement= connection.prepareStatement(sql);

            // 如果有参数 则添加参数
            if (ob != null && ob.length > 0) {
                for (int i = 0; i < ob.length; i++) {
                    preparedStatement.setObject(i + 1, ob[i]);
                }
            }

            return preparedStatement;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        if(preparedStatement==null && connection!=null)
        	DruidUtil.close(preparedStatement, connection);

        return null;
    }


    private void closeStatement(PreparedStatement stmt)
    {
    	DruidUtil.close(stmt);
    }

    private void closeResultSet(ResultSet rSet)
    {
        try {
        	DruidUtil.close(rSet.getStatement());
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(e.getMessage());
        }

    }

    private void closeResultSet(ResultSet rSet, PreparedStatement stmt )
    {
        if (rSet!=null) {
            try {
            	rSet.close();
            } catch (Exception e) {
                System.out.println("closeResultSet error: "+e.getMessage());
            }
        }

        DruidUtil.close(stmt);
    }

    /**
     * 查询单条数据，并按照json格式返回
     *
     * @param sql
     *            查询语句
     * @param obs
     *            参数变量
     * @return {@link String}
     */
    public JSONObject selectForJsonObject(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForJsonObject",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        JSONObject result = json.resultSet2JsonObject(rSet);

        closeResultSet(rSet);
        return result;
    }

    /**
     * 查询多条数据，并按照json数组格式返回
     *
     * @param sql
     * @param obs
     * @return
     */
    public JSONArray selectForJsonArray(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForJsonArray",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        JSONArray jsonArray = json.resultSet2JsonArray(rSet);

        closeResultSet(rSet);
        return jsonArray;
    }

    /**
     * 以{@link Map}格式返回一条查询结果
     */
    public Map<String, Object> selectForMap(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForMap",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (rSet.next()) {
                result = row2Map(rSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeResultSet(rSet);
        return result;
    }

    /**
     * 返回{@link ResultSet}中第一个元素的列表
     */
    public List<Object> selectForList(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForOneNode",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        List<Object> list = new ArrayList();
        try {
            while (rSet.next()) {
                list.add(rSet.getObject(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            list = null;
        } finally {
        	closeResultSet(rSet);
        }
        return list;
    }

    /**
     * 多条返回结果时以list形式返回查询结果
     */
    public List<Map<String, Object>> selectForMapList(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForList",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            while (rSet.next()) {
                result.add(row2Map(rSet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	closeResultSet(rSet);
        }
        return result;
    }

    /**
     * 多条返回结果时以list形式返回查询结果，方便查找,keyField 不能重复，否则后面的值会覆盖前面
     */
    public Map<Object, Map<String, Object>> selectForListMap(String sql, String keyField, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForList",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        Map<Object, Map<String, Object>> result = new LinkedHashMap<Object, Map<String, Object>>();
        try {
            while (rSet.next()) {
                result.put(rSet.getObject(keyField), row2Map(rSet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	closeResultSet(rSet);
        }
        return result;
    }

    /**
     * 多条返回结果时以list形式返回查询结果，方便查找,keyField 不能重复，否则后面的值会覆盖前面
     */
    public Map<Object, Map<String, Object>> selectForMapMap(String sql, String keyField, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForList",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        Map<Object, Map<String, Object>> result = new LinkedHashMap<Object, Map<String, Object>>();
        try {
            while (rSet.next()) {
                result.put(rSet.getObject(keyField), row2Map(rSet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	closeResultSet(rSet);
        }
        return result;
    }

    /**
     * 将{@link ResultSet}的当前行转化成map
     */
    private Map<String, Object> row2Map(ResultSet rSet) {
        if (rSet == null)
            return null;

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ResultSetMetaData resultSetMetaData = rSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                result.put(resultSetMetaData.getColumnLabel(i), rSet.getObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 返回{@link ResultSet}中第一行、第一个元素
     */
    public Object selectForOneNode(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC selectForOneNode",sql,obs);

        ResultSet rSet = select(sql, obs);
        if (rSet == null)
            return null;

        Object obj = null;
        try {
            if (rSet.next()) {
                obj = rSet.getObject(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	closeResultSet(rSet);
        }
        return obj;
    }

    /**
     * 返回{@link ResultSet}中第一行、第一个元素，字符串。错误，返回null
     */
    public String selectForOneString(String sql, Object... obs) {

        Object obj = selectForOneNode(sql, obs);

        if (obj != null)
            return obj.toString();

        return null;
    }

    /**
     * 返回{@link ResultSet}中第一行、第一个元素，字符串。
     * 错误时，返回 defaultValue
     */
    public int selectForOneInt(int defaultValue, String sql, Object... obs) {

        Object obj = selectForOneNode(sql, obs);

        if (obj == null)
            return defaultValue;

        if (obj instanceof Integer) {
            return (Integer)obj;
        }
        else if (obj instanceof Long) {
            return ((Long)obj).intValue();
        }

        try {
            if (obj instanceof String) {
                return Integer.parseInt((String)obj);
            }
            else {
                return Integer.parseInt(obj.toString());
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 返回{@link ResultSet}中第一行、第一个元素，字符串。
     * 错误时，返回-1 。
     * 如果-1 可能是正常的结果值，不要使用该函数，请使用带defaultValue参数的函数。
    */
    public int selectForOneInt(String sql, Object... obs) {
        return selectForOneInt(-1, sql, obs);
    }


    /**
     * 返回{@link ResultSet}中第一行、第一个元素，字符串。
     * 错误时，返回defaultValue
     */
    public long selectForOneLong(long defaultValue,String sql, Object... obs) {

        Object obj = selectForOneNode(sql, obs);

        if (obj == null)
            return defaultValue;

        if (obj instanceof Long) {
            return (Long)obj;
        }
        else if (obj instanceof Integer) {
            return ((Integer)obj).longValue();
        }

        try {
            if (obj instanceof String) {
                return Long.parseLong((String)obj);
            }
            else {
                return Long.parseLong(obj.toString());
            }
        } catch (Exception e) {
            return defaultValue;
        }

    }

    /**
     * 返回{@link ResultSet}中第一行、第一个元素，字符串。
     * 错误时，返回-1。
     * 如果-1 可能是正常的结果值，不要使用该函数，请使用带defaultValue参数的函数。
     */
    public long selectForOneLong(String sql, Object... obs) {
        return selectForOneLong(-1, sql, obs);
    }

    /**
     * 查询 返回查询的结果集合
     *
     * @param sql
     *            sql语句
     * @param obs
     *            可变参数
     * @return ResultSet结果集合
     */
    protected ResultSet select(String sql, Object... obs) {

    	Object[] params=expandArrayParams(obs);
    	PreparedStatement statement =null;

        DebugShowSql.println("SimpleJDBC select",sql,params);

        try {

            statement = getStatement(sql, false, params);
            if(statement==null)return null;

            ResultSet rSet = statement.executeQuery();
            return rSet;
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(e.getMessage());
        }

        closeStatement(statement);

        return null;
    }

    /**
     * 对数据库的增、删、改
     *
     * @param sql
     *            sql语句
     * @param obs
     *            可变参数
     * @return 操作完成的sql语句数量
     */
    public int update(String sql, Object... obs) {

    	Object[] params=expandArrayParams(obs);

        DebugShowSql.println("SimpleJDBC update",sql,params);

        PreparedStatement statement =null;

        // 执行成功的条数
        int count = -1;
        try {
            statement = getStatement(sql, false, params);
            if(statement==null)return -1;

            count = statement.executeUpdate();
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(e.getMessage());
        }

        closeStatement(statement);
        return count;
    }

    private Object updateAutoIncKeyObj(String sql, Object... obs) {

        // DebugShowSql.println("SimpleJDBC update",sql,obs);

    	Object[] params=expandArrayParams(obs);

        DebugShowSql.println("SimpleJDBC updateAutoIncKeyObj",sql,params);

        PreparedStatement statement =null;
        ResultSet rs =null;
        // 执行成功的条数
        Object resut = null;
        try {
            statement = getStatement(sql, true, obs);
            if(statement==null)return -1L;

            statement.executeUpdate();
            rs = statement.getGeneratedKeys();                                  // 获取自增主键！
            if (rs!=null && rs.next()) {

            	resut = rs.getObject(1);

            }  else {
                // throw an exception from here
            }

        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(e.getMessage());
        }


        closeResultSet(rs, statement);
        return resut;
    }

    /**
     * insert语句返回自动生成的主键值
     *
     * @param sql
     *            查询语句
     * @param obs
     *            参数变量
     * @return {@link Integer}
     */
    public int updateAutoIncKey(String sql, Object... obs) {

    	Object obj = updateAutoIncKeyObj(sql, obs);
    	Integer resut = -1;

    	if(obj instanceof Long)
    		resut = ((Long)obj).intValue();
    	else if(obj instanceof Integer)
    		resut = ((Integer)obj);
    	else {
    		resut = Integer.parseInt(obj.toString());
    	}

        return resut;
    }

    /**
     * insert语句返回自动生成的主键值Long型
     *
     * @param sql
     *            查询语句
     * @param obs
     *            参数变量
     * @return {@link Long}
     */
    public Long updateAutoIncKeyLong(String sql, Object... obs) {

    	Object obj = updateAutoIncKeyObj(sql, obs);
    	Long resut = -1L;

    	if(obj instanceof Long)
    		resut = ((Long)obj);
    	else if(obj instanceof Integer)
    		resut = ((Long)obj).longValue();
    	else {
    		resut = Long.parseLong(obj.toString());
    	}

        return resut;
    }

    //JVM在加载类的时候，会初始化类里的静态变量，或执行静态块，如果这个时候抛出了异常，该类就会加载失败，那么以后任何使用到这个类的地方，都会抛出NoClassDefFoundError异常
    //java.lang.NoClassDefFoundError: Could not initialize class com.ceit.jdbc.SimpleJDBC$SimpleJDBCCreator
    private static class SimpleJDBCCreator {
        private static final SimpleJDBC SIMPLE_JDBC = new SimpleJDBC();
    }

    public static SimpleJDBC getInstance() {
        return SimpleJDBCCreator.SIMPLE_JDBC;
    }

	private static Map<String, SimpleJDBC> otherJdbcMap = new ConcurrentHashMap<String, SimpleJDBC>();
	private String jdbcDriver =null;
	private String jdbcUrl =null;
	private String jdbcUsername =null;
	private String jdbcPassword =null;

    //用户指定的数据库连接缓冲池
    public static SimpleJDBC getInstance(String jdbcDriver, String jdbcUrl, String jdbcUsername, String jdbcPassword) {

    	String key = jdbcDriver +"\n" + jdbcUrl +"\n" + jdbcUsername;

    	SimpleJDBC jdbc = otherJdbcMap.get(key);
    	if(jdbc !=null)
    		return jdbc;

    	//新加载一个JDBC
    	jdbc = new SimpleJDBC();
    	jdbc.jdbcDriver = jdbcDriver;
    	jdbc.jdbcUrl = jdbcUrl;
    	jdbc.jdbcUsername = jdbcUsername;
    	jdbc.jdbcPassword = jdbcPassword;

    	otherJdbcMap.put(key, jdbc);

    	return jdbc;
    }

    /**
     * 根据表名获取此表的所有字段名
     * @param tableName
     * @return
     */
    public List<String> getTableField(String tableName) {
        String sql = "SELECT\n" +
                "column_name\n" +
                "FROM information_schema.columns\n" +
                "WHERE TABLE_NAME = ?";
        ArrayList<String> list = null;
        try {
            PreparedStatement preparedStatement = DruidUtil.getConnection().prepareStatement(sql);
            preparedStatement.setObject(1,tableName);
            ResultSet resultSet = preparedStatement.executeQuery();
            list = new ArrayList<>();
            while (resultSet.next()){
                list.add(resultSet.getString(1));
            }
            closeResultSet(resultSet,preparedStatement);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * 根据主键key名（不是值），表名和json对象查找数据库表的记录，有返回1无返回0
     * @param key
     * @param tableName
     * @param object
     * @return
     */
    public int query(String key, String tableName, JSONObject object){
        PreparedStatement preparedStatement = null;
        String id = object.getString(key);
        ResultSet resultSet = null;
        int res = 0;
        if (id != null){
            String sql = "select "+key+" from "+tableName+" where "+key+" = ?";
            try {
                preparedStatement = DruidUtil.getConnection().prepareStatement(sql);
                preparedStatement.setObject(1,id);
                resultSet = preparedStatement.executeQuery();
                if(resultSet.next()){
                    res = 1;
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            closeStatement(preparedStatement);
        }
        return  res;
    }

    /**
     * 传入json对象，表名，表字段list和主键，插入json中的数据，json是单层结果无嵌套
     * @param jsonObj json对象
     * @param tableName 表名
     * @param fieldsList 表字段名
     * @param key 主键
     * @return
     */
    public int insert(JSONObject jsonObj,String tableName,List<String> fieldsList,String key,boolean under2camelFlag){
        if(under2camelFlag){
            key = under2camel(key);
        }
        if(jsonObj.get(key) == null){
            return -1;
        }
        
        if (fieldsList == null){
            return -1;
        }
        
        StringBuilder fieldsSb =new StringBuilder();
        StringBuilder valuesSb =new StringBuilder();
        
        List params =new ArrayList();
        //根据list的字段自动生成插入的sql语句
        for (int i = 0;i < fieldsList.size();i++){
        	String fieldName = fieldsList.get(i);
            String keyValue = fieldName;
            if(under2camelFlag){
                keyValue = under2camel(fieldName);
            }
            Object  value = jsonObj.get(keyValue);
            if(value!=null)
            {
                fieldsSb.append(fieldName +",");
                valuesSb.append("?,");
                params.add(value);
            }
        }
        
        if(fieldsSb.length() >0 )
        	fieldsSb.deleteCharAt(fieldsSb.length()-1);
        
        if(valuesSb.length() >0 )
        	valuesSb.deleteCharAt(valuesSb.length()-1);

        String sql = "insert into " + tableName+"(" 
        		+ fieldsSb.toString() 
        		+ ") values("
        		+ valuesSb.toString() 
        		+ ") ";
        return update(sql,params);
    }
    public int insert(JSONObject jsonObj,String tableName,List<String> fieldsList,String key){
        return insert(jsonObj,tableName,fieldsList,key,false);
    }
    /**
     * 传入json对象，表名，表字段list和主键，按json中的数据更新表中相关字段，json是单层结果无嵌套
     * @param jsonObj
     * @param tableName
     * @param fieldsList
     * @param key
     * @return
     */
    public int update(JSONObject jsonObj,String tableName,List<String> fieldsList,String key,boolean under2camelFlag){
        String key2 = key;
        if(under2camelFlag){
            key2 = under2camel(key);
        }

        if(jsonObj.get(key2) == null){
            return -1;
        }
         
        if (fieldsList == null){
            return -1;
        }
        
        List params =new ArrayList();
        String sql = "update " + tableName+" set ";
        
        //根据list的字段自动生成插入的sql语句
        for (int i = 0;i < fieldsList.size();i++){
        	String fieldName = fieldsList.get(i);
            String keyValue = fieldName;
            if(under2camelFlag){
                keyValue = under2camel(fieldName);
            }
            if(!keyValue.equals(key2)){
                Object value =jsonObj.get(keyValue);
            	if(value!=null)
            	{
                    sql += fieldName +"=?,";
                    params.add(value);
            	}
            }
        }

        sql = sql.substring(0,sql.length()-1);
        sql += " where "+key+"=?";
        params.add(jsonObj.get(key));
        return update(sql,params);
    }
    public int update(JSONObject jsonObj,String tableName,List<String> fieldsList,String key){
        return update(jsonObj,tableName,fieldsList,key,false);
    };
    //驼峰转下划线命名
    public  String camel2under(String c)
    {
        String separator = "_";
        c = c.replaceAll("([a-z])([A-Z])", "$1"+separator+"$2").toLowerCase();
        return c;
    }
    //下划线转驼峰

    private  String under2camel(String s)
    {
        String separator = "_";
        String under="";
        s = s.toLowerCase().replace(separator, " ");
        String sarr[]=s.split(" ");
        for(int i=0;i<sarr.length;i++)
        {
            String w=sarr[i].substring(0,1).toUpperCase()+sarr[i].substring(1);
            under +=w;
        }


        return lowerFirst(under);
    }
    /**
     * 将字符串的首字母转小写
     * @param str 需要转换的字符串
     * @return
     */
    private static String lowerFirst(String str) {
        // 同理
        char[] cs=str.toCharArray();
        cs[0]+=32;
        return String.valueOf(cs);
    }


}
