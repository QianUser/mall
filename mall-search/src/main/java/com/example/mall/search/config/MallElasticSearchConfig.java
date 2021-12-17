package com.example.mall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MallElasticSearchConfig {

    // 参考：https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.5/java-rest-high-getting-started-request-options.html
    // 参考：https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.5/java-rest-low-usage-requests.html#java-rest-low-usage-request-options
    public static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        COMMON_OPTIONS = builder.build();
    }


    @Bean
    public RestHighLevelClient esHighLevelClient() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost("192.168.227.131", 9200, "http")));
    }

}
