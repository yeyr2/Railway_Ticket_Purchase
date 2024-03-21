package org.yeyr2.as12306.distributedid;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.yeyr2.as12306.distributedid.core.snowflake.Snowflake;

import java.util.Date;

public class DistributedApplicationTests {
    @Test
    public void PatternTest() {
        long l = System.currentTimeMillis();
        System.out.println(l);
        System.out.println(new Date(l).toInstant());
    }

    @Test
    public void SnowflakeTest(){
        Snowflake snowflake = new Snowflake();
        long l = snowflake.nextId();
        System.out.println(l);
        long generateDateTime = snowflake.getGenerateDateTime(l);
        System.out.println(new Date(generateDateTime).toInstant());
    }
}
