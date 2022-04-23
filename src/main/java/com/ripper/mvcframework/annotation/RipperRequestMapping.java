package com.ripper.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @Auther: yingd [RipperF@hotmail.com]
 * @Date:2022-04-22
 * @Description:com.ripper.mvcframework.annotation
 * @Version:1.0
 **/

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RipperRequestMapping {
    String value() default "";
}
