package com.ceit.utils;

import java.util.*;

import com.alibaba.fastjson2.JSONObject;
import com.ceit.bootstrap.ConfigLoader;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;

/**
 * sql语句生成工具
 */
public class SqlUtil {
    private List<Object> paramsList;
    private Map<String, Object> reqBody;
    private String table;
    private String[] selectFields;
    private String[] wheres;
    private Object[] wheresParams;
    private String[] searchFields;
    private String[] acceptOptions;
    private String groupBy;
    private String orderBy;
    private SimpleJDBC jdbc = SimpleJDBC.getInstance();
    
    //分页
    private static String pageNowParameterName = null;
    private static String pageSizeParameterName =null;
    
    //用户查询参数名称
    private static String searchOptionNameParameterName =null;
    private static boolean searchOptionNameParameterIngoreCase = false;  //查询字段名忽略大小写
    private static String searchOptionValueParameterName =null;
    
    //指定select的字段参数名字
    private static String selectFieldParameterName =null;
    
    public static String getParameterNamePageNow()
    {
    	return pageNowParameterName;
    }
    
    public static String getParameterNamePageSize()
    {
    	return pageSizeParameterName;
    }
    
    public static String getParameterNameSearchName()
    {
    	return searchOptionNameParameterName;
    }
    
    public static String getParameterNameSearchValue()
    {
    	return searchOptionValueParameterName;
    }
    
    public static String getParameterNameSelectField()
    {
    	return selectFieldParameterName;
    }
    
    public static void loadDefaultConfig()
    {
    	pageNowParameterName = ConfigLoader.getConfig("request.pagenow");
    	if(pageNowParameterName==null)
    		pageNowParameterName ="pageNow";
    	
    	pageSizeParameterName = ConfigLoader.getConfig("request.pagesize");
    	if(pageSizeParameterName==null)
    		pageSizeParameterName ="pageSize";
    	
    	searchOptionNameParameterName = ConfigLoader.getConfig("request.searchname");
    	if(searchOptionNameParameterName==null)
    		searchOptionNameParameterName ="option";
    	
    	String ignoreCase = ConfigLoader.getConfig("request.searchname.ignorecase");
    	if(ignoreCase !=null) {
    		if(ignoreCase.trim().equals("1") || ignoreCase.trim().equals("true") )
    			searchOptionNameParameterIngoreCase =true;
    	}

    	searchOptionValueParameterName = ConfigLoader.getConfig("request.searchvalue");
    	if(searchOptionValueParameterName==null)
    		searchOptionValueParameterName ="condition";
    	
    	selectFieldParameterName = ConfigLoader.getConfig("request.selectfield");
    	if(selectFieldParameterName==null)
    		selectFieldParameterName ="fields";
    }
    
    /**
     * 根据一个数组或者List生成逗号隔开的字符串
     */
    public static String getInString(List<Object> list)
    {
    	if(list==null)
    		return null;
    	
		return getInString(list.toArray());
    }
    
    /**
     * 根据一个数组或者List生成逗号隔开的字符串
     */
    public static String getInString(Object[] obs)
    {
    	if(obs==null)
    		return null;
    	
    	Map<String,Object> existMap=new HashMap();
    	
		//拼接成逗号隔开 IN 字符串
		StringBuilder sbIn =new StringBuilder();
		
		for(Object obj: obs)
		{
			//不重复
			if(obj==null || existMap.containsKey(obj.toString()))
			{
				continue;
			}
			
			existMap.put(obj.toString(), obj);
			
			//不是整数
    		if( !(obj instanceof Integer ||obj instanceof Long))
    		{    	
    			//字符串或者其他类型
    			sbIn.append('\'');
    			sbIn.append(obj.toString());
    			sbIn.append('\'');
    		}
    		else
    		{
    			//整数
    			sbIn.append(obj.toString());
    		}

			sbIn.append(',');
		}
		
		if(sbIn.length()>0)
			sbIn.deleteCharAt(sbIn.length()-1);
    	
		existMap.clear();
		
    	return sbIn.toString();
    }
    
    /**
     * 根据一个数组或者List生成逗号隔开的带参数?, 值添加到paramList参数
     */
    public static String getInString(List<Object> list, List<Object> paramList)
    {
    	if(list==null)
    		return null;
    	
    	return getInString(list.toArray(),paramList);
    }
    
