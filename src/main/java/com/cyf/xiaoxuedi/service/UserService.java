package com.cyf.xiaoxuedi.service;

import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.service.model.UserModel;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public interface UserService {
    void register(UserModel userModel) throws BusinessException;
    UserModel login(String telephone, String  password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException;
    UserDO  getUserDOByIdInCache(Integer id);
}

