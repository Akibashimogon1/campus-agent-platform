package com.campus.agent.mcp.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.agent.mcp.entity.McpTool;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class McpMetaObjectHandlerTest {

    private final McpMetaObjectHandler handler = new McpMetaObjectHandler();

    @BeforeAll
    static void initializeTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, McpTool.class);
    }

    @Test
    void insertFillSetsTimestampsAndSystemUsers() {
        McpTool tool = new McpTool();

        handler.insertFill(SystemMetaObject.forObject(tool));

        assertThat(tool.getCreatedAt()).isNotNull().isEqualTo(tool.getUpdatedAt());
        assertThat(tool.getCreatedBy()).isEqualTo("system");
        assertThat(tool.getUpdatedBy()).isEqualTo("system");
    }

    @Test
    void insertFillPreservesValuesAlreadyProvided() {
        McpTool tool = new McpTool();
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 2, 3, 4);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 2, 3, 4, 5);
        tool.setCreatedAt(createdAt);
        tool.setUpdatedAt(updatedAt);
        tool.setCreatedBy("creator");
        tool.setUpdatedBy("editor");

        handler.insertFill(SystemMetaObject.forObject(tool));

        assertThat(tool.getCreatedAt()).isEqualTo(createdAt);
        assertThat(tool.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(tool.getCreatedBy()).isEqualTo("creator");
        assertThat(tool.getUpdatedBy()).isEqualTo("editor");
    }

    @Test
    void updateFillSetsUpdateFieldsWithoutChangingCreationFields() {
        McpTool tool = new McpTool();
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 2, 3, 4);
        tool.setCreatedAt(createdAt);
        tool.setCreatedBy("creator");

        handler.updateFill(SystemMetaObject.forObject(tool));

        assertThat(tool.getCreatedAt()).isEqualTo(createdAt);
        assertThat(tool.getCreatedBy()).isEqualTo("creator");
        assertThat(tool.getUpdatedAt()).isNotNull();
        assertThat(tool.getUpdatedBy()).isEqualTo("system");
    }
}