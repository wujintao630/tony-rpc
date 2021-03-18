package com.tonytaotao.rpc.springsupport;

import com.tonytaotao.rpc.common.config.ApplicationConfig;
import com.tonytaotao.rpc.common.config.ProtocolConfig;
import com.tonytaotao.rpc.common.config.RegistryConfig;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tony
 */
public class XmlNamespaceHandler extends NamespaceHandlerSupport {
    public final static Set<String> protocolDefineNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final static Set<String> registryDefineNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final static Set<String> serviceConfigDefineNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final static Set<String> referenceConfigDefineNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final static Set<String> applicationConfigDefineNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void init() {
        registerBeanDefinitionParser("reference", new XmlBeanDefinitionParser(ReferenceConfigBean.class, false));
        registerBeanDefinitionParser("service", new XmlBeanDefinitionParser(ServiceConfigBean.class, true));
        registerBeanDefinitionParser("registry", new XmlBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("protocol", new XmlBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("application", new XmlBeanDefinitionParser(ApplicationConfig.class, true));
    }
}
