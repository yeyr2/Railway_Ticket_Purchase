import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

public class CacheUtil {
    @Test
    void test1(){
        String s = org.yeyr2.as12306.cache.toolkit.CacheUtil.buildKey(String.valueOf(01),"hello","key");
        String keySuffix = StrUtil.join("_",01,"hello","key");
        System.out.println(s+"\n"+keySuffix);

    }
}

