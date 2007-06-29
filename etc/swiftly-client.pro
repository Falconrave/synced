#
# $Id$
#
# Proguard configuration file for the Swiftly editor client

-injars ../dist/lib/commons-io.jar(!META-INF/*)
-injars ../dist/lib/samskivert.jar(
    com/samskivert/Log.class,**/io/**,**/net/**,**/swing/**,**/text/**,**/util/**,
    **/servlet/user/Password.class,**/servlet/user/User.class,**/servlet/user/UserUtil.class)
-injars ../dist/lib/narya-base.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/narya-distrib.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/nenya-rsrc.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-whirled.jar(**/ClusteredBodyObject.class,**/ScenedBodyObject.class)
-injars ../dist/lib/vilya-micasa.jar(**/util/**,**/client/**)
-injars ../dist/lib/vilya-parlor.jar(**/parlor/util/**)
-injars ../dist/lib/vilya-stats.jar(!META-INF/*,!**/tools/**,!**/persist/**)
-injars ../dist/lib/threerings.jar(!META-INF/*,**/threerings/util/**)
-injars ../dist/lib/whirled.jar(**/WhirledOccupantInfo.class)
-injars ../dist/lib/gwt-user.jar(**/user/client/rpc/IsSerializable.class,
    **/user/client/rpc/SerializableException.class)
-injars ../dist/msoy-code.jar(
    !**/*UnitTest.class,rsrc/i18n/**,**/msoy/Log.class,**/msoy/data/**,**/msoy/client/**,
    **/msoy/world/data/WorldMemberInfo.class,**/msoy/item/data/all/**,**/msoy/web/data/**,
    **/msoy/swiftly/data/**,**/msoy/swiftly/client/**,**/msoy/swiftly/util/**,
    **/msoy/game/data/GameMemberInfo.class,**/msoy/game/data/GameSummary.class,
    **/msoy/world/data/WorldOccupantInfo.class)
-injars ../dist/msoy-media.jar(**/icons/swiftly/**)
-injars ../dist/lib/sdoc-0.5.0-beta-ooo.jar(!META-INF/*)
-injars ../dist/lib/substance-lite.jar(!META-INF/*)

-dontskipnonpubliclibraryclasses
-dontobfuscate
-outjars ../dist/swiftly-client.jar

-keep class * extends javax.swing.plaf.ComponentUI {
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent);
}

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    !static !transient <fields>;
    private void writeObject (java.io.ObjectOutputStream);
    private void readObject (java.io.ObjectInputStream);
}

-keep public class * extends com.threerings.presents.dobj.DObject {
    !static !transient <fields>;
}
-keep public class * implements com.threerings.io.Streamable {
    !static !transient <fields>;
    <init> ();
    public void readObject (com.threerings.io.ObjectInputStream);
    public void writeObject (com.threerings.io.ObjectOutputStream);
    public void readField_* (com.threerings.io.ObjectInputStream);
    public void writeField_* (com.threerings.io.ObjectOutputStream);
}

-keep public class * extends java.lang.Enum {
    *;
}

-keep public class com.threerings.msoy.swiftly.client.SwiftlyApplet {
    *;
}
