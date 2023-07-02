package cn.itcast.hotel.constants;

/**
 * Description: Todo
 * Class Name: MqConstants
 * Date: 2023/7/2 14:17
 *
 * @author Hao
 * @version 1.1
 */
public class MqConstants {
    // 交换机
    public final static String HOTEL_EXCHANGE = "hotel.topic";

    // 监听新增和修改的队列
    public final static String HOTEL_INSERT_QUEUE = "hotel.insert.queue";

    // 监听删除的队列
    public final static String HOTEL_DELETE_QUEUE = "hotel.delete.queue";

    // 新增或修改的 RoutingKey
    public final static String HOTEL_INSERT_KEY = "hotel.insert";

    // 删除的 RoutingKey
    public final static String HOTEL_DELETE_KEY = "hotel.delete";
}
