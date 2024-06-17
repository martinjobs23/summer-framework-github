package com.ceit.bootstrap;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson2.JSONObject;
import com.ceit.interceptor.InterceptorRegistry;
import com.ceit.ioc.BeanFactory;
import com.ceit.ioc.HandlerDefinition;
import com.ceit.ioc.annotations.RequestParam;
 
import com.ceit.response.Result;
import com.ceit.response.ResultCode;

/**
 * @author: ko
 * @date: 21.07.27 17:03:58
 * @description: 拦截所有请求，并进行分发处理
 */
@MultipartConfig
public class DispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private BeanFactory beanFactory = BeanFactory.getInstance();

    //框架不处理的URL
    private static final HashMap frameworkIgnoreUrlMap =new HashMap();

    //框架自动记录接收数据的URL
    private static final HashMap frameworkWriteLogUrlMap =new HashMap();

    public static void loadIgnoreUrlSetings(String configName) {

        if(configName !=null && !configName.trim().equals("framework.ignore.url"))
            return;

        String urls = ConfigLoader.getConfig("framework.ignore.url");
        if (urls != null && urls.length() > 0) {
            String[] urlArr = urls.split(",");
            for (String u : urlArr) {
                if (u.trim().length() > 0) {
                    frameworkIgnoreUrlMap.put(u, "");
                    System.out.println("设置 framework.ignore.url: " + u);
                }
            }
        } else {
            //删除
            System.out.println("删除 framework.ignore.url:");
            frameworkIgnoreUrlMap.clear();
        }

    }

    public static void loadWritelogUrlSetings(String configName){

        if(configName !=null && !configName.trim().equals("framework.writelog.url"))
            return;

        String urls = ConfigLoader.getConfig("framework.writelog.url");
        if(urls!=null && urls.length()>0) {
            String[] urlArr = urls.split(",");
            for(String u : urlArr) {
                String[] kvs= u.split("@");
                if(kvs.length>1) {
                    frameworkWriteLogUrlMap.put(kvs[0].trim(), kvs[1].trim());
                    System.out.println("设置 framework.writelog.url: " + kvs[0].trim() + "@" + kvs[1].trim());
                }

            }
        } else {
            System.out.println("删除 framework.writelog.url:");
            frameworkWriteLogUrlMap.clear();
        }
    }

    void printOut(HttpServletResponse resp, String data) {
        try {
            byte[] dataByteArr = data.getBytes("UTF-8");

            ServletOutputStream out = resp.getOutputStream();
            out.write(dataByteArr);
        } catch (IOException err) {
            // 不处理IOException
            // err.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String url = req.getRequestURI();
        String contextPath = req.getServletContext().getContextPath();
        if (contextPath.equals("/") == false) {
            // 忽略应用名称 比如/summer
            url = url.substring(contextPath.length());
        }
        if (!url.startsWith("/")) {
            printOut(resp, new Result(ResultCode.PARAM_ERROR).toString());
            return;
        }
        // 去掉?后面的参数 /user/doxxx?id=123
        int index = url.indexOf("?");
        if (index != -1) {
            url = url.substring(0, index);
        }

        // 获取处理器、拦截器
        HandlerDefinition definition = beanFactory.getHandlerDefinition(url);
        if (definition == null) {
        	//如果错误，使用JSON返回
            resp.setContentType("application/json;charset=utf-8");
            Result result = new Result("Method " + url + " Not Found", 404, null);
            printOut(resp, result.toString());
            return;
        }

        // 执行拦截器及逻辑
        InterceptorRegistry registry = ((InterceptorRegistry) req.getServletContext()
                .getAttribute("InterceptorRegistry")).getRegistryByUrl(url);
        if (!registry.prehandle(req, resp, definition)) {
            // printOut(resp, new Result(ResultCode.REFUSE_ACCESS));
            return;
        }

        //默认都为零
        ParameterType paramType =null;
        Map reqBody = new HashMap();
        JSONObject jsonObj = null;
        String bodyString = null;
        HttpServletRequest request =null;
        HttpServletResponse response =null;

        //框架是否要忽略读取数据
        boolean ignore =false;

        String contentType = req.getContentType();
        if(contentType==null) contentType = "";
        //如果是框架应该忽略不处理的URL， 文件上传multipart/form-data
        if(frameworkIgnoreUrlMap.containsKey(url) || contentType.contains("multipart/form-data") ) {
            request =req;
            response =resp;

            ignore =true;
        } else {
            //根据Controller的类型提供参数
            paramType = definition.getParameterType();

            //如果要使用request.getParameters,必须在req.getInputStream()之前就使用，否则获取不到了
            //要放在日志之前读取
            if(paramType.contains(ParameterType.Map)){
                reqBody = addReqestParams2Map(req, null);
            }
        }

        // 如果需要记录日志，记录日志
        Result writelogResult = null;
        if(frameworkWriteLogUrlMap.containsKey(url)){
            String logPath  = frameworkWriteLogUrlMap.get(url).toString();
            writelogResult = writeLogFile(req, url, logPath);
            if(writelogResult.getCode()== 500) {
                //写记录失败，直接返回
                printOut(resp, writelogResult.toString());
                return;
            } else if(writelogResult.getCode()== 404) {
                //写记录失败，但是没有读取数据，继续处理
                writelogResult= null;
            }
        }

        //如果框架需要读取数据
        if(ignore==false ) {

            //处理Requst参数
            if(paramType.contains(ParameterType.Req)){
                request = req;
            }

            //处理Response参数
            if(paramType.contains(ParameterType.Resp)){
                response = resp;
            }

            //需要打开数据流
            InputStream inputStream = null;
            if(paramType.contains(ParameterType.Str) ||
                paramType.contains(ParameterType.Json) ||
                    paramType.contains(ParameterType.Map)){


                if( writelogResult ==null) {
                    //request数据流
                    inputStream = req.getInputStream();
                } else {
                    //打开日志文件数据流
                    inputStream = Files.newInputStream((Path)writelogResult.getData());
                }
            }

            //处理bodyString参数
            if(paramType.contains(ParameterType.Str)){
                bodyString = getBodyString(req.getCharacterEncoding(), inputStream);
            }

            //处理jsonObj参数
            if(paramType.contains(ParameterType.Json)){

                if(paramType.contains(ParameterType.Str))
                {
                    if(bodyString!=null)
                        jsonObj = body2Json(bodyString);
                    else {
                        //bodystring读取失败
                    }
                }
                else
                    jsonObj = body2Json(inputStream);
            }

            //处理reqBody参数
            if(paramType.contains(ParameterType.Map)){

                if(!contentType.contains("application/x-www-form-urlencoded")){
                    Map tmpMap = null;

                    if(paramType.contains(ParameterType.Str) || paramType.contains(ParameterType.Json))
                    {
                        if(bodyString!=null)
                            tmpMap= body2Map(bodyString);

                        if(tmpMap==null && jsonObj!=null)
                        {
                            //尝试从Map转换
                            tmpMap = jsonObj.toJavaObject(Map.class);
                        }
                    }
                    else
                        tmpMap = body2Json(inputStream);

                    if(tmpMap!=null){
                        //如果是json数据，直接替换 parameter里面参数
                        if(contentType.contains("application/json")){
                            reqBody = tmpMap;
                        } else {
                            //如果是表单数据，合并到parameter里面
                            if(reqBody==null)
                                reqBody = tmpMap;
                            else
                                reqBody.putAll(tmpMap);
                        }
                    }
                    else {
                        //转换失败
                    }
                }
            }

            //关闭数据流
            try {
                if(inputStream!=null) {
                    inputStream.close();
                }
            } catch (Exception e) {
            }
        }

        //交给应用controller处理
        registry.posthandle(req, resp, definition, reqBody);
        Object result = execute(request, response ,bodyString, jsonObj, reqBody, definition);
        registry.afterCompletion(req, resp, definition, result);

        if (result != null) {
            String outStr ="";
            if (result instanceof Result) {
                outStr = result.toString();
            } else if (result instanceof String) {
                outStr = (String)result;
            } else
                outStr= com.alibaba.fastjson2.JSON.toJSONString(result,"yyyy-MM-dd HH:mm:ss");

            resp.setContentType("application/json;charset=utf-8");
            printOut(resp, outStr);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    /**
     * 请求分发、执行逻辑
     * 
     * @param req
     *                   当前的HTTP请求
     * @param resp
     *                   当前的HTTP响应
     * @param reqBody
     *                   请求参数
     * @param definition
     *                   请求处理函数{@link HandlerDefinition}
     * @return {@link Result}
     */
    private Object execute(HttpServletRequest req,
                           HttpServletResponse resp,
                           String bodyString,
                           JSONObject jsonObj,
                           Map reqBody,
                           HandlerDefinition definition) {
        definition.getBeanName();
        Object object = beanFactory.getBean(definition.getBeanName());
        Method method = definition.getMethod();
        Parameter[] parameters = definition.getParams();

        Object result = null;
        List<Object> params = new ArrayList<>();

        // 处理方法的参数
        try {
            // 注入对应参数
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    params.add(reqBody.get(requestParam.value()));
                } else {
                    Class<?> type = parameter.getType();
                    if (HttpServletRequest.class.isAssignableFrom(type)) {
                        params.add(req);
                    } else if (JSONObject.class.isAssignableFrom(type)) {
                        //JSONObject是Map的子类，先判判断，要不参数会传递错误
                        params.add(jsonObj);
                    } else if (Map.class.isAssignableFrom(type)) {
                        params.add(reqBody);
                    } else if (String.class.isAssignableFrom(type)) {
                        params.add(bodyString);
                    } else if (HttpServletResponse.class.isAssignableFrom(type)) {
                        params.add(resp);
                    } else {
                        params.add(null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(ResultCode.PARAM_ERROR, e.getMessage()) ;
        }

        // 执行方法
        try {
            Object ret = method.invoke(object, params.toArray());
            if (ret instanceof Result) {
                result = (Result) ret;
            } else {
                //如果是返回其他类型 String，自动变成 Result类型
                //兼容之前 jsonData字符串，如果[或者{开头，转JSONObj
                if(ret instanceof String) {
                    String retStr = (String)ret;
                    if(retStr.startsWith("[") || retStr.startsWith("{")) {
                        //转成JSON，如果转换失败，还用原值
                        try{
                            ret = com.alibaba.fastjson2.JSON.parse(retStr);
                        } catch (Exception jsonerr){
                            ret = retStr;
                        }
                    }
                }
                result = new Result(ResultCode.SUCCESS, ret);
            }

        } catch (Exception e) {
            e.printStackTrace();

            String msg = e.getMessage();
            if(e.getClass().equals(InvocationTargetException.class)) {
                //获取实际的错误信息
                InvocationTargetException tt=(InvocationTargetException)e ;
                if(tt!=null)  {
                    Throwable throwable =tt.getTargetException();
                    if(throwable!=null)
                        msg = throwable.getMessage();
                }
            }

            return new Result("Call " + definition.getBeanName() + "." + method.getName() + " Error", 500,
                    msg);
        }

        return result;
    }

    //从inputStream中读取数据，并写入日志文件
    private Result writeLogFile(HttpServletRequest req, String url, String logPath) {

        Result result = new Result("", 200, null);
        Path fullPath = null;
        try {
            //检查路径是否存在, 不存在，则尝试创建
            Path path = Paths.get(logPath);
            if ( ! Files.exists(path)  ) {
                Files.createDirectories(path);
            }

            // 检查路径是否存在
            String logfile =  new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(new Date()) + ".txt";
            fullPath = path.resolve(logfile);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(url+"获取或者创建日志路径错误", 404, e.getMessage());
        }

        //打开数据流
        ServletInputStream inputStream =null;
        try {
            inputStream = req.getInputStream();
            //使用java.nio.file.Files的copy方法来简化文件写入过程。这个方法需要Java 7及以上版本
            long len = Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);
            result.setData(fullPath);
        } catch (Exception e) {
            //错误
            e.printStackTrace();
            result.setCode(500);
            result.setMsg(url+ "写日志文件错误");
        }

        //关闭数据流
        try {
            if(inputStream!=null) {
                inputStream.close();
            }
        } catch (Exception e) {

        }

        return result;
    }

    private String getBodyString(String charEncoding, InputStream inputStream) {

        String postString = null;
        try {
            //如果没有指定编码，使用UTF8
            if (charEncoding == null) {
                charEncoding = "UTF-8";
            }

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            postString =result.toString(charEncoding);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return postString;
    }

    private JSONObject body2Json(String bodyString) {

        JSONObject result = null;

        //JSON参数
        try {
            result = com.alibaba.fastjson2.JSON.parseObject(bodyString, JSONObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private JSONObject body2Json(InputStream inputStream) {

        JSONObject result = null;

        //JSON参数
        try {
            result = com.alibaba.fastjson2.JSON.parseObject(inputStream, JSONObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private Map body2Map(String bodyString) {

        Map<String, Object> result = null;

        //JSON参数
        try {
            result = com.alibaba.fastjson2.JSON.parseObject(bodyString, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private Map body2Map(InputStream inputStream) {

        Map<String, Object> result = null;
        try {
            result = com.alibaba.fastjson2.JSON.parseObject(inputStream, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> addReqestParams2Map(HttpServletRequest request, Map<String, Object> reqBody) {

        if(reqBody==null)
            reqBody = new HashMap<>(16);

        //Get或者Post的参数，如果跟JSON名字一样，会覆盖JSON
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object obj = request.getParameter(name);

            //FIXME: 如果设置了忽略查询条件参数，如何处理 name?
            reqBody.put(name, obj);
        }

        return reqBody;
    }
}
