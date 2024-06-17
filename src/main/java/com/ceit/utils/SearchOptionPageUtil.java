package com.ceit.utils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.ceit.bootstrap.ConfigLoader;

public class SearchOptionPageUtil {

    //分页
    private static String pageNowParameterName = null;
    private static String pageSizeParameterName =null;
    
    //用户查询参数名称
    private static String searchOptionNameParameterName =null;
    private static String searchOptionValueParameterName =null;
    
    //指定select的字段参数名字
    private static String selectFieldParameterName =null;

	public static void main(String[] args) 
	{
        String pattern = "^[a-z0-9A-Z,_-]+$";
        
        String test="orgId_12-3,id";
        
        if(test.matches(pattern)==false)
        	System.out.println("NotAllowed: "+ test);
        else 
        	System.out.println("OK: "+ test);
        
        test="orgId-(123";
        
        if(test.matches(pattern)==false)
        	System.out.println("NotAllowed: "+ test);
        else 
        	System.out.println("OK: "+ test);
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
    	
    	searchOptionValueParameterName = ConfigLoader.getConfig("request.searchvalue");
    	if(searchOptionValueParameterName==null)
    		searchOptionValueParameterName ="condition";
    	
    	selectFieldParameterName = ConfigLoader.getConfig("request.selectfield");
    	if(selectFieldParameterName==null)
    		selectFieldParameterName ="fields";
    }
    
