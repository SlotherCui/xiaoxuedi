package com.cyf.xiaoxuedi.controller;

import com.alibaba.druid.util.StringUtils;
import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.response.CommonReturnType;
import com.cyf.xiaoxuedi.service.UserService;
import com.cyf.xiaoxuedi.service.model.UserModel;
import com.cyf.xiaoxuedi.utils.EncrptUtils;
import com.cyf.xiaoxuedi.validator.ValidationResult;
import com.cyf.xiaoxuedi.validator.ValidatorImpl;
import com.sun.javaws.exceptions.BadMimeTypeResponseException;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


// https://github.com/SlotherCui/
@RestController
public class UserController extends BaseController{

    @Autowired
    UserService userService;

    @Autowired
    HttpServletRequest httpServletRequest;
    /**
     * 用户注册
     * @param name
     * @param gender
     * @param school
     * @param telephone
     * @param password
     * @return
     * @throws BuinessException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    @PostMapping("/register")
    public CommonReturnType Register(@RequestParam("name") String name,
                                     @RequestParam("gender") Byte gender,
                                     @RequestParam("school") String school,
                                     @RequestParam("telephone") String telephone,
                                     @RequestParam("password") String password) throws BuinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //创建UserModel
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setSchool(school);
        userModel.setTelephone(telephone);
        userModel.setEncrptPassword(EncrptUtils.encodeByMd5(password));

        //交给Service层完成注册
        userService.register(userModel);

        return CommonReturnType.create("");
    }

    /**
     *  用户登录
     * @param telephone
     * @param password
     * @return
     * @throws BuinessException
     */
    @PostMapping("/login")
    public CommonReturnType Login(@RequestParam("telephone") String telephone,
                                  @RequestParam("password") String password) throws BuinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        if(password==null){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"密码或电话号码错误");
        }



        // 交给service验证
        UserModel userModel = userService.login(telephone,password);

        String uuidToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);

//        httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
//        httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);

        return CommonReturnType.create(uuidToken);
    }

    /**
     * 获取用户model
     * @return
     * @throws BuinessException
     */
    @GetMapping("getUser")
    public CommonReturnType GetUser() throws BuinessException {

        // 获取token
        String token = httpServletRequest.getParameterMap().get("token")[0];

        // 验证是否登录
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(StringUtils.isEmpty(token)){
            throw  new BuinessException(EmBusinessError.USER_NOT_LOGIN);
        }

        if(userModel==null){
            throw  new BuinessException(EmBusinessError.USER_NOT_LOGIN);
        }

        return CommonReturnType.create(userModel);
    }


}
