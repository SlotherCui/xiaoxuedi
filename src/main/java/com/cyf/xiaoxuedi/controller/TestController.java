package com.cyf.xiaoxuedi.controller;

import com.cyf.xiaoxuedi.DAO.UserDOMapper;
import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.response.CommonReturnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController extends BaseController{

    @Autowired
    UserDOMapper userDOMapper;

    @GetMapping("/say")
    public CommonReturnType test(){
        UserDO userDO = userDOMapper.selectByPrimaryKey(1);

        return userDO==null?CommonReturnType.create("无用户","fail","无用户"):CommonReturnType.create(userDO);

    }

    @GetMapping("/testTransactional")
    @Transactional(rollbackFor = BusinessException.class)
    public CommonReturnType testTransactional() throws BusinessException {
        UserDO userDO = new UserDO();
        userDO.setName("测试");
        userDO.setSchool("测试");
        userDO.setGender((byte) 1);
        userDO.setTelephone("13485969210");
        userDOMapper.insertSelective(userDO);


        if(userDO.getName().equals("测试")){
            throw  new BusinessException(EmBusinessError.UNKNOWN_ERROR, "测试需要");
        }
        UserDO userDO2 = new UserDO();
        userDO2.setName("测试2");
        userDO2.setSchool("测试2");
        userDO2.setGender((byte) 1);
        userDO2.setTelephone("13485969211");
        userDOMapper.insertSelective(userDO2);

        return  CommonReturnType.create("");



    }
//    @GetMapping("/say1")
//    public int test2(){
//        UserDO userDO = new UserDO();
//        userDO.setGender((byte) 1);
//        userDO.setName("崔玉峰");
//        userDO.setSchool("北京航空航天大学");
//        userDO.setTelphone("18340018810");
//        userDOMapper.insert(userDO);
//
//        return userDOMapper.insert(userDO);
//    }
}
