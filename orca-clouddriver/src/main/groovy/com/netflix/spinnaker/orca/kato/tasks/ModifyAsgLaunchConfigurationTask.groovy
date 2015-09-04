/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.kato.tasks

import com.netflix.spinnaker.orca.DefaultTaskResult
import com.netflix.spinnaker.orca.ExecutionStatus
import com.netflix.spinnaker.orca.Task
import com.netflix.spinnaker.orca.TaskResult
import com.netflix.spinnaker.orca.clouddriver.KatoService
import com.netflix.spinnaker.orca.pipeline.model.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ModifyAsgLaunchConfigurationTask implements Task {
  @Autowired
  KatoService kato

  @Override
  TaskResult execute(Stage stage) {
    def deploymentDetails = (stage.context.deploymentDetails ?: []) as List<Map>
    def operationConfig = new HashMap(stage.context)
    if (!stage.context.amiName && deploymentDetails) {
      operationConfig.amiName = deploymentDetails.find { it.region == stage.context.region }?.ami
    }
    def operation = [modifyAsgLaunchConfigurationDescription: operationConfig]
    def ops = [operation]
    def taskId = kato.requestOperations(ops)
      .toBlocking()
      .first()
    new DefaultTaskResult(ExecutionStatus.SUCCEEDED, [
      "notification.type"     : "modifyasglaunchconfiguration",
      "modifyasglaunchconfiguration.account.name": stage.context.credentials,
      "modifyasglaunchconfiguration.region"      : stage.context.region,
      "kato.last.task.id"     : taskId,
      "kato.task.id"          : taskId, // TODO retire this.
      "deploy.server.groups"  : [(stage.context.region): [stage.context.asgName]]
    ])
  }
}
