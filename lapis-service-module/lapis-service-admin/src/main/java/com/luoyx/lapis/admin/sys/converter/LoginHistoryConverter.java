package com.luoyx.lapis.admin.sys.converter;

import com.luoyx.lapis.admin.api.sys.dto.SaveLoginHistoryDTO;
import com.luoyx.lapis.admin.sys.entity.LoginHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginHistoryConverter {

    LoginHistory toEntity(SaveLoginHistoryDTO saveLoginHistoryDTO);
}