    //生成逗号分割的字符串，会自动去掉重复的值
    public static String getInString(Object[] obs, List<Object> paramList)
    {
    	if(obs==null)
    		return null;
    	
    	Map<String,Object> existMap=new HashMap();
    	
		//拼接成逗号隔开 IN 字符串
		StringBuilder sbIn =new StringBuilder();
		
		for(Object obj: obs)
		{
			//不重复
			if(obj!=null && !existMap.containsKey(obj.toString()))
			{
				sbIn.append("?,");
				paramList.add(obj);
				
				existMap.put(obj.toString(), obj);
			}
		}
		
		if(sbIn.length()>0)
			sbIn.deleteCharAt(sbIn.length()-1);
    	
		existMap.clear();
		
    	return sbIn.toString();
    }
    
    public static String getInString(List<Map<String,Object>> mapList, String keyField, List<Object> paramList)
    {
    	if(mapList==null)
    		return null;
    	
    	Map<String,Object> existMap=new HashMap();
		//拼接成逗号隔开 IN 字符串
		StringBuilder sbIn =new StringBuilder();
		
		for(Map<String,Object> map: mapList)
		{
			Object obj = map.get(keyField);
			if(obj==null)
				continue;
			
			//不重复
			if(!existMap.containsKey(obj.toString()))
			{
				sbIn.append("?,");
				paramList.add(obj);
				
				existMap.put(obj.toString(), obj);
			}

		}
		
		if(sbIn.length()>0)
			sbIn.deleteCharAt(sbIn.length()-1);
    	
		existMap.clear();
		
    	return sbIn.toString();
    }

    public SqlUtil(Map<String, Object> reqBody) {
        this.paramsList = new ArrayList<>();
        this.reqBody = reqBody;
    }

    /**
     * 将接收的参数名称转化成数据库中的字段名称
     * 
     * <pre>
     * SearchOptionPageUtil.changeSearchFieldName(reqBody, "orgId", "org_id");
     * </pre>
     * 
     * @param reqBody
     *            参数
     * @param requestName
     *            请求参数名
     * @param dbFieldName
     *            sql中字段名
     */
    public static void changeSearchFieldName(Map<String, Object> reqBody, String requestName, String dbFieldName) {
        // 多表查询，name字段冲突，修改字段名称
        if (reqBody.containsKey(requestName)) {
            Object value = reqBody.get(requestName);
            if (reqBody.containsKey(dbFieldName)) {
                reqBody.replace(dbFieldName, value);
            } else {
                reqBody.put(dbFieldName, value);
            }

            reqBody.remove(requestName);
        }
    }

    /**
     * 改变option中字段名
     * 
     * <pre>
     * SearchOptionPageUtil.changeOptionFieldName(reqBody, "auth_policy_name", "ap.name");
     * </pre>
     * 
     * @param reqBody
     *            参数
     * @param requestName
     *            参数中option对应的value
     * @param dbFieldName
     *            sql中字段名
     */
    public static void changeOptionFieldName(Map<String, Object> reqBody, String requestName, String dbFieldName) {
        // 多表查询，name字段冲突，修改字段名称
        if (reqBody.containsKey(searchOptionNameParameterName)) {
            String value = (String) reqBody.get(searchOptionNameParameterName);
            if (value.equals(requestName))
                reqBody.replace(searchOptionNameParameterName, dbFieldName);
        }
    }

    /**
     * 表名
     */
    public SqlUtil setTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * 所需字段
     */
    public SqlUtil setFields(String... selectFields) {
        if(selectFields!=null && selectFields.length==1)
        {
            if(selectFields[0].contains(","))
            {
                this.selectFields = selectFields[0].split(",");
                return this;
            }
        }
        this.selectFields = selectFields;
        
        //如果acceptOptions为空，设置为selectFields
        if(this.acceptOptions==null)
        	this.acceptOptions = selectFields;
        
        return this;
    }

    /**
     * where语句，select为单条件,详见{@link SearchOptionPageUtil#getUpdateSql(String, String[], String[], Map, List)}
     */
    public SqlUtil setWhere(String where){
        this.wheres = new String[]{where};
        this.wheresParams = null;
        return this;
    }

    public SqlUtil setWhere(String wheres, Object... params){
        this.wheres = new String[]{wheres};
        //如果params是1个数组类型，但是个数为0，当成空，否则会把数组当成一个参数
        if(params!=null && params.length==1) {
            if( params[0] instanceof Collection) {
                Collection list= (Collection) params[0];
                this.wheresParams = list.toArray();
            }
            else {
                this.wheresParams = params;
            }
        }
        else
            this.wheresParams = params;

        return this;
    }

    /**
     * 设置要查询的字段，可以使用数组，或者逗号隔开的字段名称，会自动reqBody中读取对应字段，拼接到where子句中
     */
    public SqlUtil setSearchFields(String... searchFields) {
        if(searchFields!=null && searchFields.length==1)
        {
            if(searchFields[0].contains(","))
            {
                this.searchFields = searchFields[0].split(",");
                return this;
            }
        }
        this.searchFields = searchFields;
        return this;
    }

