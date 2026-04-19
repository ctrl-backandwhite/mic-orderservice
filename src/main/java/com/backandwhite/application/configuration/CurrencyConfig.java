package com.backandwhite.application.configuration;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.currency.CurrencyRateCache;
import com.backandwhite.common.currency.CurrencyRequestFilter;
import com.backandwhite.common.currency.PriceConversionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Currency conversion infrastructure for mic-orderservice. Reads X-Currency
 * header, caches exchange rates from mic-cmsservice.
 */
@Configuration
public class CurrencyConfig implements WebMvcConfigurer {

    @Value("${services.cms.url:http://localhost:6006}")
    private String cmsServiceUrl;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CurrencyRequestFilter()).addPathPatterns("/api/**");
    }

    @Bean
    public RestTemplate cmsRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        RestTemplate rt = new RestTemplate(factory);
        rt.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add(AppConstants.HEADER_NX036_AUTH, "service");
            return execution.execute(request, body);
        });
        return rt;
    }

    @Bean
    public CurrencyRateCache currencyRateCache(RestTemplate cmsRestTemplate) {
        return new CurrencyRateCache(cmsServiceUrl, cmsRestTemplate);
    }

    @Bean
    public PriceConversionService priceConversionService() {
        return new PriceConversionService();
    }
}
