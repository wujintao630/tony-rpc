package com.tonytaotao.rpc.common;

import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class URL {

    /**
     * 协议
     */
    private String protocol;

    /**
     * ip
     */
    private String host;

    /**
     * 端口
     */
    private int port;

    /**
     * 服务接口
     */
    private String path;

    /**
     * 服务注册参数
     */
    private Map<String, String> parameterMap;

    public URL(String protocol, String host, int port, String path, Map<String, String> parameterMap) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameterMap = parameterMap;
    }

    public String getVersion() {
        return getStrParameterByEnum(UrlParamEnum.version);
    }

    public String getGroup() {
        return getStrParameterByEnum(UrlParamEnum.group);
    }

    public String getStrParameter(String name) {
        return parameterMap.get(name);
    }

    public String getStrParameterByEnum(UrlParamEnum paramEnum) {
        String value = getStrParameter(paramEnum.getName());
        if (value == null) {
            return paramEnum.getDefaultValue();
        }
        return value;
    }

    public Integer getIntParameterByEnum(UrlParamEnum paramEnum) {
        String value = getStrParameter(paramEnum.getName());
        if (value == null || value.length() == 0) {
            return paramEnum.getIntValue();
        }
        return Integer.parseInt(value);
    }

    /**
     * 是否可以提供服务
     * @param refUrl
     * @return
     */
    public boolean canServe(URL refUrl) {
        if (refUrl == null || !this.getPath().equals(refUrl.getPath())) {
            return false;
        }

        if (!protocol.equals(refUrl.protocol)) {
            return false;
        }

        String version = getStrParameterByEnum(UrlParamEnum.version);
        String refVersion = refUrl.getStrParameterByEnum(UrlParamEnum.version);
        if (!version.equals(refVersion)) {
            return false;
        }
        // check serialize
        String serialize = getStrParameterByEnum(UrlParamEnum.serialization);
        String refSerialize = refUrl.getStrParameterByEnum(UrlParamEnum.serialization);
        if (!serialize.equals(refSerialize)) {
            return false;
        }
        return true;
    }

    public static URL parse(String url) {
        if (StringUtils.isBlank(url)) {
            throw new FrameworkRpcException("url is empty");
        }
        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = new HashMap<String, String>();;
        int i = url.indexOf("?"); // seperator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");

            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            }
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                }
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }

        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) {
            host = url;
        }
        return new URL(protocol, host, port, path, parameters);
    }

    /**
     * 获取url
     * @return
     */
    public String getUri() {
        return protocol + Constants.PROTOCOL_SEPARATOR + host + ":" + port + Constants.PATH_SEPARATOR + path;
    }

    /**
     * 获取url并带上参数
     * @return
     */
    public String getUriWithParam() {
        StringBuilder builder = new StringBuilder(1024);
        builder.append(getUri()).append("?");

        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            builder.append(name).append("=").append(value).append("&");
        }

        return builder.toString();
    }

    public URL clone0() {
        Map<String, String> params = new HashMap<String, String>();
        if (this.parameterMap != null) {
            params.putAll(this.parameterMap);
        }
        return new URL(protocol, host, port, path, params);
    }

    public String getHostPortString() {

        if (port <= 0) {
            return host;
        }

        int idx = host.indexOf(":");
        if (idx < 0) {
            return host + ":" + port;
        }

        int port = Integer.parseInt(host.substring(idx + 1));
        if (port <= 0) {
            return host.substring(0, idx + 1) + port;
        }
        return host;
    }


    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Override
    public String toString() {
        return getUri() + "?group=" + getGroup();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        URL url = (URL) o;

        if (port != url.port) {
            return false;
        }
        if (!protocol.equals(url.protocol)) {
            return false;
        }
        if (!host.equals(url.host)) {
            return false;
        }
        if (!path.equals(url.path)) {
            return false;
        }

        return !(parameterMap != null ? !parameterMap.equals(url.parameterMap) : url.parameterMap != null);

    }

    @Override
    public int hashCode() {
        int result = protocol.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + path.hashCode();
        result = 31 * result + (parameterMap != null ? parameterMap.hashCode() : 0);
        return result;
    }
}
