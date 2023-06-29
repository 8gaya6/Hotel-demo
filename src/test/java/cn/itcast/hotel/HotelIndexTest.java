package cn.itcast.hotel;


import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.elasticsearch.client.indices.CreateIndexRequest;


import java.io.IOException;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

/**
 * Description: Todo
 * Class Name: HotelIndexTest
 * Date: 2023/6/25 21:18
 *
 * @author Hao
 * @version 1.1
 */

public class HotelIndexTest {
    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        // 初始化 RestHighLevelClient
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.111.134:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    void createHotelIndex() throws IOException {
        // 1. 创建 Request 对象
        CreateIndexRequest request = new CreateIndexRequest("cloudhotel");
        // 2. 准备请求的参数：DSL 语句
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3. 发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteHotelIndex() throws IOException {
        // 1. 创建 Request 对象
        DeleteIndexRequest request = new DeleteIndexRequest("hotel");
        // 2. 发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testExistsHotelIndex() throws IOException {
        // 1. 创建 Request 对象
        GetIndexRequest request = new GetIndexRequest("cloudhotel");
        // 2. 发送请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        // 3. 输出
        System.err.println(exists ? "索引库已经存在！" : "索引库不存在！");
    }
}
