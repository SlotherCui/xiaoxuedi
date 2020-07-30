package com.cyf.xiaoxuedi.controller;


import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.response.CommonReturnType;
import com.cyf.xiaoxuedi.service.MissionService;
import com.cyf.xiaoxuedi.service.model.MissionItemModel;
import com.cyf.xiaoxuedi.service.model.MissionModel;
import com.cyf.xiaoxuedi.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@RestController
public class MissionController extends  BaseController{

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    MissionService missionService;

    @PostMapping(value = "/mainList",consumes = CONTENT_TYPE_FORMED)
    public CommonReturnType GetMissionList(@RequestParam("school") String school,
                                           @RequestParam("page") Integer page) throws BuinessException {

        // 获取token
        String token = httpServletRequest.getParameterMap().get("token")[0];
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(token);

        // 检验入参
        if(StringUtils.isEmpty(school)||page<0){
            throw  new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 跟据学校获取任务列表
        List<MissionItemModel> missionItemModelList = missionService.getMissionList(school, page);


        return CommonReturnType.create(missionItemModelList);
    }


    @GetMapping("/publish")
    public CommonReturnType PublishMission(@RequestParam("title") String title,
                                           @RequestParam("detail") String detail,
                                           @RequestParam("price") BigDecimal price,
                                           @RequestParam("location")String location,
                                           @RequestParam("school") String school
                                           ) throws BuinessException {

        // 获取token
        String token = httpServletRequest.getParameterMap().get("token")[0];
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(token);

        // 创建MissionModel
        MissionModel missionModel = new MissionModel();
        missionModel.setTitle(title);
        missionModel.setPrice(price);
        missionModel.setDetail(detail);
        missionModel.setLocation(location);
        missionModel.setSchool(school);
        missionModel.setStatus((byte) 0);
        missionModel.setUserId(userModel.getId());

        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        missionModel.setUserDO(userDO);


        missionService.publishMission(missionModel);


        return CommonReturnType.create("");
    }


    @GetMapping("getMission")
    public CommonReturnType GetMission(@RequestParam("id") Integer id) throws BuinessException {

        // 获取token
        String token = httpServletRequest.getParameterMap().get("token")[0];
        // 验证是否登录并获取UserModel
        UserModel userModel = isLogin(token);

        if(id==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        MissionModel missionModel = missionService.getMissionByID(id);

        return CommonReturnType.create(missionModel);
    }




}
