package org.yeyr2.as12306.distributedid.core.snowflake;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkIdWrapper {
    // 工作ID
    private Long workId;

    // 数据中心ID
    private Long dataCenterId;
}
