package com.ceit.json;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ceit.ioc.annotations.Component;

/**
 * {@link JSON}用于反序列化{@link #inputstream2Map(InputStream)}时存在线程安全问题
 * <p>
 * 反序列化使用时需要创建局部变量
 */
@Component
public class JSON {

    /*
    private static final int BEGIN_OBJECT_TOKEN = 1;
    private static final int END_OBJECT_TOKEN = 2;
    private static final int BEGIN_ARRAY_TOKEN = 4;
    private static final int END_ARRAY_TOKEN = 8;
    private static final int NULL_TOKEN = 16;
    private static final int NUMBER_TOKEN = 32;
    private static final int STRING_TOKEN = 64;
    private static final int BOOLEAN_TOKEN = 128;
    private static final int SEP_COLON_TOKEN = 256;
    private static final int SEP_COMMA_TOKEN = 512;

    private TokenList tokens;
    */

    public JSON() {
    }

    public static JSON getInstance() {
        return new JSON();
    }

    /*
    private JsonObject parse(TokenList tokens) {
        if (tokens.length() == 1) {
            return new JsonObject();
        }
        this.tokens = tokens;
        return parse();
    }

    private JsonObject parse() {
        Token token = tokens.next();
        if (token == null) {
            return new JsonObject();
        } else if (token.getTokenType() == TokenType.BEGIN_OBJECT) {
            return parseJsonObject();
        } else {
            throw new JsonParseException("Parse error, invalid Token.");
        }
    }

    private JsonObject parseJsonObject() {
        JsonObject jsonObject = new JsonObject();
        int expectToken = STRING_TOKEN | END_OBJECT_TOKEN;
        String key = null;
        Object value = null;
        while (tokens.hasMore()) {
            Token token = tokens.next();
            TokenType tokenType = token.getTokenType();
            String tokenValue = token.getValue();
            switch (tokenType) {
            case BEGIN_OBJECT:
                checkExpectToken(tokenType, expectToken);
                jsonObject.put(key, parseJsonObject()); // 递归解析 json object
                expectToken = SEP_COMMA_TOKEN | END_OBJECT_TOKEN;
                break;
            case END_OBJECT:
                checkExpectToken(tokenType, expectToken);
                return jsonObject;
            case BEGIN_ARRAY: // 解析 json array
                checkExpectToken(tokenType, expectToken);
                jsonObject.put(key, parserJsonArray());
                expectToken = SEP_COMMA_TOKEN | END_OBJECT_TOKEN;
                break;
            case NULL:
                checkExpectToken(tokenType, expectToken);
                jsonObject.put(key, null);
                expectToken = SEP_COMMA_TOKEN | END_OBJECT_TOKEN;
                break;
            case NUMBER:
                checkExpectToken(tokenType, expectToken);
                if (tokenValue.contains(".") || tokenValue.contains("e") || tokenValue.contains("E")) {
                    jsonObject.put(key, Double.valueOf(tokenValue));
                } else {
                    Long num = Long.valueOf(tokenValue);
                    if (num > Integer.MAX_VALUE || num < Integer.MIN_VALUE) {
                        jsonObject.put(key, num);
                    } else {
                        jsonObject.put(key, num.intValue());
                    }
                }
                expectToken = SEP_COMMA_TOKEN | END_OBJECT_TOKEN;
                break;
            case BOOLEAN:
                checkExpectToken(tokenType, expectToken);
                jsonObject.put(key, Boolean.valueOf(token.getValue()));
                expectToken = SEP_COMMA_TOKEN | END_OBJECT_TOKEN;
                break;
            case STRING:
                checkExpectToken(tokenType, expectToken);
                Token preToken = tokens.peekPrevious();
                /*
                 * 在 JSON 中，字符串既可以作为键，也可作为值。 作为键时，只期待下一个 Token 类型为 SEP_COLON。 作为值时，期待下一个 Token
                 * 类型为 SEP_COMMA 或 END_OBJECT
 
                if (preToken.getTokenType() == TokenType.SEP_COLON) {
                    value = token.getValue();
                    jsonObject.put(key, value);
                    expectToken = SEP_COMMA_TOKEN | END_OBJECT_TOKEN;
                } else {
                    key = token.getValue();
                    expectToken = SEP_COLON_TOKEN;
                }
                break;
            case SEP_COLON:
                checkExpectToken(tokenType, expectToken);
                expectToken = NULL_TOKEN | NUMBER_TOKEN | BOOLEAN_TOKEN | STRING_TOKEN | BEGIN_OBJECT_TOKEN
                        | BEGIN_ARRAY_TOKEN;
                break;
            case SEP_COMMA:
                checkExpectToken(tokenType, expectToken);
                expectToken = STRING_TOKEN;
                break;
            case END_DOCUMENT:
                checkExpectToken(tokenType, expectToken);
                return jsonObject;
            default:
                throw new JsonParseException("Unexpected Token.");
            }
        }

        throw new JsonParseException("Parse error, invalid Token.");
    }

    private List<Object> parserJsonArray() {
        int expectToken = BEGIN_ARRAY_TOKEN | END_ARRAY_TOKEN | BEGIN_OBJECT_TOKEN | NULL_TOKEN | NUMBER_TOKEN
                | BOOLEAN_TOKEN | STRING_TOKEN;
        List<Object> retList = new ArrayList<Object>();
        while (tokens.hasMore()) {
            Token token = tokens.next();
            TokenType tokenType = token.getTokenType();
            String tokenValue = token.getValue();
            switch (tokenType) {
            case BEGIN_OBJECT:
                checkExpectToken(tokenType, expectToken);
                retList.add(parseJsonObject());
                expectToken = SEP_COMMA_TOKEN | END_ARRAY_TOKEN;
                break;
            case BEGIN_ARRAY:
                checkExpectToken(tokenType, expectToken);
                retList.add(parserJsonArray());
                expectToken = SEP_COMMA_TOKEN | END_ARRAY_TOKEN;
                break;
            case END_ARRAY:
                checkExpectToken(tokenType, expectToken);
                return retList;
            case NULL:
                checkExpectToken(tokenType, expectToken);
                retList.add(null);
                expectToken = SEP_COMMA_TOKEN | END_ARRAY_TOKEN;
                break;
            case NUMBER:
                checkExpectToken(tokenType, expectToken);
                if (tokenValue.contains(".") || tokenValue.contains("e") || tokenValue.contains("E")) {
                    retList.add(Double.valueOf(tokenValue));
                } else {
                    Long num = Long.valueOf(tokenValue);
                    if (num > Integer.MAX_VALUE || num < Integer.MIN_VALUE) {
                        retList.add(num);
                    } else {
                        retList.add(num.intValue());
                    }
                }
                expectToken = SEP_COMMA_TOKEN | END_ARRAY_TOKEN;
                break;
            case BOOLEAN:
                checkExpectToken(tokenType, expectToken);
                retList.add(Boolean.valueOf(tokenValue));
                expectToken = SEP_COMMA_TOKEN | END_ARRAY_TOKEN;
                break;
            case STRING:
                checkExpectToken(tokenType, expectToken);
                retList.add(tokenValue);
                expectToken = SEP_COMMA_TOKEN | END_ARRAY_TOKEN;
                break;
            case SEP_COMMA:
                checkExpectToken(tokenType, expectToken);
                expectToken = STRING_TOKEN | NULL_TOKEN | NUMBER_TOKEN | BOOLEAN_TOKEN | BEGIN_ARRAY_TOKEN
                        | BEGIN_OBJECT_TOKEN;
                break;
            case END_DOCUMENT:
                checkExpectToken(tokenType, expectToken);
                return retList;
            default:
                throw new JsonParseException("Unexpected Token.");
            }
        }
        throw new JsonParseException("Parse error, invalid Token.");
    }

    private void checkExpectToken(TokenType tokenType, int expectToken) {
        if ((tokenType.getTokenCode() & expectToken) == 0) {
            throw new JsonParseException("Parse error, invalid Token.");
        }
    }
    */
    /**
     * 将json格式的{@link InputStream}解析成{@link HashMap}
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    public Map<String, Object> inputstream2Map(InputStream inputStream) throws IOException {
        /*
        CharReader charReader = new CharReader(new InputStreamReader(inputStream, "UTF-8"));
        TokenList tokens = new TokenIzer().Tokenize(charReader);
        JsonObject jsonObject = parse(tokens);
        return jsonObject.getBody();
        */

