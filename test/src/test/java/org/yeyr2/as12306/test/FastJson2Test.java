package org.yeyr2.as12306.test;

import com.alibaba.fastjson2.util.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class FastJson2Test {
    public static void main(String[] args) {
        // Create a parameterized type List<String>
        ParameterizedTypeImpl listType = new ParameterizedTypeImpl(new Type[]{String.class}, null, List.class);

        System.out.println(listType.getOwnerType()+"\n"+listType.getRawType()+"\n"+ Arrays.stream(listType.getActualTypeArguments()).toList());  // Output: java.util.List<java.lang.String>
    }
}
