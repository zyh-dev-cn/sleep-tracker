package com.zyh.sleep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyh.sleep.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