        Map<String, Object> map = com.alibaba.fastjson2.JSON.parseObject(inputStream, Map.class);
        if(map==null)
            return new HashMap();
        
        return map;
    }

    /*
    public static String removeNonAscii(String str)
    {
        return str.replaceAll("[^\\x00-\\x7F]", "");
    }

    public static String removeNonPrintable(String str) // All Control Char
    {
        return str.replaceAll("[\\p{C}]", "");
    }

    public static String removeSomeControlChar(String str) // Some Control Char
    {
        return str.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]", "");
    }

    public static String removeControlCharFull(String str)
    {
        return removeNonPrintable(str).replaceAll("[\\r\\n\\t]", "");
    }
    */
    /**
     * JSON字符串特殊字符处理，比如：“\A1;1300”
     * 
     * @param s
     * @return String

    private void appendJsonString(StringBuilder sb, String s) {    
        //System.out.println(" -------appendJsonString--------- "+ s +" ---------------- ");

        s = s.replace("\"", "\\\"")
            .replace("\\", "\\\\")
            .replace("/", "\\/")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");

        sb.append(removeNonPrintable(s));
        //System.out.println("" );
    }
     */
    /*
     * metaData.getColumnType metaData.getColumnTypeName -7 BIT -6 TINYINT -5 BIGINT
     * -4 LONGVARBINARY -3 VARBINARY -2 BINARY -1 LONGVARCHAR 0 NULL 1 CHAR 2
     * NUMERIC 3 DECIMAL 4 INTEGER 5 SMALLINT 6 FLOAT 7 REAL 8 DOUBLE 12 VARCHAR 91
     * DATE 92 TIME 93 TIMESTAMP 1111 OTHER


    void AppendObjString(StringBuilder sb, Object obj)
    {
        //if(obj!=null)
        //   System.out.println("AppendObjString Type: " + obj.getClass().getTypeName() + " " + obj.toString());

        if (obj == null || StringUtil.isPrimitive(obj)) {
            sb.append(obj);
        } else if (obj instanceof String) {
            //字符串处理特殊字符
            sb.append("\"");
            appendJsonString(sb, obj.toString());
            sb.append("\"");
        } else if (obj instanceof Timestamp) {
            // 日期转化为字符串，应该都转换成当前时区时间
            String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Timestamp)obj);
            sb.append("\"" +   timeStr + "\"");
        } else if (obj instanceof Date) {
            // 日期转化为字符串，应该都转换成当前时区时间
            String timeStr = new SimpleDateFormat("yyyy-MM-dd").format((Date)obj);
            sb.append("\"" +   timeStr + "\"");
        } else if (obj instanceof LocalDateTime) {
            // 日期转化为字符串，应该都转换成当前时区时间
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            sb.append("\"" +   df.format((LocalDateTime)obj) + "\"");
        } else if (obj instanceof OffsetDateTime) {
            // 日期转化为字符串，应该都转换成当前时区时间
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            sb.append("\"" +   df.format((OffsetDateTime)obj) + "\"");
        } else if (obj instanceof ZonedDateTime) {
            // 日期转化为字符串，应该都转换成当前时区时间
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            sb.append("\"" +   df.format((ZonedDateTime)obj) + "\"");
        }
        else
        {
            sb.append("\"" + obj.toString() + "\"");
        }
    }
     */

    /**
     * {@link ResultSet}结果直接转化成JSON对象格式，返回结果集中第一条记录
     * <p>
     * 除基本数据类型外全部加引号
     * 
     * @param rs
     * @param conditions
     *            需要的字段
     * @return
     */
    public JSONObject resultSet2JsonObject(ResultSet rs, String... conditions) {

        if (rs == null) {
            return null;
        }
        
        JSONObject jsonObj = new com.alibaba.fastjson2.JSONObject();
        try {
            if (rs.next()) {
              
                ResultSetMetaData rsMetaData = rs.getMetaData();
                if (conditions.length == 0) {
                    int columnCount = rsMetaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        jsonObj.put(rsMetaData.getColumnLabel(i), rs.getObject(i));
                    }
                } else {
                    for (String entry : conditions) {
                        jsonObj.put(entry, rs.getObject(entry));
                    }
                }
            } else {
                return null;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            return null; 
        }

        return jsonObj;

        /* 默认会把TimeStamp变成1970年以来的 ms 秒数
         fastjson 2.0.33 bug? 
        D1=> : {"id":132,"account":"120191080716","name":"张毅","org_id":155,"disabled":0,"create_time":"2021-04-20 08:40:17"}
        D2=> : {"id":132,"account":"120191080716","name":"张毅","org_id":155,"disabled":0,"create_time":1618879217000}

        String jsonString =com.alibaba.fastjson2.JSON.toJSONString(jsonObj,"yyyy-MM-dd HH:mm:ss");
        System.out.println("D1=> : " + jsonString);
        jsonString = jsonObj.toJSONString();
        System.out.println("D2=> : " + jsonString);
        
        return jsonString;
        */
    }

    /*
    public String resultSet2JsonObject(ResultSet rs, String... conditions) {

        if (rs == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try {
            if (rs.next()) {
                sb.append('{');
                ResultSetMetaData rsMetaData = rs.getMetaData();
                if (conditions.length == 0) {
                    int columnCount = rsMetaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        sb.append("\"" + rsMetaData.getColumnLabel(i) + "\":");
                        AppendObjString(sb, rs.getObject(i));
                        sb.append(',');
                    }
                } else {
                    for (String entry : conditions) {
                        sb.append("\"" + entry + "\":");
                        AppendObjString(sb, rs.getObject(entry));
                        sb.append(',');
                    }
                }

                if(sb.charAt(sb.length() - 1)==',')
                    sb.deleteCharAt(sb.length() - 1);

                sb.append("}");
            } else {
                return null;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return sb.toString();
    }
    */

    /**
     * {@link ResultSet}结果直接转化成JSON格式
     * 
     * @param rs
     * @param conditions
     *            需要的字段
     * @return
     */
    public JSONArray resultSet2JsonArray(ResultSet rs, String... conditions) {

        if (rs == null) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();

        StringBuilder sb = new StringBuilder();

        sb.append('[');
        JSONObject jsonObj = resultSet2JsonObject(rs, conditions);
        while (jsonObj != null) {
            jsonArray.add(jsonObj);
            jsonObj = resultSet2JsonObject(rs, conditions);
        }

        return jsonArray;
    }


    /**
     * 基本数据类型的包装类以及一般对象拼接成JSON格式
     * 
     * @param obj
     * @return
     */

    public String obj2Json(Object obj) {
        return com.alibaba.fastjson2.JSON.toJSONString(obj,"yyyy-MM-dd HH:mm:ss");
    }
     /*
    @SuppressWarnings("unchecked")
    public String obj2Json(Object obj) {

        if (obj == null) {
            return "null";
        }
        else if (StringUtil.isPrimitive(obj)) {
            return obj.toString();
        }
        else if (obj instanceof String) {
            return "\"" + obj + "\"";
        }
        else if (obj instanceof Map) {
            return map2Json((Map<String, Object>) obj);
        }
        else if (obj instanceof List) {
            return list2Json((List<Object>) obj);
        }
        if (obj instanceof BigDecimal) {
            return obj.toString();
        } else if (obj instanceof Timestamp) {
            // 日期转化为字符串，应该都转换成当前时区时间
            String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Timestamp)obj);
            return "\"" +   timeStr + "\"" ;
        } else if (obj instanceof Date) {
            // 日期转化为字符串，应该都转换成当前时区时间
            String timeStr = new SimpleDateFormat("yyyy-MM-dd").format((Date)obj);
            return ("\"" +   timeStr + "\"");
        } else if (obj instanceof LocalDateTime) {
            // 日期转化为字符串，应该都转换成当前时区时间
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return ("\"" +   df.format((LocalDateTime)obj) + "\"");
        } else if (obj instanceof OffsetDateTime) {
            // 日期转化为字符串，应该都转换成当前时区时间
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return ("\"" +   df.format((OffsetDateTime)obj) + "\"");
        } else if (obj instanceof ZonedDateTime) {
            // 日期转化为字符串，应该都转换成当前时区时间
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return ("\"" +   df.format((ZonedDateTime)obj) + "\"");
        }

        //其他类型或者用户自定义类
        System.out.println("obj2Json "+obj.getClass().getName() +" " + obj.toString());

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        
        sb.append("{");
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (StringUtil.isPrimitiveByClass(field.getType())) {
                    sb.append("\"" + field.getName() + "\":" + field.get(obj).toString() + ",");
                } else {
                    sb.append("\"" + field.getName() + "\":\"" + field.get(obj).toString() + "\",");
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if(sb.charAt(sb.length() - 1)==',')
            sb.deleteCharAt(sb.length() - 1);

        sb.append("}");
        return sb.toString();
    }
    */

    /**
     * map转化为json格式，{@link Character}不加引号
     * 
     * @param map
     * @return
     */

     public String map2Json(Map<String, Object> map) {
        return com.alibaba.fastjson2.JSON.toJSONString(map);
    }

     /*
    public String map2Json(Map<String, Object> map) {

        if (map == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Set<Entry<String, Object>> entries = map.entrySet();
        entries.stream()
                .filter(item -> {
                    return item.getKey() != null;
                }).forEach(item -> {
                    sb.append('\"' + item.getKey() + '\"');
                    sb.append(':');
                    sb.append(obj2Json(item.getValue()));
                    sb.append(',');
                });

        if(sb.charAt(sb.length() - 1)==',')
            sb.deleteCharAt(sb.length() - 1);

        sb.append('}');
        return sb.toString();
    }
    */

    /**
     * {@link List}转化为json字符串
     * 
     * @param list
     * @return
     */

     
     public String list2Json(List<Object> list) {
        return com.alibaba.fastjson2.JSON.toJSONString(list);
    }

    /*
    public String list2Json(List<Object> list) {
        if (list == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Object obj : list) {
            sb.append(obj2Json(obj));
            sb.append(',');
        }

        if(sb.charAt(sb.length() - 1)==',')
            sb.deleteCharAt(sb.length() - 1);

        sb.append(']');
        return sb.toString();
    }
    */

    /**
     * {@link List<Map<String, Object>>}转化为json字符串
     * 
     * @param list
     * @return
     */
    public String mapList2Json(List<Map<String, Object>> list) {
        return com.alibaba.fastjson2.JSON.toJSONString(list);
    }

    /*
    public String mapList2Json(List<Map<String, Object>> list) {
        if (list == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Object obj : list) {
            sb.append(obj2Json(obj));
            sb.append(',');
        }

        if(sb.charAt(sb.length() - 1)==',')
            sb.deleteCharAt(sb.length() - 1);

        sb.append(']');
        return sb.toString();
    }
    */

    /**
     * {@link Map<Object, <Map<String, Object>>}转化为json字符串
     * 
     * @param listMap
     * @return
     */
    public String mapListMap2Json(Map<Object, Map<String, Object>> listMap) {
        return com.alibaba.fastjson2.JSON.toJSONString(listMap);
    }

    /*
    public String mapListMap2Json(Map<Object, Map<String, Object>> listMap) {

        if (listMap == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');

        listMap.entrySet().stream().forEach((rowMap) -> {
            Object rowValue = rowMap.getValue();
            sb.append(obj2Json(rowValue));
            sb.append(',');
        });
 
        if(sb.charAt(sb.length() - 1)==',')
            sb.deleteCharAt(sb.length() - 1);

        sb.append(']');
        return sb.toString();
    }
    */

    public static String test (List list) {

        com.alibaba.fastjson2.JSONObject jsonObj = new com.alibaba.fastjson2.JSONObject();
        for(int i=0;i <list.size() ;i ++)
        {
            jsonObj.put("time"+i, list.get(i));
        }

        //默认会把TimeStamp变成1970年以来的秒数
        String jsonString =com.alibaba.fastjson2.JSON.toJSONString(jsonObj);
        System.out.println("D1=> : " + jsonString);
        jsonString = jsonObj.toJSONString();
        System.out.println("D2=> : " + jsonString);
        return jsonString;
    }

    public static void main(String[] args) {
        String html ="<>:][],{}\'\"\\hello";
        JSON json = JSON.getInstance();
        String str = json.obj2Json(html); 

        
        System.out.println("src.len="+ html.length() +"  json.len="+str.length()+" "+str);

        java.util.Date date = new java.util.Date();
        System.out.println(json.obj2Json(date));

        Calendar calendar = Calendar.getInstance(); 
        System.out.println(json.obj2Json(calendar));

        java.sql.Date sqldate = new java.sql.Date(date.getTime());
        System.out.println(json.obj2Json(sqldate));

        Timestamp timestamp = new  Timestamp(System.currentTimeMillis());

        Map map=new HashMap();
        map.put("code", -100);
        map.put("age", 100);
        map.put("html", html);
        map.put("date", date);
        map.put("calendar", calendar);
        map.put("sqldate", sqldate);
        map.put("timestamp", timestamp);

        System.out.println(json.obj2Json(map));

        List maplist = new ArrayList<>();
        maplist.add(map);
        maplist.add(map);
        maplist.add(map);    
        System.out.println(json.obj2Json(maplist));    

        com.alibaba.fastjson2.JSONObject jsonObj = new com.alibaba.fastjson2.JSONObject();
        jsonObj.put("start_time",timestamp);
        System.out.println(jsonObj.toString());    

        String jsonString =com.alibaba.fastjson2.JSON.toJSONString(jsonObj);
        System.out.println("D1=> : " + jsonString);
        jsonString = jsonObj.toJSONString();
        System.out.println("D2=> : " + jsonString);
        
        List list = new ArrayList();
        list.add("test");
        list.add(new  Timestamp(System.currentTimeMillis()));
        list.add(new  Timestamp(System.currentTimeMillis()));
        test(list);
    }
}
