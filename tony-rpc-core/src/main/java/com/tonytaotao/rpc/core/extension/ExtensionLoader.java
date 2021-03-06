package com.tonytaotao.rpc.core.extension;

import com.tonytaotao.rpc.common.util.ClassUtils;
import com.tonytaotao.rpc.common.util.ReflectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExtensionLoader<T> {

    private static final String PREFIX = "META-INF/extensions/";

    private static final ConcurrentHashMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    private ConcurrentMap<String, Class<T>> extensionClasses;

    private ConcurrentMap<String, T> singletonInstances;

    private transient volatile boolean initialized = false;

    private ClassLoader classLoader;

    private Class<T> clazz;

    private String defaultExtensionName;

    private ExtensionLoader(Class<T> clazz){
        this(clazz, ClassUtils.getClassLoader(clazz));
    }

    private ExtensionLoader(Class<T> clazz, ClassLoader classLoader){
        this.clazz = clazz;
        this.classLoader = classLoader;
        this.defaultExtensionName = clazz.getAnnotation(SPI.class).value();
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> clazz) {

        checkInterfaceType(clazz);

        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(clazz);
        if (loader == null) {
            loader = new ExtensionLoader<>(clazz);
            ExtensionLoader<T> old = (ExtensionLoader<T>) EXTENSION_LOADERS.putIfAbsent(clazz, loader);
            if(old!=null){
                loader = old;
            }
        }
        return loader;
    }

    private static void checkInterfaceType(Class<?> clazz) {

        if (clazz == null) {
            failThrows(clazz, "Extension type == null");
        }
        if (!clazz.isInterface()) {
            failThrows(clazz, "Error extension type is not interface");
        }
        if(!withSpiAnnotation(clazz)){
            failThrows(clazz, "type:" + clazz.getName() +
                    " is not a extension, because WITHOUT @SPI Annotation!");
        }
    }

    private void checkExtensionType(Class<T> clz) {
        checkClassPublic(clz);

        checkConstructorPublic(clz);

        checkClassInherit(clz);
    }

    private void checkClassInherit(Class<T> clz) {
        if (!clazz.isAssignableFrom(clz)) {
            failThrows(clz, "Error is not instanceof " + clazz.getName());
        }
    }

    private void checkClassPublic(Class<T> clz) {
        if (!Modifier.isPublic(clz.getModifiers())) {
            failThrows(clz, "Error is not a public class");
        }
    }

    private void checkConstructorPublic(Class<T> clz) {
        Constructor<?>[] constructors = clz.getConstructors();

        if (constructors == null || constructors.length == 0) {
            failThrows(clz, "Error has no public no-args constructor");
        }

        for (Constructor<?> constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterTypes().length == 0) {
                return;
            }
        }

        failThrows(clz, "Error has no public no-args constructor");
    }

    /**??????SPI????????????*/
    public T getExtension(String name) {
        if (StringUtils.isEmpty(name)){
            failThrows(clazz, "Extension name == null");
        }

        checkInit();

        SPI spi = clazz.getAnnotation(SPI.class);
        if (spi.scope() == Scope.SINGLETON) {
            return getSingletonInstance(name);
        } else {
            T extension = createExtension(name);
            return extension;
        }

    }

    public T getSingletonInstance(String name) {

        T instance = singletonInstances.get(name);
        if (instance == null) {

            instance = createExtension(name);
            T old = singletonInstances.putIfAbsent(name, instance);
            if(old!=null){
                instance = old;
            }
        }
        return instance;
    }


    private T createExtension(String name) {
        Class<?> clazz = getExtensionClass(name);
        try {
            return inject((T) clazz.newInstance(), clazz);
        } catch (Throwable t) {
            String msg = "Fail to create extension " + name + " of extension point " + clazz.getName() + ", cause: " + t.getMessage();
            throw new IllegalStateException(msg, t);
        }
    }

    private T inject(T instance, Class<?> clz) throws IntrospectionException {

        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields){
            Class<?> fieldType = field.getType();
            if (fieldType.isInterface() && withSpiAnnotation(fieldType)) {

                if(field.isAnnotationPresent(Resource.class)){  //??????????????????
                    String name = field.getAnnotation(Resource.class).name();
                    Object obj = ExtensionLoader.getExtensionLoader(fieldType).getExtension(name);
                    field.setAccessible(true);
                    try {
                        field.set(instance, obj);
                    } catch (IllegalAccessException e) {
                        String errMsg = "Fail to inject via field " + field.getName() + " of interface to extension implementation " + instance.getClass() + " for extension point " + clazz.getName();
                        throw new IllegalStateException(errMsg, e);
                    }
                } else {    //setter

                    String name = field.getName();
                    Object obj = ExtensionLoader.getExtensionLoader(fieldType).getExtension(name);
                    if(obj==null){
                        continue;
                    }
                    Method method = ReflectUtils.getWriteMethod(clz, name, field.getType());
                    if(method!=null){
                        try {
                            method.invoke(instance, obj);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return instance;
    }

    private static boolean withSpiAnnotation(Class cls){
        return cls.isAnnotationPresent(SPI.class);
    }

    private Class<?> getExtensionClass(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }

        Class<?> clazz = extensionClasses.get(name);
        if (clazz == null) {
            throw new IllegalArgumentException("not find extension with name:"+name);
        }
        return clazz;
    }

    private void checkInit() {
        if (!initialized) {
            loadExtensionClasses();
        }
    }

    private synchronized void loadExtensionClasses() {
        if (initialized) {
            return;
        }

        this.extensionClasses = loadExtensionClasses(PREFIX);
        this.singletonInstances = new ConcurrentHashMap<>();
        initialized = true;
    }

    private synchronized ConcurrentMap<String, Class<T>> loadExtensionClasses(String prefix) {
        String fullName = null;
        try {
            ConcurrentMap<String, Class<T>> extName2Class = new ConcurrentHashMap<>();
            ClassLoader classLoader = this.classLoader;
            fullName = prefix + clazz.getName();

            Enumeration<URL> urls = classLoader.getResources(fullName);
            if (urls == null || !urls.hasMoreElements()) {
                return extName2Class;
            }

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                readExtensionClasses0(extName2Class, classLoader, url);
            }

            return extName2Class;
        } catch (Throwable t) {
            throw new IllegalArgumentException("Exception when load extension point(interface: " + clazz.getName() + ", description file: " + fullName + ").", t);
        }
    }

    private void readExtensionClasses0(ConcurrentMap<String, Class<T>> extName2Class, ClassLoader classLoader, URL url) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            String line = null;
            while ((line=br.readLine())!=null){

                String config = line;
                if(config.startsWith("#")){ //????????????
                    continue;
                }
                String name = null;
                String extImpl = null;
                int i = config.indexOf('=');
                if (i > 0) {
                    name = config.substring(0, i).trim();
                    extImpl = config.substring(i + 1).trim();
                }
                if (StringUtils.isEmpty(name)) {
                    throw new IllegalStateException(
                            "missing extension name, config value: " + config);
                }
                try {
                    Class<T> clazz;
                    if (classLoader == null) {
                        clazz = (Class<T>) Class.forName(extImpl);
                    } else {
                        clazz = (Class<T>) Class.forName(extImpl, true, classLoader);
                    }
                    //????????????
                    checkExtensionType(clazz);

                    if (extName2Class.containsKey(name)) {
                        if (extName2Class.get(name) != clazz) {
                            throw new IllegalStateException("Duplicate extension " + clazz.getName() + " name " + name + " on " + clazz.getName() + " and " + clazz.getName());
                        }
                    } else {
                        extName2Class.put(name, clazz);
                    }
                } catch (Throwable e){
                    throw new IllegalStateException("Failed to load config line(" + line + ") of config file(" + url + ") for extension(" + clazz.getName() + ")", e);
                }
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Exception when load extension class(interface: " + clazz.getName(), t);
        }finally {
            if(br!=null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static <T> void failThrows(Class<T> clazz, String msg) {
        throw new IllegalStateException(clazz.getName() + ": " + msg);
    }
}