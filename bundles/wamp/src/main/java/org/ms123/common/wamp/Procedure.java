/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ms123.common.wamp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 */
public class Procedure {

		final String procName;
		final WampServiceImpl.WebSocket provider;
		final WampRouterSession.SessionContext context;
		final long registrationId;
		final List<Invocation> pendingCalls = new ArrayList<Invocation>();

		public Procedure(String name, WampRouterSession.SessionContext context, long registrationId) {
			this.procName = name;
			this.context = context;
			this.provider = context.webSocket;
			this.registrationId = registrationId;
		}

}