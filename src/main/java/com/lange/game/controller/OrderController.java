package com.lange.game.controller;/**
 * @Author linhao Hu
 * @Date 2020/6/23 20:22
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lange.App;
import com.lange.enums.ResponseEnum;
import com.lange.game.domian.Plate;
import com.lange.game.domian.User;
import com.lange.game.domian.Bills;
import com.lange.game.domian.vo.BillsInfo;
import com.lange.game.domian.vo.PaidBillsVo;
import com.lange.game.domian.vo.ProBillsVo;
import com.lange.game.mapper.BillsMapper;
import com.lange.game.mapper.PlateMapper;
import com.lange.game.mapper.UserMapper;
import com.lange.utils.AppResponseResult;
import com.lange.utils.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @Author linhao Hu
 * @Date 2020/6/23 20:22
 *
 */
@RestController
@RequestMapping("order/")
@Slf4j
public class OrderController {
    @Resource
    private BillsMapper billsMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private PlateMapper plateMapper;
    /**
     * 添加比赛订单
     * @param params
     * @return
     */
    @RequestMapping("addOrderList")
    public AppResponseResult addOrderList(@RequestBody Map params){
        if(CommUtils.isNull(params.get("betParam"))){
            return AppResponseResult.error();
        }
        List betParam = (List) params.get("betParam");
        Object userId = params.get("userId");
        Object countAmount = params.get("countAmount");
        if(CommUtils.isNull(userId)||CommUtils.isNull(countAmount)){
            return AppResponseResult.errorMsg("下单失败：userId为空或总金额为空!");
        }
        User user = userMapper.selectById(Long.parseLong(userId.toString()));
        if(user == null){
           return AppResponseResult.errorMsg("下单失败：未查到用户!");
        }
        BigDecimal sub =user.getGold().subtract(BigDecimal.valueOf(Double.parseDouble(countAmount.toString())));

//        if( sub.compareTo(new BigDecimal(0)) == -1){
//            return AppResponseResult.errorMsg("下单失败：金币不足!");
//        }
        List<Bills> list = new ArrayList<Bills>();
        for (Object m : betParam) {
            Bills order = JSONArray.parseObject( JSON.toJSONString(m), Bills.class);
            //验证盘口赔率是否被修改
            Plate p = plateMapper.selectById(order.getPlateId());
            if(p.getStatus()!=0){
                log.info("下单失败，盘口已被修改....");
                return AppResponseResult.error();
            }
            order.setUserId(Long.parseLong(userId.toString()));
            order.setType(0); //0:等待接单 1:未结算  2：已结算
            order.setCreateTime(new Date());
            list.add(order);
        }
        for (Bills order :list){
            billsMapper.insert(order);
            log.info("下单成功，userId:{}",order.getUserId());
        }
        // 下单成功减去用户金币
        user.setGold(sub);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return AppResponseResult.success(ResponseEnum.DATA_EMPTY);
    }

    /**
     * 添加项目订单
     * @param params
     * @return
     */
    @RequestMapping("addOrder")
    public AppResponseResult addOrder(@RequestBody Map params){

        Object plateId = params.get("plateId");
        Object amount = params.get("amount");
        Object userId = params.get("userId");
        if(CommUtils.isNull(plateId)||CommUtils.isNull(amount)||CommUtils.isNull(userId)){
            log.info("下单失败，参数错误！");
            return AppResponseResult.error();
        }

        User user = userMapper.selectById(Long.parseLong(userId.toString()));
        if(user == null){
            return AppResponseResult.errorMsg("下单失败：未查到用户!");
        }
        BigDecimal sub =user.getGold().subtract(BigDecimal.valueOf(Double.parseDouble(amount.toString())));

//        if( sub.compareTo(new BigDecimal(0)) == -1){
//            return AppResponseResult.errorMsg("下单失败：金币不足!");
//        }
        //验证盘口赔率是否被修改
        Plate p = plateMapper.selectById(Long.parseLong(plateId.toString()));
        if(p.getStatus()!=0){
            log.info("下单失败，盘口已被修改....");
            return AppResponseResult.error();
        }
        Bills bills = new Bills();
        bills.setUserId(Long.parseLong(userId.toString()));
        bills.setAmount(new BigDecimal(amount.toString()));
        bills.setPlateId(Long.parseLong(plateId.toString()));
        bills.setType(0); //0:等待接单 1:未结算  2：已结算
        bills.setCreateTime(new Date());
        billsMapper.insert(bills);
        log.info("下单成功，userId:{}",bills.getUserId());

        // 下单成功减去用户金币
        user.setGold(sub);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return AppResponseResult.success(ResponseEnum.DATA_EMPTY);
    }