    /**
     * select专用，接受的query
     * form条件，详见{@link SearchOptionPageUtil#getSelectSql(String, String[], String, String[], String[], String, Map, List)}
     */
    public SqlUtil setAcceptOptions(String... acceptOptions) {
        if(acceptOptions!=null && acceptOptions.length==1)
        {
            if(acceptOptions[0].contains(","))
            {
                this.acceptOptions = acceptOptions[0].split(",");
                return this;
            }
        }
        this.acceptOptions = acceptOptions;
        return this;
    }

    /**
     * select专用排序条件，详见{@link SearchOptionPageUtil#getSelectSql(String, String[], String, String[], String[], String, Map, List)}
     */
    public SqlUtil setGroupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    /**
     * select专用排序条件，详见{@link SearchOptionPageUtil#getSelectSql(String, String[], String, String[], String[], String, Map, List)}
     */
    public SqlUtil setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    /**
     * 获取当前拼接的sql语句
     */
    public String getSelect() {
        return SearchOptionPageUtil.getSelectSql(table, selectFields, wheres, wheresParams, searchFields, acceptOptions, groupBy, orderBy,
                reqBody, paramsList);
    }

    /**
     * 生成insert语句
     */
    public String getInsert() {
        return SearchOptionPageUtil.getInsertSql(table, selectFields, reqBody, paramsList);
    }

    /**
     * 生成update语句
     */
    public String getUpdate() {
        return SearchOptionPageUtil.getUpdateSql(table, selectFields, wheres, wheresParams, searchFields, reqBody, paramsList);
    }

    /*
    public String selectForJsonArray() {
        return jdbc.selectForJsonArray(getSelect(), paramsList.toArray());
    }
     */

    public Result selectForTotalRowsResult() {
        //查询数据总数，当前分页数据
        Map resultData = new HashMap();
        int total = this.selectForTotalCount();
        resultData.put("total", total);
        if(total>0) {
            resultData.put("rows", this.selectForMapList());
        } else {
            resultData.put("rows", new ArrayList());
        }

        return new Result(200, "成功", resultData);
    }

    public JSONObject selectForJsonObject() {
        return jdbc.selectForJsonObject(getSelect(), paramsList.toArray());
    }

    public Object selectForOneNode() {
        return jdbc.selectForOneNode(getSelect(), paramsList.toArray());
    }

    public Map<String, Object> selectForMap() {
        return jdbc.selectForMap(getSelect(), paramsList.toArray());
    }

    public List<Map<String, Object>> selectForMapList() {
        return jdbc.selectForMapList(getSelect(), paramsList.toArray());
    }

    public List selectForList() {
        return jdbc.selectForList(getSelect(), paramsList.toArray());
    }

    public Map<Object, Map<String, Object>> selectForListMap(String keyField) {
        return jdbc.selectForListMap(getSelect(), keyField, paramsList.toArray());
    }

    public int selectForTotalCount() {

        int ret = -1;

        // 备份几个要修改的数据
        String tmp_table = this.table;
        String[] tmp_fields = this.selectFields;
        Object tmp_pageNow = reqBody.get(pageNowParameterName);

        // 修改查询条件
        this.selectFields = new String[] { "count(*)" };
        reqBody.remove(pageNowParameterName);

        //不需要orderBy
        String tmp_OrderBy = this.orderBy;
        this.orderBy = null;
        
        //根据groupBy条件修改sql
        String tmp_select = "select count(*) from ("+ getSelect() +") tmpCount ";
        String sql = getSelect();
        if (this.groupBy!=null)
            sql = tmp_select;


        Object obj = jdbc.selectForOneNode(sql, paramsList.toArray());

        if (obj != null) {
            Long count = (Long) obj;
            ret = count.intValue();
        }

        // 恢复数据
        this.table = tmp_table;
        this.selectFields = tmp_fields;
        if (tmp_pageNow != null) {
            reqBody.put(pageNowParameterName, tmp_pageNow);
        }

        this.orderBy  = tmp_OrderBy;
        
        return ret;
    }

    /**
     * insert由此执行
     */
    public Integer insert() {
        return jdbc.update(getInsert(), paramsList.toArray());
    }

    /**
     * insert添加，返回自增的值
     */
    public Integer insertAutoIncKey() {
        return jdbc.updateAutoIncKey(getInsert(), paramsList.toArray());
    }

    /**
     * update由此执行
     */
    public Integer update() {
        return jdbc.update(getUpdate(), paramsList.toArray());
    }

}
