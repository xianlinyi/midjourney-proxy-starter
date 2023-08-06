package com.prechatting.wss;

import com.prechatting.ProxyProperties;
import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocketFactory;
import io.micrometer.common.util.StringUtils;


public interface WebSocketStarter {

	void start() throws Exception;

	Boolean isReady();


	default void initProxy(ProxyProperties properties) {
		ProxyProperties.ProxyConfig proxy = properties.getProxy();
		if (StringUtils.isNotBlank(proxy.getHost())) {
			System.setProperty("http.proxyHost", proxy.getHost());
			System.setProperty("http.proxyPort", String.valueOf(proxy.getPort()));
			System.setProperty("https.proxyHost", proxy.getHost());
			System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
		}
	}

	default WebSocketFactory createWebSocketFactory(ProxyProperties properties) {
		ProxyProperties.ProxyConfig proxy = properties.getProxy();
		WebSocketFactory webSocketFactory = new WebSocketFactory().setConnectionTimeout(10000);
		if (StringUtils.isNotBlank(proxy.getHost())) {
			ProxySettings proxySettings = webSocketFactory.getProxySettings();
			proxySettings.setHost(proxy.getHost());
			proxySettings.setPort(proxy.getPort());
		}
		return webSocketFactory;
	}

}
