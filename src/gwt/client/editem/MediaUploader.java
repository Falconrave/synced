//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.DeploymentConfig;

import com.threerings.gwt.ui.SmartFileUpload;
import com.threerings.gwt.ui.WidgetUtil;

import client.shell.CShell;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Helper class, used in ItemEditor.
 */
public class MediaUploader extends FlexTable
{
    public static final int NORMAL = 0;
    public static final int THUMBNAIL = 1;
    public static final int NORMAL_PLUS_THUMBNAIL = 2;

    /**
     * @param id the type of the uploader to create, e.g. {@link Item#MAIN_MEDIA} . This value is
     * later passed to the bridge to identify the hash/mimeType returned by the server.
     * @param mode whether we're uploading normal media, thumbnail media or normal media that
     * should also generate a thumbnail image when changed.
     * @param updater the updater that knows how to set the media hash on the item.
     */
    public MediaUploader (String mediaId, int mode, ItemEditor.MediaUpdater updater)
    {
        setStyleName("mediaUploader");
        setCellPadding(0);
        setCellSpacing(0);

        _mode = mode;
        _updater = updater;

        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HorizontalPanel.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HorizontalPanel.ALIGN_MIDDLE);
        setText(0, 0, "");

        getFlexCellFormatter().setWidth(0, 1, "5px");
        getFlexCellFormatter().setRowSpan(0, 1, 2);

        setWidget(0, 2, _hint = MsoyUI.createLabel("", "Tip"));
        _hint.setWidth((2 * MediaDesc.THUMBNAIL_WIDTH) + "px");
        getFlexCellFormatter().setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

        _form = new FormPanel();
        _panel = new HorizontalPanel();
        _form.setWidget(_panel);
        _form.setStyleName("Controls");

        if (GWT.isScript()) {
            _form.setAction("/uploadsvc");
        } else {
            _form.setAction("http://localhost:8080/uploadsvc");
        }
        _form.setEncoding(FormPanel.ENCODING_MULTIPART);
        _form.setMethod(FormPanel.METHOD_POST);

        _upload = new SmartFileUpload();
        _upload.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                uploadMedia();
            }
        });

        // appending PLUS_THUMB to the media id will indicate to the upload servlet that we want it
        // to also generate a thumbnail image and report that to us as well after uploading
        if (mode == NORMAL_PLUS_THUMBNAIL) {
            mediaId += Item.PLUS_THUMB;
        }
        _upload.setName(mediaId);
        _panel.add(_upload);

        _form.addFormHandler(new FormHandler() {
            public void onSubmit (FormSubmitEvent event) {
                // don't let them submit until they plug in a file...
                if (_upload.getFilename().length() == 0) {
                    event.setCancelled(true);
                }
            }
            public void onSubmitComplete (FormSubmitCompleteEvent event) {
                String result = event.getResults();
                result = (result == null) ? "" : result.trim();
                if (result.length() > 0) {
                    // TODO: This is fugly as all hell, but at least we're now reporting
                    // *something* to the user
                    MsoyUI.error(result);
                } else {
                    _submitted = _upload.getFilename();
                }
            }
        });

        setWidget(1, 0, _form);
        getFlexCellFormatter().setVerticalAlignment(1, 0, HorizontalPanel.ALIGN_BOTTOM);

        // TEMP: display the media chooser applet next to the old HTML upload based uploader but
        // only on dev until we're actually ready to go live with the new hotness
        if (DeploymentConfig.devDeployment) {
            String[] args = new String[] {
                "media_id", mediaId, "authtoken", CShell.ident.token,
                // "server", config.server, "port", "" + config.port,
            };
            HTML upload = WidgetUtil.createApplet(
                "upload", "/clients/" + DeploymentConfig.version + "/mchooser-applet.jar," +
                "/clients/" + DeploymentConfig.version + "/mchooser.jar",
                "com.threerings.msoy.MediaChooserApplet",
                CHOOSER_WIDTH, CHOOSER_HEIGHT, true, args);
            _panel.add(upload);
        }
    }

    /**
     * Set the media to be shown in this uploader.
     */
    public void setMedia (MediaDesc desc)
    {
        if (desc != null) {
            int width = MediaDesc.THUMBNAIL_WIDTH, height = MediaDesc.THUMBNAIL_HEIGHT;
            if (_mode != THUMBNAIL) {
                width *= 2;
                height *= 2;
            }
            setWidget(0, 0, MediaUtil.createMediaView(desc, width, height, null));
        } else {
            setText(0, 0, "");
        }
    }

    /**
     * Set the media as uploaded by the user.
     */
    public void setUploadedMedia (MediaDesc desc, int width, int height)
    {
        String result = _updater.updateMedia(_upload.getFilename(), desc, width, height);
        if (result == null) {
            setMedia(desc);
        } else {
            MsoyUI.error(result);
        }
    }

    /**
     * Set a hint to be displayed next to the media area.
     */
    public void setHint (String hint)
    {
        _hint.setText(hint);
    }

    protected void uploadMedia ()
    {
        if (_upload.getFilename().length() == 0) {
            return;
        }
        if (_submitted != null && _submitted.equals(_upload.getFilename())) {
            return;
        }
        _form.submit();
    }

    protected ItemEditor.MediaUpdater _updater;

    protected Label _hint;
    protected HorizontalPanel _panel;

    protected FormPanel _form;
    protected SmartFileUpload _upload;
    protected String _submitted;

    protected int _mode;

    protected static final int CHOOSER_WIDTH = 60;
    protected static final int CHOOSER_HEIGHT = 28;
}