    //根据用户指定的字段查询
    private static String getSelectFields(Map<String, Object> reqBody, String[] selectFieldNamesList)
    {
    	StringBuilder sb = new StringBuilder();
    	
        String requestSelectFields= (String)reqBody.get(selectFieldParameterName);
        if(requestSelectFields==null || requestSelectFields.length()==0)
        {
        	//客户端未指定,直接使用
            if (selectFieldNamesList==null || selectFieldNamesList.length==0) {
                sb.append("*");
            } else {
                for (String name : selectFieldNamesList) {
                    sb.append(name);
                    sb.append(",");
                }

                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }

        // count(*)
        if(selectFieldNamesList !=null
        	&& selectFieldNamesList.length==1 
        	&& selectFieldNamesList[0].equals("count(*)"))
        {
        	return "count(*)";
        }
        
        //用户指定字段只能包括 仅仅包含字母和数字逗号下划线中线,防止sql注入
        String pattern = "^[a-z0-9A-Z,_-]+$";
        requestSelectFields = requestSelectFields.trim();
        if(requestSelectFields.matches(pattern)==false)
        	return "";
        
        String[] fields = requestSelectFields.split(",");
        
        if (selectFieldNamesList==null || selectFieldNamesList.length==0)
        {
            //如果 selectFieldNamesList=*，但是用户指定了 id,name 根据用户指定的字段
            for(String field: fields)
            {
                sb.append(field.trim());
                sb.append(",");
            }
            if(sb.length()>0)
            	sb.deleteCharAt(sb.length() - 1);
        }
        else
        {
        	//只允许查询 selectFieldNamesList 中指定的字段 ORGANIZE_ID organizeId
        	Map<String,String> allowFieldsMap = new HashMap<String, String>();
        	for(String allowField: selectFieldNamesList)
        	{
        		//ORGANIZE_ID organizeId 
        		//ORGANIZE_ID   AS   organizeId
        		String[] keys= allowField.trim().toLowerCase().split(" ");
        		for(String key:keys)
        		{
        			if(key.length() ==0|| key.equals("as") )
        				continue;
        			else 
        				allowFieldsMap.put(key, allowField);
        		}	
        	}
        	
            for(String field: fields)
            {
            	field = field.toLowerCase().trim();
            	if(allowFieldsMap.containsKey(field))
            	{
                    sb.append(allowFieldsMap.get(field));
                    sb.append(",");
            	}
            }
            
            //去掉最后的,
            if(sb.length()>0)
            	sb.deleteCharAt(sb.length() - 1);
        }

        //FIXME: 
        //修改orderBy 和 groupBy 条件 比如 order by LAST_VISITS lastVisits 但是查询里面没有
        // concat(datediff(end_time,CURDATE()),'天') as lastdays
 
    	return sb.toString();
    }
    
    //根据reqBody中指定的searchName和searchValue自动生成where语句
    private static StringBuilder getSearchOptionWhere(StringBuilder where, Map<String, Object> reqBody, 
    		String[] acceptOptionNamesList, List<Object> sqlParamObjList )
    {   	
        if (acceptOptionNamesList == null || acceptOptionNamesList.length ==0) {
        	return where;
        }

        String optionName = (String)reqBody.get(searchOptionNameParameterName);
    	Object optionValue = reqBody.get(searchOptionValueParameterName);
        //关系运算符，默认 =
        //String optionOperator = (String)reqBody.get(searchOptionOperatorParameterName);

        if (optionName == null || optionName.trim().length()==0 || optionValue == null) {
        	return where;
        }

        // * 全局搜索,多个字段搜索
        if (optionName.equals("*")) {

            if (where.length() > 0) {
                where.append(" and ");
            }

            where.append("(");
            for (String name : acceptOptionNamesList) {
                where.append(name);
                where.append(" like ? or ");
                sqlParamObjList.add("%" + optionValue + "%");
            }
            where.delete(where.length() - 4, where.length() - 1);
            where.append(")");
        } else {

        	//有效字段数量
        	int count =0;
        	String[] searchNames = optionName.split(",");
        	for(String searchName : searchNames)
        	{
        		searchName = searchName.trim();
        		if(searchName.length()==0)
        			continue;
        		
                //要搜索的字段必须在acceptOptionNamesList列表中
                for (String name : acceptOptionNamesList) {
                    if (name.equalsIgnoreCase(searchName)) {

                        if (count==0) 
                        {
                        	if(where.length() > 0)
                        		where.append(" and (");
                        	else
                        		where.append("(");
                        }
                        else
                        {
                        	where.append(" or ");
                        }
                        
                        count++;
                        where.append(name);
                        
                        //FIXME:让用户指定关系运算符operator  > < = 或者 startWith,endWith,contain
                        //如果是整数，用=
                        if(optionValue instanceof Integer || optionValue instanceof Long)
                        {
                        	where.append("=" + optionValue);
                        }
                        else
                        {
                        	//字符串，用 like
	                        where.append(" like ?");
	                        sqlParamObjList.add("%" + optionValue + "%");
                        }
                        
                        break;
                    }
                }
        	}
        	
        	if (count>0)
        	{
        		where.append(")");
        	}
        }
        
    	return where;
    }

    //根据后台指定的searchField匹配ReqBody 自动生成where语句 自动展开orgId/groupId/ids等特殊查询参数
    private static StringBuilder getSearchFieldWhere(StringBuilder where, Map<String, Object> reqBody,
                                                     String[] searchFieldNamesList,
                                                     List<Object> sqlParamObjList)
    {
        // 处理 orgId/groupId/ids等查询参数
        if (!StringUtil.arrayIsEmpty(searchFieldNamesList)) {
            for (String fieldName : searchFieldNamesList) {
                if (reqBody.get(fieldName) != null) {
                    Object fieldValue = reqBody.get(fieldName);

                    if (fieldValue instanceof String) {

                        if (where.length() > 0) {
                            where.append(" and ");
                        }

                        // 如果是逗号隔开的整数字符串 " 123 , 456 , 789 , "
                        if ( ((String) fieldValue).matches("\\s*\\d+(\\s*\\,\\s*\\d+\\s*)*\\s*\\,?\\s*")) {
                            String[] values = ((String) fieldValue).split(",");

                            // trim
                            if (values.length == 1) {
                                where.append(fieldName + "=?");
                                sqlParamObjList.add(Integer.parseInt(values[0]));
                            } else if (values.length > 1) {
                                StringBuilder inStr = new StringBuilder();
                                for (String v : values) {
                                    if (!v.trim().isEmpty())
                                        inStr.append(v.trim() + ",");
                                }
                                inStr.deleteCharAt(inStr.length() - 1);

                                where.append(fieldName);
                                where.append(" in (");
                                where.append(inStr);
                                where.append(")");
                            }
                        }
                        else if(fieldName.equalsIgnoreCase("id") ||
                                fieldName.equalsIgnoreCase("ids") ||
                                fieldName.equalsIgnoreCase("groupId") ||
                                fieldName.equalsIgnoreCase("orgId") ){
                            String[] values = ((String) fieldValue).split(",");
                            // 拼接成 in (?,?,?)形式,防止SQL注入
                            if (values.length == 1) {
                                where.append(fieldName + "=?");
                                sqlParamObjList.add(values[0]);
                            } else if (values.length > 1) {

                                StringBuilder inStr = new StringBuilder();

                                where.append(fieldName);
                                where.append(" in (");
                                for (String v : values) {
                                    if (!v.trim().isEmpty())
                                    {
                                        where.append("?,");
                                        sqlParamObjList.add(v.trim());
                                    }
                                }
                                where.deleteCharAt(inStr.length() - 1);
                                where.append(")");
                            }
                        }
                        else {
                            where.append(fieldName + "=?");
                            sqlParamObjList.add(fieldValue);
                        }
                    } else if (fieldValue instanceof List) {

                        // 如果List没有数据，不添加查询条件
                        List list = (List) fieldValue;
                        if (list.size() > 0) {

                            if (where.length() > 0) {
                                where.append(" and ");
                            }

                            where.append(fieldName);
                            where.append(" in (");
                            for (Object v : list) {
                                where.append("?,");
                                sqlParamObjList.add(v);
                            }
                            where.deleteCharAt(where.length() - 1);
                            where.append(")");
                        }

                    } else // if (fieldValue instanceof Integer)
                    {
                        // 整形,或者其他类型
                        if (where.length() > 0) {
                            where.append(" and ");
                        }

                        where.append(fieldName + "=?");
                        sqlParamObjList.add(fieldValue);
                    }
                }

            }
        }

        return where;
    }

    //处理用户指定的where条件及参数   setWhere("id=? and name=?", id, name)   setWhere("id=? and name=?", objs)
    private static StringBuilder getWhereParams(StringBuilder where, Map<String, Object> reqBody,
                                                String[] whereConditions,
                                                Object[] whereParams,
                                                List<Object> sqlParamObjList)
    {
        //处理where条件及其参数  getSql("id=? and name=?", id, name)
        if (!StringUtil.arrayIsEmpty(whereConditions)) {
            for (String condition : whereConditions) {

                if(StringUtil.isEmpty(condition))
                    continue;

                if (where.length() > 0)
                    where.append(" and ");

                where.append(condition);

                //添加动态参数，兼容原来的，只要where里面有问号就添加可能会有错误，
                //if(whereParams==null && condition.contains("?"))
                //    sqlParamObjList.add(reqBody.get(StringUtil.getFirstWord(condition)));
            }

            //如果有where参数，加上
            if(whereParams!=null && whereParams.length >0){
                for (Object param : whereParams) {

                    if(param==null)
                        continue;

                    sqlParamObjList.add(param);
                }
            }
        }

        return where;
    }

    public static String getSelectSql(String selectTableName, String[] selectFieldNamesList,
                                      String[] whereConditions,Object[] whereParams,
                                      String[] searchFieldNamesList, String[] acceptOptionNamesList, String orderBy, Map<String, Object> reqBody,
            List<Object> sqlParamObjList) {
        return getSelectSql(selectTableName, selectFieldNamesList, whereConditions, whereParams, searchFieldNamesList,
                acceptOptionNamesList, "", orderBy, reqBody, sqlParamObjList);
    }

    /**
     * 拼接sql语句
     * 
     * @param selectTableName
     *            查询表名
     * @param selectFieldNamesList
     *            查询字段名
     * @param whereConditions
     *            where条件
     * @param searchFieldNamesList
     *            where等于条件
     * @param acceptOptionNamesList
     *            能接受的option条件
     * @oaram groupBy 分组条件
     * @param orderBy
     *            排序条件
     * @param reqBody
     *            参数
     * @param sqlParamObjList
     *            参数对应的实体，?对应的实体
     * @return sql语句
     * 
     * @apiNote 例
     * 
     *          <pre>
     *          String sql;
     *          String selectTableName = "sys_organization";
     *          String[] selectFieldNames = { "*" };
     *          String[] whereConditions = { "id=?", "pid>?" };
     *          String[] searchFiledNames = { "org_id" };
     *          String[] acceptOptionNames = { "name", "title", "email", "tel" };
     *          List<Object> sqlParamObjList = new ArrayList<Object>(); // orgId改成org_id
     *          SearchOptionPageUtil.changeSearchFieldName(reqBody, "orgId", "org_id");
     *          sql = SearchOptionPageUtil.getSelectSql(selectTableName, selectFieldNames, null, searchFiledNames,
     *                  acceptOptionNames, null, reqBody, sqlParamObjList);
     *          </pre>
     */
    public static String getSelectSql(String selectTableName, String[] selectFieldNamesList,
            String[] whereConditions, Object[] whereParams,
            String[] searchFieldNamesList, String[] acceptOptionNamesList, String groupBy, String orderBy,
            Map<String, Object> reqBody,
            List<Object> sqlParamObjList) {
        StringBuilder sql = new StringBuilder();
        StringBuilder where = new StringBuilder();
        sqlParamObjList.clear();

        if (StringUtil.isEmpty(selectTableName))
            return null;

        //select字段
        sql.append("select ");
        
        String fields = getSelectFields(reqBody,selectFieldNamesList);
        if (fields=="")
        {
        	return null;
        }

    	sql.append(fields);
    
        sql.append(" from ");
        sql.append(selectTableName);

        //处理用户指定的where条件及参数   setWhere("id=? and name=?", id, name)   setWhere("id=? and name=?", objs)
        getWhereParams(where, reqBody, whereConditions, whereParams, sqlParamObjList);

        //根据reqBody中指定的searchName和searchValue自动生成where语句
        getSearchOptionWhere(where, reqBody, acceptOptionNamesList, sqlParamObjList);

        //根据后台指定的searchField匹配ReqBody 自动生成where语句 自动展开orgId/groupId/ids等特殊查询参数
        getSearchFieldWhere(where, reqBody, searchFieldNamesList, sqlParamObjList);

        // 拼SQL where 语句
        if (where.length() > 0) {
            sql.append(" where ");
            sql.append(where);
        }

        // 分组 group by
        if (!StringUtil.isEmpty(groupBy)) {
            sql.append(" group by ");
            sql.append(groupBy);
        }

        // 排序 order by
        if (!StringUtil.isEmpty(orderBy)) {
            sql.append(" order by ");
            sql.append(orderBy);
        }

        // 分页 limit 0,20
        if (reqBody.get(pageNowParameterName) != null && reqBody.get(pageSizeParameterName) != null) {
        	
            int pageNow = Integer.parseInt(reqBody.get(pageNowParameterName).toString());
            int pageSize =Integer.parseInt(reqBody.get(pageSizeParameterName).toString());

            if (pageNow < 1)
                pageNow = 1;
            if (pageSize < 1)
                pageSize = 100;

            sql.append(String.format(" limit %d,%d", (pageNow - 1) * pageSize, pageSize));
        }

        //DebugShowSql.println("SearchOptionPageUtil getSelectSql", sql.toString(), sqlParamObjList);
        return sql.toString();
    }

    /**
     * 单表更新
     * 
     * @param updateTableName
     *            更新表名
     * @param updateFieldNames
     *            更新字段名
     * @param whereConditions
     *            更新条件
     * @param reqBody
     *            参数
     * @param sqlParamObjList
     *            参数对应的实体
     * @return sql语句
     * 
     * @apiNote 例
     * 
     *          <pre>
     *          String updateTableName = "sys_menu";
     *          String[] updateFieldNames = { "sort", "name" };
     *          String[] whereConditions = { "id=?", "pid>?" };
     *          String[] searchFieldNamesList = { "id", "pid" };
     *          List<Object> sqlParamObjList2 = new ArrayList<Object>();
     *          sql = getUpdateSql(updateTableName, updateFieldNames, whereConditions,searchFieldNamesList, reqBody, sqlParamObjList2);
     *          Integer result = SimpleJDBC.getInstance().update(sql, sqlParamObjList2.toArray());
     *          </pre>
     */
    public static String getUpdateSql(String updateTableName, String[] updateFieldNames,
                                      String[] whereConditions, Object[] whereParams,
                                      String[] searchFieldNamesList,  Map<String, Object> reqBody, List<Object> sqlParamObjList) {
        StringBuilder sql = new StringBuilder();
        StringBuilder where = new StringBuilder();
        sqlParamObjList.clear();

        if (StringUtil.isEmpty(updateTableName) || StringUtil.arrayIsEmpty(updateFieldNames)) {
            return null;
        }

        // update 必须设置where条件或者设置searchField，否则会更新所有数据
        if (StringUtil.arrayIsEmpty(whereConditions) && StringUtil.arrayIsEmpty(searchFieldNamesList) ) {
            return null;
        }

        sql.append("update ");
        sql.append(updateTableName + " set ");
        for (String name : updateFieldNames) {
            if(reqBody.get(name)==null)continue;
            sql.append(name);
            sql.append("=?,");
            sqlParamObjList.add(reqBody.get(name));
        }
        sql.deleteCharAt(sql.length() - 1);

        //处理用户指定的where条件及参数   setWhere("id=? and name=?", id, name)   setWhere("id=? and name=?", objs)
        getWhereParams(where, reqBody, whereConditions, whereParams, sqlParamObjList);

        //update时不处理浏览器传递过来的 查询参数
        //根据reqBody中指定的searchName和searchValue自动生成where语句
        //getSearchOptionWhere(where, reqBody, acceptOptionNamesList, sqlParamObjList);

        //根据后台指定的searchField匹配ReqBody 自动生成where语句 自动展开orgId/groupId/ids等特殊查询参数
        getSearchFieldWhere(where, reqBody, searchFieldNamesList, sqlParamObjList);

        // 拼SQL where 语句
        if (where.length() > 0) {
            sql.append(" where ");
            sql.append(where);
        }

        //DebugShowSql.println("SearchOptionPageUtil getUpdateSql", sql.toString(), sqlParamObjList);

        return sql.toString();
    }

    /**
     * 生成insert语句
     * 
     * @param insertTableName
     *            插入的表名
     * @param insertFieldNames
     *            插入的字段
     * @param reqBody
     *            参数
     * @param sqlParamObjList
     *            参数对应的实体
     * @return sql
     * @apiNote 例
     * 
     *          <pre>
     *          String insertTableName = "sys_menu";
     *          String[] insertFieldNames = { "sort", "name" };
     *          List<Object> sqlParamObjList3 = new ArrayList<Object>();
     *          sql = getInsertSql(insertTableName, insertFieldNames, reqBody, sqlParamObjList3);
     *          Integer result3 = SimpleJDBC.getInstance().update(sql, sqlParamObjList2.toArray());
     *          </pre>
     */
    @SuppressWarnings("unchecked")
    public static String getInsertSql(String insertTableName, String[] insertFieldNames, Map<String, Object> reqBody,
            List<Object> sqlParamObjList) {
        StringBuilder sql = new StringBuilder();
        sqlParamObjList.clear();
        int valueLength = 1;
        if (StringUtil.isEmpty(insertTableName) || StringUtil.arrayIsEmpty(insertFieldNames)) {
            return null;
        }
        sql.append("insert into ");
        sql.append(insertTableName);
        sql.append('(');
        StringBuilder values = new StringBuilder();
        values.append("(");
        for (String name : insertFieldNames) {
            Object obj = reqBody.get(name);
            if(obj==null)continue;
            sql.append(name + ",");
            values.append("?,");
            if (obj instanceof List) {
                List<Object> list = (List<Object>) obj;
                if (list.size() > valueLength) {
                    valueLength = list.size();
                }
            }
        }
        sql.deleteCharAt(sql.length() - 1);
        values.deleteCharAt(values.length() - 1);
        values.append(')');
        sql.append(')');
        sql.append(" values");
        for (int i = 0; i < valueLength; i++) {
            sql.append(" " + values + ",");
            for (String name : insertFieldNames) {
                Object obj = reqBody.get(name);
                if(obj==null)continue;
                if (obj instanceof List) {
                    List<Object> list = (List<Object>) obj;
                    sqlParamObjList.add(list.get(i));
                } else {
                    sqlParamObjList.add(obj);
                }
            }
        }
        sql.deleteCharAt(sql.length() - 1);

        //DebugShowSql.println("SearchOptionPageUtil getInsertSql", sql.toString(), sqlParamObjList);

        return sql.toString();
    }

}
