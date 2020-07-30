package com.cyf.xiaoxuedi.service.impl;

import com.cyf.xiaoxuedi.DAO.UserDOMapper;
import com.cyf.xiaoxuedi.DAO.UserPasswordDOMapper;
import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.DO.UserPasswordDO;
import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.service.UserService;
import com.cyf.xiaoxuedi.service.model.UserModel;
import com.cyf.xiaoxuedi.utils.EncrptUtils;
import com.cyf.xiaoxuedi.validator.ValidationResult;
import com.cyf.xiaoxuedi.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    ValidatorImpl validator;

    @Autowired
    UserDOMapper userDOMapper;

    @Autowired
    UserPasswordDOMapper userPasswordDOMapper;

    @Override
    @Transactional
    public void register(UserModel userModel) throws BuinessException {

        if(userModel==null){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 验证传入的注册参数
        ValidationResult validationResult = validator.validate(userModel);

        if(validationResult.isHasErrors()){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }

        // 拆分成UserDO和UserPasswordDO
        UserDO userDO =  convertToUserDO(userModel);

        // 分别进行持久化
        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException e) {
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已被注册！");
        }


        userModel.setId(userDO.getId());
        UserPasswordDO userPasswordDO = convertToUserPasswordDO(userModel);
        userPasswordDOMapper.insert(userPasswordDO);

    }

    public  UserDO convertToUserDO(UserModel userModel){

        if(userModel==null) return null;

        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);

        return userDO;
    }

    public UserPasswordDO convertToUserPasswordDO(UserModel userModel){

        if(userModel==null) return null;

        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());

        return userPasswordDO;
    }
    public UserModel converToUserModel(UserDO userDO,UserPasswordDO userPasswordDO){

        if(userDO==null||userDO==null){
            return null;
        }

        UserModel userModel = new UserModel();

        BeanUtils.copyProperties(userDO, userModel);

        userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());

        return userModel;
    }

    @Override
    public UserModel login(String telephone, String password) throws UnsupportedEncodingException, NoSuchAlgorithmException, BuinessException {

        // 获取用户信息
        UserDO userDO = userDOMapper.selectByTelephone(telephone);

        if(userDO==null){
            throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
        }

        // 查询对应密码并进行比对
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = converToUserModel(userDO, userPasswordDO);
        if(userModel == null){
            throw new BuinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        if(!StringUtils.equals(userPasswordDO.getEncrptPassword(), EncrptUtils.encodeByMd5(password))){
            throw new BuinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        return userModel;
    }
}
