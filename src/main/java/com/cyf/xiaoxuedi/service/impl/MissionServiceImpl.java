package com.cyf.xiaoxuedi.service.impl;

import com.cyf.xiaoxuedi.DAO.MissionDOMapper;
import com.cyf.xiaoxuedi.DAO.OrderDOMapper;
import com.cyf.xiaoxuedi.DAO.UserDOMapper;
import com.cyf.xiaoxuedi.DO.MissionDO;
import com.cyf.xiaoxuedi.DO.OrderDO;
import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.service.MissionService;
import com.cyf.xiaoxuedi.service.UserService;
import com.cyf.xiaoxuedi.service.model.MissionItemModel;
import com.cyf.xiaoxuedi.service.model.MissionModel;
import com.cyf.xiaoxuedi.service.model.OrderModel;
import com.cyf.xiaoxuedi.validator.ValidationResult;
import com.cyf.xiaoxuedi.validator.ValidatorImpl;
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

    @Autowired
    UserService userService;

    @Autowired
    MissionService missionService;

    /**
     *  发布任务
     * @param missionModel
     * @throws BusinessException
     */
    @Override
    public void publishMission(MissionModel missionModel) throws BusinessException {

        // 判空
        if(missionModel==null){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        // 校验参数
        ValidationResult validationResult = validator.validate(missionModel);
        if(validationResult.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }

        //持久化保存到数据库中
        MissionDO missionDO = new MissionDO();
        BeanUtils.copyProperties(missionModel, missionDO);
        missionDOMapper.insertSelective(missionDO);


//        缓存起来
//        redisTemplate.opsForValue().set("Mission_"+missionDO.getId(), missionDO);
//        redisTemplate.expire("Mission_"+missionDO.getId(), 30, TimeUnit.MINUTES);

    }

    @Override
    public List<MissionItemModel> getMissionList(String school, Integer page) throws BusinessException {

        // 验证入参
        if(page<0){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
        }

        // 获得分页偏移量
        final int pageSize = 10;
        Integer offset = page * pageSize;

        List<MissionDO> missionDOList= missionDOMapper.selectMissionBySchool(school, offset);

        List<MissionItemModel> missionItemModelList  = convertFromMissionDOList(missionDOList);

        return missionItemModelList;
    }

    @Override
    public MissionModel getMissionByID(Integer id) throws BusinessException {

        // 查询MissionDO
//        MissionDO missionDO = missionDOMapper.selectByPrimaryKey(id);
        MissionDO missionDO = missionService.getMissionDOByIdInCache(id);
        if(missionDO==null){
            throw new BusinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        // 查询该Mission的发布人
        UserDO userDO = userService.getUserDOByIdInCache(missionDO.getUserId());
        if(userDO==null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIT);
        }


        // 组装MissionModel 返回前端
        MissionModel missionModel = new MissionModel();
        BeanUtils.copyProperties(missionDO,missionModel);
        missionModel.setUserDO(userDO);

        return missionModel;
    }

    @Override
    public List<MissionItemModel> getMyMissionList(Integer status, Integer page, Integer userId) throws BusinessException {

        // 验证入参
        if(page<0){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
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
    public List<MissionItemModel> getMyAcceptedMissionList(Integer status, Integer page, Integer userId) throws BusinessException {

        // 验证入参
        if(page<0){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
        }

        // 获得分页偏移量
        final int pageSize = 10;
        Integer offset = page * pageSize;


        List<OrderDO>missionDOList = orderDOMapper.selectByAccepterId(userId, (byte)(status.intValue()), offset);

        List<MissionItemModel> missionItemModelList = missionDOList.stream().map(orderDO -> {
            MissionItemModel missionItemModel = new MissionItemModel();
            // 获得任务
            MissionDO missionDO = missionService.getMissionDOByIdInCache(orderDO.getMissionId());
//            MissionDO missionDO = missionDOMapper.selectByPrimaryKey(orderDO.getMissionId());

            BeanUtils.copyProperties(missionDO, missionItemModel);


            // 获得发布人姓名 并将UserDO缓存到Redis中
            UserDO userDO = userService.getUserDOByIdInCache(missionDO.getUserId());


            missionItemModel.setOrderId(orderDO.getId());
            missionItemModel.setUserName(userDO==null?"":userDO.getName());
            return missionItemModel;


        }).collect(Collectors.toList());


        return missionItemModelList;

    }

    @Override
    public OrderModel getMyMission(Integer id, Integer userId) throws BusinessException {

        OrderDO orderDO = orderDOMapper.selectByMissionId(id);

        OrderModel orderModel = new OrderModel();

        // 还未生成订单，无人接单情况
        if(orderDO==null){
            throw  new BusinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        if(orderDO.getUserId()!=userId&&orderDO.getAccepterId()!=userId){
            throw  new BusinessException(EmBusinessError.UNKNOWN_ERROR, "无权限访问");
        }


        BeanUtils.copyProperties(orderDO, orderModel);

        // 获得发布人信息
        UserDO userDO = userService.getUserDOByIdInCache(orderDO.getUserId());
        if(userDO==null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIT);
        }
        orderModel.setUserDO(userDO);


        // 获得接受人信息
        UserDO AccepterDO = userService.getUserDOByIdInCache(orderDO.getAccepterId());
        if(AccepterDO==null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIT);
        }
        orderModel.setAccepterDO(AccepterDO);


        // 获得任务信息
        MissionDO missionDO = missionService.getMissionDOByIdInCache(orderDO.getMissionId());
//        MissionDO missionDO = missionDOMapper.selectByPrimaryKey(orderDO.getMissionId());
        orderModel.setMissionDO(missionDO);


        return orderModel;
    }

    @Override
    public MissionDO getMissionDOByIdInCache(Integer id) {

        MissionDO missionDO = (MissionDO) redisTemplate.opsForValue().get("Mission_"+id);
        if(missionDO==null){
            missionDO = missionDOMapper.selectByPrimaryKey(id);
            redisTemplate.opsForValue().set("Mission_"+id, missionDO);
            redisTemplate.expire("Mission_"+id, 30, TimeUnit.MINUTES);
        }
        return missionDO;
    }

    private List<MissionItemModel> convertFromMissionDOList(List<MissionDO>missionDOList){

        List<MissionItemModel> missionItemModelList = missionDOList.stream().map(missionDO -> {

            // MissionDO转成MissionItemModel
            MissionItemModel missionItemModel = new MissionItemModel();
            BeanUtils.copyProperties(missionDO, missionItemModel);

            // 获得发布人姓名 并将UserDO缓存到Redis中
            UserDO userDO = userService.getUserDOByIdInCache(missionDO.getUserId());

            missionItemModel.setUserName(userDO==null?"":userDO.getName());
            return missionItemModel;

        }).collect(Collectors.toList());

        return missionItemModelList;
    }
}
