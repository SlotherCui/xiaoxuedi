package com.cyf.xiaoxuedi.service;

import com.cyf.xiaoxuedi.DO.MissionDO;
import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.service.model.MissionItemModel;
import com.cyf.xiaoxuedi.service.model.MissionModel;
import com.cyf.xiaoxuedi.service.model.OrderModel;

import java.util.List;

public interface MissionService {
    void publishMission(MissionModel missionModel) throws BusinessException;
    List<MissionItemModel> getMissionList(String School,Integer page)throws BusinessException;
    MissionModel getMissionByID(Integer id) throws BusinessException;
    List<MissionItemModel> getMyMissionList(Integer status,Integer page, Integer userId)throws BusinessException;
    List<MissionItemModel>getMyAcceptedMissionList(Integer status,Integer page, Integer userId)throws BusinessException;
    OrderModel getMyMission(Integer id, Integer userId)throws BusinessException;
    MissionDO getMissionDOByIdInCache(Integer id);
}
