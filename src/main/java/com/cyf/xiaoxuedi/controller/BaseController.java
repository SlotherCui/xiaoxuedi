package com.cyf.xiaoxuedi.controller;


import com.alibaba.druid.util.StringUtils;
import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.response.CommonReturnType;
import com.cyf.xiaoxuedi.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class BaseController {

    public static final String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

    @Autowired
    RedisTemplate redisTemplate;

    //定义exceptionhandler解决未被controller层吸收的exception
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object handlerException(HttpServletRequest request, Exception ex) {
        Map<String, Object> responseData = new HashMap<>();
        if (ex instanceof BuinessException) {
            BuinessException buinessException = (BuinessException) ex;
            return CommonReturnType.create("", String.valueOf(buinessException.getErrorCode()),buinessException.getErrorMsg());
        } else {
            ex.printStackTrace();
            return CommonReturnType.create("", String.valueOf(EmBusinessError.UNKNOWN_ERROR.getErrorCode()),EmBusinessError.UNKNOWN_ERROR.getErrorMsg());
        }

    }


    public UserModel isLogin(String token) throws BuinessException {

        if(StringUtils.isEmpty(token)){
            throw new BuinessException(EmBusinessError.USER_NOT_LOGIN);
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BuinessException(EmBusinessError.USER_NOT_LOGIN);
        }

        return userModel;
    }


}
