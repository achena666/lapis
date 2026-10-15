package com.luoyx.lapis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 文件服务冒烟测试占位。
 *
 * <p>说明：本文件原先由 Maven archetype 生成，使用早已废弃的 JUnit 3（{@code junit.framework.TestCase}），
 * 而本模块并未声明任何测试依赖，导致 {@code testCompile} 阶段直接构建失败、整个工程无法 install。
 * 现统一迁移到 JUnit 5，与其它服务（如 toolbox-server）的测试写法保持一致。</p>
 */
class AppTest {

    @Test
    void smokeTest() {
        assertNotNull(AppTest.class.getPackageName());
    }
}
