//
// $Id$

package com.threerings.msoy.game.xml;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;

import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.gwt.GameCode;

import com.threerings.parlor.game.data.GameConfig;

import com.whirled.game.data.GameDefinition;
import com.whirled.game.data.TableMatchConfig;
import com.whirled.game.xml.WhirledGameParser;

/**
 * Parses game definitions into instances of {@link MsoyGameDefinition}.
 */
public class MsoyGameParser extends WhirledGameParser
{
    public MsoyGameParser ()
    {
        _digester.addRule("game/lwjgl", new Rule() {
            public void begin (String namespace, String name, Attributes attrs)
                throws Exception {
                ((MsoyGameDefinition)digester.peek()).lwjgl = true;
            }
        });
        _digester.addRule("game/agentmponly", new Rule() {
            public void begin (String namespace, String name, Attributes attrs)
                throws Exception {
                ((MsoyGameDefinition)digester.peek()).isAgentMPOnly = true;
            }
        });
        _digester.addRule("game/roomless", new Rule() {
            public void begin (String namespace, String name, Attributes attrs)
                throws Exception {
                ((MsoyGameDefinition)digester.peek()).roomless = true;
            }
        });
    }

    /**
     * Parses a game definition from the supplied {@link GameCode} object.
     *
     * @exception IOException thrown if an error occurs reading the file.
     * @exception SAXException thrown if an error occurs parsing the XML.
     */
    public GameDefinition parseGame (GameCode code)
        throws IOException, SAXException
    {
        MsoyGameDefinition gameDef = (MsoyGameDefinition)parseGame(new StringReader(code.config));
        gameDef.setMediaPath(code.clientMedia.getMediaPath());
        if (code.serverMedia != null) {
            gameDef.setServerMediaPath(code.serverMedia.getMediaPath());
            gameDef.setBureauId(String.valueOf(code.gameId));
        }
        return gameDef;
    }

    @Override // from GameParser
    protected String getGameDefinitionClass ()
    {
        return MsoyGameDefinition.class.getName();
    }

    @Override // from GameParser
    protected void addMatchParsingRules (final Digester digester, String type)
        throws Exception
    {
        int mtype = Integer.valueOf(type);
        if (mtype == GameConfig.SEATED_GAME) {
            digester.addRule("game/match/unwatchable", new Rule() {
                public void begin (String namespace, String name, Attributes attrs)
                    throws Exception {
                    ((MsoyMatchConfig)digester.peek()).unwatchable = true;
                }
            });
            digester.addRule("game/match/auto1p", new Rule() {
                public void begin (String namespace, String name, Attributes attrs)
                    throws Exception {
                    ((MsoyMatchConfig)digester.peek()).autoSingle = true;
                }
            });
        }
        super.addMatchParsingRules(digester, type);
        ((MsoyMatchConfig)digester.peek()).type = mtype;
    }

    @Override // from GameParser
    protected TableMatchConfig createMatchConfig ()
    {
        return new MsoyMatchConfig();
    }
}
