package com.wy.ioc.bean.factory;

import com.sun.tools.javac.util.Assert;
import com.wy.common.execution.ServiceException;
import com.wy.ioc.annotation.IocResources;
import com.wy.ioc.annotation.IocService;
import lombok.extern.log4j.Log4j;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*****
 * bean管理工厂(上下文)
 * @author     : MrFox
 * @date       : 2020-02-07 11:06
 * @description:
 * @version    :
 ****/
@Log4j
public class IocClassPathApplicationContext {
    //扫描包范围
    private String basePackage;

    //bean的class信息 <beanID,Bean的无参构造>
    private ConcurrentHashMap<String, Object> noArgsBeanMaps = new ConcurrentHashMap<>();

    //bean的class信息 <beanID,Bean>
    private ConcurrentHashMap<String, Object> beanMaps = new ConcurrentHashMap<>();

    //bean的class信息 <class,BeanId>
    private ConcurrentHashMap<Class<?>, String> class2BeanMap = new ConcurrentHashMap<>();

    public IocClassPathApplicationContext(String basePackage){
        this.basePackage = basePackage;
        loadBean();
    }

    /**
     * 加载无参bean到map里
     */
    public void loadBean() {
        synchronized (this) {
            //1、扫描实现类 ,Reflections 依赖 Google 的 Guava 库和 Javassist 库
            Reflections reflections = new Reflections(basePackage);
            //2、获取该路径下所有包含Strategy注解的类
            Set<Class<?>> classList = reflections.getTypesAnnotatedWith(IocService.class);
            //使用无参构造创建bean
            initBean(classList);
            assignBean();

        }
    }

    /*****
     * 初始化Bean
     * @param
     * @return
     * @description:
     ****/
    private void assignBean() {
        //设置beanMap
        beanMaps = new ConcurrentHashMap<>(noArgsBeanMaps.size());

        for (String key : noArgsBeanMaps.keySet()) {
            Object bean = noArgsBeanMaps.get(key);
            Class clazz = bean.getClass();
            try {
                //获取所有参数
                Field[] declaredFields = clazz.getDeclaredFields();
                for (int i = 0; i < declaredFields.length; i++) {
                    Field field = declaredFields[i];
                    if (Objects.nonNull(field.getAnnotation(IocResources.class))) {
                        Object obj = noArgsBeanMaps.get(field.getName());
                        if (Objects.nonNull(obj)) {
                            Field declaredField = obj.getClass().getDeclaredField(key);
                            //判断是不是循环依赖
                            if(Objects.nonNull(declaredField) && Objects.nonNull(declaredField.getAnnotation(IocResources.class))){
                                field.setAccessible(Boolean.TRUE);
                                field.set(bean,noArgsBeanMaps.get(field.getName()));
                            }
                        } else {
                            throw new ServiceException("赋值" + field.getName() + "失败,beanMap内不存在该实例");
                        }
                    }
                }
                beanMaps.put(key,bean);
                log.info("[" + clazz + "] is assign");
            } catch (Exception e) {
                log.error("assign bean is error", e);
            }
        }
    }

    /*****
     * 使用无参构造创建bean
     * @param
     * @return
     * @description:
     ****/
    private void initBean(Set<Class<?>> classList) {
        for (Class clazz : classList) {
            try {
                String lowerCaseFirstOne = toLowerCaseFirstOne(clazz.getSimpleName());
                noArgsBeanMaps.put(lowerCaseFirstOne, clazz.getConstructor().newInstance());
                class2BeanMap.put(clazz, lowerCaseFirstOne);
                log.info("[" + lowerCaseFirstOne + "] is initialize");
            } catch (Exception e) {
                log.error("load bean is error", e);
            }
        }
    }

    public Object getBean(String beanId){
        Assert.checkNonNull(beanId,"bean名称不可为空");
        return beanMaps.get(beanId);
    }

    public <T>T getBean(Class<T> tClass){
        Assert.checkNonNull(tClass,"bean类名不可为空");
        return (T)beanMaps.get(class2BeanMap.get(tClass));
    }


    /**
     * 首字母转小写
     *
     * @param s
     * @return
     */
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }


}
