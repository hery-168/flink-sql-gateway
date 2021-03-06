/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.flink.table.gateway.operation;

import com.ververica.flink.table.gateway.config.Environment;
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.EnvironmentFileUtil;

import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link SetOperation}.
 */
public class SetOperationTest extends OperationTestBase {

	private static final String DEFAULTS_ENVIRONMENT_FILE = "test-sql-gateway-defaults.yaml";

	@Override
	protected Environment getSessionEnvironment() throws Exception {
		final Map<String, String> replaceVars = new HashMap<>();
		replaceVars.put("$VAR_PLANNER", "old");
		replaceVars.put("$VAR_EXECUTION_TYPE", "batch");
		replaceVars.put("$VAR_UPDATE_MODE", "");
		return EnvironmentFileUtil.parseModified(DEFAULTS_ENVIRONMENT_FILE, replaceVars);
	}

	@Test
	public void testSetProperties() {
		SetOperation setOperation = new SetOperation(context, "table.optimizer.join-reorder-enabled", "true");
		assertEquals(OperationUtil.OK, setOperation.execute());

		SetOperation showSetOperation = new SetOperation(context);
		ResultSet resultSet = showSetOperation.execute();
		ResultSet expected = ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(
				ColumnInfo.create(ConstantNames.KEY, new VarCharType(true, 36)),
				ColumnInfo.create(ConstantNames.VALUE, new VarCharType(true, 5)))
			.data(
				Row.of("execution.max-parallelism", "16"),
				Row.of("execution.planner", "old"),
				Row.of("execution.parallelism", "1"),
				Row.of("execution.type", "batch"),
				Row.of("deployment.response-timeout", "5000"),
				Row.of("table.optimizer.join-reorder-enabled", "true"))
			.build();
		assertEquals(expected, resultSet);
	}

	@Test
	public void testSetPropertiesWithWhitespace() {
		SetOperation setOperation = new SetOperation(context, "execution.parallelism", " 10");
		assertEquals(OperationUtil.OK, setOperation.execute());

		SetOperation showSetOperation = new SetOperation(context);
		ResultSet resultSet = showSetOperation.execute();
		ResultSet expected = ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(
				ColumnInfo.create(ConstantNames.KEY, new VarCharType(true, 36)),
				ColumnInfo.create(ConstantNames.VALUE, new VarCharType(true, 5)))
			.data(
				Row.of("execution.max-parallelism", "16"),
				Row.of("execution.planner", "old"),
				Row.of("execution.parallelism", "10"),
				Row.of("execution.type", "batch"),
				Row.of("deployment.response-timeout", "5000"),
				Row.of("table.optimizer.join-reorder-enabled", "false"))
			.build();
		assertEquals(expected, resultSet);
	}

	@Test
	public void testShowProperties() {
		SetOperation operation = new SetOperation(context);
		ResultSet resultSet = operation.execute();
		ResultSet expected = ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(
				ColumnInfo.create(ConstantNames.KEY, new VarCharType(true, 36)),
				ColumnInfo.create(ConstantNames.VALUE, new VarCharType(true, 5)))
			.data(
				Row.of("execution.max-parallelism", "16"),
				Row.of("execution.planner", "old"),
				Row.of("execution.parallelism", "1"),
				Row.of("execution.type", "batch"),
				Row.of("deployment.response-timeout", "5000"),
				Row.of("table.optimizer.join-reorder-enabled", "false"))
			.build();
		assertEquals(expected, resultSet);
	}

	@Test
	public void testSetPropertiesForTableConfig() {
		// check init state
		assertNull(context.getExecutionContext().getEnvironment().getConfiguration().asMap()
			.getOrDefault("table.optimizer.agg-phase-strategy", null));
		assertNull(context.getExecutionContext().getTableEnvironment()
			.getConfig().getConfiguration().getString("table.optimizer.agg-phase-strategy", null));

		// set table config key-value
		SetOperation setOperation = new SetOperation(context, "table.optimizer.agg-phase-strategy", "ONE_PHASE");
		assertEquals(OperationUtil.OK, setOperation.execute());
		assertEquals("ONE_PHASE", context.getExecutionContext().getEnvironment().getConfiguration().asMap()
			.getOrDefault("table.optimizer.agg-phase-strategy", null));
		assertEquals("ONE_PHASE", context.getExecutionContext().getTableEnvironment()
			.getConfig().getConfiguration().getString("table.optimizer.agg-phase-strategy", null));

		// reset all properties
		ResetOperation resetOperation = new ResetOperation(context);
		resetOperation.execute();

		assertNull(context.getExecutionContext().getEnvironment().getConfiguration().asMap()
			.getOrDefault("table.optimizer.agg-phase-strategy", null));
		assertNull(context.getExecutionContext().getTableEnvironment()
			.getConfig().getConfiguration().getString("table.optimizer.agg-phase-strategy", null));
	}

}
