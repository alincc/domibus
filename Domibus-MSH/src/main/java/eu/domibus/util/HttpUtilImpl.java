package eu.domibus.util;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.HttpUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Cosmin Baciu on 12-Jul-16.
 */
@Service
public class HttpUtilImpl implements HttpUtil {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(HttpUtilImpl.class);

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Override
    public ByteArrayInputStream downloadURL(String url) throws IOException {
        LOG.debug("Download from URL " + url);
        if (domibusConfigurationService.useProxy()) {
            String httpProxyHost = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_HTTP_HOST);
            String httpProxyPort = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_HTTP_PORT);
            String httpProxyUser = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_USER);
            String httpProxyPassword = domibusPropertyProvider.getProperty(DomibusConfigurationService.DOMIBUS_PROXY_PASSWORD);
            LOG.info("Using proxy for downloading URL " + url);
            return downloadURLViaProxy(url, httpProxyHost, Integer.parseInt(httpProxyPort), httpProxyUser, httpProxyPassword);
        }
        return downloadURLDirect(url);
    }

    @Override
    public ByteArrayInputStream downloadURLDirect(String url) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);

        try {
            LOG.debug("Executing request " + httpGet.getRequestLine() + " directly");
            return getByteArrayInputStream(httpclient, httpGet);
        } finally {
            httpclient.close();
        }
    }

    @Override
    public ByteArrayInputStream downloadURLViaProxy(String url, String proxyHost, Integer proxyPort, String proxyUser, String proxyPassword) throws IOException {

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        //Proxy requires user and password
        if(!StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword)) {
            LOG.debug("Add proxy credentials for user [{}]", proxyUser);
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(proxyHost, proxyPort),
                    new UsernamePasswordCredentials(proxyUser, proxyPassword));

            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        CloseableHttpClient httpclient = httpClientBuilder.build();

        try {
            LOG.debug("Building proxy, host [{}], port [{}]", proxyHost, proxyPort);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);

            RequestConfig config = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(config);

            LOG.debug("Executing request " + httpGet.getRequestLine() + " via " + proxy);
            return getByteArrayInputStream(httpclient, httpGet);
        } finally {
            httpclient.close();
        }
    }

    private ByteArrayInputStream getByteArrayInputStream(CloseableHttpClient httpclient, HttpGet httpGet) throws IOException {
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpGet);
            return new ByteArrayInputStream(IOUtils.toByteArray(response.getEntity().getContent()));
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

}