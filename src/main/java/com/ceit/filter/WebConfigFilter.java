package com.ceit.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ceit.bootstrap.ConfigLoader;

/**
 * @author: ko
 * @date: 21.07.27 17:04:57
 * @description: web常用配置
 */
@WebFilter(urlPatterns = "/*")
public class WebConfigFilter implements Filter {

    public static String cors_origin = null;
    public static String cors_methods = null;
    public static String cors_headers = null;
    public static String cors_maxage = null;
    public static String cors_credentials = null;
    
    public static void LoadConfig() {
    	
        cors_origin = ConfigLoader.getConfig("cors.origin");
        cors_methods = ConfigLoader.getConfig("cors.methods");
        cors_headers = ConfigLoader.getConfig("cors.headers");
        cors_maxage = ConfigLoader.getConfig("cors.maxage");
        cors_credentials = ConfigLoader.getConfig("cors.credentials");

        System.out.println("WebConfigFilter get cors.origin : "+ cors_origin);
        System.out.println("WebConfigFilter get cors.methods : "+ cors_methods);
        System.out.println("WebConfigFilter get cors.headers : "+ cors_headers);
        System.out.println("WebConfigFilter get cors.maxage : "+ cors_maxage);
        System.out.println("WebConfigFilter get cors.credentials : "+ cors_credentials);

        //System.clearProperty("cors.origin");
        //System.clearProperty("cors.methods");
        //System.clearProperty("cors.headers");
       // System.clearProperty("cors.maxage");
       // System.clearProperty("cors.credentials");
    }
 

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
                
        // 对request和response进行一些预处理
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        //文件下载不能使用application/json
        //response.setContentType("application/json;charset=utf-8");

        //根据配置设置 CORS 跨域资源共享
        HttpServletResponse res = (HttpServletResponse) response;
        
        if( cors_origin !=null)
            res.setHeader("Access-Control-Allow-Origin", cors_origin);

        if( cors_methods !=null)
            res.setHeader("Access-Control-Allow-Methods", cors_methods);

        if( cors_headers !=null)
            res.setHeader("Access-Control-Allow-Headers", cors_headers);

        if( cors_maxage !=null)
            res.setHeader("Access-Control-Max-Age", cors_maxage);

        if( cors_credentials !=null)
            res.setHeader("Access-Control-Allow-Credentials", cors_credentials);

        HttpServletRequest req = (HttpServletRequest) request;
        String method = req.getMethod();
        if (method.equalsIgnoreCase("OPTIONS")) {
            res.getOutputStream().write("Success".getBytes("utf-8"));
        }

        chain.doFilter(request, res);
    }

}
