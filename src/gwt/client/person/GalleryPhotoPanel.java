//
// $Id$

package client.person;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.web.gwt.Pages;

import client.images.slideshow.SlideshowImages;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;

/**
 * View a single image in a gallery. This panel will be a child of GalleryViewPanel which contains
 * data on all the photos in the gallery.
 */
public class GalleryPhotoPanel extends FlowPanel
{
    /**
     * Constructor.
     */
    public GalleryPhotoPanel (GalleryData galleryData)
    {
        _galleryData = galleryData;
        addStyleName("galleryPhotoPanel");
    }

    /**
     * Change which photo is being shown.
     * @param photoIndex Location of the photo in the gallery's list of photos.
     */
    protected void setPhoto (final int photoIndex)
    {
        // slideshow mode has a different look
        if (_slideshowTimer != null) {
            addStyleName("SlideshowMode");
            _slideshowTimer.schedule(SLIDESHOW_DELAY);
        } else {
            removeStyleName("SlideshowMode");
        }

        _currentPhotoIndex = photoIndex;
        final Photo photo = _galleryData.photos.get(photoIndex);
        clear();

        add(MsoyUI.createLabel(photo.name, "PhotoName"));

        // clicking the gallery name takes you back to the gallery
        InlinePanel nameAndCreator = new InlinePanel("");
        nameAndCreator.addStyleName("NameAndCreator");
        String gname = GalleryPanel.getGalleryLabel(_galleryData.gallery, _galleryData.owner);
        nameAndCreator.add(MsoyUI.createActionLabel(gname, "GalleryName", new ClickHandler() {
            public void onClick (ClickEvent event) {
                stopSlideshow();
                gotoPhotoIndex(-1);
            }
        }));
        nameAndCreator.add(new CreatorLabel(_galleryData.owner));
        add(nameAndCreator);

        // determine the width of the most constrained side and override the media constraint
        int width = photo.photoWidth;
        int height = photo.photoHeight;
        if (width > MAX_PHOTO_WIDTH) {
            width = MAX_PHOTO_WIDTH;
            height = Math.round((MAX_PHOTO_WIDTH / (float)photo.photoWidth) * photo.photoHeight);
        } else {
            width = 0;
        }
        if (height > MAX_PHOTO_HEIGHT) {
            height = MAX_PHOTO_HEIGHT;
            width = 0;
        } else {
            height = 0;
        }
        byte constraint = MediaDesc.NOT_CONSTRAINED;
        if (width > 0) {
            constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
        } else if (height > 0) {
            constraint = MediaDesc.VERTICALLY_CONSTRAINED;
        }
        photo.photoMedia = photo.photoMedia.newWithConstraint(constraint);

        // clear the photo onclick
        ClickHandler onClick = new ClickHandler() {
            public void onClick (ClickEvent event) {
                stopSlideshow();
                gotoPhotoIndex((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        Widget largePhoto = MediaUtil.createMediaView(photo.photoMedia, width, height, onClick);
        FlowPanel largePhotoContainer = MsoyUI.createFlowPanel("LargePhoto");
        largePhotoContainer.add(largePhoto);
        add(largePhotoContainer);

        // slideshow, prev/next, full size controls
        final AbsolutePanel controls = new AbsolutePanel();
        controls.addStyleName("Controls");
        add(controls);

        // start slideshow button and text
        if (_slideshowTimer == null) {
            ClickHandler slideshowClick = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    startSlideshow();
                }
            };
            controls.add(MsoyUI.createPushButton(
                _slideshowImages.play_default(),
                _slideshowImages.play_over(),
                _slideshowImages.play_down(), slideshowClick), 0, 0);
            controls.add(MsoyUI.createActionLabel(_pmsgs.gallerySlideshowStart(), slideshowClick),
                35, 5);
        }

        // prev and next buttons
        ClickHandler onPrev = new ClickHandler() {
            public void onClick (ClickEvent event) {
                gotoPhotoIndex(photoIndex == 0 ? _galleryData.photos.size() - 1 : photoIndex - 1);
            }
        };
        ClickHandler onNext = new ClickHandler() {
            public void onClick (ClickEvent event) {
                gotoPhotoIndex((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        int prevNextLeftOffset = _slideshowTimer == null ? 300 : 230;
        controls.add(MsoyUI.createPrevNextButtons(onPrev, onNext), prevNextLeftOffset, 0);

        // controls for when slideshow is running (or paused)
        if (_slideshowTimer != null) {
            final int leftOffset = 380;
            // play and pause buttons swap when clicked
            _playButton = MsoyUI.createPushButton(
                _slideshowImages.play_default(),
                _slideshowImages.play_over(),
                _slideshowImages.play_down(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _slideshowPaused = false;
                        _slideshowTimer.schedule(1);
                        controls.remove(_playButton);
                        controls.add(_pauseButton, leftOffset, 0);
                    }
                });
            _pauseButton = MsoyUI.createPushButton(
                _slideshowImages.pause_default(),
                _slideshowImages.pause_over(),
                _slideshowImages.pause_down(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _slideshowPaused = true;
                        controls.remove(_pauseButton);
                        controls.add(_playButton, leftOffset, 0);
                    }
                });
            if (_slideshowPaused) {
                controls.add(_playButton, leftOffset, 0);
            } else {
                controls.add(_pauseButton, leftOffset, 0);
            }

            controls.add(MsoyUI.createPushButton(
                _slideshowImages.close_default(),
                _slideshowImages.close_over(),
                _slideshowImages.close_down(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        stopSlideshow();
                        gotoPhotoIndex(-1);
                    }
                }), 410, 0);
        }

        // if this image has been scaled down, link to the full size
        if (_slideshowTimer == null) {
            if (photo.photoMedia.getConstraint() != MediaDesc.NOT_CONSTRAINED) {
                controls.add(MsoyUI.createExternalAnchor(photo.photoMedia.getMediaPath(),
                    _pmsgs.galleryFullSize()), 590, 5);
            }
        }
    }

    /**
     * Moves the page to the appropriate photo based on it's location in the list of photos. Use
     * itemId instead of photo index in case the order of photos changes.
     */
    public void gotoPhotoIndex (int photoIndex)
    {
        Photo photo = photoIndex >= 0 ? _galleryData.photos.get(photoIndex) : null;
        Link.go(Pages.PEOPLE, GalleryActions.VIEW_PHOTO,
                _galleryData.gallery.galleryId, photo == null ? 0 : photo.itemId);
    }

    /**
     * Start the slideshow immediately at the first photo or next photo if one is being shown.
     */
    protected void startSlideshow ()
    {
        _slideshowPaused = false;
        _slideshowTimer = new Timer() {
            @Override public void run() {
                advanceSlideshow();
            }
        };
        _slideshowTimer.schedule(1);
    }

    /**
     * Halts any active slideshow.
     */
    protected void stopSlideshow ()
    {
        if (_slideshowTimer != null) {
            _slideshowTimer.cancel();
            _slideshowTimer = null;
        }
    }

    /**
     * Display the next image in the gallery and schedule another change in 5 seconds.
     */
    protected void advanceSlideshow ()
    {
        if (!_slideshowPaused) {
            if (_currentPhotoIndex < 0) {
                gotoPhotoIndex(0);
            } else {
                gotoPhotoIndex((_currentPhotoIndex + 1) % _galleryData.photos.size());
            }
        }
        // timer will also be rescheduled after the new image loads
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final SlideshowImages _slideshowImages = GWT.create(SlideshowImages.class);

    protected PushButton _playButton;
    protected PushButton _pauseButton;

    /** While set to true, timer events will fire but do nothing */
    protected boolean _slideshowPaused = false;

    /** List of photos and gallery details */
    protected GalleryData _galleryData;

    /** The index of the photo currently being displayed, used for slideshow */
    protected int _currentPhotoIndex;

    /** Fires every 5 seconds while slideshow is running */
    protected Timer _slideshowTimer;

    protected static int MAX_PHOTO_WIDTH = 600;
    protected static int MAX_PHOTO_HEIGHT = 400;
    protected static final int SLIDESHOW_DELAY = 5000;
}
