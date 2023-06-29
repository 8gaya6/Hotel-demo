package cn.itcast.hotel.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Description: Todo
 * Class Name: PageResult
 * Date: 2023/6/29 10:31
 *
 * @author Hao
 * @version 1.1
 */
@Data
@NoArgsConstructor
public class PageResult {
    private Long total;
    private List<HotelDoc> hotels;

    public PageResult(Long total, List<HotelDoc> hotels) {
        this.total = total;
        this.hotels = hotels;
    }
}
