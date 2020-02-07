package com.wy.ioc.bean;

import com.wy.ioc.annotation.IocResources;
import com.wy.ioc.annotation.IocService;

@IocService
public class Bean2 {
    @IocResources
    private Bean1 bean1;

    public void sayHello() {
        System.out.println("hello");
    }
}
