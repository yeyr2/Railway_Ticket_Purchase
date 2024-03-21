package org.yeyr2.as12306.ticketService.job.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.yeyr2.as12306.ticketService.dao.entity.TrainDO;
import org.yeyr2.as12306.ticketService.dao.mapper.TrainMapper;
import org.yeyr2.as12306.base.ApplicationContextHolder;
import org.yeyr2.as12306.common.toolkit.EnvironmentUtil;

import java.util.List;
import java.util.Optional;

/**
 * 抽象列车&车票相关定时任务
 */
public abstract class AbstractTrainStationJobHandlerTemplate extends IJobHandler {

    /**
     * 模板方法模式具体实现子类执行定时任务
     * @param trainDOPageRecords 列车信息分页记录
     */
    protected abstract void actualExecute(List<TrainDO> trainDOPageRecords);
    /**
     *     分页查询出发时间在给定时间当天时间内的列车,并传入{@link AbstractTrainStationJobHandlerTemplate#actualExecute}
     */
    @Override
    public void execute() {
        long currentPage = 1L;
        long size = 1000L;
        String requestParam = getJobRequestParam();
        DateTime dateTime = StrUtil.isNotBlank(requestParam)
                ? DateUtil.parse(requestParam,"yyyy-MM-dd")
                : DateUtil.tomorrow();
        TrainMapper trainMapper = ApplicationContextHolder.getBean(TrainMapper.class);
        for(;; currentPage++){
            LambdaQueryWrapper<TrainDO> queryWrapper = Wrappers.lambdaQuery(TrainDO.class)
                    .between(TrainDO::getDepartureTime,DateUtil.beginOfDay(dateTime),DateUtil.endOfDay(dateTime));

            Page<TrainDO> trainDOPage = trainMapper.selectPage(new Page<>(currentPage,size),queryWrapper);
            if(trainDOPage == null || CollUtil.isEmpty(trainDOPage.getRecords())){
                break;
            }
            List<TrainDO> records = trainDOPage.getRecords();
            actualExecute(records);
        }
    }

    private String getJobRequestParam(){
        return EnvironmentUtil.isDevEnvironment()
                ? Optional.ofNullable(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())).map(ServletRequestAttributes::getRequest).map(each -> each.getHeader("requestParam")).orElse(null)
                : XxlJobHelper.getJobParam();
    }
}
