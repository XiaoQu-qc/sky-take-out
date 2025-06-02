package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        LocalDate temp = begin;

        while (!temp.equals(end)) {
            temp = temp.plusDays(1);
            dateList.add(temp);
        }

//        List<Double> turnoverList = new ArrayList<>();
//
//        for (LocalDate date : dateList) {
//            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
//            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
//
//            Map map = new HashMap<>();
//            map.put("beginTime", beginTime);
//            map.put("endTime", endTime);
//            map.put("status", Orders.COMPLETED);
//            Double turnover = orderMapper.sumByMap(map);
//            if (turnover == null) {
//                turnover = 0.0;
//            }
//            turnoverList.add(turnover);
//        }

        // 创建查询条件
        Map<String, Object> params = new HashMap<>();
        params.put("beginTime", LocalDateTime.of(begin, LocalTime.MIN));
        params.put("endTime", LocalDateTime.of(end, LocalTime.MAX));
        params.put("status", Orders.COMPLETED);
        params.put("dateList", dateList);

// 调用 Mapper 方法
        List<Map<String, Object>> result = orderMapper.sumByDateRange(params);

// 处理结果
        Map<LocalDate, Double> turnoverMap = new HashMap<>();
        for (Map<String, Object> item : result) {
            LocalDate orderDate = ((java.sql.Date) item.get("order_date")).toLocalDate();
            Object turnoverObj = item.get("turnover");
            Double turnover = turnoverObj instanceof BigDecimal ? ((BigDecimal) turnoverObj).doubleValue() : (turnoverObj instanceof Double ? (Double) turnoverObj : 0.0);
            turnoverMap.put(orderDate, turnover);
        }

// 构建返回的营业额列表
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Double turnover = turnoverMap.getOrDefault(date, 0.0);
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户信息
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end", endTime);

            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);

        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();

    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        LocalDate temp = begin;

        while (!temp.equals(end)) {
            temp = temp.plusDays(1);
            dateList.add(temp);
        }
        List<Integer> totalOrderList = new ArrayList<>();
        List<Integer> validOrderList = new ArrayList<>();

        for (LocalDate date : dateList) {
            Map map = new HashMap<>();
            map.put("begin",LocalDateTime.of(date, LocalTime.MIN));
            map.put("end",LocalDateTime.of(date, LocalTime.MAX));
            //查询每天的订单总数
            Integer totalOrder = orderMapper.countByMap(map);
            totalOrderList.add(totalOrder);
            //查询每天的有效订单数
            map.put("status",Orders.COMPLETED);
            Integer validOrder = orderMapper.countByMap(map);
            validOrderList.add(validOrder);
        }

        Integer totalOrderListCount = totalOrderList.stream().reduce(Integer::sum).get();
        Integer validOrderListCount = validOrderList.stream().reduce(Integer::sum).get();

        Double orderCompleteRate = 0.0;
        if(totalOrderListCount!=0){
            orderCompleteRate = validOrderListCount.doubleValue()/totalOrderListCount.doubleValue();
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(totalOrderList,","))
                .validOrderCountList(StringUtils.join(validOrderList,","))
                .totalOrderCount(totalOrderListCount)
                .validOrderCount(validOrderListCount)
                .orderCompletionRate(orderCompleteRate)
                .build();

    }

    /**
     * 统计指定时间区间内的销量排名前10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
