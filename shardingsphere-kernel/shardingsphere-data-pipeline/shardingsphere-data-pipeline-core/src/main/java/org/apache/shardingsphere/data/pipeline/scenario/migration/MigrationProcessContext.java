/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.scenario.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.api.detect.RuleAlteredJobAlmostCompletedParameter;
import org.apache.shardingsphere.data.pipeline.core.check.consistency.DataConsistencyCalculateAlgorithmFactory;
import org.apache.shardingsphere.data.pipeline.core.context.AbstractPipelineProcessContext;
import org.apache.shardingsphere.data.pipeline.spi.check.consistency.DataConsistencyCalculateAlgorithm;
import org.apache.shardingsphere.data.pipeline.spi.detect.JobCompletionDetectAlgorithm;
import org.apache.shardingsphere.infra.config.rule.data.pipeline.PipelineProcessConfiguration;

/**
 * Migration process context.
 */
@Getter
@Slf4j
public final class MigrationProcessContext extends AbstractPipelineProcessContext {
    
    private final JobCompletionDetectAlgorithm<RuleAlteredJobAlmostCompletedParameter> completionDetectAlgorithm;
    
    private final DataConsistencyCalculateAlgorithm dataConsistencyCalculateAlgorithm;
    
    public MigrationProcessContext(final String jobId, final PipelineProcessConfiguration originalProcessConfig) {
        super(jobId, originalProcessConfig);
        completionDetectAlgorithm = null;
        dataConsistencyCalculateAlgorithm = DataConsistencyCalculateAlgorithmFactory.newInstance("DATA_MATCH", null);
    }
}
