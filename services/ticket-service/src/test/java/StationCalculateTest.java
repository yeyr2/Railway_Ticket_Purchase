import org.yey2.as12306.ticketService.toolkit.StationCalculateUtil;

import java.util.Arrays;
import java.util.List;

public class StationCalculateTest {

    public static void main(String[] args) {
        List<String> stations = Arrays.asList("北京南", "济南西", "南京南", "杭州东", "宁波");
        String startStation = "北京南";
        String endStation = "南京南";
        StationCalculateUtil.takeoutStation(stations, startStation, endStation).forEach(System.out::println);
    }
}
