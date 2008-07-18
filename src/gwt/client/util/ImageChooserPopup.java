//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

import client.editem.EditorHost;

import client.util.MediaUtil;

import client.shell.CShell;
import client.shell.Frame;

/**
 * Allows a member to select an image from their inventory. In the future, will support fancy
 * things like immediately uploading an image for use instead of getting one from their inventory,
 * and possibly doing a Google images or Flickr search.
 */
public class ImageChooserPopup extends VerticalPanel
{
    /**
     * Displays an image chooser which will select either the main or thumbnail image of a photo
     * from the user's inventory, or allow them to upload an image directly.
     */
    public static void displayImageChooser (
        final boolean thumbnail, final AsyncCallback<MediaDesc> callback)
    {
        CShell.membersvc.loadInventory(
            CShell.ident, Item.PHOTO, 0, new AsyncCallback<List<Item>>() {
                public void onSuccess (List<Item> items) {
                    Frame.showDialog(CShell.cmsgs.icTitle(),
                                    new ImageChooserPopup(items, thumbnail, callback));
                }
                public void onFailure (Throwable caught) {
                    callback.onFailure(caught);
                }
            });
    }

    protected ImageChooserPopup (
        List<Item> images, boolean thumbnail, AsyncCallback<MediaDesc> callback)
    {
        _callback = callback;
        _thumbnail = thumbnail;
        setStyleName("imageChooser");

        if (images.size() > 0) {
            add(MsoyUI.createLabel(CShell.cmsgs.icPickPhoto(), "Title"));
            add(new PhotoGrid(images));
            add(MsoyUI.createLabel(CShell.cmsgs.icOr(), "Or"));
        }

        add(MsoyUI.createLabel(CShell.cmsgs.icUploadPhoto(), "Title"));
        add(new PhotoUploader(_thumbnail));
    }

    protected void imageChosen (MediaDesc media)
    {
        _callback.onSuccess(media);
        Frame.clearDialog();
    }

    protected class PhotoGrid extends PagedGrid<Photo>
        implements EditorHost
    {
        public PhotoGrid (List<Item> images) {
            super(2, 7, NAV_ON_BOTTOM);
            setWidth("100%");
            // for checking and strong typing
            List<Photo> photos = new ArrayList<Photo>();
            for (Item potentialPhoto : images) {
                if (potentialPhoto instanceof Photo) {
                    photos.add((Photo)potentialPhoto);
                }
            }
            setModel(new SimpleDataModel<Photo>(photos), 0);
        }

        // from interface EditorHost
        public void editComplete (Item item) {
            CShell.log("Edit complete " + item);
            if (item != null) {
                ((SimpleDataModel)_model).addItem(0, item);
                CShell.log("Added to model");
                displayPage(0, true);
                CShell.log("Updated page");
            }
        }

        @Override // from PagedGrid
        protected Widget createWidget (Photo photo) {
            final MediaDesc media = _thumbnail ? photo.getThumbnailMedia() : photo.photoMedia;
            Widget image = MediaUtil.createMediaView(
                photo.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
                    public void onClick (Widget sender) {
                        imageChosen(media);
                    }
                });
            image.addStyleName("Photo");
            return image;
        }

        @Override // from PagedGrid
        protected String getEmptyMessage () {
            return ""; // not used
        }

        @Override // from PagedGrid
        protected void formatCell (HTMLTable.CellFormatter formatter, int row, int col, int limit) {
            super.formatCell(formatter, row, col, limit);
            formatter.setWidth(row, col, MediaDesc.THUMBNAIL_WIDTH+"px");
            formatter.setHeight(row, col, MediaDesc.THUMBNAIL_HEIGHT+"px");
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items) {
            return true;
        }
    }

    protected class PhotoUploader extends SmartTable
    {
        public PhotoUploader (final boolean thumbnail)
        {
            String mediaId = thumbnail ? Item.THUMB_MEDIA : Item.MAIN_MEDIA;
            setWidget(0, 0, _preview = new SimplePanel(), 2, "Preview");
            setWidget(1, 0, new MediaUploader(mediaId, new MediaUploader.Listener() {
                public void mediaUploaded (String name, MediaDesc desc, int width, int height) {
                    // photos must be images because they are used as headshots in games
                    // and we don't want any SWF running other than the game.
                    if (!desc.isImage()) {
                        _upload.setEnabled(false);
                        _preview.setWidget(null);
                        MsoyUI.error(CShell.emsgs.errPhotoNotImage());
                        return;
                    }
                    _media = desc;
                    _upload.setEnabled(true);
                    int size = thumbnail ? MediaDesc.THUMBNAIL_SIZE : MediaDesc.PREVIEW_SIZE;
                    _preview.setWidget(MediaUtil.createMediaView(_media, size));
                }
            }));
            setWidget(1, 1, _upload = new Button(CShell.cmsgs.icUploadGo(), new ClickListener() {
                public void onClick (Widget sender) {
                    imageChosen(_media);
                }
            }));
            _upload.setEnabled(false);
        }

        protected SimplePanel _preview;
        protected Button _upload;
        protected MediaDesc _media;
    }

    protected boolean _thumbnail;
    protected AsyncCallback<MediaDesc> _callback;
}
