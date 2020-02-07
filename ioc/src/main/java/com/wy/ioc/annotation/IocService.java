package com.wy.ioc.annotation;

import java.lang.annotation.*;

/*****
 * 自定义@IocService注解
 * @author     : MrFox
 * @date       : 2020-01-27 15:42
 * @description: 被此注解标注的类将会被beanFactory管理
 * @version    :
 ****/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IocService {
}
