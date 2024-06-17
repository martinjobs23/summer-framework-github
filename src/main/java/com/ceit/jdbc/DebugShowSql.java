package com.ceit.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceit.bootstrap.ConfigLoader;

public class DebugShowSql {
    
    public static boolean show_sql = false;
    private static Logger logger = LoggerFactory.getLogger(DebugShowSql.class);

    static {
        String debugvalue = ConfigLoader.getConfig("debug.showsql");

        if(debugvalue!=null &&  ( debugvalue.equals("1") || debugvalue.equals("true") ))
        {
            show_sql = true;
        }

        logger.info("DebugShowSql get debug.showsql : "+ show_sql);
    }

    public static void loadDebugShowSql(String configName) {
        if(configName !=null && !configName.trim().equals("framework.debug.showsql"))
            return;

        String debugvalue = ConfigLoader.getConfig("framework.debug.showsql");
        if(debugvalue!=null &&  ( debugvalue.equals("1") || debugvalue.equals("true") ))
        {
            show_sql = true;
        } else {
            show_sql = false;
        }

        logger.info("设置 DebugShowSql debug.showsql : "+ show_sql);
    }

    public static void println(String funcName, String sql, Object... param)
    {
        if(show_sql==false)
            return;

        StringBuilder sb=new StringBuilder();

        sb.append(funcName + " SQL : "+ sql );
       
        if(param!=null)
        {
            for (Object object : param) {
                sb.append("\r\n" + funcName + " PARAM : "+ object);
            }
        }

        logger.info(sb.toString());
    }

}
