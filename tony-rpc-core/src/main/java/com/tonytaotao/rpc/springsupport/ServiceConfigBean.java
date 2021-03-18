package com.tonytaotao.rpc.springsupport;

import com.tonytaotao.rpc.common.config.ProtocolConfig;
import com.tonytaotao.rpc.common.config.RegistryConfig;
import com.tonytaotao.rpc.common.config.ServiceConfig;
import com.tonytaotao.rpc.common.util.FrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

@Slf4j
public class ServiceConfigBean<T> extends ServiceConfig<T> implements BeanFactoryAware, InitializingBean, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private transient BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isExported()) {
            export();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        log.debug("check service interface:%s config", getInterfaceName());
        checkApplication();
        checkRegistryConfig();
        checkProtocolConfig();
    }

    @Override
    public void destroy() throws Exception {
        super.destroy0();
    }

    private void checkRegistryConfig() {
        if (CollectionUtils.isEmpty(getRegistries())) {
            for (String name : XmlNamespaceHandler.registryDefineNames) {
                RegistryConfig rc = beanFactory.getBean(name, RegistryConfig.class);
                if (rc == null) {
                    continue;
                }
                if (XmlNamespaceHandler.registryDefineNames.size() == 1) {
                    setRegistry(rc);
                } else if (rc.isDefault() != null && rc.isDefault().booleanValue()) {
                    setRegistry(rc);
                }
            }
        }
        if (CollectionUtils.isEmpty(getRegistries())) {
            setRegistry(FrameworkUtils.getDefaultRegistryConfig());
        }
    }

    private void checkProtocolConfig() {
        if (CollectionUtils.isEmpty(getProtocols())) {
            for (String name : XmlNamespaceHandler.protocolDefineNames) {
                ProtocolConfig pc = beanFactory.getBean(name, ProtocolConfig.class);
                if (pc == null) {
                    continue;
                }
                if (XmlNamespaceHandler.protocolDefineNames.size() == 1) {
                    setProtocol(pc);
                } else if (pc.isDefault() != null && pc.isDefault().booleanValue()) {
                    setProtocol(pc);
                }
            }
        }
        if (CollectionUtils.isEmpty(getProtocols())) {
            setProtocol(FrameworkUtils.getDefaultProtocolConfig());
        }
    }
}
