package com.tonytaotao.rpc.common.config;


import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.registry.ServiceCommon;
import com.tonytaotao.rpc.common.Constants;
import com.tonytaotao.rpc.common.util.NetUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.*;

public class AbstractServiceConfig extends AbstractXmlConfig {

    private static final long serialVersionUID = 3928005245888186559L;
    protected String interfaceName;
    protected String group;
    protected String version;
    protected Integer timeout;
    protected Integer retries;

    // 应用信息
    protected ApplicationConfig application;

    //server暴露服务使用的协议，暴露可以使用多种协议，但client只能用一种协议进行访问，原因是便于client的管理
    protected List<ProtocolConfig> protocols;
    // 注册中心的配置列表
    protected List<RegistryConfig> registries;

    // 是否进行check，如果为true，则在监测失败后抛异常
    protected Boolean check = Boolean.TRUE;

    protected List<URL> loadRegistryUrls() {
        List<URL> registryList = new ArrayList<URL>();
        if (registries != null && !registries.isEmpty()) {
            for (RegistryConfig config : registries) {
                String address = config.getAddress();
                String protocol = config.getProtocol();
                if (StringUtils.isBlank(address)) {
                    address = NetUtils.LOCALHOST + Constants.HOST_PORT_SEPARATOR + Constants.DEFAULT_INT_VALUE;
                    protocol = Constants.REGISTRY_PROTOCOL_LOCAL;
                }
                Map<String, String> map = new HashMap<>();
                map.put(UrlParamEnum.application.getName(), StringUtils.isNotEmpty(application.getName()) ? application.getName() : UrlParamEnum.application.getDefaultValue());
                map.put(UrlParamEnum.path.getName(), ServiceCommon.class.getName());
                map.put(UrlParamEnum.registryAddress.getName(), String.valueOf(address));
                map.put(UrlParamEnum.registryProtocol.getName(), String.valueOf(protocol));
                map.put(UrlParamEnum.timestamp.getName(), String.valueOf(System.currentTimeMillis()));
                map.put(UrlParamEnum.protocol.getName(), protocol);

                Integer connectTimeout = UrlParamEnum.registryConnectTimeout.getIntValue();
                if(config.getConnectTimeout()!=null) {
                    connectTimeout = config.getConnectTimeout();
                }
                map.put(UrlParamEnum.registryConnectTimeout.getName(), String.valueOf(connectTimeout));

                Integer sessionTimeout = UrlParamEnum.registrySessionTimeout.getIntValue();
                if(config.getSessionTimeout()!=null) {
                    sessionTimeout = config.getSessionTimeout();
                }
                map.put(UrlParamEnum.registrySessionTimeout.getName(), String.valueOf(sessionTimeout));

                String[] arr = address.split(Constants.HOST_PORT_SEPARATOR);
                URL url = new URL(protocol, arr[0], Integer.parseInt(arr[1]), ServiceCommon.class.getName(), map);
                registryList.add(url);
            }
        }
        return registryList;
    }

    protected String getLocalHostAddress(ProtocolConfig protocol) {
        String hostAddress = protocol.getHost();
        if (StringUtils.isBlank(hostAddress)) {
            hostAddress = getLocalHostAddress();
        }
        /**
         * @TODO 特意返回本机默认地址，实际上应该是本机的实际IP地址
         */
        //return hostAddress;
        return "127.0.0.1";
    }

    protected Integer getProtocolPort(ProtocolConfig protocol) {
        Integer port = protocol.getPort();
        if(port==null || port<1) {
            port = Constants.DEFAULT_PORT;
        }
        return port;
    }

    protected String getLocalHostAddress() {
        InetAddress address = NetUtils.getLocalAddress();
        if(address==null || StringUtils.isBlank(address.getHostAddress())){
            throw new FrameworkRpcException("retrieve local host address failure. Please config <tonyrpc:protocol host='...' />");
        }
        return address.getHostAddress();
    }

    protected void checkApplication() {
        if (application == null) {
            application = new ApplicationConfig();
        }
        if (application == null) {
            throw new IllegalStateException(
                    "No such application config! Please add <tonyrpc:application name=\"...\" /> to your spring config.");
        }
        application.setName(UUID.randomUUID().toString());
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }

    public List<ProtocolConfig> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolConfig> protocols) {
        this.protocols = protocols;
    }

    public void setProtocol(ProtocolConfig protocol) {
        this.protocols = Collections.singletonList(protocol);
    }
    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        this.registries = registries;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registries = Collections.singletonList(registry);
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }
}
