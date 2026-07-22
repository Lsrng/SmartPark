package com.smartpark.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartpark.pojo.entity.MeetingRoom;
import com.smartpark.service.MeetingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "会议室管理", description = "会议室相关操作接口")
@RequestMapping("/meeting-rooms") // 添加统一路径前缀
public class MeetingRoomController {
    private final MeetingRoomService meetingRoomService;

    @PostMapping
    @Operation(
            summary = "保存会议室信息",
            description = "创建或更新会议室信息"
    )
    public String save(@org.springframework.web.bind.annotation.RequestBody MeetingRoom meetingRoom) {
        return "{" +
                "\"msg\":" + (meetingRoomService.save(meetingRoom) ? "\"保存成功\"" : "\"保存失败\"") +
                "}";
    }

    @GetMapping
    @Operation(
            summary = "分页查询会议室信息",
            description = "根据页码和每页数量查询会议室列表"
    )
    public String selectPage(Integer pageNo, Integer pageSize) {
        // 设置默认值，避免空指针异常
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        Page<MeetingRoom> page = new Page<>(pageNo, pageSize);
        meetingRoomService.page(page);

        String msg = "数据总条数: " + page.getTotal() +
                ",当前页数为: " + page.getCurrent() +
                ",每页数据条数: " + page.getSize() +
                ",总页数为: " + page.getPages();

        return "{" +
                "\"msg\":\"" + msg + "\"" +
                ",\"persons\": " + page.getRecords() +
                "}";
    }
}