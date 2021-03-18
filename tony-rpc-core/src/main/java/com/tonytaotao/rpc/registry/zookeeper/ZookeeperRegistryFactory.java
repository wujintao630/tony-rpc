package com.tonytaotao.rpc.registry.zookeeper;

import com.google.common.base.Preconditions;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.registry.NotifyListener;
import com.tonytaotao.rpc.common.Constants;
import com.tonytaotao.rpc.registry.ServiceCommon;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ZookeeperRegistryFactory implements ServiceCommon {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryFactory.class);

    protected Set<URL> registeredServiceUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private URL registryUrl;

    private final ReentrantLock clientLock = new ReentrantLock();
    private final ReentrantLock serverLock = new ReentrantLock();
    private final ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> serviceListeners = new ConcurrentHashMap<>();

    private ZkClient zkClient;

    public ZookeeperRegistryFactory(URL url, ZkClient zkClient) {
        this.registryUrl = url.clone0();
        this.zkClient = zkClient;
        IZkStateListener zkStateListener = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                // do nothing
            }

            @Override
            public void handleNewSession() throws Exception {
                logger.info("zkRegistry get new session notify.");

            }

            @Override
            public void handleSessionEstablishmentError(Throwable throwable) throws Exception {

            }
        };
        this.zkClient.subscribeStateChanges(zkStateListener);
    }


    @Override
    public void register(URL url) throws Exception {
        Preconditions.checkNotNull(url);
        doRegister(url.clone0());
        registeredServiceUrls.add(url);
    }

    @Override
    public void unregister(URL url) throws Exception {
        Preconditions.checkNotNull(url);
        doUnregister(url.clone0());
        registeredServiceUrls.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(listener);

        URL urlCopy = url.clone0();
        doSubscribe(urlCopy, listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(listener);
        doUnsubscribe(url.clone0(), listener);
    }

    @Override
    public List<URL> discover(URL url) throws Exception {
        Preconditions.checkNotNull(url);
        List<URL> results = new ArrayList<>();
        List<URL> urlsDiscovered = doDiscover(url);
        if (urlsDiscovered != null) {
            for (URL u : urlsDiscovered) {
                results.add(u.clone0());
            }
        }
        return results;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    private void doRegister(URL url) {
        try {
            serverLock.lock();
            // 防止旧节点未正常注销
            removeNode(url, ZkNodeTypeEnum.SERVER);
            createNode(url, ZkNodeTypeEnum.SERVER);
        } catch (Throwable e) {
            throw new FrameworkRpcException(String.format("Failed to register %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    private void doUnregister(URL url) {
        try {
            serverLock.lock();
            removeNode(url, ZkNodeTypeEnum.SERVER);
        } catch (Throwable e) {
            throw new FrameworkRpcException(String.format("Failed to unregister %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    private void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            clientLock.lock();

            ConcurrentHashMap<NotifyListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners == null) {
                serviceListeners.putIfAbsent(url, new ConcurrentHashMap<>());
                childChangeListeners = serviceListeners.get(url);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(listener);
            if (zkChildListener == null) {
                childChangeListeners.putIfAbsent(listener, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) {

                        listener.notify(getUrl(), childrenNodeToUrls(parentPath, currentChilds));
                        logger.info(String.format("[ZookeeperRegistryFactory] service list change: path=%s, currentChilds=%s", parentPath, currentChilds.toString()));
                    }
                });
                zkChildListener = childChangeListeners.get(listener);
            }

            // 防止旧节点未正常注销
            removeNode(url, ZkNodeTypeEnum.CLIENT);
            createNode(url, ZkNodeTypeEnum.CLIENT);

            String serverTypePath = ZkUtils.toNodeTypePath(url, ZkNodeTypeEnum.SERVER);
            zkClient.subscribeChildChanges(serverTypePath, zkChildListener);
            logger.info(String.format("[ZookeeperRegistryFactory] subscribe service: path=%s, info=%s", ZkUtils.toNodePath(url, ZkNodeTypeEnum.SERVER), url.getUriWithParam()));
        } catch (Throwable e) {
            throw new FrameworkRpcException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    private void doUnsubscribe(URL url, NotifyListener listener) {
        try {
            clientLock.lock();
            Map<NotifyListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners != null) {
                IZkChildListener zkChildListener = childChangeListeners.get(listener);
                if (zkChildListener != null) {
                    zkClient.unsubscribeChildChanges(ZkUtils.toNodeTypePath(url, ZkNodeTypeEnum.CLIENT), zkChildListener);
                    childChangeListeners.remove(listener);
                }
            }
        } catch (Throwable e) {
            throw new FrameworkRpcException(String.format("Failed to unsubscribe service %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    private List<URL> doDiscover(URL url) {
        return discoverService(url);
    }

    private void createNode(URL url, ZkNodeTypeEnum nodeType) {
        String nodeTypePath = ZkUtils.toNodeTypePath(url, nodeType);
        if (!zkClient.exists(nodeTypePath)) {
            zkClient.createPersistent(nodeTypePath, true);
        }
        zkClient.createEphemeral(ZkUtils.toNodePath(url, nodeType), url.getUriWithParam());
    }

    private void removeNode(URL url, ZkNodeTypeEnum nodeType) {
        String nodePath = ZkUtils.toNodePath(url, nodeType);
        if (zkClient.exists(nodePath)) {
            zkClient.delete(nodePath);
        }
    }

    private List<URL> discoverService(URL url) {
        try {
            String parentPath = ZkUtils.toNodeTypePath(url, ZkNodeTypeEnum.SERVER);
            List<String> children = new ArrayList<String>();
            if (zkClient.exists(parentPath)) {
                children = zkClient.getChildren(parentPath);
            }
            return childrenNodeToUrls(parentPath, children);
        } catch (Throwable e) {
            throw new FrameworkRpcException(String.format("Failed to discover service %s from zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        }
    }

    private List<URL> childrenNodeToUrls(String parentPath, List<String> children) {
        List<URL> urls = new ArrayList<>();
        if (children != null) {
            for (String node : children) {
                String nodePath = parentPath + Constants.PATH_SEPARATOR + node;
                String data = zkClient.readData(nodePath, true);
                try {
                    URL url = URL.parse(data);
                    urls.add(url);
                } catch (Exception e) {
                    logger.warn(String.format("Found malformed urls from ZookeeperRegistryFactory, path=%s", nodePath), e);
                }
            }
        }
        return urls;
    }

    @Override
    public void close() {
        this.zkClient.close();
    }
}
