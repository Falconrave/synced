//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;

/**
 * Displays the Gallery meta data: name, description, thumbnail, yada yada.
 *
 * @author mjensen
 */
public class GalleryDetailPanel extends AbsolutePanel
{
    public GalleryDetailPanel (GalleryData galleryData, boolean readOnly)
    {
        _gallery = galleryData.gallery;
        setStyleName("galleryDetailPanel");

        // Thumbnail
        if (_gallery.thumbMedia != null) {
            add(MsoyUI.createSimplePanel("GalleryThumbnail", MediaUtil.createMediaView(
                _gallery.thumbMedia, MediaDesc.THUMBNAIL_SIZE)), 10, 60);
        } else {
            add(MsoyUI.createLabel("no image", "GalleryThumbnail"), 10, 60);
        }

        // TODO needs to listen for changes to count
        add(MsoyUI.createLabel(_pmsgs.photoCount(galleryData.photos.size() + ""), "Count"), 110,
            70);

        if (readOnly) {
            // add name and description labels
            add(MsoyUI.createLabel(GalleryPanel.getGalleryLabel(_gallery), "Name"), 10, 10);
            add(MsoyUI.createLabel(_gallery.description, "Description"), 10, 140);

        } else {
            // do not allow profile gallery name to be edited
            if (_gallery.isProfileGallery()) {
                add(MsoyUI.createLabel(GalleryPanel.getGalleryLabel(_gallery), "Name"), 10, 10);
            } else {
                add(MsoyUI.createTextArea(_gallery.name, "Name", new ChangeListener() {
                    public void onChange (Widget sender) {
                        _gallery.name = ((TextArea) sender).getText();
                    }
                }), 10, 10);
            }

            add(MsoyUI.createTextArea(_gallery.description, "Description", new ChangeListener() {
                public void onChange (Widget sender) {
                    _gallery.description = ((TextArea) sender).getText();
                }
            }), 10, 140);
        }

        // if the current member owns this read-only gallery, add an edit button
        if (readOnly && galleryData.ownerId == CShell.getMemberId()) {
            final String args = Args.compose(GalleryEditPanel.EDIT_ACTION, _gallery.galleryId);
            final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
            add(MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.editButton(), listener), 40, 270);
        }
    }

    protected Gallery _gallery;

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
