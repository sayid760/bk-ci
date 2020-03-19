/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.dao.PipelineStageTagDao
import com.tencent.devops.process.pojo.PipelineStageTag
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StageTagService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineStageTagDao: PipelineStageTagDao
) {
    private val logger = LoggerFactory.getLogger(StageTagService::class.java)

    /**
     * 获取所有阶段标签信息
     */
    fun getAllStageTag(): Result<List<PipelineStageTag>> {
        val pipelineStageTagList =
            pipelineStageTagDao.getAllStageTag(dslContext).map { pipelineStageTagDao.convert(it) }
        return Result(pipelineStageTagList)
    }

    /**
     * 获取所有阶段标签信息
     */
    fun getDefaultStageTag(): Result<List<PipelineStageTag>> {
        val pipelineStageTagList =
            pipelineStageTagDao.getDefaultStageTag(dslContext).map { pipelineStageTagDao.convert(it) }
        return Result(pipelineStageTagList)
    }

    /**
     * 根据id获取阶段标签信息
     */
    fun getStageTag(id: String): Result<PipelineStageTag?> {
        val pipelineStageTagRecord = pipelineStageTagDao.getStageTag(dslContext, id)
        logger.info("the pipelineStageTagRecord is :$pipelineStageTagRecord")
        return Result(
            if (pipelineStageTagRecord == null) {
                null
            } else {
                pipelineStageTagDao.convert(pipelineStageTagRecord)
            }
        )
    }

    /**
     * 保存阶段标签信息
     */
    fun saveStageTag(stageTag: String, defaultFlag: Boolean): Result<Boolean> {
        logger.info("the save stageTagName is:$stageTag")
        // 判断阶段标签名称是否存在
        val count = getCountByName(stageTag)
        if (count > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(stageTag),
                false
            )
        }
        val id = UUIDUtil.generate()
        if (getDefaultCount() > 0 || defaultFlag)
            pipelineStageTagDao.add(dslContext, id, stageTag, defaultFlag)
        else return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.ERROR_INVALID_CONFIG,
            arrayOf("defaultFlag", true.toString()),
            false
        )
        return Result(true)
    }

    /**
     * 更新阶段标签信息
     */
    fun updateStageTag(id: String, stageTagName: String, defaultFlag: Boolean): Result<Boolean> {
        logger.info("the update stageTagName is:$stageTagName")
        // 判断阶段标签代码是否存在
        if (getCountByName(stageTagName) > 0) {
            // 判断更新的阶段标签代码是否属于自已
            val pipelineStageTag = pipelineStageTagDao.getStageTag(dslContext, id)
            if (null != pipelineStageTag && stageTagName != pipelineStageTag.stageTagName) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(stageTagName),
                    false
                )
            }
        }
        if (getDefaultCount() > 0 || defaultFlag)
            pipelineStageTagDao.update(dslContext, id, stageTagName, defaultFlag)
        else return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.ERROR_INVALID_CONFIG,
            arrayOf("defaultFlag", true.toString()),
            false
        )
        return Result(true)
    }

    /**
     * 删除阶段标签信息
     */
    fun deleteStageTag(id: String): Result<Boolean> {
        logger.info("the delete id is :{}", id)
        pipelineStageTagDao.delete(dslContext, id)
        return Result(true)
    }

    /**
     * 根据阶段标签名称查询数据库记录数
     */
    private fun getCountByName(stageTagName: String): Int {
        val recordList = pipelineStageTagDao.countByName(dslContext, stageTagName)
        var result = 0
        if (recordList != null) {
            result = recordList.get(0) as Int
        }
        return result
    }

    /**
     *  判断是否存在默认阶段标签
     */
    private fun getDefaultCount(): Int {
        val recordList = pipelineStageTagDao.countDefaultTag(dslContext)
        var result = 0
        if (recordList != null) {
            result = recordList.get(0) as Int
        }
        return result
    }
}