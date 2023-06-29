package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * Description: Todo
 * Class Name: RequestParams
 * Date: 2023/6/29 10:32
 *
 * @author Hao
 * @version 1.1
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;
    // 用于条件过滤的字段
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;
    // 当前位置
    private String location;
}
