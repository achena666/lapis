package com.luoyx.lapis.admin.sys.converter;

import com.luoyx.lapis.admin.sys.entity.User;
import com.luoyx.lapis.admin.sys.entity.UserProfile;
import com.luoyx.lapis.admin.sys.request.UserCreateDTO;
import com.luoyx.lapis.admin.sys.request.UserUpdateDTO;
import com.luoyx.lapis.admin.sys.response.CreatedUserVO;
import com.luoyx.lapis.admin.sys.response.UserEditFormVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserConverter {
    @Mappings(value = {
            @Mapping(target = "id", source = "user.id"),
            @Mapping(target = "username", source = "user.username"),
            @Mapping(target = "enabled", source = "user.enabled"),

            @Mapping(target = "profile.realName", source = "userProfile.realName"),
            @Mapping(target = "profile.nickname", source = "userProfile.nickname"),
            @Mapping(target = "profile.avatar", source = "userProfile.avatar"),
            @Mapping(target = "profile.gender", source = "userProfile.gender"),
            @Mapping(target = "profile.birthday", source = "userProfile.birthday"),
            @Mapping(target = "profile.phone", source = "userProfile.phone"),
            @Mapping(target = "profile.email", source = "userProfile.email"),
            @Mapping(target = "profile.position", source = "userProfile.position"),
            @Mapping(target = "profile.remark", source = "userProfile.remark")
    })
    CreatedUserVO toCreatedUserVO(User user, UserProfile userProfile);


    User toUser(UserCreateDTO addDTO);

    @Mappings(value = {
            @Mapping(target = "id", source = "user.id"),
            @Mapping(target = "username", source = "user.username"),
            @Mapping(target = "enabled", source = "user.enabled"),

            @Mapping(target = "profile.realName", source = "userProfile.realName"),
            @Mapping(target = "profile.nickname", source = "userProfile.nickname"),
            @Mapping(target = "profile.avatar", source = "userProfile.avatar"),
            @Mapping(target = "profile.gender", source = "userProfile.gender"),
            @Mapping(target = "profile.birthday", source = "userProfile.birthday"),
            @Mapping(target = "profile.phone", source = "userProfile.phone"),
            @Mapping(target = "profile.email", source = "userProfile.email"),
            @Mapping(target = "profile.position", source = "userProfile.position"),
            @Mapping(target = "profile.remark", source = "userProfile.remark")
    })
    UserEditFormVO toUserDetailsVO(User user, UserProfile userProfile);

    User toUser(UserUpdateDTO userUpdateDTO);
}
