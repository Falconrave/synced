//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.data.SwiftlyConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link SwiftlyService}.
 */
public interface SwiftlyServiceAsync
{
    /**
     * The asynchronous version of {@link SwiftlyService#getConnectConfig}.
     */
    public void getConnectConfig (WebIdent ident, int projectId,
                                  AsyncCallback<SwiftlyConnectConfig> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getRemixableProjects}.
     */
    public void getRemixableProjects (WebIdent ident, AsyncCallback<List<SwiftlyProject>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getMembersProjects}.
     */
    public void getMembersProjects (WebIdent ident, AsyncCallback<List<SwiftlyProject>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#createProject}.
     */
    public void createProject (WebIdent ident, String projectName, byte projectType,
                               boolean remixable, AsyncCallback<SwiftlyProject> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#updateProject}.
     */
    public void updateProject (WebIdent ident, SwiftlyProject project,
                               AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadProject}.
     */
    public void loadProject (WebIdent ident, int projectId, AsyncCallback<SwiftlyProject> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectOwner}.
     */
    public void getProjectOwner (WebIdent ident, int projectId, AsyncCallback<MemberName> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#deleteProject}.
     */
    public void deleteProject (WebIdent ident, int projectId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectCollaborators}.
     */
    public void getProjectCollaborators (WebIdent ident, int projectId,
                                         AsyncCallback<List<MemberName>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getFriends}.
     */
    public void getFriends (WebIdent ident, AsyncCallback<List<FriendEntry>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#leaveCollaborators}.
     */
    public void leaveCollaborators (WebIdent ident, int projectId, MemberName name,
                                    AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#joinCollaborators}.
     */
    public void joinCollaborators (WebIdent ident, int projectId, MemberName name,
                                   AsyncCallback<Void> callback);
}
