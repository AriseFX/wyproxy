package com.wy.ioc.bean;

import com.wy.ioc.annotation.IocResources;
import com.wy.ioc.annotation.IocService;

@IocService
public class Bean1 {

    @IocResources
    private Bean2 bean2;

    public void testBean2(){
        bean2.sayHello();
    }
}
