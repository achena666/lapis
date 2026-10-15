package com.luoyx.lapis.admin.sys.converter;

import com.luoyx.lapis.admin.api.sys.dto.AuthorityDTO;
import com.luoyx.lapis.admin.sys.entity.Authority;
import com.luoyx.lapis.admin.sys.request.AuthorityCreateDTO;
import com.luoyx.lapis.admin.sys.request.AuthorityUpdateDTO;
import com.luoyx.lapis.admin.sys.response.AuthorityCheckBoxTreeVO;
import com.luoyx.lapis.admin.sys.response.AuthorityTreeNodeVO;
import com.luoyx.lapis.admin.sys.response.AuthorityVO;
import com.luoyx.lapis.common.converter.BooleanYesNoConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * 权限类 实体转换接口
 * <p>
 * componentModel="spring" 表示使用spring依赖注入
 *
 * @author 罗英雄
 */
@Mapper(componentModel = "spring", imports = BooleanYesNoConverter.class)
public interface AuthorityConverter {

    Authority toEntity(AuthorityUpdateDTO authorityUpdateDTO);

    /**
     * 新增权限VO对象转换为实体类对象
     * <p>
     * leaf设为true, 默认为叶子节点
     *
     * @param authorityCreateDTO
     * @return
     */
    @Mapping(target = "leaf", constant = "true")
    Authority toEntity(AuthorityCreateDTO authorityCreateDTO);



    @Mappings(value = {
            @Mapping(target = "key", source = "id"),
            @Mapping(target = "parentKey", source = "parentId"),
            @Mapping(target = "title", source = "authorityName"),
    })

    AuthorityCheckBoxTreeVO toAuthorityCheckBoxTreeVO(Authority authority);



    AuthorityDTO toAuthorityDTO(Authority authority);

    AuthorityTreeNodeVO toAuthorityTreeNode(AuthorityVO authority);
}
