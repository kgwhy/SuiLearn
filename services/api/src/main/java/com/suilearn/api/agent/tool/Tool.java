package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.runtime.TurnContext;
import java.util.Map;

/**
 * Tool boundary declared by the refactor plan. Change-1 stabilizes the contract only;
 * tool beans and registry are delivered by change-2.
 */
public interface Tool {
    ToolDefinition definition();

    ToolResult execute(TurnContext context, Map<String, Object> args);
}
