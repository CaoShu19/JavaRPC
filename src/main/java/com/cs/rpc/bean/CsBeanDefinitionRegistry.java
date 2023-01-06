package com.cs.rpc.bean;

import com.cs.rpc.annotation.EnableHttpClient;
import com.cs.rpc.annotation.CsHttpClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

/**
 * 让自己定义的注解实现的作用类
 *
 * 1. ImportBeanDefinitionRegistrar类只能通过其他类@Import的方式来加载，通常是启动类或配置类。
 * 2. 使用@Import，如果括号中的类是ImportBeanDefinitionRegistrar的实现类，则会调用接口方法，将其中要注册的类注册成bean
 * 3. 实现该接口的类拥有注册bean的能力
 */
public class CsBeanDefinitionRegistry implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, EnvironmentAware {

    private Environment environment;
    private ResourceLoader resourceLoader;

    public CsBeanDefinitionRegistry(){}

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    /**
     * ImportBeanDefinitionRegistrar接口中的registerBeanDefinitions()方法
     *  会在spring启动的时候自动运行,就会运行你自定义的方法
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata,
                                        BeanDefinitionRegistry registry){
        registerCsHttpClient(metadata,registry);
    }
    /**
     * 将CsHttpClient所标识的接口生成代理类,将其注册到容器中
     */
    private void registerCsHttpClient(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        //依靠 AnnotationMetadata 接口判断是否存在指定元注解

        //拿到注解EnableHttpClient中的属性,并将属性值存入annotationAttributes
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(EnableHttpClient.class.getName());
        //获取EnableHttpClient中的属性
        Object basePackage = annotationAttributes.get("basePackage");
        for (String key:annotationAttributes.keySet()){
            System.out.println(annotationAttributes.get(key));
        }
        if(basePackage != null){
            //通过注解属性值拿到要扫描的包的路径
            String basePath = basePackage.toString();

            //ClassPathScanningCandidateComponentProvider是Spring提供的工具，可以按自定义的类型，查找classpath下符合要求的class文件
            //定义一个扫描器scanner
            ClassPathScanningCandidateComponentProvider scanner = getScanner();
            //配置扫描器scanner的资源环境
            scanner.setResourceLoader(resourceLoader);

            //将扫描CsHttpClient注解的注解过滤器配置到扫描器中
            AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(CsHttpClient.class);
            //注意这里要用addIncludeFilter()方法添加注解过滤器
            scanner.addIncludeFilter(annotationTypeFilter);

            //运行扫描器,去扫描basePath路径下的CsHttpClient注解,得到bean定义集合
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePath);

            for (BeanDefinition candidateComponent : candidateComponents) {
                //BeanDefinition是bean定义,可以拿到bean的所有信息

                //判断bean是否是AnnotationTypeDefinition
                if(candidateComponent instanceof AnnotatedBeanDefinition){
                    //若是注解bean定义,那么强转一下
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    //获取到注解Bean的元数据
                    AnnotationMetadata annotationMetadata = annotatedBeanDefinition.getMetadata();

                    //添加一个断言,判定一下这个注解使用用在接口上
                    Assert.isTrue(annotationMetadata.isInterface(),"CsHttpClient注解只能定义再接口上");



                    //断言通过后,获取到具体注解属性
                    Map<String, Object> httpclientAttributes = annotationMetadata.getAnnotationAttributes(CsHttpClient.class.getName());

                    //获取注解中的属性值,ClientHttp中value属性是逻辑bean的名称
                    String beanName = getClientName(httpclientAttributes);

                    //将bean注册到ioc容器中,接口无法实例化,所以生成代理实例化bean再注册
                    //BeanDefinitionBuilder通过实现了FactoryBean的CsHttpClientFactoryBean工厂对象将注解bean生成
                    //其中CsHttpClientFactoryBean中会有具体实例化注解的方法
                    //代理实现对象beanDefinitionBuilder,实现接口转化为类
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(CsHttpClientFactoryBean.class);
                    //给代理类添加属性值:
                    //   会将被代理的CsHttpClientFactoryBean中的名为interfaceClass的属性值赋值为annotationMetadata.getClassName()
                    beanDefinitionBuilder.addPropertyValue("interfaceClass",annotationMetadata.getClassName());

                    //断言beanName找到了,才将器注入IOC容器中
                    assert beanName != null;

                    registry.registerBeanDefinition(beanName,beanDefinitionBuilder.getBeanDefinition());

                }
            }
        }

        //

    }

    /**
     *获取注解属性值
     */
    private String getClientName(Map<String, Object> clientAnnotationAttributes) {
        if (clientAnnotationAttributes == null){
            throw new RuntimeException("value必须有值");
        }
        Object value = clientAnnotationAttributes.get("value");
        if (value != null && !value.toString().equals("")){
            return value.toString();
        }
        return null;
    }

    /**
     * ClassPathScanningCandidateComponentProvider 是 spring 的一个内部工具类，可以帮助我们从包路径中获取到所需的 BeanDefinition 集合，
     * 然后动态注册 BeanDefinition 到 BeanDefinitionRegistry，到达在容器中动态生成 Bean 的目的。
     * //此方法使用Feign组件中学习的
     */
    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }


}
