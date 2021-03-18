package com.tonytaotao.rpc.springsupport;

import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.common.config.ProtocolConfig;
import com.tonytaotao.rpc.common.config.ReferenceConfig;
import com.tonytaotao.rpc.common.config.RegistryConfig;
import com.tonytaotao.rpc.common.util.FrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;

@Slf4j
public class ReferenceConfigBean<T> extends ReferenceConfig<T> implements FactoryBean<T>, BeanFactoryAware, InitializingBean, DisposableBean {

    private transient BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public T getObject() throws Exception {
        return get();
    }

    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        log.debug("check reference interface:%s config", getInterfaceName());
        //检查依赖的配置
        checkApplication();
        checkProtocolConfig();
        checkRegistryConfig();

        if(StringUtils.isEmpty(getGroup())) {
            setGroup(UrlParamEnum.group.getDefaultValue());
        }
        if(StringUtils.isEmpty(getVersion())) {
            setVersion(UrlParamEnum.version.getDefaultValue());
        }

        if(getTimeout()==null) {
            setTimeout(UrlParamEnum.requestTimeout.getIntValue());
        }
        if(getRetries()==null) {
            setRetries(UrlParamEnum.retries.getIntValue());
        }
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
