package com.ccctc.adaptor.config;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Value("${http.client.max.connections:25}")
    private int maxConnections;

    @Value("${http.client.keep.alive:120}")
    private int keepAliveSeconds;

    /**
     * A Pooling HTTP Connection Manager for use by the Colleague API, or any other future service that needs to
     * make HTTP calls
     *
     * @return Connection Manager
     */
    @Bean
    public PoolingHttpClientConnectionManager getConnectionManager() {
        PoolingHttpClientConnectionManager m = new PoolingHttpClientConnectionManager();

        // the Colleague API has a single route, so set max connections and max per route to the same
        m.setDefaultMaxPerRoute(maxConnections);
        m.setMaxTotal(maxConnections);

        return m;
    }

    /**
     * An HTTP Client for use by the Colleague API, or any other future service that needs to make HTTP calls
     *
     * @param connectionManager Connection Manager
     * @return HTTP Client
     */
    @Bean
    public CloseableHttpClient getHttpClient(PoolingHttpClientConnectionManager connectionManager) {
        // keep alive strategy
        ConnectionKeepAliveStrategy kas = new ConnectionKeepAliveStrategy() {
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                // Honor 'keep-alive' header
                HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }

                return keepAliveSeconds * 1000;
            }
        };

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(kas)
                .build();
    }
}
