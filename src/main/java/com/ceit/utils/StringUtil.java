package com.ceit.utils;

public class StringUtil {
    /**
     * String为null或为""
     */
    public static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }

    /**
     * String[]为null或为""
     */
    public static boolean arrayIsEmpty(String[] str) {
        return str == null || str.length == 0;
    }

    /**
     * char 是否为字母
     */
    public static boolean isLetter(char c) {
        if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否包含英语
     */
    public static boolean hasLetter(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为json格式
     */
    public static boolean isJson(String str) {
    	
    	if(str==null || str.length() ==0)
    		return false;
    	
        str = str.trim();
        
        if(str.length() ==0)
    		return false;
        
        char firstChar = str.charAt(0);
        char lastChar = str.charAt(str.length() - 1);
        return (firstChar == '{' && lastChar == '}') || (firstChar == '[' && lastChar == ']');
    }

    public static boolean isFirstChar(char ch, String str) {
        char firstChar = str.charAt(0);
        return ch == firstChar;
    }

    public static boolean isLastChar(char ch, String str) {
        char lastChar = str.charAt(str.length() - 1);
        return ch == lastChar;
    }

    /**
     * 对象是否为基本数据类型或其包装类
     */
    public static boolean isPrimitive(Object obj) {
        if (obj == null) {
            return false;
        }
        return isPrimitiveByClass(obj.getClass());
    }

    /**
     * 是否为基本数据类型
     */
    public static boolean isPrimitiveByClass(Class<?> cls) {
        try {
            return ((Class<?>) cls.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获得字符串中的第一个英语单词
     */
    public static String getFirstWord(String str) {
       
        if(str==null)
            return null;
            
        StringBuilder sb = new StringBuilder();
        int i = 0;
        // 忽略前方无用字符
        for (; i < str.length(); i++) {
            char c = str.charAt(i);
            if (isLetter(c)) {
                break;
            }
        }
        for (; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!isLetter(c)) {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * str是否能匹配url
     * 
     * @param url
     *            请求路径，默认两段式/../..
     * @param str
     *            匹配规则
     * @return 是否
     */
    public static boolean urlPatterns(String url, String str) {
        if ("/*".equals(str)) {
            return true;
        }
        if (str.contains("*")) {
            String[] strings = str.split("\\*");
            // /../*
            if (strings.length == 1) {
                String subString = strings[0];
                return url.substring(0, subString.length()).equals(subString);
            }
            // /*/..
            if (strings.length == 2) {
                String subString = strings[1];
                return url.substring(url.length() - subString.length()).equals(subString);
            }
        }
        if (url.equals(str)) {
            return true;
        }
        return false;
    }
}
