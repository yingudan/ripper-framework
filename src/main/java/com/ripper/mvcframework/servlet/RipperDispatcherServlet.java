package com.ripper.mvcframework.servlet;

import com.ripper.mvcframework.annotation.RipperAutowired;
import com.ripper.mvcframework.annotation.RipperController;
import com.ripper.mvcframework.annotation.RipperRequestMapping;
import com.ripper.mvcframework.annotation.RipperService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @Auther: yingd [RipperF@hotmail.com]
 * @Date:2022-04-22
 * @Description:com.ripper.mvcframwork
 * @Version:1.0
 **/
//@WebServlet(name = "ripper-mvc",urlPatterns = "/*",loadOnStartup = 1,initParams ={
//        @WebInitParam(name ="contextConfigLocation",value ="application.properties")
//})
public class RipperDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    //跟web.xml中param-name一致
    private static final String LOCATION = "contextConfigLocation";

    //保存所有的配置信息
    private Properties p = new Properties();

    //保存所有被扫描到的相关类名
    private List<String> classNames = new ArrayList<String>();

    //核心IOC容器,保存所有初始化的Bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存所有的Url和方法的映射关系
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();


    public RipperDispatcherServlet() {
        super();
    }

    /**
     * 初始化，加载配置文件
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //2、扫描所有相关类
        doScanner(p.getProperty("scanPackage"));

        //3、初始化所有相关类的实例,并保存到IOC容器中
        doInstance();

        //4、依赖注入
        doAutowired();

        //5、构造HandlerMapping
        initHandlerMapping();

        //6、等待请求、匹配URL,定位方法,反射调用执行
        //调用doGet或者doPost方法

        //提示信息
        System.out.println("Ripper mvcframework is init");


    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * 执行业务处理
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (this.handlerMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
//        String URL = req.getRequestURL().toString();
//        String contextPath = req.getContextPath();
//        System.out.println("url=="+url);
//        System.out.println("URL=="+URL);
//        System.out.println("contextPath=="+contextPath);
//        url = url.replace(contextPath, "").replaceAll("/+", "/");
//        System.out.println("进入doDispatch的url{}"+url+"handlerMapping=="+handlerMapping);
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!");
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        //获取方法参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称,做某些处理
            Class<?> parameterType = parameterTypes[i];
            if (HttpServletRequest.class == parameterType) {
                paramValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (parameterType == String.class) {

                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", "");
                    paramValues[i] = value;
                }
            }
        }
        try {
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            method.invoke(this.ioc.get(beanName), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 初始化加载配置文件
     *
     * @param location
     */
    private void doLoadConfig(String location) {
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream(location);
            p.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 递归扫描所有的class文件
     */
    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    /**
     * 首字母小写
     *
     * @return
     */
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * doInstance()方法，初始化所有相关的类，并放入到IOC容器之中。
     * IOC容器的key默认是类名首字母小写，如果是自己设置类名，
     * 则优先使用自定义的。因此，要先写一个针对类名首字母处理的工具方法。
     */
    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(RipperController.class)) {
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(RipperService.class)) {
                    RipperService service = clazz.getAnnotation(RipperService.class);
                    String beanName = service.value();
                    //如果用户设置了名字,就用用户的名字来设置
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //如果用户没有设置就按接口类型创建一个实例  ?多个实例spring 抛错
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * doAutowired()方法，
     * 将初始化到IOC容器中的类，需要赋值的字段进行赋值
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (!field.isAnnotationPresent(RipperAutowired.class)) {
                    continue;
                }
                RipperAutowired autowired = field.getAnnotation(RipperAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                //设置私有属性的访问权限
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * initHandlerMapping()方法，
     * 将GPRequestMapping中配置的信息和Method进行关联，并保存这些关系。
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(RipperController.class)) {
                continue;
            }
            String baseUrl = "";
            //获取RequestMapping的url配置
            if (clazz.isAnnotationPresent(RipperRequestMapping.class)) {
                RipperRequestMapping requestMapping = clazz.getAnnotation(RipperRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //获取Method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //没有RequestMapping注解的 直接忽略
                if (!method.isAnnotationPresent(RipperRequestMapping.class)) {
                    continue;
                }
                //映射Url
                RipperRequestMapping requestMapping = method.getAnnotation(RipperRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("mapper：" + url + "," + method);
            }

        }


    }


}
