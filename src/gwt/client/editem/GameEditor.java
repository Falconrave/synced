//
// $Id$

package client.editem;

import java.util.List;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.NumberTextBox;
import client.util.InfoCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.all.GameGenre;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

/**
 * A class for creating and editing {@link Game} digital items.
 */
public class GameEditor extends ItemEditor
{
    /** Constants from com.threerings.parlor.game.data.GameConfig. These can't be imported directly
     * since this is gwt code. The names are the same in order to increase coupling and hence the
     * likelihood that they will show up in a search.  */
    public static String SEATED_GAME = "0";
    public static String SEATED_CONTINUOUS = "1";
    public static String PARTY = "2";

    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Game)item;
        setUploaderMedia(Item.MAIN_MEDIA, _game.gameMedia);
        setUploaderMedia(Item.AUX_MEDIA, _game.shotMedia);
        setUploaderMedia(Game.SPLASH_MEDIA, _game.splashMedia);
        setUploaderMedia(Game.SERVER_CODE_MEDIA, _game.serverMedia);

        // fetch the list of whirleds this player is a manager of which don't have a game
        _groupsvc.getGameGroups(_game.gameId, new InfoCallback<List<GroupMembership>>() {
            public void onSuccess (List<GroupMembership> whirleds)
            {
                setWhirleds(whirleds);
            }
        });

        // configure our shop tag
        _shopTag.setText(_game.shopTag);

        // if we have no game configuration, leave everything as default
        if (_game.config == null || _game.config.length() == 0) {
            return;
        }

        // configure our genre
        for (int ii = 0; ii < GameGenre.GENRES.length; ii++) {
            if (GameGenre.GENRES[ii] == _game.genre) {
                _genre.setSelectedIndex(ii);
                break;
            }
        }

        // read our configuration information out of the game's XML config data
        Document xml;
        try {
            xml = XMLParser.parse(_game.config);
        } catch (DOMException de) {
            CShell.log("XML Parse Failed", de);
            return; // leave everything at defaults
        }

        NodeList matches = xml.getElementsByTagName("match");
        if (matches.getLength() > 0) {
            Element match = (Element)matches.item(0);
            Node option = match.getFirstChild();
            // TODO <start_seats>, also game_type might be merged with the "type" attributed on
            // <match> - right now it merely refers to which type of table game we're playing
            while (option != null) {
                if (option.getNodeType() == Node.ELEMENT_NODE) {
                    final String name = option.getNodeName();
                    if ("min_seats".equals(name)) {
                        _minPlayers.setNumber(Integer.valueOf(option.getFirstChild().toString()));
                    } else if ("max_seats".equals(name)) {
                        _maxPlayers.setNumber(Integer.valueOf(option.getFirstChild().toString()));
                    } else if ("unwatchable".equals(name)) {
                        _watchable.setValue(false);
                    }
                }
                option = option.getNextSibling();
            }
            if (match.hasAttribute("type")) {
                // this will be more sensible when SEATED_CONTINUOUS is re-instated as a game type
                for (GameType gtype : GameType.values()) {
                    if (match.getAttribute("type").equals(gtype.getMatchType())) {
                        _gameType.setSelectedIndex(gtype.ordinal());
                    }
                }
            }
        }

        // avrg overrides the match element, it is not a real match type
        if (xml.getElementsByTagName("avrg").getLength() > 0) {
            _gameType.setSelectedIndex(GameType.AVRG.ordinal());
        }

        // configure our noprogress checkbox
        _noprogress.setValue(xml.getElementsByTagName("noprogress").getLength() > 0);

        // disable min, max, watchable, auto 1p for non-seated games
        if (_gameType.getSelectedIndex() != GameType.SEATED.ordinal()) {
            _minPlayers.setEnabled(false);
            _maxPlayers.setEnabled(false);
            _watchable.setEnabled(false);
        }

        NodeList params = xml.getElementsByTagName("params");
        if (params.getLength() > 0) {
            Element param = (Element)params.item(0);
            Node child = param.getFirstChild();
            String childrenText = "";
            while (child != null) {
                // TODO make this create spiffy widgets for editing these parameters, rather than
                // the XML
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    childrenText += child + "\n";
                }
                child = child.getNextSibling();
            }
            _extras.setText(childrenText);
        }

        Object[] bits = { "serverclass", _serverClass };
        for (int ii = 0; ii < bits.length; ii += 2) {
            NodeList elems = xml.getElementsByTagName((String)bits[ii]);
            if (elems.getLength() > 0) {
                Element elem = (Element)elems.item(0);
                ((TextBox)bits[ii+1]).setText(elem.getFirstChild().toString());
            }
        }
        _serverMPOnly.setValue(xml.getElementsByTagName("agentmponly").getLength() > 0);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        Game game = new Game();
        game.config = "";
        return game;
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        _genre = new ListBox();
        for (byte genre : GameGenre.GENRES) {
            _genre.addItem(_dmsgs.xlate("genre" + genre));
        }
        addRow(_emsgs.gameGenre(), _genre);
        addSpacer();

        addTab(_emsgs.gameTabConfig());

        addRow(_emsgs.gameGameType(), bind(_gameType = new ListBox(), new Binder() {
            @Override public void valueChanged () {
                boolean isSeated = (_gameType.getSelectedIndex() == GameType.SEATED.ordinal());
                boolean wasSeated = _minPlayers.isEnabled();
                if (isSeated == wasSeated) {
                    return; // don't re-overwrite the stored values
                }
                if (!isSeated) {
                    _oldMin = _minPlayers.getNumber().intValue();
                    _minPlayers.setNumber(1);
                    _oldMax = _maxPlayers.getNumber().intValue();
                    _maxPlayers.setNumber(99);
                    _oldWatch = _watchable.getValue();
                    _watchable.setValue(true);
                } else {
                    _minPlayers.setNumber(_oldMin);
                    _maxPlayers.setNumber(_oldMax);
                    _watchable.setValue(_oldWatch);
                }
                // TODO: it would be nicer to just hide these
                _minPlayers.setEnabled(isSeated);
                _maxPlayers.setEnabled(isSeated);
                _watchable.setEnabled(isSeated);
            }
            protected int _oldMin = 1, _oldMax = 1;
            protected boolean _oldWatch;
        }));

        for (GameType gtype : GameType.values()) {
            _gameType.addItem(gtype.getText());
        }

        addRow(_emsgs.gameMinPlayers(), _minPlayers = new NumberTextBox(false, 5));
        _minPlayers.setNumber(1);
        addRow(_emsgs.gameMaxPlayers(), _maxPlayers = new NumberTextBox(false, 5));
        _maxPlayers.setNumber(1);
        addRow(_emsgs.gameWatchable(), _watchable = new CheckBox());
        _watchable.setValue(true);

        addSpacer();
        addRow(_emsgs.gameDefinition(), _extras = new TextArea());
        _extras.setCharacterWidth(55);
        _extras.setVisibleLines(5);

        addTab(_emsgs.gameTabCode());

        // add a UI for uploading the game client and server code
        addRow(_emsgs.gameLabel(), createMainUploader(TYPE_SWF, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidGameMedia(desc)) {
                    return _emsgs.errGameNotFlash();
                }
                _game.gameMedia = desc;
                return null;
            }
            public void clearMedia () {
                _game.gameMedia = null;
            }
        }), _emsgs.gameTip());

        addSpacer();
        MediaUpdater serverMediaUpdater = new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidServerAgentMedia(desc)) {
                    return _emsgs.errMediaNotABC();
                }
                _game.serverMedia = desc;
                return null;
            }
            public void clearMedia () {
                _game.serverMedia = null;
            }
        };
        ItemMediaUploader serverMediaUploader = createUploader(
            Game.SERVER_CODE_MEDIA, TYPE_ABC, ItemMediaUploader.MODE_NORMAL, serverMediaUpdater);
        addRow(_emsgs.gameServerMediaLabel(), serverMediaUploader, _emsgs.gameServerMediaTip());
        addRow(_emsgs.gameServerClass(), _serverClass = new TextBox());
        addTip(_emsgs.gameServerClassTip());
        _serverClass.setVisibleLength(40);
        addRow(_emsgs.gameServerMPOnly(), _serverMPOnly = new CheckBox());

        addSpacer();

        addTab(_emsgs.gameTabMedia());

        // add a tab for uploading the game screenshot and splash bitmap

        ItemMediaUploader shotter = addImageUploader(
            Item.AUX_MEDIA, MediaDesc.GAME_SHOT_SIZE, ItemMediaUploader.MODE_GAME_SHOT,
            new MediaSetter() {
                public void set (MediaDesc media) {
                    _game.shotMedia = media;
                };
            });
        addRow(_emsgs.gameShotLabel(), shotter, _emsgs.gameShotTip());

        super.addExtras();

        // splash screen goes below the standard extras
        addSpacer();
        ItemMediaUploader splasher = addImageUploader(
            Game.SPLASH_MEDIA, MediaDesc.GAME_SPLASH_SIZE, ItemMediaUploader.MODE_GAME_SPLASH,
            new MediaSetter() {
                public void set (MediaDesc media) {
                    _game.splashMedia = media;
                };
            });
        addRow(_emsgs.gameSplashLabel(), splasher, _emsgs.gameSplashTip());

        addTab(_emsgs.gameTabExtras());

        // a UI for selecting this game's associated whirled
        _whirled = new ListBox();
        addRow(_emsgs.gameWhirledLabel(), _whirled);
        addTip(_emsgs.gameWhirledTip());

        // allow them to specify their shop tag
        addSpacer();
        addRow(_emsgs.gameShopTag(), _shopTag = new TextBox());
        addTip(_emsgs.gameShopTagTip());

        // add a toggle for disabling loading progress
        addSpacer();
        addRow(_emsgs.gameNoProgress(), _noprogress = new CheckBox());
        _noprogress.setValue(false);
        addTip(_emsgs.gameNoProgressTip());
    }

    /** Adds and configures an image uploader widget. */
    protected ItemMediaUploader addImageUploader (
        String mediaIds, int mediaSize, int uploaderMode, final MediaSetter setterFn)
    {
        final int mediawidth = MediaDesc.getWidth(mediaSize);
        final int mediaheight = MediaDesc.getHeight(mediaSize);
        final ItemMediaUploader[] fuploader = new ItemMediaUploader[1]; // filthy hack
        ItemMediaUploader uploader = createUploader(
            mediaIds, TYPE_IMAGE, uploaderMode, new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    if (!desc.isImage()) {
                        return _emsgs.errInvalidShot(""+mediawidth, ""+mediaheight);
                    }
                    if (width != mediawidth || height != mediaheight) {
                        fuploader[0].openImageEditor(desc, false);
                        return SUPPRESS_ERROR;
                    }
                    setterFn.set(desc);
                    return null;
                }
                public void clearMedia () {
                    setterFn.set(null);
                }
            });
        fuploader[0] = uploader;

        uploader.setHint(_emsgs.gameImageHint(""+mediawidth, ""+mediaheight));

        return uploader;
    }

    /**
     * Populate the list of eligible whirleds and select the correct one for this game.
     */
    protected void setWhirleds (List<GroupMembership> whirleds)
    {
        _whirled.addItem(_emsgs.gameWhirledNone(), Game.NO_GROUP+"");
        for (GroupMembership whirled : whirleds) {
            _whirled.addItem(whirled.group.toString(), whirled.group.getGroupId()+"");
        }

        // default to the first item if creating a new whirled
        if (_game.itemId == 0) {
            return;
        }
        for (int ii = 0; ii < _whirled.getItemCount(); ii++) {
            if (_whirled.getValue(ii).equals(_game.groupId+"")) {
                _whirled.setSelectedIndex(ii);
                break;
            }
        }
    }

    @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        // configure our genre
        _game.genre = GameGenre.GENRES[_genre.getSelectedIndex()];

        // convert our configuration information back to an XML document
        Document xml = XMLParser.createDocument();
        xml.appendChild(xml.createElement("game"));

        // Set the game type
        // NOTE that AVRG games get set to type = PARTY, but it makes no difference
        Element match = xml.createElement("match");
        GameType gameType = GameType.values()[_gameType.getSelectedIndex()];
        String type = gameType.getMatchType();
        if (type == null) {
            type = PARTY;
        }
        match.setAttribute("type", type);
        xml.getFirstChild().appendChild(match);

        Element minSeats = xml.createElement("min_seats");
        minSeats.appendChild(xml.createTextNode(_minPlayers.getNumber().toString()));
        match.appendChild(minSeats);
        Element maxSeats = xml.createElement("max_seats");
        maxSeats.appendChild(xml.createTextNode(_maxPlayers.getNumber().toString()));
        match.appendChild(maxSeats);
        if (!_watchable.getValue()) {
            match.appendChild(xml.createElement("unwatchable"));
        }

        Object[] bits = { "serverclass", _serverClass };
        for (int ii = 0; ii < bits.length; ii += 2) {
            String text = ((TextBox)bits[ii+1]).getText();
            if (text.length() > 0) {
                Element elem = xml.createElement((String)bits[ii]);
                elem.appendChild(xml.createTextNode(text));
                xml.getFirstChild().appendChild(elem);
            }
        }

        // add some boolean bits
        if (gameType == GameType.AVRG) {
            // this is really what makes an avrg do its special thing
            xml.getFirstChild().appendChild(xml.createElement("avrg"));
        }
        if (_noprogress.getValue()) {
            xml.getFirstChild().appendChild(xml.createElement("noprogress"));
        }
        if (_serverMPOnly.getValue()) {
            xml.getFirstChild().appendChild(xml.createElement("agentmponly"));
        }

        // show a notice that a new Whirled will be created
        _game.groupId = new Integer(_whirled.getValue(_whirled.getSelectedIndex()));

        // configure our shop tag
        String shopTag = _shopTag.getText().trim();
        if (shopTag.equals("")) {
            shopTag = null;
        }
        _game.shopTag = shopTag;

        String extras = _extras.getText();
        if (extras.length() > 0) {
            try {
                Element pelem = xml.createElement("params");
                // need a valid document (single child element) for parsing to work
                Document params = XMLParser.parse("<params>" + extras + "</params>");
                if (params.getFirstChild() != null && params.getFirstChild().hasChildNodes()) {
                    Node param = params.getFirstChild().getFirstChild();
                    while (param != null) {
                        // only support elements as children of <params> - this strips out
                        // whitespace and comments and random bits of text
                        if (param.getNodeType() == Node.ELEMENT_NODE) {
                            pelem.appendChild(param.cloneNode(true));
                        }
                        param = param.getNextSibling();
                    }
                }
                if (pelem.getFirstChild() != null) {
                    xml.getFirstChild().appendChild(pelem);
                }

            } catch (DOMException de) {
                throw new Exception(_emsgs.gameDefinitionError(de.getMessage()));
            }
        }

        _game.config = xml.toString();

        if (gameType == GameType.AVRG && _game.serverMedia == null) {
            throw new Exception(_emsgs.errServerMediaRequired());
        }
    }

    protected static void setOnlyChild (Node parent, Node child)
    {
        while (parent.hasChildNodes()) {
            parent.removeChild(parent.getFirstChild());
        }
        parent.appendChild(child);
    }

    /** Is the specified MediaDesc a valid game media? */
    protected boolean isValidGameMedia (MediaDesc desc)
    {
        // game media must be swfs. maybe we'll want remixable in the future?
        return desc.isSWF();
    }

    /** Checks mime type for use as a server agent. */
    protected boolean isValidServerAgentMedia (MediaDesc desc)
    {
        return desc.mimeType == MediaDesc.COMPILED_ACTIONSCRIPT_LIBRARY;
    }

    // this is for populating the game type list box. AVRG is not analogous to a "match" type but
    // is most appropriately treated as a type of game.
    protected enum GameType
    {
        // seated continuous games are disabled for now
        SEATED("gameType0", SEATED_GAME),
        PARTY("gameType2", GameEditor.PARTY),
        //SEATED_CONT("gameType1", SEATED_CONTINUOUS),
        AVRG("gameType3", null);

        GameType (String lookup, String matchType) {
            _lookup = lookup;
            _matchType = matchType;
        }

        public String getText () {
            return _dmsgs.xlate(_lookup);
        }

        public String getMatchType () {
            return _matchType;
        }

        protected String _lookup;
        protected String _matchType;
    };

    /** Interface for functions that set various game media. */
    // my kingdom for function pointers! :)
    protected interface MediaSetter {
        public void set (MediaDesc media);
    }

    protected Game _game;

    protected ListBox _genre;
    protected NumberTextBox _minPlayers, _maxPlayers;
    protected ListBox _gameType;
    protected CheckBox _watchable;
    protected CheckBox _noprogress;
    protected TextBox _serverClass;
    protected CheckBox _serverMPOnly;
    protected TextArea _extras;
    protected ListBox _whirled;
    protected TextBox _shopTag;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    // connection to fetching list of the player's Whirleds
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
