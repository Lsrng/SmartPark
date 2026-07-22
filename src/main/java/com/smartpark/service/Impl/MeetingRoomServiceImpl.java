package com.smartpark.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartpark.mapper.MeetingRoomMapper;
import com.smartpark.pojo.entity.MeetingRoom;
import com.smartpark.service.MeetingRoomService;
import org.springframework.stereotype.Service;

@Service
public class MeetingRoomServiceImpl extends ServiceImpl<MeetingRoomMapper, MeetingRoom> implements MeetingRoomService {
}
