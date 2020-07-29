package com.cyf.xiaoxuedi.Controller;

import com.cyf.xiaoxuedi.DAO.UserDOMapper;
import com.cyf.xiaoxuedi.DO.UserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    UserDOMapper userDOMapper;

    @GetMapping("/say")
    public String test(){
        UserDO userDO = userDOMapper.selectByPrimaryKey(1);

        return userDO==null?"无用户":userDO.getName();
    }

    @GetMapping("/say1")
    public int test2(){
        UserDO userDO = new UserDO();
        userDO.setGender((byte) 1);
        userDO.setName("崔玉峰");
        userDO.setSchool("北京航空航天大学");
        userDO.setTelphone("18340018810");
        userDOMapper.insert(userDO);

        return userDOMapper.insert(userDO);
    }
}
