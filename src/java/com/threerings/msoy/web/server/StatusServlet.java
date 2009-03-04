//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.Tuple;

import com.threerings.crowd.chat.data.ChatChannel;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.util.FutureResult;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.MsoyEventLogger;

import static com.threerings.msoy.Log.log;

/**
 * Reports server status in plain text.
 */
public class StatusServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        final Details details = parseDetails(req.getPathInfo());
        Callable<Map<String, ServerInfo>> collector = new Callable<Map<String, ServerInfo>>() {
            public Map<String, ServerInfo> call () throws Exception {
                return collectInfo(details);
            }
        };
        FutureTask<Map<String, ServerInfo>> task =
            new FutureTask<Map<String, ServerInfo>>(collector);
        _omgr.postRunnable(task);

        try {
            Map<String, ServerInfo> info = task.get();
            PrintStream out = null;
            try {
                out = new PrintStream(rsp.getOutputStream());
                for (ServerInfo sinfo : info.values()) {
                    out.println(sinfo);
                    if (sinfo.details != null) {
                        try {
                            out.println(sinfo.details.call());
                        } catch (Exception e) {
                            out.println("Failed to get details: " + e.getMessage());
                            e.printStackTrace(out);
                        }
                    }
                }
            } finally {
                StreamUtil.close(out);
            }

        } catch (Throwable t) {
            if (t instanceof ExecutionException) {
                t = t.getCause();
            }
            log.warning("Failed to gather stats.", t);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    protected static Details parseDetails (String pathinfo)
    {
        try {
            if (pathinfo != null && pathinfo.startsWith("/")) {
                return Enum.valueOf(Details.class, pathinfo.substring(1).toUpperCase());
            }
        } catch (Exception e) {
            log.info("Ignoring invalid status details", "pinfo", pathinfo);
        }
        return Details.NONE;
    }

    protected Map<String, ServerInfo> collectInfo (final Details details)
    {
        final Map<String,ServerInfo> info = Maps.newHashMap();

        // collect info on this server
        MsoyNodeObject nodeobj = (MsoyNodeObject)_peerMan.getNodeObject();
        info.put(nodeobj.nodeName, collectInfo(details, null, nodeobj));

        // collect info on our peers
        _peerMan.invokeOnNodes(new Function<Tuple<Client,NodeObject>,Boolean>() {
            public Boolean apply (Tuple<Client, NodeObject> args) {
                info.put(args.right.nodeName,
                         collectInfo(details, args.left, (MsoyNodeObject)args.right));
                return true;
            }
        });

        return info;
    }

    protected ServerInfo collectInfo (Details details, Client client, MsoyNodeObject nodeobj)
    {
        final ServerInfo info = new ServerInfo();
        info.name = nodeobj.nodeName;

        info.rooms = nodeobj.hostedScenes.size();
        if (details == Details.ROOMS) {
            for (HostedRoom room : nodeobj.hostedScenes) {
                info.modeinfo.append("- ").append(room).append("\n");
            }
        }

        info.games = nodeobj.hostedGames.size();
        if (details == Details.GAMES) {
            for (HostedGame game : nodeobj.hostedGames) {
                info.modeinfo.append("- ").append(game).append("\n");
            }
        }

        info.channels = nodeobj.hostedChannels.size();
        if (details == Details.CHANNELS) {
            for (ChatChannel channel : nodeobj.hostedChannels) {
                info.modeinfo.append("- ").append(channel);
            }
        }

        for (ClientInfo cinfo : nodeobj.clients) {
            info.clients++;
        }

        for (MemberLocation mloc : nodeobj.memberLocs) {
            if (mloc.sceneId != 0) {
                info.inScene++;
            }
            if (mloc.gameId != 0) {
                info.inGame++;
            }
            if (details == Details.MEMBERS) {
                info.modeinfo.append("- ").append(mloc).append("\n");
            }
        }

        switch (details) {
        case REPORT:
            collectReportInfo(info, client, nodeobj, ReportManager.DEFAULT_TYPE);
            break;

        case PANOPTICON:
            collectReportInfo(info, client, nodeobj, MsoyEventLogger.PANOPTICON_REPORT_TYPE);
            break;

        case NONE:
            // leave details as null in this case
            break;

        default:
            info.details = new Callable<String>() {
                public String call () throws Exception {
                    return info.modeinfo.toString();
                }
            };
            break;
        }

        return info;
    }

    protected void collectReportInfo (ServerInfo info, Client client, MsoyNodeObject nodeobj,
                                      final String type)
    {
        if (client == null) { // nextgen narya will make this less hacky
            final FutureResult<String> report = new FutureResult<String>();
            try {
                _peerMan.generateReport(null, type, report);
                info.details = new Callable<String>() {
                    public String call () throws Exception {
                        return report.get();
                    }
                };
            } catch (Exception e) {
                // will not happen; if it does, details will just be null
                log.warning("Mission impossible!", e);
            }

        } else {
            final FutureResult<String> report = new FutureResult<String>();
            nodeobj.peerService.generateReport(client, type, report);
            info.details = new Callable<String>() {
                public String call () throws Exception {
                    return report.get();
                }
            };
        }
    }

    protected static enum Details {
        NONE, MEMBERS, ROOMS, GAMES, CHANNELS, REPORT, PANOPTICON
    };

    protected static class ServerInfo
    {
        public String name;

        public int clients;
        public int rooms;
        public int games;
        public int channels;

        public int inScene;
        public int inGame;

        public Callable<String> details;

        public StringBuilder modeinfo = new StringBuilder();

        public String toString () {
            return name + " [clients=" + clients + ", inScene=" + inScene + ", inGame=" + inGame +
                ", rooms=" + rooms + ", games=" + games + ", channels=" + channels + "] ";
        }
    }

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyPeerManager _peerMan;
}
