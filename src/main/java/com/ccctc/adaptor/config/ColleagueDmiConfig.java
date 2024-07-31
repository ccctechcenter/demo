package com.ccctc.adaptor.config;

import org.ccctc.colleaguedmiclient.service.DmiCTXService;
import org.ccctc.colleaguedmiclient.service.DmiDataService;
import org.ccctc.colleaguedmiclient.service.DmiEntityService;
import org.ccctc.colleaguedmiclient.service.DmiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(ColleagueCondition.class)
public class ColleagueDmiConfig {

    @Bean
    public DmiService dmiService(@Value("${dmi.account}") String account,
                                 @Value("${dmi.username}") String username,
                                 @Value("${dmi.password}") String password,
                                 @Value("${dmi.host}") String host,
                                 @Value("${dmi.port}") int port,
                                 @Value("${dmi.secure:false}") boolean secure,
                                 @Value("${dmi.host.name.override:null}") String hostnameOverride,
                                 @Value("${dmi.shared.secret}") String sharedSecret,
                                 @Value("${dmi.pool.size:10}") int poolSize,
                                 @Value("${dmi.authorization.expiration.seconds:-1}") long authorizationExpirationSeconds,
                                 @Value("${dmi.max.transaction.retry:-1}") int maxDmiTransactionRetry) {
        DmiService d = new DmiService(account, username, password, host, port, secure, hostnameOverride, sharedSecret, poolSize);

        if (authorizationExpirationSeconds >= 0)
            d.setAuthorizationExpirationSeconds(authorizationExpirationSeconds);

        if (maxDmiTransactionRetry >= 0)
            d.setMaxDmiTransactionRetry(maxDmiTransactionRetry);

        return d;
    }

    @Bean
    public DmiCTXService dmiCTXService(DmiService dmiService,
                                       @Value("${dmi.metadata.expiration.seconds:-1}") int metadataExpirationSeconds) {
        DmiCTXService d = new DmiCTXService(dmiService);

        if (metadataExpirationSeconds >= 0)
            d.getCtxMetadataService().setCacheExpirationSeconds(metadataExpirationSeconds);

        return d;
    }

    @Bean
    public DmiDataService dmiDataService(DmiService dmiService, DmiCTXService dmiCTXService,
                                         @Value("${dmi.metadata.expiration.seconds:-1}") int metadataExpirationSeconds) {
        DmiDataService d = new DmiDataService(dmiService, dmiCTXService);

        if (metadataExpirationSeconds >= 0)
            d.getEntityMetadataService().setCacheExpirationSeconds(metadataExpirationSeconds);

        return d;
    }

    @Bean
    public DmiEntityService dmiEntityService(DmiDataService dmiDataService,
                                             @Value("${dmi.metadata.expiration.seconds:-1}") int metadataExpirationSeconds) {
        DmiEntityService d = new DmiEntityService(dmiDataService);

        if (metadataExpirationSeconds >= 0)
            d.setCacheExpirationSeconds(metadataExpirationSeconds);

        return d;
    }
}
