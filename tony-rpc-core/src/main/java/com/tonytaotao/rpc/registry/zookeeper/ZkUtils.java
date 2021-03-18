package com.tonytaotao.rpc.registry.zookeeper;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.Constants;

public class ZkUtils {

    public static String toGroupPath(URL url) {
        return Constants.ZOOKEEPER_REGISTRY_NAMESPACE + Constants.PATH_SEPARATOR + url.getGroup();
    }

    public static String toServicePath(URL url) {
        return toGroupPath(url) + Constants.PATH_SEPARATOR + url.getPath();
    }

    public static String toNodeTypePath(URL url, ZkNodeTypeEnum nodeType) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + nodeType.getValue();
    }

    public static String toNodePath(URL url, ZkNodeTypeEnum nodeType) {
        return toNodeTypePath(url, nodeType) + Constants.PATH_SEPARATOR + url.getHostPortString();
    }
}
