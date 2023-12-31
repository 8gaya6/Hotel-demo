package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    private RestHighLevelClient client;

    /**
     * 构建查询请求
     *
     * @param params
     * @param request
     */
    private void buildBasicQuery(RequestParams params, SearchRequest request) {
        // 1. 构建 BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2. 关键字
        String key = params.getKey();
        if (key == null || "".equals(key)) boolQuery.must(QueryBuilders.matchAllQuery());
        else boolQuery.must(QueryBuilders.matchQuery("all", key));

        // 3. 城市条件
        if (params.getCity() != null && !params.getCity().equals(""))
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        // 4. 品牌条件
        if (params.getBrand() != null && !params.getBrand().equals(""))
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        // 5. 星级条件
        if (params.getStarName() != null && !params.getStarName().equals(""))
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        // 6. 价格
        if (params.getMinPrice() != null && params.getMaxPrice() != null)
            boolQuery.filter(QueryBuilders
                    .rangeQuery("price")
                    .gte(params.getMinPrice())
                    .lte(params.getMaxPrice())
            );

        // 7. 算分控制
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        // 原始查询
                        boolQuery,
                        // function score 的数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 其中的一个 function score 元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // 过滤条件
                                        QueryBuilders.termQuery("isAD", true),
                                        // 算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });

        // 8. 放入 source
        request.source().query(functionScoreQuery);
    }

    @Override
    public PageResult search(RequestParams params) {
        try {
            // 1. 准备 Request
            SearchRequest request = new SearchRequest("cloudhotel");
            // 2. 准备 DSL
            // 2.1. query
            buildBasicQuery(params, request);

            // 2.2. 分页
            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);

            // 2.3. 排序
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)
                );
            }

            // 3. 发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4. 解析响应
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析 es 响应的结果
     *
     * @param response
     * @return
     */
    private PageResult handleResponse(SearchResponse response) {
        // 4. 解析响应
        SearchHits searchHits = response.getHits();
        // 4.1. 获取总条数
        long total = searchHits.getTotalHits().value;
        // 4.2. 文档数组
        SearchHit[] hits = searchHits.getHits();
        // 4.3. 遍历
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            // 获取文档 source
            String json = hit.getSourceAsString();
            // 反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            // 获取排序值，是个数组：因为可能以后不止一个排序条件
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) hotelDoc.setDistance(sortValues[0]);
            // 放入集合
            hotels.add(hotelDoc);
        }
        // 4.4. 封装返回
        return new PageResult(total, hotels);
    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {
        try {
            // 1. 准备 Request
            SearchRequest request = new SearchRequest("cloudhotel");
            // 2. 准备 DSL
            // 2.1. query
            buildBasicQuery(params, request);
            // 2.2. 设置 size
            request.source().size(0);
            // 2.3. 聚合
            buildAggregation(request);
            // 3. 发出请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4. 解析结果
            Map<String, List<String>> result = new HashMap<>();
            Aggregations aggregations = response.getAggregations();
            // 4.1. 根据品牌名称，获取品牌结果
            List<String> brandList = getAggByName(aggregations, "brandAgg");
            result.put("brand", brandList);
            // 4.2. 根据城市名称，获取城市结果
            List<String> cityList = getAggByName(aggregations, "cityAgg");
            result.put("city", cityList);
            // 4.3. 根据星级名称，获取星级结果
            List<String> starList = getAggByName(aggregations, "starAgg");
            result.put("starName", starList);

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("starAgg")
                .field("starName")
                .size(100)
        );
    }

    private List<String> getAggByName(Aggregations aggregations, String aggName) {
        // 4.1. 根据聚合名称获取聚合结果
        Terms terms = aggregations.get(aggName);
        // 4.2. 获取 buckets
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        // 4.3. 遍历
        List<String> list = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            // 4.4. 获取 key
            String key = bucket.getKeyAsString();
            list.add(key);
        }
        return list;
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            // 1. 准备 Request
            SearchRequest request = new SearchRequest("cloudhotel");
            // 2. 准备 DSL
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));
            // 3. 发起请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4. 解析结果
            Suggest suggest = response.getSuggest();
            // 4.1. 根据补全查询名称，获取补全结果
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            // 4.2. 获取 options
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
            // 4.3. 遍历
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            // 1. 准备 Request
            DeleteRequest request = new DeleteRequest("cloudhotel", id.toString());
            // 2. 发送请求
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id) {
        try {
            // 0. 根据 id 查询酒店数据
            Hotel hotel = getById(id);
            // 转换为文档类型
            HotelDoc hotelDoc = new HotelDoc(hotel);

            // 1. 准备 Request 对象
            IndexRequest request = new IndexRequest("cloudhotel").id(hotel.getId().toString());
            // 2. 准备 Json 文档
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            // 3. 发送请求
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
