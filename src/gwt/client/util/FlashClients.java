//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Utility methods for generating flash clients.
 */
public class FlashClients
{
    public static final String HOOD_SKIN_URL = "/media/static/hood_pastoral.swf";

    public static HTML createWorldClient (String flashVars)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/" + DeploymentConfig.version + "/world-client.swf",
            "100%", getClientHeight(false), flashVars);
    }

    public static HTML createLobbyClient (int gameId, String token)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/" + DeploymentConfig.version + "/world-client.swf",
            "100%", getClientHeight(false), "gameLobby=" + gameId + "&token=" + token);
    }

    public static HTML createNeighborhood (String hoodData)
    {
        return createNeighborhood(hoodData, "100%", getClientHeight(true));
    }

    public static HTML createNeighborhood (String hoodData, String width, String height)
    {
        return WidgetUtil.createFlashContainer(
            "hood", "/clients/" + DeploymentConfig.version + "/neighborhood.swf", width, height,
            "skinURL= " + HOOD_SKIN_URL + "&neighborhood=" + hoodData);
    }

    public static HTML createPopularPlaces (String hotspotData)
    {
        return WidgetUtil.createFlashContainer(
            "hotspots", "/clients/" + DeploymentConfig.version + "/neighborhood.swf",
            "100%", getClientHeight(true),
            "skinURL= " + HOOD_SKIN_URL + "&neighborhood=" + hotspotData);
    }

    public static HTML createAvatarViewer (String avatarPath, float scale, boolean allowScaleChange)
    {
        String flashVars = "avatar=" + URL.encodeComponent(avatarPath) + "&scale=" + scale;
        if (allowScaleChange) {
            flashVars += "&scaling=true";
        }
        return WidgetUtil.createFlashContainer(
            "avatarViewer", "/clients/" + DeploymentConfig.version + "/avatarviewer.swf",
            360, 450, flashVars);
    }

    public static HTML createVideoViewer (String videoPath)
    {
        return WidgetUtil.createFlashContainer(
            "videoViewer", "/clients/" + DeploymentConfig.version + "/videoviewer.swf",
            320, 240, "video=" + URL.encodeComponent(videoPath));
    }

    public static HTML createDecorViewer ()
    {
        return WidgetUtil.createFlashContainer(
            "decorViewer", "/clients/" + DeploymentConfig.version + "/decorviewer.swf",
            600, 400, "");
    }

    /**
     * Checks if the flash client can be found on this page.
     */
    public static boolean clientExists ()
    {
        return clientExistsNative();
    }

    /** 
     * Checks with the actionscript client to find out if our current scene is in fact a room.
     */
    public static boolean inRoom ()
    {
        return inRoomNative();
    }

    /**
     * Tells the actionscript client that we'd like to use this item in the current room.  This can
     * be used to add furni, or set the background audio or decor.
     */
    public static void useItem (int itemId, byte itemType) 
    {
        useItemNative(itemId, itemType);
    }

    /**
     * Tells the actionscript client to remove the given item from the current room. 
     */
    public static void removeFurni (int itemId, byte itemType)
    {
        removeFurniNative(itemId, itemType);
    }

    /**
     * Gets the item list of items being used as furni in the current room.
     */
    public static List getFurniList () 
    {
        JavaScriptObject items = getFurniListNative();
        List furnis = new ArrayList();
        for (int ii = 0; ii < getLength(items); ii++) {
            JavaScriptObject furni = getJavaScriptElement(items, ii);
            furnis.add(new ItemIdent(getByteElement(furni, 0), getIntElement(furni, 1)));
        }
        return furnis;
    }

    public static List getPetList ()
    {
        JavaScriptObject petIds = getPetsNative();
        List pets = new ArrayList();
        for (int ii = 0; ii < getLength(petIds); ii++) {
            pets.add(new ItemIdent(Item.PET, getIntElement(petIds, ii)));
        }
        return pets;
    }

    /**
     * Fetches the id for the given itemType, where itemType can be type that the scene can have
     * only one of, such as decor or audio.
     */
    public static int getSceneItemId (byte itemType)
    {
        return getSceneItemIdNative(itemType);
    }

    /**
     * Tells the actionscript client that we'd like to use this avatar.  If 0 is passed in for the
     * avatarId, the current avatar is simply cleared away, leaving you tofulicious.
     */
    public static void useAvatar (int avatarId, float scale)
    {
        useAvatarNative(avatarId, scale);
    }

    /**
     * Fetches the currently active avatar id that is being used by the client.  If 0 is returned,
     * the flash client is either not open (check clientExits() before calling), or we're using 
     * the default avatar.
     */
    public static int getAvatarId ()
    {
        return getAvatarIdNative();
    }

    public static void usePet (int petId)
    {
        usePetNative(petId);
    }

    public static void removePet (int petId)
    {
        removePetNative(petId);
    }

    /**
     * Computes the height to use for our Flash clients based on the smaller of our desired client
     * height and the vertical room available minus the header and an annoying "we don't know how
     * to implement scrollbars" bullshit browser factor.
     */
    protected static String getClientHeight (boolean subtractBlackBarHeight)
    {
        int height = Math.min(Window.getClientHeight()-HEADER_HEIGHT-1, CLIENT_HEIGHT);
        if (subtractBlackBarHeight) {
            height -= BLACKBAR_HEIGHT;
        }
        return String.valueOf(height);
    }

    /**
     * Helpy helper function.
     */
    public static native int getLength (JavaScriptObject array) /*-{
        return array.length;
    }-*/;

    /**
     * Helpy helper function.  Makes it possible to retrieve objects from arrays that are members
     * of arrays.
     */
    public static native JavaScriptObject getJavaScriptElement (JavaScriptObject array, 
        int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native String getStringElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native int getIntElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native byte getByteElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native boolean getBooleanElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native boolean getBoolean (JavaScriptObject value) /*-{
        return value;
    }-*/;

    /** 
     * Does the actual <code>clientExists()</code> call.
     */
    protected static native boolean clientExistsNative () /*-{
        return $doc.getElementById("asclient") != null;
    }-*/;

    /**
     * Does the actual <code>inRoom()</code> call.
     */
    protected static native boolean inRoomNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                return client.inRoom();
            } catch (e) {
                // fall through
            }
        } 
        return false;
    }-*/;

    /**
     * Does the actual <code>useItem()</code> call.
     */
    protected static native void useItemNative (int itemId, byte itemType) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.useItem(itemId, itemType);
            } catch (e) {
                // fall through
            }
        }
    }-*/;

    /**
     * Does the actual <code>removeFurni()</code> call.
     */
    protected static native void removeFurniNative (int itemId, byte itemType) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.removeFurni(itemId, itemType);
            } catch (e) {
                // fall through
            }
        }
    }-*/;

    /**
     * Does the actual <code>getFurniList()</code> call.
     */
    protected static native JavaScriptObject getFurniListNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                return client.getFurniList();
            } catch (e) {
                // fall through
            }
        }
        return [];
    }-*/;

    /**
     * Does the actual <code>getSceneItemId()</code> call.
     */
    protected static native int getSceneItemIdNative(byte itemType) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                return client.getSceneItemId(itemType);
            } catch (e) {
                // fall through
            }
        }
        return 0;
    }-*/;

    /**
     * Does the actual <code>useAvatar()</code> call.
     */
    protected static native void useAvatarNative (int avatarId, float scale) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.useAvatar(avatarId, scale);
            } catch (e) {
                // fall through
            }
        }
    }-*/;

    /**
     * Does the actual <code>getAvatarId()</code> call.
     */
    protected static native int getAvatarIdNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                return client.getAvatarId();
            } catch (e) {
                // fall through
            }
        }
        return 0;
    }-*/;

    /**
     * Does the actual <code>usePet()</code> call.
     */
    protected static native void usePetNative (int petId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.usePet(petId);
            } catch (e) {
                // fall through
            }
        }
    }-*/;

    /**
     * Does the actual <code>removePet()</code> call.
     */
    protected static native void removePetNative (int petId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.removePet(petId);
            } catch (e) {
                // fall through
            }
        }
    }-*/;

    /**
     * Does the actual <code>getPets()</code> call.
     */
    protected static native JavaScriptObject getPetsNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                return client.getPets();
            } catch (e) {
            }
        }
        return [];
    }-*/;

    // TODO: put this in Application?
    protected static final int HEADER_HEIGHT = 50;
    protected static final int BLACKBAR_HEIGHT = 20;
    protected static final int CLIENT_HEIGHT = 550;
}
