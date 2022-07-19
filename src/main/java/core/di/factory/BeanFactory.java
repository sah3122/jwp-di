package core.di.factory;

import com.google.common.collect.Maps;
import core.annotation.Inject;
import core.annotation.web.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(BeanFactory.class);

    private Set<Class<?>> preInstanticateBeans;

    private Map<Class<?>, Object> beans = Maps.newHashMap();

    public BeanFactory(Set<Class<?>> preInstanticateBeans) {
        this.preInstanticateBeans = preInstanticateBeans;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        return (T) beans.get(requiredType);
    }

    public void initialize() {
        initIndependentBeans();

        preInstanticateBeans.stream()
                            .filter(bean -> !beans.containsKey(bean))
                            .forEach(bean -> initDependentBeans(bean));
    }

    private void initIndependentBeans() {
        preInstanticateBeans.forEach(bean -> createIndependentBean(bean));
    }

    private void createIndependentBean(Class<?> bean) {
        Arrays.stream(bean.getDeclaredConstructors())
              .filter(constructor -> !constructor.isAnnotationPresent(Inject.class))
              .forEach(__ -> {
                  Object obj = BeanUtils.instantiateClass(bean);
                  Arrays.stream(bean.getInterfaces()).forEach(aClass -> beans.put(aClass, obj));
                  beans.put(bean, obj);
              });
    }

    private void initDependentBeans(Class<?> bean) {
        Arrays.stream(bean.getDeclaredConstructors())
              .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
              .forEach(constructor -> createDependentBean(bean, constructor));
    }

    private void createDependentBean(Class<?> bean, Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Arrays.stream(parameterTypes)
              .filter(parameterType -> !beans.containsKey(parameterType))
              .forEach(parameterType -> initDependentBeans(parameterType));

        List<Object> parameters = Arrays.stream(parameterTypes)
                                     .map(parameterType -> beans.get(parameterType))
                                     .collect(Collectors.toList());
        Object obj = BeanUtils.instantiateClass(constructor, parameters.toArray());

        Arrays.stream(bean.getInterfaces()).forEach(aClass -> beans.put(aClass, obj));
        beans.put(bean, obj);
    }

    public Set<Object> getControllers() {
        return beans.keySet()
                     .stream()
                     .filter(beanClass -> beanClass.isAnnotationPresent(Controller.class))
                     .map(controllerBean -> beans.get(controllerBean))
                     .collect(Collectors.toSet());
    }
}
