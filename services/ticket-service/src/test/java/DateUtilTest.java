import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.yey2.as12306.ticketService.toolkit.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class DateUtilTest {
    @SneakyThrows
    @Test
    void dateUtilTest() {
        String startTimeStr = "2023-10-01 01:00:00";
        String endTimeStr = "2023-11-01 12:23:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = formatter.parse(startTimeStr);
        Date endTime = formatter.parse(endTimeStr);
        String calculateHourDifference = DateUtil.calculateHourDifference(startTime, endTime);
        log.info("开始时间：{}，结束时间：{}，两个时间相差时分：{}", startTimeStr, endTimeStr, calculateHourDifference);
    }
}
