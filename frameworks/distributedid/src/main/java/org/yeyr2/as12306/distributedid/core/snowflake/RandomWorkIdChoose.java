package org.yeyr2.as12306.distributedid.core.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

// 使用随机数获取雪花WorkId
@Slf4j
public class RandomWorkIdChoose extends AbstractWorkIdChooseTemplate implements InitializingBean {
    protected WorkIdWrapper chooseWorkId(){
        int start = 0,end = 31;
        return new WorkIdWrapper(getRandom(start,end),getRandom(start,end));
    }

    @Override
    public void afterPropertiesSet() throws Exception{
        chooseWorkId();
    }

    private static long getRandom(int start,int end){
        return (long) (Math.random() * (end - start + 1) + start);
    }
}
