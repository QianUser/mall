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
		indexRequest.id("1");   // 数据的id

//		indexRequest.source("userName","张三","age",18,"gender","男");

		User user = new User();
		user.setUserName("张三");
		user.setAge(18);
		user.setGender("男");

		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);
		IndexResponse index = client.index(indexRequest, MallElasticSearchConfig.COMMON_OPTIONS);
		System.out.println(index);
	}


	/**
	 * 复杂检索：在bank中搜索address中包含mill的所有人的年龄分布以及平均年龄、平均薪资
	 */
	@Test
	public void searchData() throws IOException {
		// 创建检索请求
		SearchRequest searchRequest = new SearchRequest();

		// 指定索引
		searchRequest.indices("bank");
		// 构造检索条件
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.matchQuery("address", "Mill"));

		// 按照年龄分布进行聚合
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		sourceBuilder.aggregation(ageAgg);

		// 计算平均年龄
		AvgAggregationBuilder ageAvg = AggregationBuilders.avg("ageAvg").field("age");
		sourceBuilder.aggregation(ageAvg);
		// 计算平均薪资
		AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
		sourceBuilder.aggregation(balanceAvg);

		System.out.println("检索条件：" + sourceBuilder);
		searchRequest.source(sourceBuilder);
		// 执行检索
		SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		System.out.println("检索结果：" + searchResponse);

		// 将检索结果封装为Bean
		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();
		for (SearchHit searchHit : searchHits) {
			String sourceAsString = searchHit.getSourceAsString();
			Account account = JSON.parseObject(sourceAsString, Account.class);
			System.out.println(account);

		}

		// 获取聚合信息
		Aggregations aggregations = searchResponse.getAggregations();

		Terms ageAgg1 = aggregations.get("ageAgg");

		for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
			String keyAsString = bucket.getKeyAsString();
			System.out.println("年龄：" + keyAsString + " ==> " + bucket.getDocCount());
		}
		Avg ageAvg1 = aggregations.get("ageAvg");
		System.out.println("平均年龄：" + ageAvg1.getValue());

		Avg balanceAvg1 = aggregations.get("balanceAvg");
		System.out.println("平均薪资：" + balanceAvg1.getValue());
	}

}
