package com.tonytaotao.rpc.common.util;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectUtils {

    private static final String SETTER_PREFIX = "set";

    public static Method getWriteMethod(Class<?> clazz, String propertyName, Class<?> parameterType) {
        String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(propertyName);
        return getAccessibleMethod(clazz, setterMethodName, parameterType);
    }


    public static Method getAccessibleMethod(final Class<?> clazz, final String methodName, Class<?>... parameterTypes) {

        // 处理原子类型与对象类型的兼容
        ClassUtils.wrapClasses(parameterTypes);

        for (Class<?> searchType = clazz; searchType != Object.class; searchType = searchType.getSuperclass()) {
            try {
                Method method = searchType.getDeclaredMethod(methodName, parameterTypes);
                if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                        && !method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                // Method不在当前类定义,继续向上转型
            }
        }
        return null;
    }

}
