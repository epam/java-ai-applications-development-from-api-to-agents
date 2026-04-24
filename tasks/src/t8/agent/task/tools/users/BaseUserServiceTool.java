package t8.agent.task.tools.users;

import commons.user.service.UserServiceClient;
import t8.agent.task.tools.BaseTool;

public abstract class BaseUserServiceTool extends BaseTool {

    protected final UserServiceClient userClient;

    public BaseUserServiceTool(UserServiceClient userClient) {
        this.userClient = userClient;
    }
}
