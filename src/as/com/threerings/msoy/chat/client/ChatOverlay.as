//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.BlendMode;
import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Sprite;
import flash.display.Stage;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextFormat;

import flash.utils.getTimer; // function import

import mx.events.ResizeEvent;
import mx.events.ScrollEvent;

import mx.core.Container;
import mx.core.IRawChildrenContainer;

import mx.controls.scrollClasses.ScrollBar;
import mx.controls.VScrollBar;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.MessageBundle;
import com.threerings.util.StringUtil;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.flash.ColorUtil;

import com.threerings.whirled.spot.data.SpotCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;

public class ChatOverlay
    implements ChatDisplay
{
    public var log :Log = Log.getLog(this);

    public function ChatOverlay (ctx :WorldContext)
    {
        _ctx = ctx;

        _overlay = new Sprite();
        _overlay.mouseChildren = false;
        _overlay.mouseEnabled = false;
//        _overlay.alpha = ALPHA;
        _overlay.blendMode = BlendMode.LAYER;

        // NOTE: Any null values in the override formats will use the value from the default, so if
        // a property is added to the default then it should be explicitely negated if not desired
        // in an override.
        _defaultFmt = new TextFormat();
        _defaultFmt.font = FONT;
        _defaultFmt.size = 10;
        _defaultFmt.color = 0x000070;
        _defaultFmt.bold = false;
        _defaultFmt.underline = false;

        _userSpeakFmt = new TextFormat();
        _userSpeakFmt.font = FONT;
        _userSpeakFmt.size = 12;
        _userSpeakFmt.color = 0x000000;
        _userSpeakFmt.bold = false;
        _userSpeakFmt.underline = false;

        // listen for preferences changes, update history mode
        Prefs.config.addEventListener(ConfigValueSetEvent.TYPE, handlePrefsUpdated, false, 0, true);
    }

    /**
     * Are we active with a target?
     */
    public function isActive () :Boolean
    {
        return (_target != null);
    }

    /**
     * Configures this chat overlay with its chat history list.
     */
    public function setHistory (history :HistoryList) :void
    {
        _history = history;
    }

    /**
     * Set the target container where this chat should add its overlay. This resets any custom
     * target bounds previously set via {@link #setTargetBounds}.
     *
     * @param target the container to which a chat overlay should be added; or null to release
     * references and internal resources associated with the previous target.
     * @param targetWidth an optional parameter forcing the target width to the specified value so
     * that message layout will work properly even if the target has not yet been laid out and does
     * not yet have its proper width.
     */
    public function setTarget (target :Container, targetWidth :int = -1) :void
    {
        if (_target != null) {
            // removing from the old
            _target.removeEventListener("childrenChanged", handleContainerPopulate);
            _target.removeEventListener(ResizeEvent.RESIZE, handleContainerResize);
            _target.rawChildren.removeChild(_overlay);

            // stop listening to our chat history
            _history.removeChatOverlay(this);

            // clear all subtitles, blow away the overlay
            clearGlyphs(_subtitles);
            clearGlyphs(_showingHistory);
            setHistoryEnabled(false);
        }

        _target = target;

        if (_target != null) {
            // adding to the new
            _overlay.x = 0;
            _overlay.y = 0;
            _target.rawChildren.addChildAt(
                _overlay, Math.max(0, _target.rawChildren.numChildren - 1));
            _target.addEventListener(ResizeEvent.RESIZE, handleContainerResize);
            _target.addEventListener("childrenChanged", handleContainerPopulate);

            // resume listening to our chat history
            _history.addChatOverlay(this);

            layout(null, targetWidth);
            setHistoryEnabled(Prefs.getShowingChatHistory());
        }
    }

    /**
     * Configures the region of our target over which we will render. This overrides our natural
     * calculations based on the target's reported width and the percentage of the target's height
     * to use for chat.
     */
    public function setTargetBounds (bounds :Rectangle) :void
    {
        log.info("Setting chat bounds " + bounds);
        layout(bounds, -1);
    }

    /**
     * Set whether history is enabled or not.
     * TODO: Prefs install defaults.
     */
    public function setHistoryEnabled (historyEnabled :Boolean) :void
    {
        if (historyEnabled == (_historyBar != null)) {
            return; // no change
        }

        if (historyEnabled) {
            _historyBar = new VScrollBar();
            _historyBar.addEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
            _historyBar.includeInLayout = false;
            configureHistoryBarSize();
            _target.addChild(_historyBar);
            _target.addEventListener(Event.ADDED_TO_STAGE, handleTargetAdded);
            _target.addEventListener(Event.REMOVED_FROM_STAGE, handleTargetRemoved);
            handleTargetAdded();
            resetHistoryOffset();

            // out with the subtitles
            clearGlyphs(_subtitles);

            // "scroll" down to the latest history entry
            updateHistBar(_history.size() - 1);

            // figure our history
            figureCurrentHistory();

        } else {
            _target.removeEventListener(Event.ADDED_TO_STAGE, handleTargetAdded);
            _target.removeEventListener(Event.REMOVED_FROM_STAGE, handleTargetRemoved);
            handleTargetRemoved();
            _target.removeChild(_historyBar);
            _historyBar.removeEventListener(ScrollEvent.SCROLL, handleHistoryScroll);
            _historyBar = null;

            clearGlyphs(_showingHistory);
        }
    }

    /**
     * Are we currently showing chat history?
     */
    public function isHistoryMode () :Boolean
    {
        return (_historyBar != null);
    }

    /**
     * Sets whether or not the glyphs are clickable.
     */
    public function setClickableGlyphs (clickable :Boolean) :void
    {
        //_overlay.mouseEnabled = clickable;
        _overlay.mouseChildren = clickable;
    }

    /**
     * Set the percentage of the bottom of the screen to use for subtitles.
     * TODO: by pixel?
     */
    public function setSubtitlePercentage (perc :Number) :void
    {
        if (_subtitlePercentage != perc) {
            _subtitlePercentage = perc;
            if (_target) {
                layout(null, -1);
            }
        }
    }

    // from ChatDisplay
    public function clear () :void
    {
        clearGlyphs(_subtitles);
    }

    // from ChatDisplay
    public function displayMessage (msg :ChatMessage, alreadyDisp :Boolean) :Boolean
    {
        if (_target == null) {
            return false;
        }

        return displayMessageNow(msg);
    }

    protected function handlePrefsUpdated (event :ConfigValueSetEvent) :void
    {
        if ((_target != null) && (event.name == Prefs.CHAT_HISTORY)) {
            setHistoryEnabled(Boolean(event.value));
        }
    }

    /**
     * Layout.
     */
    protected function layout (bounds :Rectangle, targetWidth :int) :void
    {
        clearGlyphs(_subtitles);

        // if special bounds were provided, use them, otherwise compute them
        if (bounds == null) {
            var height :int = _target.height * _subtitlePercentage;
            _targetBounds = new Rectangle(0, _target.height - height,
                                          targetWidth == -1 ? _target.width : targetWidth, height);
        } else {
            _targetBounds = bounds;
        }

        // make a guess as to the extent of the history (how many avg sized subtitles will fit in
        // the subtitle area
        _historyExtent = (_targetBounds.height - PAD) / SUBTITLE_HEIGHT_GUESS;

        var msg :ChatMessage;
        var now :int = getTimer();
        var histSize :int = _history.size();
        var index :int = histSize - 1;
        for ( ; index >= 0; index--) {
            msg = _history.get(index);
            _lastExpire = 0;
            if (now > getChatExpire(msg.timestamp, msg.message)) {
                break;
            }
        }

        // now that we've found the message that's one too old, increment the index so that it
        // points to the first message we should display
        index++;
        _lastExpire = 0;

        // now dispatch from that point
        for ( ; index < histSize; index++) {
            msg = _history.get(index);
            if (shouldShowFromHistory(msg, index)) {
                displayMessage(msg, false);
            }
        }

        // reset the history offset
        resetHistoryOffset();

        // finally, if we're in history mode, we should figure that out too
        if (isHistoryMode()) {
            configureHistoryBarSize();
            updateHistBar(histSize - 1);
            figureCurrentHistory();
        }
    }

    /**
     * We're looking through history to figure out which messages we should be showing, should we
     * show the following?
     */
    protected function shouldShowFromHistory (msg :ChatMessage, index :int) :Boolean
    {
        return true; // all for subtitles
    }

    /**
     * Update the history scrollbar with the specified value.
     */
    protected function updateHistBar (val :int) :void
    {
        // we may need to figure out the new history offset amount...
        if (!_histOffsetFinal && (_history.size() > _histOffset)) {
            figureHistoryOffset();
        }

        // then figure out the new value and range
        var oldVal :int = Math.max(_histOffset, val);
        var newMaxVal :int = Math.max(0, _history.size() - 1);
        var newVal :int = (oldVal >= newMaxVal - 1) ? newMaxVal :oldVal;

        // _settingBar protects us from reacting to our own change
        _settingBar = true;
        try {
            _historyBar.setScrollProperties(_historyExtent, _histOffset, newMaxVal);
            _historyBar.scrollPosition = newVal;
        } finally {
            _settingBar = false;
        }
    }

    /**
     * Reset the history offset so that it will be recalculated next time it is needed.
     */
    protected function resetHistoryOffset () :void
    {
        _histOffsetFinal = false;
        _histOffset = 0;
    }

    /**
     * Display the specified message now, unless we are to ignore it.
     *
     * @return true if the message was displayed.
     */
    protected function displayMessageNow (msg :ChatMessage) :Boolean
    {
        var type :int = getType(msg, false);
        if (type == IGNORECHAT) {
            return false;
        }

        return displayTypedMessageNow(msg, type);
    }

    /**
     * Display a non-history message now.
     */
    protected function displayTypedMessageNow (msg :ChatMessage, type :int) :Boolean
    {
        // if we're in history mode, this will show up in the history and we'll rebuild our
        // subtitle list if and when history goes away
        if (isHistoryMode()) {
            return false;
        }

        addSubtitle(createSubtitle(msg, type, true));
        return true;
    }

    /**
     * Add the specified subtitle glyph for immediate display.
     */
    protected function addSubtitle (glyph :SubtitleGlyph) :void
    {
        var height :int = int(glyph.height);
        glyph.x = _targetBounds.x + PAD;
        glyph.y = _targetBounds.bottom - height - PAD;
        scrollUpSubtitles(height + getSubtitleSpacing(glyph.getType()));
        _subtitles.push(glyph);
        _overlay.addChild(glyph);
    }

    /**
     * Create a subtitle glyph.
     */
    protected function createSubtitle (msg :ChatMessage, type :int, expires :Boolean) :SubtitleGlyph
    {
        var texts :Array = formatMessage(msg, type, true, _userSpeakFmt);
        var lifetime :int = getLifetime(msg, expires);
        return new SubtitleGlyph(this, type, lifetime, _defaultFmt, texts);
    }

    /**
     * Return an array of Strings and TextFormats for creating a ChatGlyph.
     */
    protected function formatMessage (
        msg :ChatMessage, type :int, forceSpeaker :Boolean, userSpeakFmt :TextFormat) :Array
    {
        // first parse the message text into plain and links
        var texts :Array = parseLinks(msg.message, userSpeakFmt);

        // possibly insert the formatting
        if (forceSpeaker || alwaysUseSpeaker(type)) {
            var format :String = msg.getFormat();
            if (format != null) {
                var umsg :UserMessage = (msg as UserMessage);
                var prefix :String = _ctx.xlate(
                    MsoyCodes.CHAT_MSGS, format, umsg.getSpeakerDisplayName()) + " ";

                if (useQuotes(type)) {
                    prefix += "\"";
                    texts.push("\"");
                }
                texts.unshift(prefix);
            }
        }

        return texts;
    }

    /**
     * Return an array of text strings, with any string needing special formatting preceeded by
     * that format.
     */
    protected function parseLinks (text :String, userSpeakFmt :TextFormat) :Array
    {
        // parse the text into an array with urls at odd elements
        var array :Array = StringUtil.parseURLs(text);

        // insert the appropriate format before each element
        for (var ii :int = array.length - 1; ii >= 0; ii--) {
            if (ii % 2 == 0) {
                // normal text at even-numbered elements...
                array.splice(ii, 0, userSpeakFmt);
            } else {
                // links at the odd indexes
                array.splice(ii, 0, createLinkFormat(String(array[ii]), userSpeakFmt));
            }
        }
        return array;
    }

    /**
     * Create a link format for the specified link text.
     */
    protected function createLinkFormat (url :String, userSpeakFmt :TextFormat) :TextFormat
    {
        var fmt :TextFormat = new TextFormat();
        fmt.align = userSpeakFmt.align;
        fmt.font = FONT;
        fmt.size = 10;
        fmt.underline = true;
        fmt.color = 0xFF0000;
        fmt.bold = true;
        fmt.url = "event:" + url;
        return fmt;
    }

    /**
     * Get the lifetime, in milliseconds, of the specified chat message.
     */
    protected function getLifetime (msg :ChatMessage, expires :Boolean) :int
    {
        if (expires) {
            return getChatExpire(msg.timestamp, msg.message) - msg.timestamp;
        }
        return int.MAX_VALUE;
    }

    /**
     * Get the expire time for the specified chat.
     */
    protected function getChatExpire (stamp :int, text :String) :int
    {
        // load the configured durations
        var durations :Array =
            (DISPLAY_DURATION_PARAMS[getDisplayDurationIndex()] as Array);

        // start the computation from the maximum of the timestamp
        // or our last expire time.
        var start :int = Math.max(stamp, _lastExpire);

        // set the next expire to a time proportional to the text length.
        _lastExpire = start + Math.min(text.length * int(durations[0]),
                                       int(durations[2]));

        // but don't let it be longer than the maximum display time.
        _lastExpire = Math.min(stamp + int(durations[2]), _lastExpire);

        // and be sure to pop up the returned time so that it is above the min.
        return Math.max(stamp + int(durations[1]), _lastExpire);
    }

    /**
     * Should we be using quotes with the specified format?
     */
    protected function useQuotes (type :int) :Boolean
    {
        return (modeOf(type) != EMOTE);
    }

    /**
     * Should we force the use of the speaker in the formatting of
     * the message?
     */
    protected function alwaysUseSpeaker (type :int) :Boolean
    {
        return (modeOf(type) == EMOTE) || (placeOf(type) == BROADCAST);
    }

    /**
     * Get the outline color for the specified chat type.
     */
    protected function getOutlineColor (type :int) :uint
    {
        switch (type) {
        case BROADCAST: return BROADCAST_COLOR;
        case TELL: return TELL_COLOR;
        case TELLFEEDBACK: return TELLFEEDBACK_COLOR;
        case INFO: return INFO_COLOR;
        case FEEDBACK: return FEEDBACK_COLOR;
        case ATTENTION: return ATTENTION_COLOR;
        default:
            switch (placeOf(type)) {
            case GAME: return GAME_COLOR;
            default: return BLACK;
            }
        }
    }

    /**
     * Used by ChatGlyphs to draw the shape on their Graphics.
     */
    internal function drawSubtitleShape (g :Graphics, type :int, width :int, height :int) :int
    {
        var outline :uint = getOutlineColor(type);
        var background :uint;
        if (BLACK == outline) {
            background = WHITE;
        } else {
            background = ColorUtil.blend(WHITE, outline, .8);
        }
        width += PAD * 2;

        var shapeFunction :Function = getSubtitleShape(type);

        // clear any old graphics
        g.clear();
        // fill the shape with the background color
        g.beginFill(background);
        shapeFunction(g, width, height);
        g.endFill();
        // draw the shape with the outline color
        g.lineStyle(1, outline);
        shapeFunction(g, width, height);

        return PAD;
    }

    /**
     * Get the function that draws the subtitle shape for the
     * specified type of subtitle.
     */
    protected function getSubtitleShape (type :int) :Function
    {
        switch (placeOf(type)) {
        case PLACE: {
            switch (modeOf(type)) {
            case SPEAK:
            default:
                return drawRoundedSubtitle;

            case EMOTE:
                return drawEmoteSubtitle;

            case THINK:
                return drawThinkSubtitle;
            }
        }

        case FEEDBACK:
            return drawFeedbackSubtitle;

        case BROADCAST:
        case CONTINUATION:
        case INFO:
        case ATTENTION:
        default:
            return drawRectangle;
        }
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawRectangle (g :Graphics, w :int, h :int) :void
    {
        g.drawRect(0, 0, w, h);
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawRoundedSubtitle (g :Graphics, w :int, h :int) :void
    {
        g.drawRoundRect(0, 0, w, h, PAD * 2, PAD * 2);
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawEmoteSubtitle (g :Graphics, w :int, h :int) :void
    {
        g.moveTo(0, 0);
        g.lineTo(w, 0);
        g.curveTo(w - PAD, h / 2, w, h);
        g.lineTo(0, h);
        g.curveTo(PAD, h / 2, 0, 0);
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawThinkSubtitle (g :Graphics, w :int, h :int) :void
    {
        // thinky bubbles on the left and right
        const DIA :int = 8;
        g.moveTo(PAD/2, 0);
        g.lineTo(w - PAD/2, 0);

        var yy :int;
        var ty :int;
        for (yy = 0; yy < h; yy += DIA) {
            ty = Math.min(h, yy + DIA);
            g.curveTo(w, (yy + ty)/2, w - PAD/2, ty);
        }

        g.lineTo(PAD/2, h);
        for (yy = h; yy > 0; yy -= DIA) {
            ty = Math.max(0, yy - DIA);
            g.curveTo(0, (yy + ty)/2, PAD/2, ty);
        }
    }

    /** Subtitle draw function. See getSubtitleShape() */
    protected function drawFeedbackSubtitle (g :Graphics, w :int, h :int) :void
    {
        g.moveTo(PAD / 2, 0);
        g.lineTo(w, 0);
        g.lineTo(w - PAD / 2, h);
        g.lineTo(0, h);
        g.lineTo(PAD / 2, 0);
    }

    /**
     * Called from the HistoryList to notify us that messages were added to the history.
     *
     * @param adjustment if non-zero, the number of old history entries that were pruned.
     */
    internal function historyUpdated (adjustment :int) :void
    {
        if (adjustment != 0) {
            for each (var glyph :SubtitleGlyph in _showingHistory) {
                glyph.histIndex -= adjustment;
            }
            // some history entries were deleted, we need to re-figure the
            // history scrollbar action
            resetHistoryOffset();
        }

        if (_target != null && isHistoryMode()) {
            var val :int = _historyBar.scrollPosition;
            updateHistBar(val - adjustment);

            // only refigure if needed
            if ((val != _historyBar.scrollPosition) || (adjustment != 0) || !_histOffsetFinal) {
                figureCurrentHistory();
            }
        }
    }

    /**
     * Callback from a ChatGlyph when it wants to be removed.
     */
    internal function glyphExpired (glyph :ChatGlyph) :void
    {
        ArrayUtil.removeFirst(_subtitles, glyph);
        // the glyph may have already been removed, but still expire
        // TODO: possibly fix that, so that a removed glyph is 
        if (glyph.parent == _overlay) {
            removeGlyph(glyph);
        }
    }

    /**
     * Remove a glyph from the overlay.
     */
    protected function removeGlyph (glyph :ChatGlyph) :void
    {
        _overlay.removeChild(glyph);
        glyph.wasRemoved();
    }

    /**
     * Convert the message class/localtype/mode into our internal type code.
     */
    protected function getType (msg :ChatMessage, history :Boolean) :int
    {
        var localtype :String = msg.localtype;

        if (msg is TellFeedbackMessage) {
            if (history || isApprovedLocalType(localtype)) {
                return (msg as TellFeedbackMessage).isFailure() ? FEEDBACK : TELLFEEDBACK;
            }
            return IGNORECHAT;

        } else if (msg is UserMessage) {
            var type :int = (ChatCodes.USER_CHAT_TYPE == localtype) ? TELL : PLACE;
            // factor in the mode
            switch ((msg as UserMessage).mode) {
            case ChatCodes.DEFAULT_MODE:
                return type | SPEAK;
            case ChatCodes.EMOTE_MODE:
                return type | EMOTE;
            case ChatCodes.THINK_MODE:
                return type | THINK;
            case ChatCodes.SHOUT_MODE:
                return type | SHOUT;
            case ChatCodes.BROADCAST_MODE:
                return BROADCAST; // broadcast always looks like broadcast
            }

        } else if (msg is SystemMessage) {
            if (history || isApprovedLocalType(localtype)) {
                switch ((msg as SystemMessage).attentionLevel) {
                case SystemMessage.INFO:
                    return INFO;
                case SystemMessage.FEEDBACK:
                    return FEEDBACK;
                case SystemMessage.ATTENTION:
                    return ATTENTION;
                default:
                    log.warning("Unknown attention level for system message " +
                        "[msg=" + msg + "].");;
                    break;
                }
            }

            // otherwise
            return IGNORECHAT;
        }

        log.warning("Skipping received message of unknown type [msg=" + msg + "].");
        return IGNORECHAT;
    }

    /**
     * Check to see if we want ti display the specified localtype.
     */
    protected function isApprovedLocalType (localtype :String) :Boolean
    {
        // we show everything
        return true;
    }

    /**
     * Get the spacing above the specified subtitle type.
     */
    protected function getSubtitleSpacing (type :int) :int
    {
//        switch (placeOf(type)) {
//        default:
            return 1;
//        }
    }

    /**
     * Get the spacing for the specified type in history.
     */
    protected function getHistorySubtitleSpacing (index :int) :int
    {
        var msg :ChatMessage = _history.get(index);
        return getSubtitleSpacing(getType(msg, true));
    }

    /**
     * Scroll up all the subtitles by the specified amount.
     */
    protected function scrollUpSubtitles (dy :int) :void
    {
        var minY :int = _targetBounds.y;
        for (var ii :int = 0; ii < _subtitles.length; ii++) {
            var glyph :ChatGlyph = (_subtitles[ii] as ChatGlyph);
            var newY :int = int(glyph.y) - dy;
            if (newY <= minY) {
                _subtitles.splice(ii, 1);
                ii--;
                removeGlyph(glyph);

            } else {
                glyph.y = newY;
            }
        }
    }

    /**
     * Extract the mode constant from the type value.
     */
    protected function modeOf (type :int) :int
    {
        return (type & 0xF);
    }

    /**
     * Extract the place constant from the type value. 
     */
    protected function placeOf (type :int) :int
    {
        return (type & ~0xF);
    }

    /**
     * Get the display duration parameters.
     */
    protected function getDisplayDurationIndex () :int
    {
        // by default we add one, because it's assumed that we're in
        // subtitle-only view.
        return Prefs.getChatDecay() + 1;
    }

    /**
     * Remove all the glyphs in the specified list.
     */
    protected function clearGlyphs (glyphs :Array) :void
    {
        if (_overlay != null) {
            for each (var glyph :ChatGlyph in glyphs) {
                removeGlyph(glyph);
            }
        }

        glyphs.length = 0; // array truncation
    }

    /**
     * React to the scrollbar being changed.
     */
    protected function handleHistoryScroll (event :ScrollEvent) :void
    {
        if (!_settingBar) {
            figureCurrentHistory();
        }
    }

    /**
     * When we're in history mode, listen for our target being added
     * to the hierarchy.
     */
    protected function handleTargetAdded (... ignored) :void
    {
        if (_target.stage) {
            // we need to listen to the stage for mouse wheel events,
            // otherwise we don't get them over many targets
            _stage = _target.stage;
            _stage.addEventListener(MouseEvent.MOUSE_WHEEL, handleMouseWheel);
        }
    }

    /**
     * When in history mode, listen for our target being removed
     * from the hierarchy.
     */
    protected function handleTargetRemoved (... ignored) :void
    {
        if (_stage) {
            _stage.removeEventListener(MouseEvent.MOUSE_WHEEL, handleMouseWheel);
            _stage = null;
        }
    }

    /**
     * Handle mouse wheel events detected in our target container.
     */
    protected function handleMouseWheel (event :MouseEvent) :void
    {
        var p :Point = new Point(event.stageX, event.stageY);
        p = _target.globalToLocal(p);

        var subtitleY :Number = p.y - _targetBounds.y;
        if (subtitleY >= 0 && subtitleY < _targetBounds.height) {
            // The delta factor is configurable per OS, and so may range from 1-3 or even
            // higher. We normalize this based on observed values so that a single click of the
            // mouse wheel always scrolls one line.
            if (_wheelFactor > Math.abs(event.delta)) {
                _wheelFactor = Math.abs(event.delta);
            }
            var newPos :int = _historyBar.scrollPosition - int(event.delta / _wheelFactor);
            // Note: the scrollPosition setter function will ensure the value is bounded by min/max
            // for setting the position of the thumb, but it does NOT bound the actual underlying
            // value. Thus, the scrollPosition can "go negative" and must climb back out again
            // before the thumb starts to move from 0.  It's retarded, and it means we have to
            // bound the value ourselves.
            newPos = Math.min(_historyBar.maxScrollPosition,
                              Math.max(_historyBar.minScrollPosition, newPos));

            // only update if changed
            if (newPos != int(_historyBar.scrollPosition)) {
                _historyBar.scrollPosition = newPos;
                // Retardedly, as of Flex v2.0.1, setting the scroll position does not dispatch a
                // scroll event, so we must fake it.
                figureCurrentHistory();
            }
        }
    }

    /**
     * Handle a resize on the container hosting the overlay.
     */
    protected function handleContainerResize (event :ResizeEvent) :void
    {
        layout(null, -1);
    }

    /**
     * React to child changes in the container, ensure the overlay
     * is the last thing visible.
     */
    protected function handleContainerPopulate (event :Event) :void
    {
        if (!_popping) {
            // goddamn flash can't keep a child at a location anymore
            popOverlayToFront();
        }
    }

    /**
     * Configure the history scrollbar size.
     */
    protected function configureHistoryBarSize () :void
    {
        if (_targetBounds != null) {
            _historyBar.height = _targetBounds.height;
            _historyBar.move(_targetBounds.x + _targetBounds.width - ScrollBar.THICKNESS + 1,
                             _targetBounds.y);
        }
    }

    /**
     * Ensure that the overlay is the top-level component in the container.
     */
    protected function popOverlayToFront () :void
    {
        _popping = true;
        try {
            _target.rawChildren.setChildIndex(_overlay, _target.rawChildren.numChildren - 1);
        } finally {
            _popping = false;
        }
    }

    /**
     * Figure out how many of the first history elements fit in our bounds such that we can set the
     * bounds on the scrollbar correctly such that the scrolling to the smallest value just barely
     * puts the first element onscreen.
     */
    protected function figureHistoryOffset () :void
    {
        if (_target == null || _targetBounds == null) {
            return;
        }

        var hsize :int = _history.size();
        var ypos :int = _targetBounds.bottom - PAD;
        var min :int = _targetBounds.y;
        for (var ii :int = 0; ii < hsize; ii++) {
            var glyph :ChatGlyph = getHistorySubtitle(ii);
            ypos -= int(glyph.height);

            // oop, we passed it, it was the last one
            if (ypos <= min) {
                _histOffset = Math.max(0, ii - 1);
                _histOffsetFinal = true;
                return;
            }

            ypos -= getHistorySubtitleSpacing(ii);
        }

        // basically, this means there isn't yet enough history to fill the first 'page' of the
        // history scrollback, so we set the offset to the max value but do not set histOffsetFinal
        // to be true so that this will be recalculated
        _histOffset = hsize - 1;
    }

    /**
     * Figure out which ChatMessages in the history should currently appear in the showing history.
     */
    protected function figureCurrentHistory () :void
    {
        var first :int = _historyBar.scrollPosition;
        var count :int = 0;
        var glyph :SubtitleGlyph;
        var ii :int;

        if (_history.size() > 0) {
            // start from the bottom...
            var ypos :int = _targetBounds.bottom - PAD;
            var min :int = _targetBounds.y;
            for (ii = first; ii >= 0; ii--, count++) {
                glyph = getHistorySubtitle(ii);

                // see if it will fit
                ypos -= int(glyph.height);
                if ((count != 0) && ypos <= min) {
                    break; // don't add that one
                }

                // position it
                glyph.x = _targetBounds.x + PAD;
                glyph.y = ypos;
                ypos -= getHistorySubtitleSpacing(ii);
            }
        }

        // finally, because we've been adding to the _showingHistory here we need to prune out the
        // ChatGlyphs that aren't actually needed and make sure the ones that are are positioned on
        // the screen correctly
        for (ii = _showingHistory.length - 1; ii >= 0; ii--) {
            glyph = (_showingHistory[ii] as SubtitleGlyph);
            var managed :Boolean = _overlay.contains(glyph);
            if (glyph.histIndex <= first && glyph.histIndex > (first - count)) {
                // it should be showing
                if (!managed) {
                    _overlay.addChild(glyph);
                }
            } else {
                // it shouldn't be showing
                if (managed) {
                    removeGlyph(glyph);
                }
                _showingHistory.splice(ii, 1);
            }
        }
    }

    /**
     * Get the subtitle for the specified history index, creating if necessary.
     */
    protected function getHistorySubtitle (index :int) :SubtitleGlyph
    {
        var glyph :SubtitleGlyph;

        // do a brute search (over a small set) for an already-created glyph
        for each (glyph in _showingHistory) {
            if (glyph.histIndex == index) {
                return glyph;
            }
        }

        // it looks like we've got to create a new one
        glyph = createHistorySubtitle(index);
        glyph.histIndex = index;
        _showingHistory.push(glyph);
        return glyph;
    }

    /**
     * Create a new subtitle for use in history.
     */
    protected function createHistorySubtitle (index :int) :SubtitleGlyph
    {
        var msg :ChatMessage = _history.get(index);
        return createSubtitle(msg, getType(msg, true), false);
    }

    internal function getTargetTextWidth () :int
    {
        var w :int = _targetBounds.width;
        if (_historyBar != null) {
            w -= ScrollBar.THICKNESS;
        }
        // there is PAD between the text and the edges of the bubble, and another PAD between the
        // bubble and the container edges, on each side for a total of 4 pads.
        w -= (PAD * 4);
        return w;
    }

    /** The light of our life. */
    protected var _ctx :WorldContext;

    /** The overlay we place on top of our target that contains all the chat glyphs. */
    protected var _overlay :Sprite;

    /** The target container over which we're overlaying chat. */
    protected var _target :Container;

    /** The region of our target over which we render. */
    protected var _targetBounds :Rectangle;

    /** The stage of our target, while tracking mouseWheel in history mode. */
    protected var _stage :Stage;

    /** The currently displayed list of subtitles. */
    protected var _subtitles :Array = [];

    /** The currently displayed subtitles in history mode. */
    protected var _showingHistory :Array = [];

    /** The percent of the bottom of the screen to use for subtitles. */
    protected var _subtitlePercentage :Number = 1;

    /** The history offset (from 0) such that the history lines (0, _histOffset - 1) will all fit
     * onscreen if the lowest scrollbar positon is _histOffset. */
    protected var _histOffset :int = 0;

    /** True if the histOffset does need to be recalculated. */
    protected var _histOffsetFinal :Boolean = false;

    /** A guess of how many history lines fit onscreen at a time. */
    protected var _historyExtent :int;

    /** The unbounded expire time of the last chat glyph displayed. */
    protected var _lastExpire :int;

    /** The default text format to be applied to subtitles. */
    protected var _defaultFmt :TextFormat;

    /** The format for user-entered text. */
    protected var _userSpeakFmt :TextFormat;

    /** The history scrollbar. */
    protected var _historyBar :VScrollBar;

    /** The smallest absolute value seen for delta in a mouse wheel event. */
    protected var _wheelFactor :int = int.MAX_VALUE;

    /** True while we're setting the position on the scrollbar, so that we
     * know to ignore the event. */
    protected var _settingBar :Boolean = false;

    /** True while popping the overlay to the front. */
    protected var _popping :Boolean = false;

    /* The history used by this overlay. */
    protected var _history :HistoryList;

    /** Used to guess at the 'page size' for the scrollbar. */
    protected static const SUBTITLE_HEIGHT_GUESS :int = 26;

    /**
     * Times to display chat.
     * { (time per character), (min time), (max time) }
     *
     * Groups 0/1/2 are short/medium/long for chat bubbles,
     * and groups 1/2/3 are short/medium/long for subtitles.
     */
    protected static const DISPLAY_DURATION_PARAMS :Array = [
        [ 125, 10000, 30000 ],
        [ 200, 15000, 40000 ],
        [ 275, 20000, 50000 ],
        [ 350, 25000, 60000 ]
    ];

    /** Type mode code for default chat type (speaking). */
    protected static const SPEAK :int = 0;

    /** Type mode code for shout chat type. */
    protected static const SHOUT :int = 1;

    /** Type mode code for emote chat type. */
    protected static const EMOTE :int = 2;

    /** Type mode code for think chat type. */
    protected static const THINK :int = 3;

    /** Type place code for default place chat (cluster, scene). */
    protected static const PLACE :int = 1 << 4;

    /** Our internal code for tell chat. */
    protected static const TELL :int = 2 << 4;
    
    /** Our internal code for tell feedback chat. */
    protected static const TELLFEEDBACK :int = 3 << 4;
    
    /** Our internal code for info system messges. */
    protected static const INFO :int = 4 << 4;
    
    /** Our internal code for feedback system messages. */
    protected static const FEEDBACK :int = 5 << 4;

    /** Our internal code for attention system messages. */
    protected static const ATTENTION :int = 6 << 4;

    /** Type place code for broadcast chat type. */
    protected static const BROADCAST :int = 7 << 4;

    /** Type code for a chat type that was used in some special context,
     * like in a negotiation. */
    protected static const SPECIALIZED :int = 8 << 4;

    /** Our internal code for any type of chat that is continued in a
     * subtitle. */
    protected static const CONTINUATION :int = 9 << 4;

    /** Type code for game chat. */
    protected static const GAME :int = 10 << 4;

    /** Our internal code for a chat type we will ignore. */
    protected static const IGNORECHAT :int = -1;

    /** Pixel padding surrounding most things. */
    public static const PAD :int = 10;

    // used to color chat bubbles
    protected static const BROADCAST_COLOR :uint = 0x990000;
    protected static const FEEDBACK_COLOR :uint = 0x00AA00;
    protected static const TELL_COLOR :uint = 0x0000AA;
    protected static const TELLFEEDBACK_COLOR :uint = 0x00AAAA;
    protected static const INFO_COLOR :uint = 0xAAAA00;
    protected static const ATTENTION_COLOR :uint = 0xFF5000;
    protected static const GAME_COLOR :uint = 0x777777;
    protected static const BLACK :uint = 0x000000;
    protected static const WHITE :uint = 0xFFFFFF;

    /** The font for all chat. */
    protected static const FONT :String = "Arial";

    /** The normal alpha value for bubbles on the overlay. */
    protected static const ALPHA :Number = .8;
}
}
