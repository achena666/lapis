package com.luoyx.lapis.admin.sys.service;

import com.luoyx.lapis.admin.api.sys.dto.UserFullNameDTO;
import com.luoyx.lapis.admin.sys.entity.UserProfile;
import com.luoyx.lapis.mybatisplus.service.BaseService;
//import com.luoyx.lapis.usersnapshot.msg.UserSnapshotMessage;

import java.util.List;
import java.util.Set;

/**
 * @author admini
 */
public interface UserProfileService extends BaseService<UserProfile> {

    /**
     * 按用户id集合查询用户全名
     *
     * @param userIds 用户id 集合
     * @return
     */
    List<UserFullNameDTO> findFullNamesByUserIds(Set<Long> userIds);

    /**
     * 检查手机号是否唯一
     *
     * @param excludeUserId 要排除的用户Id（编辑用户的场景）
     * @param phone         手机号
     * @return true=可用，false=已被占用
     */
    boolean isPhoneAvailable(Long excludeUserId, String phone);

    /**
     * 检查邮箱是否唯一
     *
     * @param excludeUserId 要排除的用户Id（编辑用户的场景）
     * @param email         邮箱
     * @return true=可用，false=已被占用
     */
    boolean isEmailAvailable(Long excludeUserId, String email);

}
