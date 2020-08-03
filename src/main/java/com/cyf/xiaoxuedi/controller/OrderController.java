package com.cyf.xiaoxuedi.controller;

import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.response.CommonReturnType;
import com.cyf.xiaoxuedi.service.OrderService;
import com.cyf.xiaoxuedi.service.model.OrderModel;
import com.cyf.xiaoxuedi.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class OrderController extends BaseController {

    @Autowired
    OrderService orderService;

    @Autowired
    HttpServletRequest httpServletRequest;

    /**
     * 抢任务
     * @param id
     * @return
     * @throws BusinessException
     */
    @PostMapping(value = "/acceptMission", consumes = CONTENT_TYPE_FORMED)
    public CommonReturnType AcceptMission(@RequestParam("id") Integer id) throws BusinessException {
        // 获取token
        String [] parameterMap = httpServletRequest.getParameterMap().get("token");
        if(parameterMap==null)
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(parameterMap[0]);


        OrderModel orderModel = null;

        // 如果执行失败则执行方法

        // 0. 在缓存中判断该任务是否被抢
        Boolean missionStatus = redisTemplate.opsForValue().setIfAbsent("Mission_status_"+id, 1, 1, TimeUnit.DAYS);

        if(missionStatus){

            try{
                orderModel = orderService.acceptMission(id, userModel.getId());
            }catch (Exception e){
                // 事务回滚则回复缓存中的标志
                redisTemplate.delete("Mission_status_"+id);
                throw e;
            }

        }else{
            throw new BusinessException(EmBusinessError.MISSION_HAS_GONE);
        }



        return CommonReturnType.create(orderModel);
    }

    /**
     *  通过订单编号结束订单
     * @param id
     * @return
     * @throws BusinessException
     */
    @PostMapping(value = "/finishMission", consumes = CONTENT_TYPE_FORMED)
    public CommonReturnType FinishMission(@RequestParam("id") String id ) throws BusinessException {
        // 获取token
        String [] parameterMap = httpServletRequest.getParameterMap().get("token");
        if(parameterMap==null)
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(parameterMap[0]);


        orderService.finishOrder(id ,userModel.getId());

        return CommonReturnType.create("");
    }

    /**
     * 获取当前进行中的订单列表
     * @param type 0 我发布的任务 1 我接受的任务
     * @param page
     * @return
     * @throws BusinessException
     */
    @GetMapping("/getOrderList")
    public CommonReturnType GetOrderList(@RequestParam("type") Integer type,
                                         @RequestParam("page") Integer page) throws BusinessException {
        // 获取token
        String [] parameterMap = httpServletRequest.getParameterMap().get("token");
        if(parameterMap==null)
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(parameterMap[0]);


        List<OrderModel> orderModelList = orderService.getCurrentOrderList(userModel.getId(), type, page);

        return CommonReturnType.create(orderModelList);
    }

}
