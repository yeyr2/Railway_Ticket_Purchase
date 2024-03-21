package org.yeyr2.as12306.distributedid.core;

// id生成器
public interface IdGenerator {
    // 下一个Id
    default long nextId(){
        return 0L;
    }

    //下一个Id字符串
    default String nextIdStr(){
        return "";
    }
}
