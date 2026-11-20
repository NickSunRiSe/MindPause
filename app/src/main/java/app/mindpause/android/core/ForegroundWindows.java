package app.mindpause.android.core;

import java.util.List;

/** Selects an application without mistaking an unfocused notification for a switch away. */
public final class ForegroundWindows {
    public static final int APPLICATION=1, SYSTEM=3;
    public static final class Window {
        public final int type, layer;
        public final boolean active, focused, pictureInPicture;
        public Window(int type,int layer,boolean active,boolean focused,boolean pictureInPicture) {
            this.type=type;this.layer=layer;this.active=active;this.focused=focused;this.pictureInPicture=pictureInPicture;
        }
    }
    public static int select(List<Window> windows) {
        int active=-1,focused=-1;boolean systemActive=false;
        for(int i=0;i<windows.size();i++) {
            Window w=windows.get(i);
            if(w.type==SYSTEM) {
                // An expanded shade or system dialog owns input focus. A heads-up
                // banner can be active while the underlying app keeps that focus.
                if(w.focused)return -1;
                systemActive|=w.active;
            }
            if(w.type!=APPLICATION || w.pictureInPicture)continue;
            if(w.active && (active<0 || w.layer>windows.get(active).layer))active=i;
            if(w.focused && (focused<0 || w.layer>windows.get(focused).layer))focused=i;
        }
        if(systemActive)return focused;
        return active>=0?active:focused;
    }
    private ForegroundWindows(){}
}
