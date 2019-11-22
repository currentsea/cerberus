package com.nike.cerberus.config;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.CryptoMaterialsCache;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Cache;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.cerberus.cache.MetricReportingCache;
import com.nike.cerberus.cache.MetricReportingCryptoMaterialsCache;
import com.nike.cerberus.domain.AwsIamKmsAuthRequest;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.error.DefaultApiErrorsImpl;
import com.nike.cerberus.service.MetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.nike.cerberus.service.EncryptionService.initializeKeyProvider;

@Slf4j
@Configuration
@ComponentScan({
  "com.nike.cerberus.external", // Hook for external stuff (plugins) // TODO move this into a config that is disabled by default and has to be explicitly enabled.
  "com.nike.cerberus.config",
  "com.nike.cerberus.security",
  "com.nike.cerberus.util",
  "com.nike.cerberus.service",
  "com.nike.cerberus.auth.connector.config",
  "com.nike.cerberus.aws",
  "com.nike.cerberus.dao",
  "com.nike.cerberus.controller",
  "com.nike.backstopper", // error management TODO explicitly import the config bean
  "com.nike.wingtips.springboot" // dist tracing TODO explicitly import the config bean
})
@EnableAutoConfiguration(exclude = { // TODO remove this, What does this auto load? Could this auto load bad stuff?
  FlywayAutoConfiguration.class // Have no idea how this magic works, but we will manually configure this ourselves so that it works the way it works in the old guice config
})
@EnableAsync
@EnableScheduling
public class ApplicationConfiguration {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.findAndRegisterModules();
    om.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    om.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    om.enable(SerializationFeature.INDENT_OUTPUT);
    return om;
  }

  /**
   * TODO un enum-ify this so errors can be created in individuals modules and gathered dynamically here
   */
  @Bean
  public ProjectApiErrors getProjectApiErrors() {
    return new DefaultApiErrorsImpl();
  }

  @Bean
  public Validator getJsr303Validator() { //todo is this already available as a bean
    return Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Bean
  public Cache<AwsIamKmsAuthRequest, EncryptedAuthDataWrapper> kmsAuthCache(MetricsService metricsService,
                                                                            @Value("${cms.iam.token.cache.maxAgeInSeconds:10}") int maxAge) {

    return new MetricReportingCache<>("auth.kms", maxAge, metricsService, null);
  }

  @Bean
  public Region currentRegion() {
    return Region.getRegion(Regions.DEFAULT_REGION); // TODO
//    return Optional.ofNullable(Regions.getCurrentRegion())
//      .orElse(Region.getRegion(Regions.DEFAULT_REGION ));
  }

  @Bean("encryptCryptoMaterialsManager")
  public CryptoMaterialsManager encryptCryptoMaterialsManager(@Value("${cerberus.encryption.cmk.arns}") String cmkArns,
                                                              @Value("${cerberus.encryption.cache.enabled:false}") boolean cacheEnabled,
                                                              @Value("${cerberus.encryption.cache.encrypt.maxSize:0}")
                                                              int encryptMaxSize ,
                                                              @Value("${cerberus.encryption.cache.encrypt.maxAgeInSeconds:0}")
                                                              int encryptMaxAge ,
                                                              @Value("${cerberus.encryption.cache.encrypt.messageUseLimit:0}")
                                                              int encryptMessageUseLimit ,
                                                              Region currentRegion,
                                                              MetricsService metricsService) {
    MasterKeyProvider<KmsMasterKey> keyProvider = initializeKeyProvider(cmkArns, currentRegion);
    if (cacheEnabled) {
      log.info("Initializing caching encryptCryptoMaterialsManager with CMK: {}, maxSize: {}, maxAge: {}, " +
        "messageUseLimit: {}", cmkArns, encryptMaxSize, encryptMaxAge, encryptMessageUseLimit);
      CryptoMaterialsCache cache = new MetricReportingCryptoMaterialsCache(encryptMaxSize, metricsService);
      CryptoMaterialsManager cachingCmm =
        CachingCryptoMaterialsManager.newBuilder().withMasterKeyProvider(keyProvider)
          .withCache(cache)
          .withMaxAge(encryptMaxAge, TimeUnit.SECONDS)
          .withMessageUseLimit(encryptMessageUseLimit)
          .build();
      return cachingCmm;
    } else {
      log.info("Initializing encryptCryptoMaterialsManager with CMK: {}", cmkArns);
      return new DefaultCryptoMaterialsManager(keyProvider);
    }
  }

  @Bean("decryptCryptoMaterialsManager")
  public CryptoMaterialsManager decryptCryptoMaterialsManager(@Value("${cerberus.encryption.cmk.arns}") String cmkArns,
                                                              @Value("${cerberus.encryption.cache.enabled:#{false}}") boolean cacheEnabled,
                                                              @Value("${cerberus.encryption.cache.decrypt.maxSize:0}")
                                                                  int decryptMaxSize ,
                                                              @Value("${cerberus.encryption.cache.decrypt.maxAgeInSeconds:0}")
                                                                  int decryptMaxAge,
                                                              Region currentRegion,
                                                              MetricsService metricsService) {
    MasterKeyProvider<KmsMasterKey> keyProvider = initializeKeyProvider(cmkArns, currentRegion);
    if (cacheEnabled) {
      log.info("Initializing caching decryptCryptoMaterialsManager with CMK: {}, maxSize: {}, maxAge: {}",
        cmkArns, decryptMaxSize, decryptMaxAge);
      CryptoMaterialsCache cache = new MetricReportingCryptoMaterialsCache(decryptMaxAge, metricsService);
      CryptoMaterialsManager cachingCmm =
        CachingCryptoMaterialsManager.newBuilder().withMasterKeyProvider(keyProvider)
          .withCache(cache)
          .withMaxAge(decryptMaxAge, TimeUnit.SECONDS)
          .build();
      return cachingCmm;
    } else {
      log.info("Initializing decryptCryptoMaterialsManager with CMK: {}", cmkArns);
      return new DefaultCryptoMaterialsManager(keyProvider);
    }
  }

  @Bean
  AwsCrypto awsCrypto() {
    return new AwsCrypto();
  }

  /**
   * TODO, we can probably delete this, but the API tests from Highlander check for this.
   */
  @Bean
  public OncePerRequestFilter AddXRefreshTokenHeaderFilter() {
    return new LambdaFilter((request, response) ->
      response.addHeader("X-Refresh-Token", Boolean.FALSE.toString()));
  }

  /**
   * This filter maps null responses for PUT and POST requests to 204's rather than 200's
   * This is done in order to maintain backwards compatibility from the pre-spring API.
   */
  @Bean
  public OncePerRequestFilter nullOkResponsesShouldReturnNoContentFilter() {
    return new LambdaFilter(true, (request, response) -> {
      var typeOptional = Optional.ofNullable(response.getContentType()).filter(Predicate.not(String::isBlank));
      if (typeOptional.isEmpty() && response.getStatus() == HttpStatus.OK.value()) {
        response.setStatus(HttpStatus.NO_CONTENT.value());
      }
    });
  }
}
