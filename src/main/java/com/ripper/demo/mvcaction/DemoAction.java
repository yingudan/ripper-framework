package com.ripper.demo.mvcaction;

import com.ripper.demo.service.impl.DemoServiceImpl;
import com.ripper.mvcframework.annotation.RipperAutowired;
import com.ripper.mvcframework.annotation.RipperController;
import com.ripper.mvcframework.annotation.RipperRequestMapping;
import com.ripper.mvcframework.annotation.RipperRequestParam;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Auther: yingd [RipperF@hotmail.com]
 * @Date:2022-04-22
 * @Description:com.ripper.demo.mvcaction
 * @Version:1.0
 **/

@RipperController
@RipperRequestMapping("/demo/")
public class DemoAction {

    @RipperAutowired
    private DemoServiceImpl demoService;

    @RipperRequestMapping("query")
    public void query(HttpServletRequest request, HttpServletResponse response, @RipperRequestParam("name") String name) {
        try {
            System.out.println("name>>>" + name);
            String result = "My name is " + name;
            response.getWriter().write(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
