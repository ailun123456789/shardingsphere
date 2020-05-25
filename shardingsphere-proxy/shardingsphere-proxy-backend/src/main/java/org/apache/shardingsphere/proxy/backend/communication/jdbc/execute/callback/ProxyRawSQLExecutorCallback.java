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

package org.apache.shardingsphere.proxy.backend.communication.jdbc.execute.callback;

import org.apache.shardingsphere.infra.executor.sql.RawSQLExecuteUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.raw.callback.RawSQLExecutorCallback;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.execute.response.ExecuteResponse;

import java.util.Collection;
import java.util.Map;

/**
 * Raw SQL executor callback for Proxy.
 */
public final class ProxyRawSQLExecutorCallback implements RawSQLExecutorCallback<ExecuteResponse> {
    
    @Override
    public Collection<ExecuteResponse> execute(final Collection<RawSQLExecuteUnit> inputs, final boolean isTrunkThread, final Map<String, Object> dataMap) {
        // TODO
        return null;
    }
}