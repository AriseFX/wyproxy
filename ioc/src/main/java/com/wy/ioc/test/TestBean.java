package com.wy.ioc.test;

import com.wy.ioc.bean.Bean1;
import com.wy.ioc.bean.factory.IocClassPathApplicationContext;

public class TestBean {
    public static void main(String[] args) {
        IocClassPathApplicationContext context = new IocClassPathApplicationContext("com.wy.ioc.bean");
        Bean1 bean1 = (Bean1) context.getBean("bean1");
        bean1.testBean2();
        System.out.println(bean1);
    }
}
