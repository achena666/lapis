package com.luoyx.lapis.admin.sys.converter;

import com.luoyx.lapis.admin.sys.entity.DictItem;
import com.luoyx.lapis.admin.sys.request.DictItemCreateDTO;
import com.luoyx.lapis.admin.sys.request.DictItemEditDTO;
import com.luoyx.lapis.admin.sys.response.DictItemDetailVO;
import com.luoyx.lapis.admin.sys.response.DictItemDropdownDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 数据字典 类转换器
 * <p>
 * componentModel="spring" 表示使用spring依赖注入
 *
 * @author 罗英雄
 */
@Mapper(componentModel = "spring")
public interface DictItemConverter {

    /**
     * VO对象转换为 entity对象
     *
     * @param dictItemCreateDTO 数据字典表单数据VO对象
     * @return 数据字典实体对象
     */
    DictItem toEntity(DictItemCreateDTO dictItemCreateDTO);

    DictItem toEntity(DictItemEditDTO editFormVO);

    /**
     * 实体 转详情VO类
     *
     * @param dictItem
     * @return
     */
    DictItemDetailVO toDictItemDetailVO(DictItem dictItem);

    @Mapping(target = "label", source = "dictItemCode")
    @Mapping(target = "value", source = "dictItemName")
    DictItemDropdownDTO toDictItemOptionVO(DictItem dictItem);

}
