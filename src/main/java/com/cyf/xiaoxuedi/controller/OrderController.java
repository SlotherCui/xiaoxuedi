package com.cyf.xiaoxuedi.controller;

import com.cyf.xiaoxuedi.error.BuinessException;
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
     * @throws BuinessException
     */
    @PostMapping(value = "/acceptMission", consumes = CONTENT_TYPE_FORMED)
    public CommonReturnType AcceptMission(@RequestParam("id") Integer id) throws BuinessException {
        // 获取token
        String [] parameterMap = httpServletRequest.getParameterMap().get("token");
        if(parameterMap==null)
            throw new BuinessException(EmBusinessError.USER_NOT_LOGIN);
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(parameterMap[0]);

        OrderModel orderModel = orderService.acceptMission(id, userModel.getId());

        return CommonReturnType.create(orderModel);
    }

    /**
     *  通过订单编号结束订单
     * @param id
     * @return
     * @throws BuinessException
     */
    @PostMapping(value = "/finishMission", consumes = CONTENT_TYPE_FORMED)
    public CommonReturnType FinishMission(@RequestParam("id") String id ) throws  BuinessException {
        // 获取token
        String [] parameterMap = httpServletRequest.getParameterMap().get("token");
        if(parameterMap==null)
            throw new BuinessException(EmBusinessError.USER_NOT_LOGIN);
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
     * @throws BuinessException
     */
    @GetMapping("/getOrderList")
    public CommonReturnType GetOrderList(@RequestParam("type") Integer type,
                                         @RequestParam("page") Integer page) throws  BuinessException {
        // 获取token
        String [] parameterMap = httpServletRequest.getParameterMap().get("token");
        if(parameterMap==null)
            throw new BuinessException(EmBusinessError.USER_NOT_LOGIN);
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(parameterMap[0]);


        List<OrderModel> orderModelList = orderService.getCurrentOrderList(userModel.getId(), type, page);

        return CommonReturnType.create(orderModelList);
    }

}
