package com.cyf.xiaoxuedi.service;

import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.service.model.UserModel;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public interface UserService {
    void register(UserModel userModel) throws BuinessException;
    UserModel login(String telephone, String  password) throws BuinessException, UnsupportedEncodingException, NoSuchAlgorithmException;
}

