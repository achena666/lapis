package com.luoyx.lapis.toolbox.codegen.service;

import com.luoyx.lapis.jpa.dto.PageResult;
import com.luoyx.lapis.jpa.service.BaseService;
import com.luoyx.lapis.toolbox.codegen.entity.DataSource;
import com.luoyx.lapis.toolbox.codegen.query.DataSourceQuery;
import com.luoyx.lapis.toolbox.codegen.vo.response.DataSourcePageResultVO;

/**
 * <p>
 * 数据源配置表 服务类
 * </p>
 *
 * @author 1564469545@qq.com
 * @since 2023-03-31
 */
public interface DataSourceService extends BaseService<DataSource, Long> {


    PageResult<DataSourcePageResultVO> pageList(DataSourceQuery query);
}
