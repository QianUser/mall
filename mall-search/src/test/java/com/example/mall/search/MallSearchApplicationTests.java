package com.example.mall.search;

import com.alibaba.fastjson.JSON;
import com.example.mall.search.config.MallElasticSearchConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class MallSearchApplicationTests {

	@Autowired
	private RestHighLevelClient client;

	@ToString
	@Data
	static class Account {
		private int account_number;
		private int balance;
		private String firstname;
		private String lastname;
		private int age;
		private String gender;
		private String address;
		private String employer;
		private String email;
		private String city;
		private String state;
	}

	@Getter
	@Setter
	static class User {
		private String userName;
		private String gender;
		private Integer age;
	}

	@Test
	public void indexData() throws IOException {

		IndexRequest indexRequest = new IndexRequest("users");
		indexRequest.id("1");   // ?????????id

//		indexRequest.source("userName","??????","age",18,"gender","???");

		User user = new User();
		user.setUserName("??????");
		user.setAge(18);
		user.setGender("???");

		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);
		IndexResponse index = client.index(indexRequest, MallElasticSearchConfig.COMMON_OPTIONS);
		System.out.println(index);
	}


	/**
	 * ??????????????????bank?????????address?????????mill????????????????????????????????????????????????????????????
	 */
	@Test
	public void searchData() throws IOException {
		// ??????????????????
		SearchRequest searchRequest = new SearchRequest();

		// ????????????
		searchRequest.indices("bank");
		// ??????????????????
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.matchQuery("address", "Mill"));

		// ??????????????????????????????
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		sourceBuilder.aggregation(ageAgg);

		// ??????????????????
		AvgAggregationBuilder ageAvg = AggregationBuilders.avg("ageAvg").field("age");
		sourceBuilder.aggregation(ageAvg);
		// ??????????????????
		AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
		sourceBuilder.aggregation(balanceAvg);

		System.out.println("???????????????" + sourceBuilder);
		searchRequest.source(sourceBuilder);
		// ????????????
		SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		System.out.println("???????????????" + searchResponse);

		// ????????????????????????Bean
		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();
		for (SearchHit searchHit : searchHits) {
			String sourceAsString = searchHit.getSourceAsString();
			Account account = JSON.parseObject(sourceAsString, Account.class);
			System.out.println(account);

		}

		// ??????????????????
		Aggregations aggregations = searchResponse.getAggregations();

		Terms ageAgg1 = aggregations.get("ageAgg");

		for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
			String keyAsString = bucket.getKeyAsString();
			System.out.println("?????????" + keyAsString + " ==> " + bucket.getDocCount());
		}
		Avg ageAvg1 = aggregations.get("ageAvg");
		System.out.println("???????????????" + ageAvg1.getValue());

		Avg balanceAvg1 = aggregations.get("balanceAvg");
		System.out.println("???????????????" + balanceAvg1.getValue());
	}

}
