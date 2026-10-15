package com.luoyx.lapis.toolbox.codegen.repository;

import com.luoyx.lapis.jpa.repository.BaseRepository;
import com.luoyx.lapis.toolbox.codegen.entity.DataSource;
import com.luoyx.lapis.toolbox.codegen.query.DataSourceQuery;
import com.luoyx.lapis.toolbox.codegen.vo.response.DataSourcePageResultVO;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public interface DataSourceRepository extends BaseRepository<DataSource, Long> {

    Page<DataSourcePageResultVO> findPage(DataSourceQuery query);
}