    /**
     * 查询用户订单
     * @param params
     * @return
     */
    @RequestMapping("getUserOrders")
    public AppResponseResult getUserOrders(@RequestBody Map params){
        Object userId = params.get("userId");
        if(CommUtils.isNull(userId)){
            return AppResponseResult.error();
        }
        List<BillsInfo> list = billsMapper.getBillsByuserId(Long.parseLong(userId.toString()));
        //log.info(list.toString());
        return AppResponseResult.success(list);
    }

    /**
     * 查询所有订单
     * @return
     */
    @RequestMapping("getAllOrders")
    public AppResponseResult getAllOrders(){

        List<BillsInfo> list = billsMapper.getAllBills();
        //log.info(list.toString());
        return AppResponseResult.success(list);
    }

    /**
     * 查询所有待接订单
     * @return
     */
    @RequestMapping("getTakeOrders")
    public AppResponseResult getTakeOrders(){

        List<BillsInfo> list = billsMapper.getTakeOrders();
        //log.info(list.toString());
        return AppResponseResult.success(list);
    }

    /**
     * 接单/取消
     * @return
     */
    @RequestMapping("takeOrder")
    public AppResponseResult takeOrder(@RequestBody Map<String,Object> param){
        Object id= param.get("id");
        if(CommUtils.isNull(id)){
            return AppResponseResult.error();
        }
        Bills b = billsMapper.selectById(Long.parseLong(id.toString()));
        if(CommUtils.isNull(b)){
            return AppResponseResult.error();
        }
        if(b.getType() == 0){
            b.setType(1);
            b.setUpdateTime(new Date());
            billsMapper.updateById(b);
            return AppResponseResult.success(true);
        }else if(b.getType() == 1){
            b.setType(0);
            b.setUpdateTime(new Date());
            billsMapper.updateById(b);
            return AppResponseResult.success(false);
        }else {
            return AppResponseResult.error();
        }

    }

    /**
     * 待赔付的订单
     * @return
     */
    @RequestMapping("paidOrders")
    public AppResponseResult paidOrders(){
        //0:等待接单 1:未结算  2：已结算 3:已赔付
        List<BillsInfo>  bills = billsMapper.getAllPaidBills();
        List<PaidBillsVo> result = new ArrayList<PaidBillsVo>();
        bills.stream().collect(Collectors.groupingBy(BillsInfo:: getUserId,Collectors.toList())).forEach((userId,childList) ->{
            PaidBillsVo pv = new PaidBillsVo();
            pv.setUserId(userId);
            List<ProBillsVo> proBillsVos = new ArrayList<>();
            childList.stream().collect(Collectors.groupingBy(BillsInfo:: getProId,Collectors.toList())).forEach((proId,proBills)->{
                DoubleSummaryStatistics doubleSummaryStatistics = proBills.stream().mapToDouble(p ->{
                    return Double.parseDouble(p.getSettlementAmount() == null?"0":p.getSettlementAmount().toString());
                }).summaryStatistics();
                ProBillsVo pb = new ProBillsVo();
                pb.setProjectId(proId);
                pb.setBillsInfos(proBills);
                pv.setCountAmount(new BigDecimal(doubleSummaryStatistics.getSum()));
                proBillsVos.add(pb);
            });
            pv.setProBillsVos(proBillsVos);
            result.add(pv);
        });
        //log.info(JSON.toJSONString(result));
        return AppResponseResult.success(result);
    }

    @RequestMapping("takePaidOrders")
    public AppResponseResult takePaidOrders(@RequestBody Map param){
        Object id = param.get("userId");
        if(CommUtils.isNull(id)){
            return AppResponseResult.error();
        }
        User user = userMapper.selectById(Long.parseLong(id.toString()));
        if(user == null ){
            return AppResponseResult.error();
        }
        List<Bills> bills = billsMapper.selectList(new LambdaQueryWrapper<Bills>().eq(Bills::getUserId ,user.getId()).eq(Bills::getType,2));
        for (Bills b:bills) {
            b.setType(3);
            b.setUpdateTime(new Date());
            billsMapper.updateById(b);
            //添加用户结算金额
            user.setWithdrawGold((user.getWithdrawGold()==null?new BigDecimal(0):user.getWithdrawGold())
                    .add(b.getSettlementAmount()==null?new BigDecimal(0):b.getSettlementAmount()));
        }
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return AppResponseResult.success();
    }

}
