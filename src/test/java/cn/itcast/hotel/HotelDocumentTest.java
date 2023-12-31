package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

/**
 * Description: Todo
 * Class Name: HotelDocumentTest
 * Date: 2023/6/26 16:08
 *
 * @author Hao
 * @version 1.1
 */

@SpringBootTest
public class HotelDocumentTest {

    @Autowired
    private IHotelService hotelService;

    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.111.134:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    void testAddDocument() throws IOException {
        // 1. 根据 id 查询酒店数据
        Hotel hotel = hotelService.getById(61083L);
        // 2. 转换为文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);
        // 3. 将 HotelDoc 转 json
        String json = JSON.toJSONString(hotelDoc);

        // 4. 准备 Request 对象
        IndexRequest request = new IndexRequest("cloudhotel").id(hotelDoc.getId().toString());
        // 5. 准备 Json 文档
        request.source(json, XContentType.JSON);
        // 6. 发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocumentById() throws IOException {
        // 1. 准备 Request
        GetRequest request = new GetRequest("cloudhotel", "61083");
        // 2. 发送请求，得到响应
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 3 .解析响应结果
        String json = response.getSourceAsString();

        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void testUpdateDocument() throws IOException {
        // 1. 准备 Request
        UpdateRequest request = new UpdateRequest("cloudhotel", "61083");
        // 2. 准备请求参数
        request.doc(
                "price", "952",
                "starName", "四钻"
        );
        // 3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocument() throws IOException {
        // 1. 准备 Request
        DeleteRequest request = new DeleteRequest("cloudhotel", "61083");
        // 2. 发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkRequest() throws IOException {
        // 批量查询酒店数据
        List<Hotel> hotels = hotelService.list();

        // 1. 创建 Request
        BulkRequest request = new BulkRequest();
        // 2. 准备参数，添加多个新增的 Request
        for (Hotel hotel : hotels) {
            // 2.1. 转换为文档类型 HotelDoc
            HotelDoc hotelDoc = new HotelDoc(hotel);
            // 2.2. 创建新增文档的 Request 对象
            request.add(new IndexRequest("cloudhotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON));
        }
        // 3. 发送请求
        client.bulk(request, RequestOptions.DEFAULT);
    }

}
