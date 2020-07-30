package com.cyf.xiaoxuedi.service;

import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.service.model.MissionItemModel;
import com.cyf.xiaoxuedi.service.model.MissionModel;

import java.util.List;

public interface MissionService {
    void publishMission(MissionModel missionModel) throws BuinessException;
    List<MissionItemModel> getMissionList(String School,Integer page)throws BuinessException;
    MissionModel getMissionByID(Integer id) throws BuinessException;
}
