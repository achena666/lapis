package com.luoyx.lapis.admin.sys.converter;

import com.luoyx.lapis.admin.sys.entity.UserProfile;
import com.luoyx.lapis.admin.sys.request.UserCreateDTO;
import com.luoyx.lapis.admin.sys.request.UserUpdateDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileConverter {


    UserProfile toUserProfile(UserCreateDTO.UserProfileDTO userProfileDTO);

    UserProfile toUserProfile(UserUpdateDTO.UserProfileDTO userUpdateDTO);


}
