package com.cyf.xiaoxuedi.service.impl;

import com.cyf.xiaoxuedi.DAO.MissionDOMapper;
import com.cyf.xiaoxuedi.DAO.OrderDOMapper;
import com.cyf.xiaoxuedi.DAO.UserDOMapper;
import com.cyf.xiaoxuedi.DO.MissionDO;
import com.cyf.xiaoxuedi.DO.OrderDO;
import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.service.MissionService;
import com.cyf.xiaoxuedi.service.model.MissionItemModel;
import com.cyf.xiaoxuedi.service.model.MissionModel;
import com.cyf.xiaoxuedi.service.model.OrderModel;
import com.cyf.xiaoxuedi.validator.ValidationResult;
import com.cyf.xiaoxuedi.validator.ValidatorImpl;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MissionServiceImpl implements MissionService {

    @Autowired
    ValidatorImpl validator;

    @Autowired
    MissionDOMapper missionDOMapper;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    UserDOMapper userDOMapper;

    @Autowired
    OrderDOMapper orderDOMapper;

    /**
     *  发布任务
     * @param missionModel
     * @throws BuinessException
     */
    @Override
    public void publishMission(MissionModel missionModel) throws BuinessException {

        // 判空
        if(missionModel==null){
            throw new BuinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        // 校验参数
        ValidationResult validationResult = validator.validate(missionModel);
        if(validationResult.isHasErrors()){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }

        //持久化保存到数据库中
        MissionDO missionDO = new MissionDO();
        BeanUtils.copyProperties(missionModel, missionDO);
        missionDOMapper.insertSelective(missionDO);


        //缓存起来
//        redisTemplate.opsForValue().set("mission_"+missionDO.getId(), missionModel);
//        redisTemplate.expire("mission_"+missionDO.getId(), 30, TimeUnit.MINUTES);

    }

    @Override
    public List<MissionItemModel> getMissionList(String school, Integer page) throws BuinessException {

        // 验证入参
        if(page<0){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
        }

        // 获得分页偏移量
        final int pageSize = 10;
        Integer offset = page * pageSize;

        List<MissionDO> missionDOList= missionDOMapper.selectMissionBySchool(school, offset);

        List<MissionItemModel> missionItemModelList  = convertFromMissionDOList(missionDOList);

        return missionItemModelList;
    }

    @Override
    public MissionModel getMissionByID(Integer id) throws BuinessException {

        // 查询MissionDO
        MissionDO missionDO = missionDOMapper.selectByPrimaryKey(id);
        if(missionDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        // 查询该Mission的发布人

//        UserDO userDO = userDOMapper.selectByPrimaryKey(missionDO.getUserId());
        UserDO userDO = (UserDO) redisTemplate.opsForValue().get("User_"+missionDO.getUserId());
        if(userDO==null){
            System.out.println("here");
            userDO = userDOMapper.selectByPrimaryKey(missionDO.getUserId());
            if(userDO==null){
                throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
            }
            redisTemplate.opsForValue().set("User_"+missionDO.getUserId(), userDO);
            redisTemplate.expire("User_"+missionDO.getUserId(), 10,TimeUnit.MINUTES);
        }


        // 组装MissionModel 返回前端
        MissionModel missionModel = new MissionModel();
        BeanUtils.copyProperties(missionDO,missionModel);
        missionModel.setUserDO(userDO);

        return missionModel;
    }

    @Override
    public List<MissionItemModel> getMyMissionList(Integer status, Integer page, Integer userId) throws BuinessException {

        // 验证入参
        if(page<0){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
        }

        // 获得分页偏移量
        final int pageSize = 10;
        Integer offset = page * pageSize;

        System.out.println((byte)(status.intValue()));
        // 查询返回结果
        List<MissionDO> missionDOList =  missionDOMapper.selectMyMissionByStatus(userId,(byte)(status.intValue()),offset);

        List<MissionItemModel> missionItemModelList  = convertFromMissionDOList(missionDOList);



        return missionItemModelList;
    }

    @Override
    public List<MissionItemModel> getMyAcceptedMissionList(Integer status, Integer page, Integer userId) throws BuinessException {

        // 验证入参
        if(page<0){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
        }

        // 获得分页偏移量
        final int pageSize = 10;
        Integer offset = page * pageSize;


        List<OrderDO>missionDOList = orderDOMapper.selectByAccepterId(userId, (byte)(status.intValue()), offset);

        List<MissionItemModel> missionItemModelList = missionDOList.stream().map(orderDO -> {
            MissionItemModel missionItemModel = new MissionItemModel();
            // 获得任务
            MissionDO missionDO = missionDOMapper.selectByPrimaryKey(orderDO.getMissionId());

            BeanUtils.copyProperties(missionDO, missionItemModel);


            // 获得发布人姓名 并将UserDO缓存到Redis中
            UserDO userDO = (UserDO) redisTemplate.opsForValue().get("User_"+missionDO.getUserId());
            if(userDO==null){
                userDO = userDOMapper.selectByPrimaryKey(missionDO.getUserId());
                redisTemplate.opsForValue().set("User_"+missionDO.getUserId(), userDO);
                redisTemplate.expire("User_"+missionDO.getUserId(), 10,TimeUnit.MINUTES);
            }
            missionItemModel.setOrderId(orderDO.getId());
            missionItemModel.setUserName(userDO==null?"":userDO.getName());
            return missionItemModel;


        }).collect(Collectors.toList());


        return missionItemModelList;

    }

    @Override
    public OrderModel getMyMission(Integer id, Integer userId) throws BuinessException {

        OrderDO orderDO = orderDOMapper.selectByMissionId(id);

        OrderModel orderModel = new OrderModel();

        // 还未生成订单，无人接单情况
        if(orderDO==null){
            throw  new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        if(orderDO.getUserId()!=userId&&orderDO.getAccepterId()!=userId){
            throw  new BuinessException(EmBusinessError.UNKNOWN_ERROR, "无权限访问");
        }


        BeanUtils.copyProperties(orderDO, orderModel);

        // 获得发布人信息
        UserDO userDO = (UserDO) redisTemplate.opsForValue().get("User_"+orderDO.getUserId());
        if(userDO==null){
            userDO = userDOMapper.selectByPrimaryKey(orderDO.getUserId());
            if(userDO==null){
                throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
            }
            redisTemplate.opsForValue().set("User_"+orderDO.getUserId(), userDO);
            redisTemplate.expire("User_"+orderDO.getUserId(), 10,TimeUnit.MINUTES);
        }

        orderModel.setUserDO(userDO);


        // 获得接受人信息
        UserDO AccepterDO = (UserDO) redisTemplate.opsForValue().get("User_"+orderDO.getAccepterId());
        if(AccepterDO==null){
            AccepterDO = userDOMapper.selectByPrimaryKey(orderDO.getAccepterId());
            if(AccepterDO==null){
                throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
            }
            redisTemplate.opsForValue().set("User_"+orderDO.getAccepterId(), AccepterDO);
            redisTemplate.expire("User_"+orderDO.getAccepterId(), 10,TimeUnit.MINUTES);
        }

        orderModel.setAccepterDO(AccepterDO);


        // 获得任务信息
        MissionDO missionDO = missionDOMapper.selectByPrimaryKey(orderDO.getMissionId());
        orderModel.setMissionDO(missionDO);



        return orderModel;
    }

    private List<MissionItemModel> convertFromMissionDOList(List<MissionDO>missionDOList){

        List<MissionItemModel> missionItemModelList = missionDOList.stream().map(missionDO -> {

            // MissionDO转成MissionItemModel
            MissionItemModel missionItemModel = new MissionItemModel();
            BeanUtils.copyProperties(missionDO, missionItemModel);

            // 获得发布人姓名 并将UserDO缓存到Redis中
            UserDO userDO = (UserDO) redisTemplate.opsForValue().get("User_"+missionDO.getUserId());
            if(userDO==null){
                userDO = userDOMapper.selectByPrimaryKey(missionDO.getUserId());
                redisTemplate.opsForValue().set("User_"+missionDO.getUserId(), userDO);
                redisTemplate.expire("User_"+missionDO.getUserId(), 10,TimeUnit.MINUTES);
            }

            missionItemModel.setUserName(userDO==null?"":userDO.getName());
            return missionItemModel;

        }).collect(Collectors.toList());

        return missionItemModelList;
    }
}
