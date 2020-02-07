package com.wy.ioc.annotation;

import java.lang.annotation.*;

/*****
 * 自定义@IocResources
 * @author     : MrFox
 * @date       : 2020-01-27 15:42
 * @description: 被此注解标注的类将会在bean初始化时赋值,前提是他是一个bean
 * @version    :
 ****/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IocResources {
}
