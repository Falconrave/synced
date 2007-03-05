package {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.net.*;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.ui.*;
import flash.utils.*;
import flash.external.ExternalInterface;

import com.threerings.msoy.hood.*;
import com.threerings.util.DateUtil;
import com.threerings.util.NetUtil;
import com.threerings.util.Random;
import com.threerings.util.StringUtil;

import com.threerings.flash.DisplayUtil;

import com.adobe.serialization.json.JSONDecoder;

[SWF(width="640", height="480", backgroundColor=0xCBFE98)]
public class HoodViz extends Sprite
{
    public static const SWF_WIDTH :uint = 640;
    public static const SWF_HEIGHT :uint = 480;

    public function HoodViz ()
    {
        var data :Object;
        if (false) {
            data = new DebugData();
        } else {
            data = this.root.loaderInfo.parameters;
        }
        _hood = Neighborhood.fromParameters(data);

        var seed :int = 0;
        if (_hood.centralMember != null) {
            seed = _hood.centralMember.memberId;
        } else if (_hood.centralGroup != null) {
            seed = _hood.centralGroup.groupId;
        }
        _random = new Random(seed);

        _loader = new Loader();
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, loaderDone);
        var context :LoaderContext = new LoaderContext();
        context.applicationDomain = ApplicationDomain.currentDomain;
        _loader.load(new URLRequest(data.skinURL), context);
    }

    protected function getClass(name :String) :Class
    {
        return getDefinitionByName(name) as Class;
    }

    protected function loaderDone(event :Event) :void
    {
        var soy :Class = getClass("soy_master");

        _house = new Building(getClass("house_tile"), getClass("populate_house"), soy);
        stage.addChild(_house);
        _plaqueHouse = getClass("plaque_house");

        _group = new Building(getClass("group_tile"), getClass("populate_group"), soy);
        stage.addChild(_group);
        _plaqueGroup = getClass("plaque_group");

        _game = new Building(getClass("game_tile"), getClass("populate_game"), soy);
        stage.addChild(_game);
        _plaqueGame = getClass("plaque_game");

        _vacant = getClass("vacant_tile");
        _roadHouse = getClass("road_house_tile");
        _roadNS = getClass("road_ns_tile");
        _roadEW = getClass("road_ew_tile");
        _roadNSE = getClass("road_nse_tile");
        _roadNSW = getClass("road_nsw_tile");
        _road4Way = getClass("road_nsew_tile");
        _roadHouseEndW = getClass("road_end_w_tile");
        _roadHouseEndE = getClass("road_end_e_tile");

        _rule = getClass("rule");

        _canvas = new Sprite();
        this.addChild(_canvas);

        // compute a very rough bounding rectangle for the visible houses
        var radius :int =
            3 + Math.ceil(Math.sqrt(_hood.groups.length +
                                    _hood.houses.length +
                                    _hood.games.length));

        var drawables :Array = new Array();
        var distances :Array = new Array();
        // draw the grid, building a metric mapping at the same time
        for (var y :int = -radius-1; y <= radius; y ++) {
            drawables[y] = new Array();
            for (var x :int = radius; x >= -radius; x --) {
                if (x == 0) {
                    continue;
                }
                if (y == 0 && ((_hood.centralMember != null && x == 1) ||
                               (_hood.centralGroup != null && x == -1))) {
                    continue;
                }
                if ((y % 2) == 0) {
                    var d :Number = x*x + y*y;
                    distances.push({ x: x, y: y, dist: d });
                }
            }
        }

        // sort the metric according to distance
        distances.sortOn([ "dist", "x", "y" ], Array.NUMERIC);

        // then go through buildings in order of radial distance and register houses and groups
        // pick tiles randomly from weighted intervals - generalizes to N tile types
        var housesLeft :int = _hood.houses.length;
        var groupsLeft :int = _hood.groups.length;
        var gamesLeft :int = _hood.games.length;
        var totalLeft :int = housesLeft + groupsLeft + gamesLeft;
        while (totalLeft > 0) {
            // pick a spot within [0, totalLeft)
            var rnd :Number = totalLeft * _random.nextNumber();
            // grab the tile to place
            var tile :Object = distances[--totalLeft];
            // and figure out which tile type's interval the spot's in
            if (rnd < groupsLeft) {
                drawables[tile.y][tile.x] = _hood.groups[--groupsLeft];
            } else if (rnd - groupsLeft < housesLeft) {
                drawables[tile.y][tile.x] = _hood.houses[--housesLeft];
            } else {
                drawables[tile.y][tile.x] = _hood.games[--gamesLeft];
            }
        }

        if (_hood.centralMember != null) {
            drawables[0][1] = _hood.centralMember;
        }
        if (_hood.centralGroup != null) {
            drawables[0][-1] = _hood.centralGroup;
        }

        for (y = -radius; y <= radius; y ++) {
            for (x = radius; x >= -radius; x --) {
                if ((y % 2) == 0) {
                    if (x == 0) {
                        addBit(_roadNS, 0, y, false, null);
                    } else if (drawables[y][x] is NeighborMember) {
                        addBit(_house, x, y, true, drawables[y][x]);
                    } else if (drawables[y][x] is NeighborGroup) {
                        addBit(_group, x, y, true, drawables[y][x]);
                    } else if (drawables[y][x] is NeighborGame) {
                        addBit(_game, x, y, true, drawables[y][x]);
                    } else {
                        addBit(_vacant, x, y, false, null);
                    }
                } else {
                    if (x == 0) {
                        if (drawables[y-1][-1] == null) {
                            if (drawables[y-1][1] == null) {
                                addBit(_roadNS, 0, y, false, null);
                            } else {
                                addBit(_roadNSE, 0, y, false, null);
                            }
                        } else if (drawables[y-1][1] == null) {
                            addBit(_roadNSW, 0, y, false, null);
                        } else {
                            addBit(_road4Way, 0, y, false, null);
                        }
                    } else if (drawables[y-1][x] == null) {
//                        this bit has no purpose until we interject empty plots
//                        addBit(_roadEW, x, y, false, null);
                        addBit(_vacant, x, y, false, null);
                    } else if (x != 1 && drawables[y-1][x-1] == null) {
                        addBit(_roadHouseEndW, x, y, true, null);
                    } else if (x != -1 && drawables[y-1][x+1] == null) {
                        addBit(_roadHouseEndE, x, y, true, null);
                    } else {
                        addBit(_roadHouse, x, y, true, null);
                    }
                }
            }
        }

        if (_hood.totalPop >= 0) {
            var field :TextField = new TextField();
            field.text = "Number of people in the Whirled: " + _hood.totalPop;
            field.autoSize = TextFieldAutoSize.CENTER;
            field.background = true;
            field.backgroundColor = 0x8A360F; // burnt sienna
            field.textColor = 0xFFCC11; // mustard
            field.wordWrap = false;

            stage.addChild(field);
        }


        // figure a canvas scale that'll safely display all that was actually drawn
        var scale :Number = Math.min(SWF_WIDTH / (160 + _bound.width),
                                     SWF_HEIGHT / (120 + _bound.height));
        _canvas.scaleX = _canvas.scaleY = scale;

        // constants to encode any visual bias inside each tile, i.e. our house road
        // tiles have ~50 pixels of emptiness underneath them that should adjusted for
        // for a visually pleasant centering -- not sure how to make this skinnable
        const xBiasInTiles :Number = 0;
        const yBiasInTiles :Number = -50;
        // and center the canvas in the SWF, tweaked by any imbalance in tile placement
        _canvas.x = (SWF_WIDTH -scale*(xBiasInTiles + _bound.right + _bound.left))/2;
        _canvas.y = (SWF_HEIGHT -scale*(yBiasInTiles + _bound.top + _bound.bottom))/2;
    }


    protected function addBit (bitType :Object, x :Number, y :Number, update:Boolean,
                               neighbor: Neighbor) :void
    {
        var bit :MovieClip;
        if (bitType is Class) {
            bit = new bitType();
            bit.gotoAndStop(1+_random.nextInt(bit.totalFrames));
        } else {
            var building :Building = (bitType as Building);
            // let's illustrate the population of a place by the square root of its
            // actual population in soy figures, lest we utterly overwhelm the map;
            // thus 1 pop -> 1 soy, 25 pop -> 5 soys, 100 pop -> 10 soys.
            bit = building.getPopulatedTile(
                1+_random.nextInt(building.variationCount),
                Math.round(Math.sqrt(neighbor.population)));
        }

        if (neighbor is LogoHolder) {
            var logo :String = (neighbor as LogoHolder).getLogoHash();
            if (logo != null) {
                // if there is a logo, we dynamically load it
                var loader :Loader = new Loader();
                // first, find the designated logo-holding area in the tile
                var logoHolder :MovieClip = bit.getChildByName("logo_placer") as MovieClip;

                // if we did find a logoHolder, load the logo into it
                if (logoHolder != null) {
                    logoHolder.addChild(loader);
                    // set the scale to zero so that a large image doesn't resize the holder
                    loader.scaleX = loader.scaleY = 0;
                    // by default the image loaders in the center; shift it up and left
                    loader.x = -logoHolder.width/2;
                    loader.y = -logoHolder.height/2;

                    // we want to know when the logo is loaded so we can do resize magic
                    loader.contentLoaderInfo.addEventListener(Event.COMPLETE, logoLoaded);
                    // and we'll swallow IO errors rather than burden the user with them
                    loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, logoError);
                    loader.load(new URLRequest("/media/" + logo));
                }
            }
        }

        var bitHolder :ToolTipSprite = new ToolTipSprite();
        bitHolder.addChild(bit);
        var p :Point = skew(x, y);
        bitHolder.x = p.x;
        bitHolder.y = p.y;
        if (neighbor != null) {
            bitHolder.neighbor = neighbor;
            bitHolder.addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
            bitHolder.addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
            bitHolder.addEventListener(MouseEvent.CLICK, clickHandler);
        }
        _canvas.addChild(bitHolder);

        if (update) {
            _bound = _bound.union(bitHolder.getBounds(_canvas));
        }
    }

    // scales the loaded logo to the dimensions of the logo holder
    protected function logoLoaded(event: Event) :void
    {
        // get references to the loaded image, the image loader, and the logo holding clip
        var content: DisplayObject = event.target.content;
        var loader :DisplayObjectContainer = content.parent;
        var holder :DisplayObjectContainer = loader.parent;
        // now scale the image depending on which dimension is constrained
        var scale :Number = Math.min(holder.width / content.width, holder.height/content.height);
        // and center in either the x or y direction as needed
        content.x = (holder.width - scale*content.width)/2;
        content.y = (holder.height - scale*content.height)/2;
        // reset the loader's scale (it was set to zero during loading)
        loader.scaleX = loader.scaleY = 1;
        // and finally apply the image scale
        content.scaleX = content.scaleY = scale;
    }

    // the magic numbers that describe the drawn tiles' geometry
    protected function skew(x :Number, y :Number) :Point
    {
        return new Point(x*153 + y*71, -x*61 + y*136);
    }

    protected function clickHandler (event :MouseEvent) :void
    {
        var neighbor :Neighbor = (event.currentTarget as ToolTipSprite).neighbor;
        var url :String = "/world/#";
        if (neighbor.sceneId > 0) {
            url = "/world/#s" + neighbor.sceneId;
        } else if (neighbor is NeighborMember) {
            var house :NeighborMember = neighbor as NeighborMember;
            // clicking on a member takes us to their home scene
            url = "/world/#m" + house.memberId;
        } else if (neighbor is NeighborGroup) {
            var group :NeighborGroup = neighbor as NeighborGroup;
            // clicking on a group takes us to that group's home scene
            url = "/world/#g" + group.groupId;
        } else {
            var game :NeighborGame = neighbor as NeighborGame;
            // clicking on a game takes us to that game's lobby
            url = "/game/#" + game.gameId;
        }
        NetUtil.navigateToURL(url);
    }

    protected function logoError (event :IOErrorEvent) :void
    {
        trace("Error loading URL: " + event.text);
        // do nothing else
    }

    protected function rollOverHandler (event :MouseEvent) :void
    {
        _tipTile = event.target as ToolTipSprite;
        var neighbor :Neighbor = _tipTile.neighbor;
        var tipContent :Sprite = new Sprite();
        var tip :DisplayObjectContainer;
        var rule :Sprite;
        var obj :DisplayObject;
        var tipHeight :Number = 0;
        if (neighbor is NeighborMember) {
            var house :NeighborMember = neighbor as NeighborMember;

            tip = new _plaqueHouse();

            obj = getTextField(house.memberName, true);
            obj.y = tipHeight; tipHeight += obj.height;
            tipContent.addChild(obj);

            var str :String;
            if (house.isOnline) {
                str = "Online";
            } else if (house.lastSession != null) {
                str = "Last on: " + DateUtil.getConversationalDateString(house.lastSession);
            }
            if (str != null) {
                rule = new _rule();
                rule.y = 10 + tipHeight; tipHeight += 10 + rule.height;
                tipContent.addChild(rule);

                obj = getTextField(str);
                obj.y = 10 + tipHeight; tipHeight += obj.height;
                tipContent.addChild(obj);
            }

        } else if (neighbor is NeighborGroup) {
            var group :NeighborGroup = neighbor as NeighborGroup;

            tip = new _plaqueGroup();

            if (group.getLogoHash() != null) {
                obj = getTextField(group.groupName, true);
                obj.y = tipHeight; tipHeight += obj.height;
                tipContent.addChild(obj);

                // if there is a logo, we dynamically load it
                var loader :Loader = new Loader();

                // we want to know when the logo is loaded so we can do resize magic
                loader.contentLoaderInfo.addEventListener(Event.COMPLETE, popupLogoLoaded);
                // and we'll swallow IO errors rather than burden the user with them
                loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, logoError);

                loader.load(new URLRequest("/media/" + group.getLogoHash()));

                rule = new _rule();
                rule.y = 10 + tipHeight; tipHeight += 10 + rule.height;
                tipContent.addChild(rule);

                tipContent.addChild(loader);
                loader.y = 10 + tipHeight; tipHeight += 40;
            } else {
                obj = getTextField(group.groupName, true);
                obj.y = tipHeight; tipHeight += obj.height;
                tipContent.addChild(obj);
            }
        } else {
            var game :NeighborGame = neighbor as NeighborGame;

            tip = new _plaqueGame();
            obj = getTextField(game.gameName, true);
            obj.y = tipHeight; tipHeight += obj.height;
            tipContent.addChild(obj);

            // TODO: thumbnail?
        }

        _tip = new ToolTipSprite();
        _tip.neighbor = neighbor;

        _tip.addEventListener(MouseEvent.ROLL_OUT, popupRollOutHandler);
        _tip.addEventListener(MouseEvent.CLICK, clickHandler);
        _tip.mouseChildren = false;

        tip.height = 80 + Math.max(tipHeight, 20);
        _tip.addChild(tip);
        tipContent.y = -tip.height/2 - tipHeight/2;
        _tip.addChild(tipContent);

        this.addChild(_tip);

        _tip.x = event.stageX - 20;
        _tip.y = event.stageY - 20;

        var tipBounds :Rectangle = _tip.getBounds(this);

        if (tipBounds.left < 0) {
            _tip.x -= tipBounds.left;
        } else if (tipBounds.x > SWF_WIDTH) {
            _tip.x -= (tipBounds.right - SWF_WIDTH);
        }
        if (tipBounds.top < 0) {
            _tip.y -= tipBounds.top;
        } else if (tipBounds.bottom > SWF_HEIGHT) {
            _tip.y -= (tipBounds.bottom - SWF_HEIGHT);
        }
    }

    protected function getTextField (text :String, bold :Boolean = false) :TextField
    {
        var format :TextFormat = new TextFormat();
        format.align = TextFormatAlign.CENTER;
//        format.font = "Arial";
        format.bold = bold;

        var tipText :TextField = new TextField();
        tipText.defaultTextFormat = format;
        tipText.text = text;
        tipText.autoSize = TextFieldAutoSize.CENTER;
        tipText.wordWrap = true;
        tipText.width = 100;
        tipText.x = -tipText.width/2;
        return tipText;
    }


    // scales the loaded logo to the dimensions of the popup bottom
    protected function popupLogoLoaded(event: Event) :void
    {
        // get references to the loaded image, the image loader, and the logo holding clip
        var content: DisplayObject = event.target.content;
        var loader :DisplayObjectContainer = content.parent;
        // now scale the image depending on which dimension is constrained
        var scale :Number = Math.min(70/content.width, 40/content.height);

        // and center in either the x or y direction as needed
//        content.x = (holder.width - scale*content.width)/2;
//        content.y = (holder.height - scale*content.height)/2;
        // and finally apply the image scale
        content.scaleX = content.scaleY = scale;
        loader.x = -content.width / 2;
    }

    protected function rollOutHandler (event :MouseEvent) :void
    {
        if (_tip is Sprite && event.relatedObject != _tip) {
            _tip.parent.removeChild(_tip);
            _tip = _tipTile = null;

        }
    }

    protected function popupRollOutHandler (event :MouseEvent) :void
    {
        if (_tip != null && event.relatedObject != _tipTile) {
            _tip.parent.removeChild(_tip);
            _tip = _tipTile = null;
        }
    }

    protected var _random :Random;

    protected var _loader :Loader;

    protected var _tip :ToolTipSprite;
    protected var _tipTile :ToolTipSprite;

    protected var _hood :Neighborhood;
    protected var _canvas :Sprite;
    protected var _bound :Rectangle = new Rectangle();

    protected var _house :Building;
    protected var _group :Building;
    protected var _game :Building;

    protected var _plaqueHouse :Class;
    protected var _plaqueGroup :Class;
    protected var _plaqueGame :Class;

//    [Embed(source="Arial.ttf", fontName="hoodFont", fontWeight='Regular')]
//    protected var _font :Class;
    protected var _rule :Class;

    protected var _vacant :Class;
    protected var _roadNS :Class;
    protected var _roadEW :Class;
    protected var _road4Way :Class;
    protected var _roadNSE :Class;
    protected var _roadNSW :Class;
    protected var _roadHouse :Class;
    protected var _roadHouseEndW :Class;
    protected var _roadHouseEndE :Class;
}
}
